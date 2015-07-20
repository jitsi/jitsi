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
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionalities for the Zeroconf protocol.
 *
 * @author Christian Vincenot
 *
 */
public class OperationSetBasicInstantMessagingZeroconfImpl
    extends AbstractOperationSetBasicInstantMessaging
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingZeroconfImpl.class);

    /**
     * The currently valid persistent presence operation set..
     */
    private final OperationSetPersistentPresenceZeroconfImpl opSetPersPresence;

    /**
     * The protocol provider that created us.
     */
    private final ProtocolProviderServiceZeroconfImpl parentProvider;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>OperationSetPersistentPresenceZeroconfImpl</tt> instance.
     */
    public OperationSetBasicInstantMessagingZeroconfImpl(
                ProtocolProviderServiceZeroconfImpl        provider,
                OperationSetPersistentPresenceZeroconfImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageZeroconfImpl(content, encoding, contentType,
            MessageZeroconfImpl.MESSAGE);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying Zeroconf stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     */
    public void sendInstantMessage(Contact to, Message message) throws
        IllegalStateException, IllegalArgumentException
    {
        if( !(to instanceof ContactZeroconfImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Zeroconf contact."
               + to);

        MessageZeroconfImpl msg =
            (MessageZeroconfImpl)createMessage(message.getContent());

        deliverMessage(msg, (ContactZeroconfImpl)to);
    }

    /**
     * In case the to the <tt>to</tt> Contact corresponds to another zeroconf
     * protocol provider registered with SIP Communicator, we deliver
     * the message to them, in case the <tt>to</tt> Contact represents us, we
     * fire a <tt>MessageReceivedEvent</tt>, and if <tt>to</tt> is simply
     * a contact in our contact list, then we simply echo the message.
     *
     * @param message the <tt>Message</tt> the message to deliver.
     * @param to the <tt>Contact</tt> that we should deliver the message to.
     */
    private void deliverMessage(Message message, ContactZeroconfImpl to)
    {
            ClientThread thread = to.getClientThread();
            try
            {
                if (thread == null)
                {
                    Socket sock;
                    if (logger.isDebugEnabled())
                        logger.debug("ZEROCONF: Creating a chat connexion to "
                            +to.getIpAddress()+":"+to.getPort());
                    sock = new Socket(to.getIpAddress(), to.getPort());
                    thread = new ClientThread(sock, to.getBonjourService());
                    thread.setStreamOpen();
                    thread.setContact(to);
                    to.setClientThread(thread);
                    thread.sendHello();
                    if (to.getClientType() == ContactZeroconfImpl.GAIM)
                    {
                        try
                        {
                            Thread.sleep(300);
                        }
                        catch (InterruptedException ex)
                        {
                            logger.error(ex);
                        }
                    }
                }

                //System.out.println("ZEROCONF: Message content => "+
                //message.getContent());
                thread.sendMessage((MessageZeroconfImpl) message);

                fireMessageDelivered(message, to);
            }
            catch (IOException ex)
            {
                logger.error(ex);
            }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     *
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    @Override
    public void fireMessageReceived(Message message, Contact from)
    {
        super.fireMessageReceived(message, from);
    }

    /**
     * Determines whether the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }

    /**
     * Determines whether the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        return contentType.equals(DEFAULT_MIME_TYPE);
    }
}
