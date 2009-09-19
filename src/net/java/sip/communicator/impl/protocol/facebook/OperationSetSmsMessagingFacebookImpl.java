/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

public class OperationSetSmsMessagingFacebookImpl
    implements OperationSetSmsMessaging
{

    private static final Logger logger =
        Logger.getLogger(OperationSetBasicInstantMessagingFacebookImpl.class);

    /**
     * Currently registered message listeners.
     */
    private final List<MessageListener> messageListeners
        = new Vector<MessageListener>();

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceFacebookImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceFacebookImpl parentProvider = null;

    public OperationSetSmsMessagingFacebookImpl(
        ProtocolProviderServiceFacebookImpl provider,
        OperationSetPersistentPresenceFacebookImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }
    
    public void addMessageListener(MessageListener listener)
    {
        if (!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    public Message createMessage(byte[] content, String contentType,
        String contentEncoding)
    {
        return new MessageFacebookImpl(new String(content), contentType,
            contentEncoding, "A message sent over SIP Communicator");
    }

    public Message createMessage(String messageText)
    {
        return new MessageFacebookImpl(messageText, DEFAULT_MIME_TYPE,
            DEFAULT_MIME_ENCODING, "A message sent over SIP Communicator");
    }

    public boolean isContentTypeSupported(String contentType)
    {
        if (contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
            return false;
    }

    public void removeMessageListener(MessageListener listener)
    {
        messageListeners.remove(listener);
    }

    /**
     * send as a message
     * 
     */
    public void sendSmsMessage(final Contact to, final Message message)
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
                    OperationSetSmsMessagingFacebookImpl.this.parentProvider
                        .getAdapter().postMessage(message, to);
                if (errorEvent == null)
                {
                    fireMessageDelivered(message, to);
                    return;
                }
                // if we got the message: 
                // Error(1357001): Not Logged In; You must be logged in to do that.
                // we just unregister.
                // and the reason we got this message may be "multiple logins"
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
                    fireMessageDeliveryFailed(message, to, errorEvent);
                    return;
                }

                // if the above delivery failed, we try again!
                // coding like this is so ugly! God, forgive me!
                try
                {
                    // wait a moment
                    Thread.sleep(1000);
                    errorEvent =
                        OperationSetSmsMessagingFacebookImpl.this.parentProvider
                            .getAdapter().postMessage(message, to);
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
                fireMessageDeliveryFailed(message, to, errorEvent);
                // now do message delivery.----what's this?
                // in case that the "to" we deliver this message to is
                // ourselves?
                // deliverMessageToMyself(message, (ContactFacebookImpl)to);
            }
        });
        sender.start();
    }

    public void sendSmsMessage(String to, Message message)
        throws IllegalStateException,
        IllegalArgumentException
    {
        Map<String, OperationSet> supportedOperationSets =
            parentProvider.getSupportedOperationSets();

        if (supportedOperationSets == null || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        OperationSetPersistentPresenceFacebookImpl operationSetPP =
            (OperationSetPersistentPresenceFacebookImpl) supportedOperationSets
                .get(OperationSetPersistentPresence.class.getName());

        Contact toContact = operationSetPP.findContactByID(to);
        sendSmsMessage(toContact, message);
    }
    
    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     * 
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered
     *            to.
     */
    private void fireMessageDelivered(Message message, Contact to)
    {
        // we succeeded in sending a message to contact "to",
        // so we know he is online
        // but -- if we support the "invisible" status,
        // this line could be commented
        opSetPersPresence.setPresenceStatusForContact((ContactFacebookImpl) to,
            FacebookStatusEnum.ONLINE);

        MessageDeliveredEvent evt =
            new MessageDeliveredEvent(message, to);

        Iterable<MessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners = new ArrayList<MessageListener>(messageListeners);
        }

        for (MessageListener listener : listeners)
            listener.messageDelivered(evt);
    }

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     * 
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered
     *            to.
     */
    private void fireMessageDeliveryFailed(Message message, Contact to,
        MessageDeliveryFailedEvent event)
    {
        Iterable<MessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners = new ArrayList<MessageListener>(messageListeners);
        }

        for (MessageListener listener : listeners)
            listener.messageDeliveryFailed(event);
    }
}
