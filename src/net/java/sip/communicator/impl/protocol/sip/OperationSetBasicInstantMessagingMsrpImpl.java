/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Basic IM with an MSRP flavour.
 * @author Tom Uijldert
 */
public class OperationSetBasicInstantMessagingMsrpImpl
    extends OperationSetBasicInstantMessagingSipImpl
{
    /** We need SIP signalling to establish a chat-session  */
    private OperationSetBasicTelephonySipImpl opsetTelephony;

    /** List of active chat sessions. */
    private List<CallPeerMsrpImpl> sessionList;

    /**
     * @param provider
     */
    public OperationSetBasicInstantMessagingMsrpImpl(
        ProtocolProviderServiceSipImpl provider)
    {
        super(provider);
        this.opsetTelephony = (OperationSetBasicTelephonySipImpl)
            provider.getOperationSet(OperationSetBasicTelephony.class);
        sessionList =
            Collections.synchronizedList(new ArrayList<CallPeerMsrpImpl>());
    }

    /* (non-Javadoc)
     * @see OperationSetBasicInstantMessagingSipImpl#sendInstantMessage(
     *  net.java.sip.communicator.service.protocol.Contact,
     *  net.java.sip.communicator.service.protocol.Message)
     */
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        CallPeerMsrpImpl session = getSession(to);
        if (session == null)
        {
            try
            {
                session = createConversation(to);
            }
            catch (OperationFailedException e)
            {
                fireMessageDeliveryFailed(message, to,
                    MessageDeliveryFailedEvent.NETWORK_FAILURE);
                return;
            }
        }
        session.sendMessage(message);
    }

    /**
     * For use by peer handler to signal message delivery
     * @param to    who did we send it to
     * @param message   what we delivered
     */
    protected void messageDelivered(Contact to, Message message)
    {
        MessageDeliveredEvent event = new MessageDeliveredEvent(message, to);
        fireMessageEvent(event);
    }

    /**
     * For use by peer handler to signal reception of a new message.
     * @param from  who sent it.
     * @param message what was received
     */
    protected void messageReceived(Contact from, Message message)
    {
        MessageReceivedEvent event = new MessageReceivedEvent(
                        message, from, new Date());
        fireMessageEvent(event);
    }

    /* (non-Javadoc)
     * @see OperationSetBasicInstantMessagingSipImpl#isContentTypeSupported(
     *  java.lang.String)
     */
    public boolean isContentTypeSupported(String contentType)
    {
        // TODO: expand on content type handling...
        if(contentType.equals(DEFAULT_MIME_TYPE)
            || contentType.equals(HTML_MIME_TYPE)
            || contentType.equals(javax.net.msrp.wrap.cpim.Message.WRAP_TYPE))
            return true;
        else
           return false;
    }

    public boolean isContentTypeSupported(String contentType, Contact contact)
    {
        return isContentTypeSupported(contentType);
    }

    /**
     * Find an active chat session with this contact.
     * @param contact   peer contact
     * @return  the peer session, <tt>null</tt> when not found.
     */
    private CallPeerMsrpImpl getSession(Contact contact)
    {
        synchronized(sessionList)
        {
            for (CallPeerMsrpImpl peer : sessionList)
                if (peer.getContact().equals(contact))
                    return peer;
        }
        return null;
    }

    /**
     * No call/chat session with this contact yet, create one.
     * @param contact the peer to call for chat
     * @return  the peer session
     * @throws OperationFailedException
     */
    private CallPeerMsrpImpl createConversation(Contact contact)
        throws OperationFailedException
    {
        CallPeerMsrpImpl peer = (CallPeerMsrpImpl)
            opsetTelephony.createSession(contact).getCallPeers().next();
        sessionList.add(peer);
        return peer;
    }

    /**
     * A peer contact initiated a chat, add the call to my list
     * and yank presence.
     * @param peer  the peer session
     */
    protected void addSession(CallPeerMsrpImpl peer)
    {
        String id = peer.getAddress();
        Contact contact = getOpSetPersPresence().resolveContactID(id);
        if (contact == null)
        {
            contact = getOpSetPersPresence().createVolatileContact(
                        id, peer.getDisplayName());
        }
        sessionList.add(peer);
    }
}
