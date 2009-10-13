/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionality for the Facebook protocol.
 * 
 * @author Dai Zhiwei
 * @author Lubomir Marinov
 */
public class OperationSetBasicInstantMessagingFacebookImpl
    extends AbstractOperationSetBasicInstantMessaging
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingFacebookImpl.class);

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceFacebookImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceFacebookImpl parentProvider = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     * 
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     *            <tt>OperationSetPersistentPresenceFacebookImpl</tt>
     *            instance.
     */
    public OperationSetBasicInstantMessagingFacebookImpl(
        ProtocolProviderServiceFacebookImpl provider,
        OperationSetPersistentPresenceFacebookImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    /*
     * Implements
     * AbstractOperationSetBasicInstantMessaging#createMessage(String, String,
     * String, String). Creates a new MessageFacebookImpl instance with the
     * specified properties.
     */
    public Message createMessage(
        String content, String contentType, String encoding, String subject)
    {
        return
            new MessageFacebookImpl(content, contentType, encoding, subject);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * 
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *             registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *             belonging to the underlying implementation.
     */
    public void sendInstantMessage(final Contact to, final Message message)
        throws IllegalStateException,
        IllegalArgumentException
    {
        if (!(to instanceof ContactFacebookImpl))
            throw new IllegalArgumentException(
                "The specified contact is not a Facebook contact." + to);

        Thread sender = new Thread(new Runnable()
        {

            public void run()
            {
                // deliver the facebook chat Message
                MessageDeliveryFailedEvent errorEvent = null;
                errorEvent =
                    OperationSetBasicInstantMessagingFacebookImpl.this.parentProvider
                        .getAdapter().postFacebookChatMessage(message, to);
                if (errorEvent == null)
                {
                    fireMessageDelivered(message, to);
                    return;
                }
                if(errorEvent.getErrorCode() == FacebookErrorException.kError_Async_NotLoggedIn
                    || errorEvent.getErrorCode() == FacebookErrorException.kError_Async_LoginChanged)
                {
                    try 
                    {
                        parentProvider.unregister(RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS);
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error(
                            "Unable to unregister the protocol provider: "
                            + this
                            + " due to the following exception: " + e1);
                    }
                    fireMessageDeliveryFailed(
                        message,
                        to,
                        MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED);
                    return;
                }

                // if the above delivery failed, we try again!
                try
                {
                    // wait a moment
                    Thread.sleep(1000);
                    errorEvent =
                        OperationSetBasicInstantMessagingFacebookImpl.this.parentProvider
                            .getAdapter().postFacebookChatMessage(message, to);
                    if (errorEvent == null)
                    {
                        fireMessageDelivered(message, to);
                        return;
                    }
                    if(errorEvent.getErrorCode() == FacebookErrorException.kError_Async_NotLoggedIn
                        || errorEvent.getErrorCode() == FacebookErrorException.kError_Async_LoginChanged)
                    {
                        try 
                        {
                            parentProvider.unregister(RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS);
                        }
                        catch (OperationFailedException e1)
                        {
                            logger.error(
                                "Unable to unregister the protocol provider: "
                                + this
                                + " due to the following exception: " + e1);
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    logger.warn(e.getMessage());
                }
                // if we get here, we have failed to deliver this message,
                // twice.
                fireMessageDeliveryFailed(
                    message,
                    to,
                    MessageDeliveryFailedEvent.UNKNOWN_ERROR);
                // now do message delivery.----what's this?
                // in case that the "to" we deliver this message to is
                // ourselves?
                // deliverMessageToMyself(message, (ContactFacebookImpl)to);
            }
        });
        sender.start();
    }

    /**
     * Invoked by the facebook adapter when we got messages from the server.
     * 
     * @param fbmsg the received Facebook instant message
     */
    public void receivedInstantMessage(FacebookMessage fbmsg)
    {
        Message message = this.createMessage(fbmsg.getText());
        String fromID = fbmsg.getFrom();

        // TODO handle the msgID.
        // it's generated when we createMessage() by now.
        // We should set the message id according to the fbmsg.msgID.
        // But it's not important.

        Contact fromContact = opSetPersPresence.findContactByID(fromID);
        if (fromContact == null)
        {
            // from facebook user who are not on our contact list
            // TODO creat volatile contact, fire event.
            fromContact = opSetPersPresence.createVolatileContact(fromID);
            // opSetPersPresence.subscribe(fromID);
        }
        fireMessageReceived(message, fromContact);
    }

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     * 
     * @param message
     *            the <tt>Message</tt> that has been delivered.
     * @param to
     *            the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    protected void fireMessageDelivered(Message message, Contact to)
    {
        // we succeeded in sending a message to contact "to",
        // so we know he is online
        // but -- if we support the "invisible" status,
        // this line could be commented
        opSetPersPresence
            .setPresenceStatusForContact(
                (ContactFacebookImpl) to,
                FacebookStatusEnum.ONLINE);

        super.fireMessageDelivered(message, to);
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     * 
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received
     *            from.
     */
    protected void fireMessageReceived(Message message, Contact from)
    {
        // we got a message from contact "from", so we know he is online
        opSetPersPresence
            .setPresenceStatusForContact(
                (ContactFacebookImpl) from,
                FacebookStatusEnum.ONLINE);

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
     *         <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return false;
    }

    /**
     * Determines wheter the protocol supports the supplied content type
     * 
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and <tt>false</tt>
     *         otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        return contentType.equals(DEFAULT_MIME_TYPE);
    }
}
