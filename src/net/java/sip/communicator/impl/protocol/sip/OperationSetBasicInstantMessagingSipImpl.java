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

import java.io.*;
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
    extends AbstractOperationSetBasicInstantMessaging
{
    /**
     * Our class logger.
     */
    private static final Logger logger
    = Logger.getLogger(OperationSetBasicInstantMessagingSipImpl.class);

    /**
     * A list of processors registered for incoming sip messages.
     */
    private final List<SipMessageProcessor> messageProcessors
        = new Vector<SipMessageProcessor>();

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceSipImpl sipProvider;

    /**
     * Registration listener instance.
     */
    private final RegistrationStateListener registrationListener;

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
    private final Map<String, Message> sentMsg
        = new Hashtable<String, Message>(3);

    /**
     * It can be implemented in some servers.
     */
    private final boolean offlineMessageSupported;

    /**
     * Gives access to presence states for the Sip protocol.
     */
    private final SipStatusEnum sipStatusEnum;

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

        registrationListener = new RegistrationStateListener();
        provider.addRegistrationStateChangeListener(
            registrationListener);

        offlineMessageSupported =
            provider.getAccountID().getAccountPropertyBoolean(
                "OFFLINE_MSG_SUPPORTED", false);

        sipProvider.registerMethodProcessor(Request.MESSAGE,
            new BasicInstantMessagingMethodProcessor());

        this.sipStatusEnum = sipProvider.getSipStatusEnum();
    }

    /**
     * Registers a SipMessageProcessor with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param processor the <tt>SipMessageProcessor</tt> to register.
     */
    void addMessageProcessor(SipMessageProcessor processor)
    {
        synchronized (this.messageProcessors)
        {
            if (!this.messageProcessors.contains(processor))
            {
                this.messageProcessors.add(processor);
            }
        }
    }

    /**
     * Unregisters <tt>processor</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param processor the <tt>SipMessageProcessor</tt> to unregister.
     */
    void removeMessageProcessor(SipMessageProcessor processor)
    {
        synchronized (this.messageProcessors)
        {
            this.messageProcessors.remove(processor);
        }
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageSipImpl(content, contentType, encoding, subject);
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
        return offlineMessageSupported;
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
        if(contentType.equals(DEFAULT_MIME_TYPE)
            || contentType.equals(HTML_MIME_TYPE))
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

        // offline message
        if (to.getPresenceStatus().equals(
                sipStatusEnum.getStatus(SipStatusEnum.OFFLINE))
            && !offlineMessageSupported)
        {
            if (logger.isDebugEnabled())
                logger.debug("trying to send a message to an offline contact");
            fireMessageDeliveryFailed(
                message,
                to,
                MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED);
            return;
        }

        // create the message
        Message[] transformedMessages = transformSIPMessage(to, message);
        for (Message msg : transformedMessages)
        {
            Request mes;
            try
            {
                mes = createMessageRequest(to, msg);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to create the message.", ex);

                fireMessageDeliveryFailed(message, to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR);
                continue;
            }

            try
            {
                sendMessageRequest(mes, to, message);
            }
            catch (TransactionUnavailableException ex)
            {
                logger.error("Failed to create messageTransaction.\n"
                    + "This is most probably a network connection error.", ex);

                fireMessageDeliveryFailed(message, to,
                    MessageDeliveryFailedEvent.NETWORK_FAILURE);
                continue;
            }
            catch (SipException ex)
            {
                logger.error("Failed to send the message.", ex);

                fireMessageDeliveryFailed(message, to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR);
                continue;
            }
        }
    }

    /**
     * Sends <tt>messageRequest</tt> to the specified destination and logs
     * <tt>messageContent</tt> for later use.
     *
     * @param messageRequest the <tt>SipRequest</tt> that we are about to send.
     * @param to the Contact that we are sending <tt>messageRequest</tt> to.
     * @param messageContent the SC <tt>Message</tt> that was used to create
     * the <tt>Request</tt>
     * .
     * @throws TransactionUnavailableException if we fail creating the
     * transaction required to send <tt>messageRequest</tt>.
     * @throws SipException if we fail sending <tt>messageRequest</tt>.
     */
    void sendMessageRequest(Request messageRequest, Contact to,
                    Message messageContent)
        throws TransactionUnavailableException, SipException
    {
        //Transaction
        ClientTransaction messageTransaction;
        SipProvider jainSipProvider
            = this.sipProvider.getDefaultJainSipProvider();

        messageTransaction = jainSipProvider
            .getNewClientTransaction(messageRequest);

        // send the message
        messageTransaction.sendRequest();

        // we register the reference to this message to retrieve it when
        // we'll receive the response message
        String key = ((CallIdHeader)messageRequest.getHeader(CallIdHeader.NAME))
            .getCallId();

        this.sentMsg.put(key, messageContent);
    }

    /**
     * Construct a <tt>Request</tt> represent a new message.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     *
     * @return a Message Request destined to the contact
     *
     * @throws OperationFailedException if an error occurred during
     * the creation of the request
     */
    Request createMessageRequest(Contact to, Message message)
        throws OperationFailedException
    {
        Address toAddress = null;
        try
        {
            toAddress = sipProvider.parseAddressString(to.getAddress());
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
            // protect seqN
            synchronized (this)
            {
                cSeqHeader = this.sipProvider.getHeaderFactory()
                    .createCSeqHeader(seqN++, Request.MESSAGE);
            }
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
        String localTag = SipMessageFactory.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            //FromHeader
            fromHeader = this.sipProvider.getHeaderFactory()
                .createFromHeader(
                    sipProvider.getOurSipAddress(toAddress), localTag);

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
        ArrayList<ViaHeader> viaHeaders = this.sipProvider.getLocalViaHeaders(
            toAddress);

        //MaxForwards
        MaxForwardsHeader maxForwards = this.sipProvider
            .getMaxForwardsHeader();

        // Content params
        ContentTypeHeader contTypeHeader;
        ContentLengthHeader contLengthHeader;
        try
        {
            contTypeHeader = this.sipProvider.getHeaderFactory()
                .createContentTypeHeader(getType(message),
                                         getSubType(message));

            if (! DEFAULT_MIME_ENCODING.equalsIgnoreCase(message.getEncoding()))
                contTypeHeader.setParameter("charset", message.getEncoding());

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
                toHeader.getAddress().getURI(), Request.MESSAGE, callIdHeader,
                cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards,
                contTypeHeader, message.getRawData());
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

        req.addHeader(contLengthHeader);

        return req;
    }

    /**
     * Transforms SIP message via transformation layer.
     *
     * @param to The <tt>Contact</tt> to send the <tt>message</tt> to.
     * @param message The <tt>message</tt> to send.
     *
     * @return The new transformed <tt>Message</tt>
     */
    private Message[] transformSIPMessage(final Contact to,
        final Message message)
    {
        MessageDeliveredEvent msgDeliveryPendingEvt
           = new MessageDeliveredEvent(message, to);

        MessageDeliveredEvent[] msgDeliveryPendingEvts =
            messageDeliveryPendingTransform(msgDeliveryPendingEvt);

        if (msgDeliveryPendingEvts == null
            || msgDeliveryPendingEvts.length == 0)
        {
            return new Message[0];
        }

        OperationSetBasicInstantMessaging opSetBasicIM =
            (OperationSetBasicInstantMessaging) sipProvider
                .getSupportedOperationSets().get(
                    OperationSetBasicInstantMessaging.class.getName());
        Message[] transformedMessages =
            new Message[msgDeliveryPendingEvts.length];
        for (int i = 0; i < msgDeliveryPendingEvts.length; i++)
        {
            MessageDeliveredEvent event = msgDeliveryPendingEvts[i];
            String content = event.getSourceMessage().getContent();
            transformedMessages[i] =
                opSetBasicIM.createMessage(content, message.getContentType(),
                    message.getEncoding(), message.getSubject());
        }
        return transformedMessages;
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
     * Frees allocated resources.
     */
    void shutdown()
    {
        sipProvider.removeRegistrationStateChangeListener(registrationListener);
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
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                        + evt.getOldState()
                        + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPresenceSipImpl) sipProvider
                        .getOperationSet(OperationSetPersistentPresence.class);
            }
        }
    }

    /**
     * Class for listening incoming packets.
     */
    private class BasicInstantMessagingMethodProcessor
        extends MethodProcessorAdapter
    {
        @Override
        public boolean processTimeout(TimeoutEvent timeoutEvent)
        {
            synchronized (messageProcessors)
            {
                for (SipMessageProcessor listener : messageProcessors)
                    if(!listener.processTimeout(timeoutEvent, sentMsg))
                        return true;
            }

            // this is normaly handled by the SIP stack
            logger.error("Timeout event thrown : " + timeoutEvent.toString());

            if (timeoutEvent.isServerTransaction()) {
                logger.warn("The sender has probably not received our OK");
                return false;
            }

            Request req = timeoutEvent.getClientTransaction().getRequest();

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

            Contact to = opSetPersPresence.resolveContactID(
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
                failedMessage = sentMsg.get(key);

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
            fireMessageDeliveryFailed(
                // we don't know what message it concerns
                failedMessage,
                to,
                MessageDeliveryFailedEvent.INTERNAL_ERROR);
            return true;
        }

        /**
         * Process a request from a distant contact
         *
         * @param requestEvent the <tt>RequestEvent</tt> containing the newly
         *            received request.
         * @return <tt>true</tt> if the specified event has been handled by this
         *         processor and shouldn't be offered to other processors
         *         registered for the same method; <tt>false</tt>, otherwise
         */
        @Override
        public boolean processRequest(RequestEvent requestEvent)
        {
            synchronized (messageProcessors)
            {
                for (SipMessageProcessor listener : messageProcessors)
                    if(!listener.processMessage(requestEvent))
                        return true;
            }

            // get the content
            String content = null;
            Request req = requestEvent.getRequest();
            try
            {

                content = new String(req.getRawContent(), getCharset(req));
            }
            catch (UnsupportedEncodingException ex)
            {
                if (logger.isDebugEnabled())
                    logger.debug("failed to convert the message charset");
                content = new String(requestEvent.getRequest().getRawContent());
            }

            // who sent this request ?
            FromHeader fromHeader = (FromHeader)
                requestEvent.getRequest().getHeader(FromHeader.NAME);

            if (fromHeader == null)
            {
                logger.error("received a request without a from header");
                return false;
            }

            Contact from = opSetPersPresence.resolveContactID(
                fromHeader.getAddress().getURI().toString());

            ContentTypeHeader ctheader =
                (ContentTypeHeader)req.getHeader(ContentTypeHeader.NAME);

            String ctype = null;
            String cencoding = null;

            if(ctheader == null)
            {
                ctype = DEFAULT_MIME_TYPE;
            }
            else
            {
                ctype = ctheader.getContentType() + "/" +
                    ctheader.getContentSubType();
                cencoding = ctheader.getParameter("charset");
            }

            if(cencoding == null)
                cencoding = DEFAULT_MIME_ENCODING;

            Message newMessage = createMessage(content, ctype, cencoding, null);

            if (from == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("received a message from an unknown contact: "
                            + fromHeader.getAddress().getURI().toString());

                //create the volatile contact
                if (fromHeader.getAddress().getDisplayName() != null)
                {
                    from = opSetPersPresence.createVolatileContact(
                        fromHeader.getAddress().getURI().toString(),
                        fromHeader.getAddress().getDisplayName().toString());
                }
                else
                {
                    from = opSetPersPresence.createVolatileContact(
                        fromHeader.getAddress().getURI().toString());
                }
            }

            // answer ok
            try
            {
                Response ok = sipProvider.getMessageFactory()
                    .createResponse(Response.OK, requestEvent.getRequest());
                SipStackSharing.getOrCreateServerTransaction(requestEvent).
                    sendResponse(ok);
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
                if (logger.isDebugEnabled())
                    logger.debug("Invalid argument for createResponse : "
                            + exc.getMessage(), exc);
            }

            // fire an event
            MessageReceivedEvent msgReceivedEvt
                = new MessageReceivedEvent(
                        newMessage, from, new Date());
            fireMessageEvent(msgReceivedEvt);

            return true;
        }

        /**
         * Process a response from a distant contact.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> containing the newly
         *            received SIP response.
         * @return <tt>true</tt> if the specified event has been handled by this
         *         processor and shouldn't be offered to other processors
         *         registered for the same method; <tt>false</tt>, otherwise
         */
        @Override
        public boolean processResponse(ResponseEvent responseEvent)
        {
            synchronized (messageProcessors)
            {
                for (SipMessageProcessor listener : messageProcessors)
                    if(!listener.processResponse(responseEvent, sentMsg))
                        return true;
            }

            Request req = responseEvent.getClientTransaction().getRequest();
            int status = responseEvent.getResponse().getStatusCode();
            // content of the response
            String content = null;

            try
            {
                content = new String(req.getRawContent(), getCharset(req));
            }
            catch (UnsupportedEncodingException exc)
            {
                if (logger.isDebugEnabled())
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

            Contact to = opSetPersPresence.resolveContactID(toHeader.getAddress()
                    .getURI().toString());

            if (to == null)
            {
                logger.error(
                        "Error received a response from an unknown contact : "
                        + toHeader.getAddress().getURI().toString() + " : "
                        + responseEvent.getResponse().getStatusCode()
                        + " "
                        + responseEvent.getResponse().getReasonPhrase());

                // error for delivering the message
                fireMessageDeliveryFailed(
                    // we don't know what message it concerns
                    createMessage(content),
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR);
                return false;
            }

            // we retrieve the original message
            String key = ((CallIdHeader)req.getHeader(CallIdHeader.NAME))
                .getCallId();

            Message newMessage = sentMsg.get(key);

            if (newMessage == null)
            {
                // should never happen
                logger.error("Couldn't find the message sent");

                // error for delivering the message
                fireMessageDeliveryFailed(
                    // we don't know what message it is
                    createMessage(content),
                    to,
                    MessageDeliveryFailedEvent.INTERNAL_ERROR);
                return true;
            }

            // status 401/407 = proxy authentification
            if (status >= 400 && status != 401 && status != 407)
            {
                if (logger.isInfoEnabled())
                    logger.info(responseEvent.getResponse().getStatusCode()
                            + " "
                            + responseEvent.getResponse().getReasonPhrase());

                // error for delivering the message
                MessageDeliveryFailedEvent evt =
                    new MessageDeliveryFailedEvent(
                            newMessage,
                            to,
                            MessageDeliveryFailedEvent.NETWORK_FAILURE,
                            System.currentTimeMillis(),
                            responseEvent.getResponse().getStatusCode()
                                + " "
                                + responseEvent.getResponse().getReasonPhrase());
                fireMessageEvent(evt);
                sentMsg.remove(key);
            }
            else if (status == 401 || status == 407)
            {
                // proxy ask for authentification
                if (logger.isDebugEnabled())
                    logger.debug("proxy asks authentication : "
                            + responseEvent.getResponse().getStatusCode()
                            + " "
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
                                System.currentTimeMillis(),
                                ex.getMessage());
                    fireMessageEvent(evt);
                    sentMsg.remove(key);
                }
            }
            else if (status >= 200)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Ack received from the network : "
                            + responseEvent.getResponse().getStatusCode()
                            + " "
                            + responseEvent.getResponse().getReasonPhrase());

                // we delivered the message
                MessageDeliveredEvent msgDeliveredEvt
                    = new MessageDeliveredEvent(
                            newMessage, to, new Date());

                fireMessageEvent(msgDeliveredEvt);

                // we don't need this message anymore
                sentMsg.remove(key);
            }

            return true;
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
                if (logger.isDebugEnabled())
                    logger.debug("Authenticating a message request.");

                ClientTransaction retryTran = null;

                // we synch here to protect seqN increment
                synchronized(this)
                {
                    retryTran = sipProvider.getSipSecurityManager()
                        .handleChallenge(
                            response
                            , clientTransaction
                            , jainSipProvider,
                            seqN++);

                }

                if(retryTran == null)
                {
                    if (logger.isTraceEnabled())
                        logger.trace("No password supplied or error occured!");
                    return;
                }

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
