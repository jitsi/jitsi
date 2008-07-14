/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A straightforward implementation of the basic instant messaging operation
 * set.
 *
 * @author Benoit Pradelle
 */
public class OperationSetSmsMessagingSipImpl
    implements OperationSetSmsMessaging, SipMessageProcessor
{
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicInstantMessagingSipImpl.class);

    /**
     * A list of listeners registered for message events.
     */
    private Vector messageListeners = new Vector();

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPresenceSipImpl opSetPersPresence = null;
    
    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetBasicInstantMessagingSipImpl opSetBasicIm = null;
    
    private static final String SMS_OUTGOING_MESSAGE_HEADER = "P-Send-SMS";
    private static final String[] SMS_OUTGOING_MESSAGE_HEADER_VALUES = 
        new String[]{"simple", "confirmation-request", "premium"};
    private static final String SMS_OUTGOING_DEFAULT_MESSAGE_HEADER_VALUE = 
        SMS_OUTGOING_MESSAGE_HEADER_VALUES[0];
    private static final String SMS_RECEIVED_MESSAGE_HEADER = "P-Received-SMS";
    
    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetSmsMessagingSipImpl(
        ProtocolProviderServiceSipImpl provider, 
        OperationSetBasicInstantMessagingSipImpl opSetBasicIm)
    {
        this.sipProvider = provider;

        provider.addRegistrationStateChangeListener(new
            RegistrationStateListener());
        this.opSetBasicIm = opSetBasicIm;
        opSetBasicIm.addMessageProcessor(this);
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
        synchronized (this.messageListeners)
        {
            if (!this.messageListeners.contains(listener))
            {
                this.messageListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        synchronized (this.messageListeners)
        {
            this.messageListeners.remove(listener);
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding)
    {
        return opSetBasicIm.createMessage(
            content, contentType, contentEncoding, null);
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
        return opSetBasicIm.createMessage(messageText);
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
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
           return false;
    }
    
    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the destination to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendSmsMessage(String to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        ContactSipImpl volatileContact = 
            (ContactSipImpl)opSetPersPresence.findContactByID(to);
        
        // check if contact already exist
        if(volatileContact == null)
            volatileContact = opSetPersPresence.createVolatileContact(to);
       
        sendSmsMessage(volatileContact, message);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendSmsMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        if (! (to instanceof ContactSipImpl))
            throw new IllegalArgumentException(
                "The specified contact is not a Sip contact."
                + to);

        assertConnected();

        // create the message
        Request mes;
        try
        {
            mes = opSetBasicIm.createMessage(to, message);
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to create the message."
                , ex);

            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    message,
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR,
                    new Date());
            fireMessageEvent(evt);
            return;
        }

        Header smsHeader = null;
        try
        {
            smsHeader = this.sipProvider.getHeaderFactory()
                .createHeader(SMS_OUTGOING_MESSAGE_HEADER, 
                              SMS_OUTGOING_DEFAULT_MESSAGE_HEADER_VALUE);
        }
        catch (ParseException exc)
        {
            //shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the SmsHeadder", exc);
            
            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    message,
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR,
                    new Date());
            fireMessageEvent(evt);
            return;
        }
    
        mes.addHeader(smsHeader);
          
        try
        {
            opSetBasicIm.sendRequestMessage(mes, to, message);
        }
        catch(TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create messageTransaction.\n"
                + "This is most probably a network connection error."
                , ex);

            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    message,
                    to,
                    MessageDeliveryFailedEvent.NETWORK_FAILURE,
                    new Date());
            fireMessageEvent(evt);
            return;
        }
        catch(SipException ex)
        {
            logger.error(
                "Failed to send the message."
                , ex);

            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    message,
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR,
                    new Date());
            fireMessageEvent(evt);
            return;
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws IllegalStateException
    {
        if (this.sipProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                + "service before being able to communicate.");
        if (!this.sipProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                + "being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                 opSetPersPresence = (OperationSetPresenceSipImpl)
                    sipProvider.getSupportedOperationSets()
                    .get(OperationSetPersistentPresence.class.getName());
            }
        }
    }

    /**
     * Delivers the specified event to all registered message listeners.
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     * registered message listeners.
     */
    private void fireMessageEvent(EventObject evt)
    {
        Iterator listeners = null;
        synchronized (this.messageListeners)
        {
            listeners = new ArrayList(this.messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            if (evt instanceof MessageDeliveredEvent)
            {
                listener.messageDelivered( (MessageDeliveredEvent) evt);
            }
            else if (evt instanceof MessageReceivedEvent)
            {
                listener.messageReceived( (MessageReceivedEvent) evt);
            }
            else if (evt instanceof MessageDeliveryFailedEvent)
            {
                listener.messageDeliveryFailed(
                    (MessageDeliveryFailedEvent) evt);
            }
        }
    }
    
    /**
     * Process the incoming sip messages
     * @param requestEvent the incoming event holding the message
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processMessage(RequestEvent requestEvent)
    {
        // get the content
        String content = null;

        try
        {
            Request req = requestEvent.getRequest();

            // ignore messages which are not sms and continue processing
            if(req.getHeader(SMS_RECEIVED_MESSAGE_HEADER) == null)
                return true;

            content = new String(req.getRawContent(), getCharset(req));
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.debug("failed to convert the message charset");
            content = new String(requestEvent.getRequest().getRawContent());
        }

        // who sent this request ?
        FromHeader fromHeader = (FromHeader)
            requestEvent.getRequest().getHeader(FromHeader.NAME);

        if (fromHeader == null)
        {
            logger.error("received a request without a from header");
            // no further processing
            return false;
        }

        Contact from = resolveContact(
            fromHeader.getAddress().getURI().toString());
        Message newMessage = createMessage(content);

        if (from == null) {
            logger.debug("received a message from an unknown contact: "
                         + fromHeader.getAddress().getURI().toString());
            //create the volatile contact
            from = opSetPersPresence
                .createVolatileContact(fromHeader.getAddress().getURI()
                                       .toString().substring(4));
        }

        // answer ok
        try
        {
            Response ok = sipProvider.getMessageFactory()
                .createResponse(Response.OK, requestEvent.getRequest());
            SipProvider jainSipProvider = (SipProvider) requestEvent.
                getSource();
            jainSipProvider.getNewServerTransaction(
                requestEvent.getRequest()).sendResponse(ok);
        }
        catch (ParseException exc)
        {
            logger.error("failed to build the response", exc);
        }
        catch (SipException exc)
        {
            logger.error("failed to send the response : "
                         + exc.getMessage(),
                         exc);
        }
        catch (InvalidArgumentException exc)
        {
            logger.debug("Invalid argument for createResponse : "
                         + exc.getMessage(),
                         exc);
        }

        // fire an event
        MessageReceivedEvent msgReceivedEvt
            = new MessageReceivedEvent(
                newMessage, from, new Date(),                                                                                                                                       
                    MessageReceivedEvent.SMS_MESSAGE_RECEIVED);
        fireMessageEvent(msgReceivedEvt);
        
        // no further processing
        return false;
    }
    
    /**
     * Process the responses of sent messages
     * @param responseEvent the incoming event holding the response
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processResponse(ResponseEvent responseEvent, Map sentMsg)
    {
        Request req = responseEvent.getClientTransaction().getRequest();

        // ignore messages which are not sms and continue processing
        if(req.getHeader(SMS_OUTGOING_MESSAGE_HEADER) == null)
            return true;

        int status = responseEvent.getResponse().getStatusCode();
            
        // content of the response
        String content = null;

        try
        {
            content = new String(req.getRawContent(), getCharset(req));
        }
        catch (UnsupportedEncodingException exc)
        {
            logger.debug("failed to convert the message charset", exc);
            content = new String(req.getRawContent());
        }

        // to who did we send the original message ?
        ToHeader toHeader = (ToHeader)
            req.getHeader(ToHeader.NAME);

        if (toHeader == null)
        {
            // should never happen
            logger.error("send a request without a to header");
            return false;
        }

        Contact to = resolveContact(toHeader.getAddress()
                .getURI().toString());

        if (to == null) {
            logger.error(
                    "Error received a response from an unknown contact : "
                    + toHeader.getAddress().getURI().toString() + " : "
                    + responseEvent.getResponse().getReasonPhrase());

            // error for delivering the message
            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    // we don't know what message it concerns
                    createMessage(content),
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR,
                    new Date());
            fireMessageEvent(evt);

            return false;
        }

        // we retrieve the original message
        String key = ((CallIdHeader)req.getHeader(CallIdHeader.NAME))
            .getCallId();

        Message newMessage = (Message) sentMsg.get(key);

        if (newMessage == null) {
            // should never happen
            logger.error("Couldn't find the message sent");

            // error for delivering the message
            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    // we don't know what message it is
                    createMessage(content),
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR,
                    new Date());
            fireMessageEvent(evt);

            return false;
        }

        // status 401/407 = proxy authentification
        if (status >= 400 && status != 401 && status != 407)
        {
            logger.info(
                "Error received from the network : "
                + responseEvent.getResponse().getReasonPhrase());

            // error for delivering the message
            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    newMessage,
                    to,
                    MessageDeliveryFailedEvent.NETWORK_FAILURE,
                    new Date(),
                    responseEvent.getResponse().getReasonPhrase());
            fireMessageEvent(evt);
            sentMsg.remove(key);
            
            return false;
        }
        else if (status == 401 || status == 407)
        {
            return true;
        }
        else if (status >= 200)
        {
            logger.debug(
                "Ack received from the network : "
                + responseEvent.getResponse().getReasonPhrase());

            // we delivered the message
            MessageDeliveredEvent msgDeliveredEvt
                = new MessageDeliveredEvent(
                    newMessage, to, new Date());

            fireMessageEvent(msgDeliveredEvt);

            // we don't need this message anymore
            sentMsg.remove(key);
            
            return false;
        }
        
        return false;
    }
    
    public boolean processTimeout(TimeoutEvent timeoutEvent, Map sentMessages)
    {
        // this is normaly handled by the SIP stack
        logger.error("Timeout event thrown : " + timeoutEvent.toString());

        if (timeoutEvent.isServerTransaction()) 
        {
            logger.warn("The sender has probably not received our OK");
            return false;
        }

        Request req = timeoutEvent.getClientTransaction().getRequest();

        // ignore messages which are not sms and continue processing
        if(req.getHeader(SMS_OUTGOING_MESSAGE_HEADER) == null)
            return true;
        
        // get the content
        String content = null;
        try
        {
            content = new String(req.getRawContent(), getCharset(req));
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.warn("failed to convert the message charset", ex);
            content = new String(req.getRawContent());
        }

        // to who this request has been sent ?
        ToHeader toHeader = (ToHeader) req.getHeader(ToHeader.NAME);

        if (toHeader == null)
        {
            logger.error("received a request without a to header");
            return false;
        }

        Contact to = resolveContact(
                toHeader.getAddress().getURI().toString());

        Message failedMessage = null;

        if (to == null) {
            logger.error(
                    "timeout on a message sent to an unknown contact : "
                    + toHeader.getAddress().getURI().toString());

            //we don't know what message it concerns, so create a new
            //one
            failedMessage = createMessage(content);
        }
        else
        {
            // try to retrieve the original message
            String key = ((CallIdHeader)req.getHeader(CallIdHeader.NAME))
                .getCallId();
            failedMessage = (Message) sentMessages.get(key);

            if (failedMessage == null)
            {
                // should never happen
                logger.error("Couldn't find the sent message.");

                // we don't know what the message is so create a new one
                //based on the content of the failed request.
                failedMessage = createMessage(content);
            }
        }

        // error for delivering the message
        MessageDeliveryFailedEvent evt =
            new MessageDeliveryFailedEvent(
                // we don't know what message it concerns
                failedMessage,
                to,
                MessageDeliveryFailedEvent.INTERNAL_ERROR,
                new Date());
        fireMessageEvent(evt);
        
        return false;
    }
    
    /**
     * Try to find a charset in a MESSAGE request for the
     * text content. If no charset is defined, the default charset
     * for text messages is returned.
     * 
     * @param req the MESSAGE request in which to look for a charset
     * @return defined charset in the request or DEFAULT_MIME_ENCODING
     *  if no charset is specified
     */
    private String getCharset(Request req)
    {
        String charset = null;
        Header contentTypeHeader = req.getHeader(ContentTypeHeader.NAME);
        if (contentTypeHeader instanceof ContentTypeHeader)
            charset = ((ContentTypeHeader) contentTypeHeader)
                                            .getParameter("charset");
        if (charset == null)
            charset = DEFAULT_MIME_ENCODING;
        return charset;
    }

    /**
     * Try to find a contact registered using a string to identify him.
     *
     * @param contactID A string with which the contact may have
     *  been registered
     * @return A valid contact if it has been found, null otherwise
     */
    private Contact resolveContact(String contactID) {
        Contact res = opSetPersPresence.findContactByID(contactID);

        if (res == null) {
            // we try to resolve the conflict by removing "sip:" from the id
            if (contactID.startsWith("sip:")) {
                res = opSetPersPresence.findContactByID(
                        contactID.substring(4));
            }

            if (res == null) {
                // we try to remove the part after the '@'
                if (contactID.indexOf('@') > -1) {
                    res = opSetPersPresence.findContactByID(
                            contactID.substring(0,
                                contactID.indexOf('@')));

                    if (res == null) {
                        // try the same thing without sip:
                        if (contactID.startsWith("sip:")) {
                            res = opSetPersPresence.findContactByID(
                                    contactID.substring(4,
                                        contactID.indexOf('@')));
                        }
                    }
                }
            }
        }

        return res;
    }
}
