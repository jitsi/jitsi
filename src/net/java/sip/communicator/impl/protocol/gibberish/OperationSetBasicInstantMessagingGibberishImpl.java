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
package net.java.sip.communicator.impl.protocol.gibberish;

import net.java.sip.communicator.service.protocol.*;

/**
 * Instant messaging functionalites for the Gibberish protocol.
 *
 * @author Emil Ivov
 */
public class OperationSetBasicInstantMessagingGibberishImpl
    extends AbstractOperationSetBasicInstantMessaging
{

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceGibberishImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceGibberishImpl parentProvider = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>OperationSetPersistentPresenceGibberishImpl</tt> instance.
     */
    public OperationSetBasicInstantMessagingGibberishImpl(
                ProtocolProviderServiceGibberishImpl        provider,
                OperationSetPersistentPresenceGibberishImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageGibberishImpl(content, contentType, encoding, subject);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     */
    public void sendInstantMessage(Contact to, Message message) throws
        IllegalStateException, IllegalArgumentException
    {
        if( !(to instanceof ContactGibberishImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Gibberish contact."
               + to);

        //first fire an event that we've delivered the message.
        //Note that in a real world protocol implementation we would first wait
        //for a delivery confirmation coming from the protocol stack.
        fireMessageDelivered(message, to);

        //now do message delivery.
        deliverMessage(message, (ContactGibberishImpl)to);
    }

    /**
     * In case the to the <tt>to</tt> Contact corresponds to another gibberish
     * protocol provider registered with SIP Communicator, we deliver
     * the message to them, in case the <tt>to</tt> Contact represents us, we
     * fire a <tt>MessageReceivedEvent</tt>, and if <tt>to</tt> is simply
     * a contact in our contact list, then we simply echo the message.
     *
     * @param message the <tt>Message</tt> the message to deliver.
     * @param to the <tt>Contact</tt> that we should deliver the message to.
     */
    private void deliverMessage(Message message, ContactGibberishImpl to)
    {
        String userID = to.getAddress();

        //if the user id is owr own id, then this message is being routed to us
        //from another instance of the gibberish provider.
        if (userID.equals(this.parentProvider.getAccountID().getUserID()))
        {
            //check who is the provider sending the message
            String sourceUserID
                = to.getProtocolProvider().getAccountID().getUserID();

            //check whether they are in our contact list
            Contact from = opSetPersPresence.findContactByID(sourceUserID);


            //and if not - add them there as volatile.
            if(from == null)
            {
                from = opSetPersPresence.createVolatileContact(sourceUserID);
            }

            //and now fire the message received event.
            fireMessageReceived(message, from);
        }
        else
        {
            //if userID is not our own, try an check whether another provider
            //has that id and if yes - deliver the message to them.
            ProtocolProviderServiceGibberishImpl gibberishProvider
                = this.opSetPersPresence.findProviderForGibberishUserID(userID);
            if(gibberishProvider != null)
            {
                OperationSetBasicInstantMessagingGibberishImpl opSetIM
                    = (OperationSetBasicInstantMessagingGibberishImpl)
                        gibberishProvider.getOperationSet(
                            OperationSetBasicInstantMessaging.class);
                opSetIM.deliverMessage(message, to);
            }
            else
            {
                //if we got here then "to" is simply someone in our contact
                //list so let's just echo the message.
                fireMessageReceived(message, to);
            }
        }
    }

    /**
     * Determines wheter the protocol provider (or the protocol itself) support
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
     * Determines wheter the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
           return false;
    }
}
