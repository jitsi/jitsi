/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionalites for the Zeroconf protocol.
 *
 * @author Christian Vincenot
 *
 */
public class OperationSetBasicInstantMessagingZeroconfImpl
    implements OperationSetBasicInstantMessaging
{
      private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingZeroconfImpl.class);
    
    /**
     * Currently registered message listeners.
     */
    private Vector messageListeners = new Vector();

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceZeroconfImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceZeroconfImpl parentProvider = null;

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

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        if(!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageZeroconfImpl(new String(content), 
                                  contentEncoding, contentType, 
                                  MessageZeroconfImpl.MESSAGE);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MessageZeroconfImpl(messageText, 
                            DEFAULT_MIME_ENCODING, 
                            DEFAULT_MIME_TYPE,
                            MessageZeroconfImpl.MESSAGE);
    }

    /**
     * Unregisteres <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception
     * of incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        messageListeners.remove(listener);
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
        
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(
                msg, to, new Date());

        deliverMessage(msg, (ContactZeroconfImpl)to);
        
    }

    /**
     * In case the to the <tt>to</tt> Contact corresponds to another zeroconf
     * protocol provider registered with SIP Communicator, we deliver
     * the message to them, in case the <tt>to</tt> Contact represents us, we
     * fire a <tt>MessageReceivedEvent</tt>, and if <tt>to</tt> is simply
     * a contact in our contact list, then we simply echo the message.
     *
     *
     *
     *
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
                    logger.debug("ZEROCONF: Creating a chat connexion to "
                            +to.getIpAddress()+":"+to.getPort());
                    sock = new Socket(to.getIpAddress(), to.getPort());
                    thread = new ClientThread(sock, to.getBonjourService());
                    thread.setStreamOpen();
                    thread.setContact(to);
                    to.setClientThread(thread);
                    thread.sendHello();
                    if (to.getClientType() == to.GAIM) 
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
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    private void fireMessageDelivered(Message message, Contact to)
    {
        MessageDeliveredEvent evt
            = new MessageDeliveredEvent(message, to, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageDelivered(evt);
        }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     *
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    public void fireMessageReceived(Message message, Contact from)
    {
       
        MessageReceivedEvent evt
            = new MessageReceivedEvent(message, from, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageReceived(evt);
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
}
