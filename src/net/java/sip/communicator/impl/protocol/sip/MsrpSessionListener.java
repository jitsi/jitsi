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

import javax.net.msrp.*;
import javax.net.msrp.events.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.Logger;

/**
 * Catch events from MSRP stack and pass on to peer handler.
 * @author tuijldert
 */
class MsrpSessionListener implements SessionListener
{
    private static final Logger logger =
        Logger.getLogger(MsrpSessionListener.class);

    /** the peer handler */
	private CallPeerMsrpImpl peer;

    private OperationSetBasicInstantMessagingMsrpImpl opsetBasicIM;

        public MsrpSessionListener(CallPeerMsrpImpl peer)
	{
		this.peer = peer;
		this.opsetBasicIM = peer.getOpsetBasicIM();

	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#acceptHook(
	 *         javax.net.msrp.Session, javax.net.msrp.IncomingMessage)
	 */
	public boolean acceptHook(Session session, IncomingMessage message)
	{
        try
        {
            if (peer.isFileTransfer())
                message.setDataContainer(
                            peer.getTransferActivity().getDataContainer());
            else
            {
                if (message.getSize() > SdpUtils.MAX_SIZE)  // too big.
                    return false;
                message.setDataContainer(
                    new MemoryDataContainer((int) message.getSize()));
            }
            return true;
        }
        catch (Exception e)
        {
            logger.warn("Error assigning data container: ", e);
        }
        return false;
	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#receivedMessage(
	 *         javax.net.msrp.Session, javax.net.msrp.IncomingMessage)
	 */
	public void receivedMessage(Session session, IncomingMessage message)
	{
	    if (message instanceof IncomingAliveMessage)
	        return;
	    Contact from = peer.getContact();

	    if (message.isWrapped())
	    {
	        /*
	         * TODO: possible MUC - relate sender to room-participant. Not now.
	         * 
	        WrappedMessage msg = message.getWrappedMessage();
	        String sender = msg.getHeader(Headers.FROM);
	        for (CallPeer member : 
	            peer.getPeer().getCall().getConference().getCallPeers())
	        {
	            if (member.getContact().getAddress().toString().equals(sender))
	            {
	                from = member.getContact();
	                break;
	            }
	        }
	        */
	    }
	    if (peer.isFileTransfer())
	    {
	        peer.getTransferActivity().completed();
	        peer.close();
	    }
	    else
	    {
	        String content = message.getSize() == 0 ? "" : message.getContent();

	        net.java.sip.communicator.service.protocol.Message msg = 
	                                    opsetBasicIM.createMessage(content);

	        opsetBasicIM.messageReceived(from, msg);
	    }
    }

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#receivedReport(
	 *         javax.net.msrp.Session, javax.net.msrp.Transaction)
	 */
	public void receivedReport(Session session, Transaction report)
	{
	    // TODO
 	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#abortedMessageEvent(
	 *         javax.net.msrp.events.MessageAbortedEvent)
	 */
	public void abortedMessageEvent(MessageAbortedEvent abortEvent)
	{
        // TODO
	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#updateSendStatus(
	 *             javax.net.msrp.Session, javax.net.msrp.Message, long)
	 */
	public void updateSendStatus(Session session, Message message,
	                            long numberBytesSent)
	{
		// TODO
	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#connectionLost(
	 *         javax.net.msrp.Session, java.lang.Throwable)
	 */
	public void connectionLost(Session session, Throwable cause)
	{
        // TODO
	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#receivedNickname(
	 *         javax.net.msrp.Session, javax.net.msrp.Transaction)
	 */
	public void receivedNickname(Session session, Transaction request)
	{
	    /*
	     * TODO: For MUC (conferencing). Not now.
	    ContactSipImpl contact = (ContactSipImpl) peer.getPeer().getContact();
	    if (contact != null)
	    {
	        contact.setDisplayName(request.getNickname());
	    }
         */
	}

	/* (non-Javadoc)
	 * @see javax.net.msrp.SessionListener#receivedNickNameResult(
	 *         javax.net.msrp.Session, javax.net.msrp.TransactionResponse)
	 */
	public void receivedNickNameResult(Session session,
                                    TransactionResponse result)
	{
		// TODO 
	}
}
