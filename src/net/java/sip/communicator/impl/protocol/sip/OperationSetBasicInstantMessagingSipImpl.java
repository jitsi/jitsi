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
public class OperationSetBasicInstantMessagingSipImpl
    implements OperationSetBasicInstantMessaging
{
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicInstantMessagingSipImpl.class);

    /**
     * A list of listeneres registered for message events.
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
     * Hashtable containing the CSeq of each discussion
     */
    private long seqN = hashCode();

    /**
     * Hashtable containing the message sent
     */
    private Hashtable sentMsg = null;

    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetBasicInstantMessagingSipImpl(
        ProtocolProviderServiceSipImpl provider)
    {
        this.sipProvider = provider;
        this.sentMsg = new Hashtable(3);
        provider.addRegistrationStateChangeListener(new
            RegistrationStateListener());

        sipProvider.registerMethodProcessor(Request.MESSAGE,
                                            new SipMessageListener());
    }

    /**
     * Registeres a MessageListener with this operation set so that it gets
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
     * Unregisteres <tt>listener</tt> so that it won't receive any further
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
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageSipImpl(new String(content), contentType
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
        return new MessageSipImpl(messageText, DEFAULT_MIME_TYPE
                                  , DEFAULT_MIME_ENCODING, null);
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
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
           return false;
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
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        if (! (to instanceof ContactSipImpl))
            throw new IllegalArgumentException(
                "The specified contact is not a Sip contact."
                + to);

        assertConnected();

        // no offline message
        if (to.getPresenceStatus().equals(SipStatusEnum.OFFLINE))
        {
            logger.debug("trying to send a message to an offline contact");
            MessageDeliveryFailedEvent evt =
                new MessageDeliveryFailedEvent(
                    message,
                    to,
                    MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED,
                    new Date());
            fireMessageEvent(evt);
            return;
        }

        // create the message
        Request mes;
        try
        {
            mes = createMessage(to, message);
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

        //Transaction
        ClientTransaction messageTransaction;
        SipProvider jainSipProvider
            = this.sipProvider.getDefaultJainSipProvider();
        try
        {
            messageTransaction = jainSipProvider.getNewClientTransaction(mes);
        }
        catch (TransactionUnavailableException ex)
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

        // send the message
        try
        {
            messageTransaction.sendRequest();
        }
        catch (SipException ex)
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

        // we register the reference to this message to retrieve it when
        // we'll receive the response message
        String key = ((CallIdHeader)mes.getHeader(CallIdHeader.NAME))
            .getCallId();

        this.sentMsg.put(key, message);
    }

    /**
     * Construct a Request which represent a new message
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @return a Message Request destinated to the contact
     * @throws OperationFailedException if an error occured during
     * the creation of the request
     */
    private Request createMessage(Contact to, Message message)
        throws OperationFailedException
    {
        // Address
        InetAddress destinationInetAddress = null;
        Address toAddress = null;
        try
        {
            toAddress = parseAddressStr(to.getAddress());

            destinationInetAddress = InetAddress.getByName(
                ( (SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                ( (SipURI) toAddress.getURI()).getHost()
                + " is not a valid internet address " + ex.getMessage());
        }
        catch (ParseException exc)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the address", exc);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the address"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }

        // Call ID
        CallIdHeader callIdHeader = this.sipProvider
            .getDefaultJainSipProvider().getNewCallId();

        //CSeq
        CSeqHeader cSeqHeader = null;


        try
        {
            cSeqHeader = this.sipProvider.getHeaderFactory()
                .createCSeqHeader(seqN++, Request.MESSAGE);
        }
        catch (InvalidArgumentException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeadder", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (ParseException exc)
        {
            //shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeadder", exc);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }

        //FromHeader and ToHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            //FromHeader
            fromHeader = this.sipProvider.getHeaderFactory()
                .createFromHeader(this.sipProvider.getOurSipAddress()
                                  , localTag);

            //ToHeader
            toHeader = this.sipProvider.getHeaderFactory()
                .createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //ViaHeaders
        ArrayList viaHeaders = this.sipProvider.getLocalViaHeaders(
            destinationInetAddress
            , this.sipProvider.getDefaultListeningPoint());

        //MaxForwards
        MaxForwardsHeader maxForwards = this.sipProvider
            .getMaxForwardsHeader();

        // Content params
        ContentTypeHeader contTypeHeader;
        ContentEncodingHeader contEncodHeader;
        ContentLengthHeader contLengthHeader;
        try
        {
            contTypeHeader = this.sipProvider.getHeaderFactory()
                .createContentTypeHeader(getType(message),
                                         getSubType(message));

            contEncodHeader = this.sipProvider.getHeaderFactory()
                .createContentEncodingHeader(message.getEncoding());

            contLengthHeader = this.sipProvider.getHeaderFactory()
                .createContentLengthHeader(message.getSize());
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the content headers", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the content headers"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (InvalidArgumentException exc)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the content length header", exc);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the content length header"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }

        Request req;
        try
        {
            req = this.sipProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI(),
                Request.MESSAGE,
                callIdHeader,
                cSeqHeader,
                fromHeader,
                toHeader,
                viaHeaders,
                maxForwards,
                contTypeHeader,
                message.getContent().getBytes());
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create message Request!", ex);
            throw new OperationFailedException(
                "Failed to create message Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        req.addHeader(contEncodHeader);
        req.addHeader(contLengthHeader);

        return req;
    }

    /**
     * Parses the the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    private Address parseAddressStr(String uriStr)
        throws ParseException
    {
        uriStr = uriStr.trim();

        //Handle default domain name (i.e. transform 1234 -> 1234@sip.com)
        //assuming that if no domain name is specified then it should be the
        //same as ours.
        if (uriStr.indexOf('@') == -1
            && !uriStr.trim().startsWith("tel:"))
        {
            uriStr = uriStr + "@"
                + ( (SipURI)this.sipProvider.getOurSipAddress().getURI())
                .getHost();
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (uriStr.toLowerCase().indexOf("sip:") == -1 //no sip scheme
            && uriStr.indexOf('@') != -1) //most probably a sip uri
        {
            uriStr = "sip:" + uriStr;
        }

        //Request URI
        Address uri
            = this.sipProvider.getAddressFactory().createAddress(uriStr);

        return uri;
    }

    /**
     * Parses the content type of a message and return the type
     *
     * @param msg the Message to scan
     * @return the type of the message
     */
    private String getType(Message msg)
    {
        String type = msg.getContentType();

        return type.substring(0, type.indexOf('/'));
    }

    /**
     * Parses the content type of a message and return the subtype
     *
     * @param msg the Message to scan
     * @return the subtype of the message
     */
    private String getSubType(Message msg)
    {
        String subtype = msg.getContentType();

        return subtype.substring(subtype.indexOf('/') + 1);
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
         * The method is called by a ProtocolProvider implementation whenver
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
     * registered message listerners.
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
     * Class for listening incoming packets.
     */
    private class SipMessageListener
        implements SipListener
    {
        public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent)
        {
            // never fired
        }

        public void processIOException(IOExceptionEvent exceptionEvent)
        {
            // never fired
        }

        public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent)
        {
            // nothing to do
        }

        /**
         *
         * @param timeoutEvent TimeoutEvent
         */
        public void processTimeout(TimeoutEvent timeoutEvent)
        {
            // this is normaly handled by the SIP stack
            logger.error("Timeout event thrown : " + timeoutEvent.toString());

            if (timeoutEvent.isServerTransaction()) {
                logger.warn("The sender has probably not received our OK");
                return;
            }

            Request req = timeoutEvent.getClientTransaction().getRequest();

            // get the content
            String content = null;
            try
            {
                content = new String(
                    req.getRawContent()
                    , req.getContentEncoding()
                        .getEncoding());
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
                return;
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
                failedMessage = (Message) sentMsg.get(key);

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
        }

        /**
         * Process a request from a distant contact
         *
         * @param requestEvent the <tt>RequestEvent</tt> containing the newly
         * received request.
         */
        public void processRequest(RequestEvent requestEvent)
        {
            // get the content
            String content = null;

            try
            {
                content = new String(
                    requestEvent.getRequest().getRawContent(),
                    requestEvent.getRequest().getContentEncoding()
                    .getEncoding());
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
                return;
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
                    newMessage, from, new Date());
            fireMessageEvent(msgReceivedEvt);
        }

        /**
         * Process a response from a distant contact.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> containing the newly
         * received SIP response.
         */
        public void processResponse(ResponseEvent responseEvent)
        {
            Request req = responseEvent.getClientTransaction().getRequest();
            int status = responseEvent.getResponse().getStatusCode();
            // content of the response
            String content = null;

            try
            {
                content = new String(
                    req.getRawContent(),
                    req.getContentEncoding()
                    .getEncoding());
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
                return;
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

                return;
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

                return;
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
            }
            else if (status == 401 || status == 407)
            {
                // proxy ask for authentification
                logger.debug(
                    "proxy asks authentication : "
                    + responseEvent.getResponse().getReasonPhrase());

                ClientTransaction clientTransaction = responseEvent
                    .getClientTransaction();
                SipProvider sourceProvider = (SipProvider)
                    responseEvent.getSource();

                try
                {
                    processAuthenticationChallenge(clientTransaction,
                        responseEvent.getResponse(),
                        sourceProvider);
                }
                catch (OperationFailedException ex)
                {
                    logger.error("can't solve the challenge", ex);
                    
                    // error for delivering the message
                    MessageDeliveryFailedEvent evt =
                        new MessageDeliveryFailedEvent(
                            newMessage,
                            to,
                            MessageDeliveryFailedEvent.NETWORK_FAILURE,
                            new Date(),
                            ex.getMessage());
                    fireMessageEvent(evt);
                    sentMsg.remove(key);
                }
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
            }
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

        /**
         * Attempts to re-generate the corresponding request with the proper
         * credentials.
         *
         * @param clientTransaction the corresponding transaction
         * @param response the challenge
         * @param jainSipProvider the provider that received the challenge
         *
         * @throws OperationFailedException if processing the authentication
         * challenge fails.
         */
        private void processAuthenticationChallenge(
            ClientTransaction clientTransaction,
            Response response,
            SipProvider jainSipProvider)
            throws OperationFailedException
        {
            try
            {
                logger.debug("Authenticating a message request.");

                ClientTransaction retryTran
                    = sipProvider.getSipSecurityManager().handleChallenge(
                        response
                        , clientTransaction
                        , jainSipProvider);

                retryTran.sendRequest();
                return;
            }
            catch (Exception exc)
            {
                logger.error("We failed to authenticate a message request.",
                             exc);

                throw new OperationFailedException("Failed to authenticate"
                    + "a message request"
                    , OperationFailedException.INTERNAL_ERROR
                    , exc);
            }
        }
    }
}

