/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * OperationSetBasicInstantMessagingSSHImpl.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import java.io.*;
import java.util.*;
import net.java.sip.communicator.util.Logger;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Instant messaging functionalites for the SSH protocol.
 *
 * @author Shobhit Jindal
 */
public class OperationSetBasicInstantMessagingSSHImpl
        implements OperationSetBasicInstantMessaging
{
    private static final Logger logger
            = Logger.getLogger(OperationSetBasicInstantMessagingSSHImpl.class);
    
    /**
     * Currently registered message listeners.
     */
    private Vector messageListeners = new Vector();
    
    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceSSHImpl opSetPersPresence = null;
    
    /**
     * The currently valid file transfer operation set
     */
    private OperationSetFileTransferSSHImpl fileTransfer;
    
    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSSHImpl parentProvider = null;
    
    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     */
    public OperationSetBasicInstantMessagingSSHImpl(
            ProtocolProviderServiceSSHImpl        provider)
    {
        this.parentProvider = provider;
        
        this.opSetPersPresence = (OperationSetPersistentPresenceSSHImpl)
                provider.getOperationSet(OperationSetPersistentPresence.class);
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
    public Message createMessage(
            byte[] content,
            String contentType,
            String contentEncoding,
            String subject)
    {
        return new MessageSSHImpl(new String(content), contentType
                , contentEncoding, subject);
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
        return new MessageSSHImpl(messageText, DEFAULT_MIME_TYPE
                , DEFAULT_MIME_ENCODING, null);
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
     * <tt>to</tt> contact. An attempt is made to re-establish the shell
     * connection if the current one is invalid.
     * The reply from server is sent by a seperate reader thread
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     */
    public void sendInstantMessage(
            Contact to,
            Message message)
            throws IllegalStateException,
            IllegalArgumentException
    {
        if( !(to instanceof ContactSSHImpl) )
            throw new IllegalArgumentException(
                    "The specified contact is not a SSH contact."
                    + to);
        
        ContactSSH sshContact = (ContactSSH)to;
        
        // making sure no messages are sent and no new threads are triggered,
        // until a thread trying to connect to remote server returns
        if(sshContact.isConnectionInProgress())
        {
            deliverMessage(
                    createMessage("A connection attempt is in progress"),
                    (ContactSSHImpl)to);
            return;
        }
        
        if( !parentProvider.isShellConnected(sshContact) )
        {
            
            try
            {
                /**
                 * creating a new SSH session / shell channel
                 * - first message
                 * - session is timed out
                 * - network problems
                 */
                parentProvider.connectShell(sshContact, message);
                
                //the first message is ignored
                return;
            }
            catch (Exception ex)
            {
                throw new IllegalStateException(ex.getMessage());
            }
        }
        
        if(wrappedMessage(message.getContent(), sshContact))
        {
            fireMessageDelivered(message, to);
            return;
        }
        
        try
        {
            sshContact.sendLine(message.getContent());
            sshContact.setCommandSent(true);
        }
        catch (IOException ex)
        {
            // Closing IO Streams
            sshContact.closeShellIO();
            
            throw new IllegalStateException(ex.getMessage());
        }
        
        fireMessageDelivered(message, to);
    }
    
    /**
     * Check the message for wrapped Commands
     * All commands begin with /
     *
     * @param message from user
     * @param sshContact of the remote machine
     *
     * @return true if the message had commands, false otherwise
     */
    private boolean wrappedMessage(
            String message,
            ContactSSH sshContact)
    {
        
        if(message.startsWith("/upload"))
        {
            int firstSpace = message.indexOf(' ');
            sshContact.getFileTransferOperationSet().sendFile(
                    sshContact,
                    null,
                    message.substring(message.indexOf(' ', firstSpace+1) + 1),
                    message.substring(
                        firstSpace+1, 
                        message.indexOf(' ', firstSpace+1)));
            
            return true;
        }
        else if(message.startsWith("/download"))
        {
            int firstSpace = message.indexOf(' ');
            sshContact.getFileTransferOperationSet().sendFile(
                    null,
                    sshContact,
                    message.substring(firstSpace+1, message.indexOf(' ', 
                                                                firstSpace+1)),
                    message.substring(message.indexOf(' ', firstSpace+1) + 1));
            
            return true;
        }
        
        return false;
    }
    
    /**
     * In case the <tt>to</tt> Contact corresponds to another ssh
     * protocol provider registered with SIP Communicator, we deliver
     * the message to them, in case the <tt>to</tt> Contact represents us, we
     * fire a <tt>MessageReceivedEvent</tt>, and if <tt>to</tt> is simply
     * a contact in our contact list, then we simply echo the message.
     *
     * @param message the <tt>Message</tt> the message to deliver.
     * @param to the <tt>Contact</tt> that we should deliver the message to.
     */
    void deliverMessage(
            Message message,
            ContactSSH to)
    {
        String userID = to.getAddress();
        
        //if the user id is owr own id, then this message is being routed to us
        //from another instance of the ssh provider.
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
            ProtocolProviderServiceSSHImpl sshProvider
                    = this.opSetPersPresence.findProviderForSSHUserID(userID);
            if(sshProvider != null)
            {
                OperationSetBasicInstantMessagingSSHImpl opSetIM
                        = (OperationSetBasicInstantMessagingSSHImpl)
                        sshProvider.getOperationSet(
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
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    private void fireMessageDelivered(
            Message message,
            Contact to)
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
    private void fireMessageReceived(
            Message message,
            Contact from)
    {
        MessageReceivedEvent evt
                = new MessageReceivedEvent(
                message,
                from,
                new Date(),
                ((ContactSSH)from).getMessageType());
        
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
     * Determines wheter the SSH protocol provider supports
     * sending and receiving offline messages.
     *
     * @return <tt>false</tt>
     */
    public boolean isOfflineMessagingSupported()
    {
        return false;
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
        return MessageSSHImpl.contentType.equals(contentType);
    }
    
}
