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

import gov.nist.javax.sip.address.*;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.message.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.message.Message;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * This <tt>MessageFactory</tt> is just a wrapper around a "real"
 * <tt>Messagefactory</tt>, which will be passed to the constructor. Its only
 * purpose is to mark every created message with its
 * <tt>ProtocolProviderServiceSipImpl</tt>, so that the generated
 * <tt>Message</tt>-s will be easy to route or dispatch.
 *
 * @author Sebastien Mazy
 * @author Emil Ivov
 */
public class SipMessageFactory
    implements MessageFactory
{
    /**
     * The logger for this class
     */
    public final static Logger logger
        = Logger.getLogger(SipMessageFactory.class);

    /**
     * The service this <tt>SipMessageFactory</tt> belongs to;
     * used to mark messages.
     */
    private final ProtocolProviderServiceSipImpl protocolProvider;

    /**
     * The wrapped factory. <tt>SipMessageFactory</tt> does nothing by itself
     * and will just forward method calls to <tt>factory</tt> (besides marking
     * messages).
     */
    private final MessageFactory wrappedFactory;

    /**
     * A random generator we use to generate tags.
     */
    private static Random localTagGenerator = null;

    /**
     * The constructor for this class.
     *
     * @param service the service this <tt>MessageFactory</tt> belongs to.
     * @param wrappedFactory the <tt>MessageFactory</tt> which will really
     * create messages.
     */
    public SipMessageFactory(ProtocolProviderServiceSipImpl service,
            MessageFactory wrappedFactory)
    {
        if (service == null)
            throw new NullPointerException("service is null");
        if (wrappedFactory == null)
            throw new NullPointerException("wrappedFactory is null");

        this.protocolProvider = service;
        this.wrappedFactory = wrappedFactory;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @param requestURI the new URI object of the requestURI value of this
     * <tt>Message</tt>.
     * @param method the new string of the method value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Request</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new Object of the body content value of this Message.
     *
     * @return a <tt>Request</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                Object content)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        return (Request)attachScSpecifics(request);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @param requestURI the new URI object of the requestURI value of this
     * <tt>Message</tt>.
     * @param method the new string of the method value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Request</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new Object of the body content value of this Message.
     *
     * @return a <tt>Request</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                byte[] content)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        return (Request)attachScSpecifics(request);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @param requestURI the new URI object of the requestURI value of this
     * <tt>Message</tt>.
     * @param method the new string of the method value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Request</tt>.
     *
     * @return a <tt>Request</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards);
        return (Request)attachScSpecifics(request);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @param requestParam the new string value of the Request.
     * @return a <tt>Request</tt> marked with its service
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the request.
     */
    public Request createRequest(String requestParam)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestParam);
        return (Request)attachScSpecifics(request);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Response</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new Object of the body content value of this
     * <tt>Message</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                Object content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Response</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new Object of the body content value of this
     * <tt>Message</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                byte[] content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param callId the new CallIdHeader object of the callId value of this
     * <tt>Message</tt>.
     * @param cSeq the new CSeqHeader object of the cSeq value of this
     * <tt>Message</tt>.
     * @param from the new FromHeader object of the from value of this
     * <tt>Message</tt>.
     * @param to the new ToHeader object of the to value of this
     * <tt>Message</tt>.
     * @param via the new List object of the ViaHeaders of this
     * <tt>Message</tt>.
     * @param maxForwards the Max-Forwards header for the new <tt>Response</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param request the received <tt>Request</tt> object upon which to base the
     * <tt>Response</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new byte array of the body content value of this
     * <tt>Message</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, Request request,
            ContentTypeHeader contentType, Object content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                request, contentType, content);
        extractAndApplyDialogToTag((SIPRequest)request, response);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param request the received <tt>Request</tt> object upon which to base the
     * <tt>Response</tt>.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this <tt>Message</tt>.
     * @param content the new byte array of the body content value of this
     * <tt>Message</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, Request request,
            ContentTypeHeader contentType, byte[] content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                request, contentType, content);
        extractAndApplyDialogToTag((SIPRequest)request, response);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param statusCode the new integer of the statusCode value of this
     * <tt>Message</tt>.
     * @param request the received <tt>Request</tt> object upon which to base the
     * <tt>Response</tt>.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, Request request)
        throws ParseException
    {
        Response response
            = this.wrappedFactory.createResponse(statusCode, request);
        extractAndApplyDialogToTag((SIPRequest)request, response);
        return (Response)attachScSpecifics(response);
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @param responseParam is a string representing the response. The argument
     * should only contain the Sip Headers and not the body of the response.
     *
     * @return a <tt>Response</tt> marked with its service
     *
     * @throws ParseException which signals an error has been reached unexpectedly
     * while parsing the response.
     */
    public Response createResponse(String responseParam)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(responseParam);
        return (Response)attachScSpecifics(response);
    }

    /**
     * It appears that when the JAIN-SIP message factory creates responses it
     * does not add the dialog to tag (if it exists) to the response. We
     * therefore try some heavy-weight JAIN-SIP hacking in order to do this
     * ourselves.
     *
     * @param request the <tt>SIPRequest</tt> that we are constructing a
     * response to.
     * @param response the <tt>Response</tt> that we have just constructed
     * and that we'd like to patch with a To tag.
     */
    private void extractAndApplyDialogToTag(
                    SIPRequest request, Response response)
    {
        ServerTransaction tran = (ServerTransaction)request.getTransaction();

        //be extra cautious
        if(tran == null)
            return;

        Dialog dialog = tran.getDialog();

        if(dialog == null)
            return;

        String localDialogTag = dialog.getLocalTag();

        if(localDialogTag == null || localDialogTag.length() == 0)
        {
            //no local tag yet. return and we'll generate one later.
            return;
        }

        ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);

        if( to == null)
            return;

        try
        {
            to.setTag(localDialogTag);
        }
        catch (ParseException e)
        {
            //we just extracted the tag from a Dialog. This is therefore
            //very unlikely to happen and we are going to just log it.
            if (logger.isDebugEnabled())
                logger.debug("Failed to attach a to tag", e);
        }

    }

    /**
     * Attaches to <tt>message</tt> headers and object params that we'd like to
     * be present in absolutely all messages we create (like for example the
     * user agent header, the contact header or the provider message object
     * tag).
     *
     * @param message the message that we'd like to tag
     * @return returns a reference to <tt>message</tt> for convenience reasons.
     */
    private Message attachScSpecifics(Message message)
    {
        SipApplicationData.setApplicationData(message,
                        SipApplicationData.KEY_SERVICE, this.protocolProvider);

        //the jain-sip semantics allow the application to "forget" attaching a
        //To tag to a response so let's make sure we do this here
        if(message instanceof Response)
        {
            FromHeader from = (FromHeader)message.getHeader(From.NAME);
            String fromTag = (from == null) ? null : from.getTag();
            Response response = (Response)message;

            //if there's a from tag and this is a non-failure response,
            //that still doesn't have a To tag, then we are adding a to tag.
            if(fromTag != null && fromTag.trim().length() > 0
                && response.getStatusCode() > 100
                && response.getStatusCode() < 300)
            {
                attachToTag(response, null);
            }
        }

        //add a contact header.
        attachContactHeader(message);


        // If this is a SIP request (other than ACK) then let's try to
        // pre-authenticate it.
        if(message instanceof Request
           && !Request.ACK.equals(((Request)message).getMethod()))
        {
            preAuthenticateRequest((Request)message);
        }

        // User Agent
        UserAgentHeader userAgentHeader
                    = protocolProvider.getSipCommUserAgentHeader();

        //beware: if UA header generation failed for some reason, we don't want
        //it to mess up the entire request.
        if (userAgentHeader != null)
        {
            message.setHeader(userAgentHeader);
        }

        // attach any custom headers pre-configured for the account
        ConfigHeaders.attachConfigHeaders(message, protocolProvider);

        return message;
    }

    /**
     * The method tries to determine an address that would be reachable by the
     * destination of <tt>message</tt> and then creates a <tt>ContactHeader</tt>
     * out of it and attaches it to the <tt>message</tt>. The method takes into
     * account both <tt>Request</tt>s and <tt>Response</tt>s. The method
     * is meant to be used only for messages that have been otherwise
     * initialized (in particular the Request URI in requests or the Via
     * headers in responses.). The method is only meant for use by
     * <tt>attachScSpecifics()</tt>. Any requests and responses created through
     * this message factory would go through <tt>attachScSpecifics</tt> so they
     * don't need to explicitly call this method.
     *
     * @param message the message that we'd like to attach a
     * <tt>ContactHeader</tt> to.
     *
     * @return a reference to the <tt>message</tt> that was also passed as
     * a parameter in order to allow for more agility when using the method.
     */
    private Message attachContactHeader(Message message)
    {
        if(message instanceof Request)
        {
            Request request = (Request)message;

            SipURI requestURI = (SipURI)request.getRequestURI();

            request.setHeader(protocolProvider.getContactHeader(requestURI));
            return request;
        }
        else
        {
            Response response = (Response)message;

            ViaHeader via = (ViaHeader)response.getHeader(ViaHeader.NAME);
            SipURI intendedDestinationURI;

            String transport = via.getTransport();

            String host = via.getHost();
            int port = via.getPort();

            try
            {
                intendedDestinationURI = protocolProvider.getAddressFactory()
                    .createSipURI(null, host);
                intendedDestinationURI.setPort(port);

                if(transport != null)
                    intendedDestinationURI.setTransportParam(transport);
            }
            catch (ParseException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug(via + " does not seem to be a valid header.");

                FromHeader from = (FromHeader)response.getHeader(From.NAME);
                intendedDestinationURI = (SipURI)from.getAddress().getURI();
            }

            ContactHeader contactHeader
                = protocolProvider.getContactHeader(intendedDestinationURI);

            response.setHeader(contactHeader);

            return response;
        }
    }

    /**
     * Generate a tag for a FROM header or TO header. Just return a random 4
     * digit integer (should be enough to avoid any clashes!) Tags only need to
     * be unique within a call.
     *
     * @return a string that can be used as a tag parameter.
     *
     * synchronized: needed for access to 'rand', else risk to generate same tag
     * twice
     */
    public static synchronized String generateLocalTag()
    {
        if(localTagGenerator == null)
            localTagGenerator = new Random();
        return Integer.toHexString(localTagGenerator.nextInt());
    }

    /**
     * Generates a ToTag and attaches it to the to header of <tt>response</tt>.
     *
     * @param response the response that is to get the ToTag.
     * @param containingDialog the <tt>Dialog</tt> instance that the response
     * would be sent in or <tt>null</tt> if we are not aware of the
     * <tt>Dialog</tt> when calling this method.
     */
    private void attachToTag(Response response, Dialog containingDialog)
    {
        ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
        if (to == null) {
            if (logger.isDebugEnabled())
                logger.debug("Strange ... no to To header in response:" + response);
            return;
        }

        if( containingDialog != null
            && containingDialog.getLocalTag() != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("We seem to already have a tag in this dialog. "
                        +"Returning");
            return;
        }

        try
        {
            if (to.getTag() == null || to.getTag().trim().length() == 0)
            {

                String toTag = generateLocalTag();

                if (logger.isDebugEnabled())
                    logger.debug("generated to tag: " + toTag);
                to.setTag(toTag);
            }
        }
        catch (ParseException ex)
        {
            //a parse exception here mean an internal error so we can only log
            logger.error("Failed to attach a to tag to an outgoing response."
                         , ex);
        }
    }

    /**
     * Creates a new {@link Request} of a specific method which is to be sent in
     * a specific <tt>Dialog</tt> and populates its generally-necessary
     * headers such as the Authorization header.
     *
     * @param dialog the <tt>Dialog</tt> to create the new
     *            <tt>Request</tt> in
     * @param method the method of the newly-created <tt>Request<tt>
     * @return a new {@link Request} of the specified <tt>method</tt> which
     * is to be sent in the specified <tt>dialog</tt> and populated with its
     * generally-necessary headers such as the Authorization header
     *
     * @throws OperationFailedException if we get a SipException while creating
     * the new request from <tt>dialog</tt>.
     */
    public Request createRequest(Dialog dialog, String method)
        throws OperationFailedException
    {
        Request request = null;
        try
        {
            request = dialog.createRequest(method);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create " + method + " request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        //override the via and contact headers as jain-sip is generating one
        //from the listening point which is 0.0.0.0 or ::0
        ArrayList<ViaHeader> viaHeaders
            = protocolProvider.getLocalViaHeaders(dialog.getRemoteParty());
        request.setHeader(viaHeaders.get(0));


        //now that we've set the Via headers right, we can attach our SC
        //specifics
        attachScSpecifics(request);

        return request;
    }

    //---------------- higher level methods ----------------------------------
    /**
     * Creates an invite request destined for <tt>callee</tt>.
     *
     * @param toAddress the sip address of the callee that the request is meant
     * for.
     *
     * @return a newly created sip <tt>Request</tt> destined for
     * <tt>callee</tt>.
     *
     * @throws OperationFailedException with the corresponding code if creating
     * the request fails.
     * @throws IllegalArgumentException if <tt>toAddress</tt> does not appear
     * to be a valid destination.
     */
    public Request createInviteRequest( Address toAddress)
        throws OperationFailedException, IllegalArgumentException
    {
        // Call ID
        CallIdHeader callIdHeader = protocolProvider.getDefaultJainSipProvider()
            .getNewCallId();

        // CSeq
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = headerFactory.createCSeqHeader(1l, Request.INVITE);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Error occurred while constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        catch (ParseException exc)
        {
            // shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Error while constructing a CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, exc, logger);
        }

        // ReplacesHeader
        Header replacesHeader = stripReplacesHeader(toAddress);

        // FromHeader
        String localTag = generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            // FromHeader
            fromHeader = headerFactory.createFromHeader(
                    protocolProvider.getOurSipAddress(toAddress), localTag);

            // ToHeader
            toHeader = headerFactory.createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            // these two should never happen.
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the ToHeader",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // ViaHeaders
        ArrayList<ViaHeader> viaHeaders =
            protocolProvider.getLocalViaHeaders(toAddress);

        // MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();

        Request invite = null;
        try
        {
            invite = protocolProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI(), Request.INVITE, callIdHeader,
                cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

        }
        catch (ParseException ex)
        {
            // shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create invite Request!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // Call-info header
        CallInfoHeader callInfoHeader = null;
        try
        {
            ProtocolProviderService cusaxProvider = null;

            OperationSetCusaxUtils cusaxOpSet =
                protocolProvider.getOperationSet(OperationSetCusaxUtils.class);

            if(cusaxOpSet != null)
                cusaxProvider = cusaxOpSet.getLinkedCusaxProvider();

            String alternativeImppAddress = null;

            if (cusaxProvider != null)
                alternativeImppAddress
                    = cusaxProvider.getAccountID().getAccountAddress();

            if (alternativeImppAddress != null)
            {
               callInfoHeader
                   = headerFactory.createCallInfoHeader(
                       new GenericURI("xmpp:" + alternativeImppAddress));

               callInfoHeader.setParameter("purpose", "impp");
            }
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        if (callInfoHeader != null)
            invite.setHeader(callInfoHeader);

        // Add the ReplacesHeader if any.
        if (replacesHeader != null)
        {
            invite.setHeader(replacesHeader);
        }

        return invite;
    }

    /**
     * Creates an invite request destined for <tt>callee</tt> and reflects
     * any possible non-null cause (e.g. a Refer request causing a transfer) on
     * the newly created request.
     *
     * @param toAddress the sip address of the callee that the request is meant
     * for.
     * @param cause the SIP <tt>Message</tt> from which the information is
     * to be copied or <tt>null</tt> if there is no cause to be reflected (
     * e.g. a refer request).
     *
     * @return a newly created sip <tt>Request</tt> destined for
     * <tt>callee</tt>.
     *
     * @throws OperationFailedException with the corresponding code if creating
     *             the request fails.
     * @throws IllegalArgumentException if <tt>toAddress</tt> does not appear
     * to be a valid destination.
     */
    public Request createInviteRequest( Address                   toAddress,
                                        javax.sip.message.Message cause)
        throws OperationFailedException, IllegalArgumentException
    {
        Request invite = createInviteRequest(toAddress);

        /*
         * Whatever the cause of the outgoing call is, reflect the appropriate
         * information from it into the INVITE request (and do it elsewhere
         * because this method is already long enough and difficult to grasp).
         */
        if (cause != null)
        {
            reflectCauseOnEffect(cause, invite);
        }

        return invite;
    }

    /**
     * Copies and possibly modifies information from a given SIP
     * <tt>Message</tt> into another SIP <tt>Message</tt> (the first of
     * which is being thought of as the cause for the existence of the second
     * and the second is considered the effect of the first for the sake of
     * clarity in the most common of use cases).
     * <p>
     * The Referred-By header and its optional token are common examples of such
     * information which is to be copied without modification by the referee
     * from the REFER <tt>Request</tt> into the resulting <tt>Request</tt>
     * to the refer target.
     * </p>
     *
     * @param cause the SIP <tt>Message</tt> from which the information is
     * to be copied
     * @param effect the SIP <tt>Message</tt> into which the information is
     * to be copied
     */
    private void reflectCauseOnEffect(javax.sip.message.Message cause,
                                      javax.sip.message.Message effect)
    {

        /*
         * Referred-By (which comes from a referrer) should be copied to the
         * refer target without tampering.
         *
         * TODO Apart from Referred-By, its token should also be copied if
         * present.
         */
        Header referredBy = cause.getHeader(ReferredByHeader.NAME);

        if (referredBy != null)
        {
            effect.setHeader(referredBy);
        }
    }

    /**
     * Returns the <tt>ReplacesHeader</tt> contained, if any, in the
     * <tt>URI</tt> of a specific <tt>Address</tt> after removing it
     * from there.
     *
     * @param address the <tt>Address</tt> which is to have its <tt>URI</tt>
     * examined and modified
     *
     * @return a <tt>Header</tt> which represents the Replaces header contained
     * in the <tt>URI</tt> of the specified <tt>address</tt>; <tt>null</tt> if
     * no such header is present
     *
     * @throws OperationFailedException if we can't parse the replaces header.
     */
    private Header stripReplacesHeader( Address address )
        throws OperationFailedException
    {
        javax.sip.address.URI uri = address.getURI();
        Header replacesHeader = null;

        if (uri.isSipURI())
        {
            SipURI sipURI = (SipURI) uri;
            String replacesHeaderValue = sipURI.getHeader(ReplacesHeader.NAME);

            if (replacesHeaderValue != null)
            {
                for (Iterator<?> headerNameIter = sipURI.getHeaderNames();
                        headerNameIter.hasNext();)
                {
                    if (ReplacesHeader.NAME.equals(headerNameIter.next()))
                    {
                        headerNameIter.remove();
                        break;
                    }
                }

                try
                {
                    replacesHeader = protocolProvider.getHeaderFactory()
                        .createHeader(ReplacesHeader.NAME,
                           URLDecoder.decode(replacesHeaderValue, "UTF-8"));
                }
                catch (Exception ex)
                {
                    //ParseException, EncodingNotSupportedException.
                    throw new OperationFailedException(
                        "Failed to create ReplacesHeader from "
                            + replacesHeaderValue,
                        OperationFailedException.INTERNAL_ERROR, ex);
                }
            }
        }
        return replacesHeader;
    }

    /**
     * Creates an ACK request that can be sent in the context of
     * <tt>clientTransaction</tt>'s <tt>Dialog</tt>.
     *
     * @param clientTransaction the transaction that caused us to send an Ack.
     *
     * @return the newly created ACK <tt>Request</tt>.
     *
     * @throws InvalidArgumentException if there is a problem with the supplied
     * CSeq ( for example <= 0 ).
     * @throws SipException if the CSeq does not relate to a previously sent
     * INVITE or if the Method that created the Dialog is not an INVITE ( for
     * example SUBSCRIBE)
     */
    public Request createAck(ClientTransaction clientTransaction)
        throws InvalidArgumentException, SipException
    {
        // Need to use dialog generated ACKs so that the remote UA core
        // sees them - Fixed by M.Ranganathan
        CSeqHeader cseq = ((CSeqHeader) clientTransaction.getRequest()
                        .getHeader(CSeqHeader.NAME));
        Request ack = clientTransaction.getDialog()
            .createAck(cseq.getSeqNumber());

        attachScSpecifics(ack);

        return ack;
    }

    /**
     * Verifies wither we have already authenticated requests with the same
     * <tt>Call-ID</tt> as <tt>request</tt> and attaches the corresponding
     * credentials in an effort to avoid receiving an authentication challenge
     * from the server and having to re-send the request. This method has no
     * effect if the <tt>Call-ID</tt> has not been seen by our security manager.
     *
     * @param request the request that we'd like to try pre-authenticating.
     */
    public void preAuthenticateRequest( Request request )
    {
        //check whether there's a cached authorization header for this
        // call id and if so - attach it to the request.
        // add authorization header
        CallIdHeader callIdHeader
            = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

        if(callIdHeader == null)
            return;

        String callid = callIdHeader.getCallId();

        AuthorizationHeader authorization =
            protocolProvider.getSipSecurityManager()
                .getCachedAuthorizationHeader(callid);

        if (authorization != null)
            request.setHeader(authorization);
    }

    /**
     * Creates a REGISTER <tt>Request</tt> as per the specified parameters.
     *
     * @param addressOfRecord the address that we shall be registering
     * @param registrationsExpiration the expiration interval for the AOR
     * @param callIdHeader the Call-ID header for our new <tt>Request</tt>.
     * @param cSeqValue the sequence number of the new request.
     *
     * @return the newly created REGISTER <tt>Request</tt>
     * @throws InvalidArgumentException in case there's a problem with any of
     * the arguments that we received for this request.
     * @throws ParseException in case there's a problem with any of
     * the arguments that we received for this request.
     * @throws OperationFailedException if we have a problem with the
     * <tt>MaxForwardsHeader</tt>.
     */
    public Request createRegisterRequest(Address      addressOfRecord,
                                         int          registrationsExpiration,
                                         CallIdHeader callIdHeader,
                                         long         cSeqValue)
        throws InvalidArgumentException,
               ParseException,
               OperationFailedException
    {
        //From
        FromHeader fromHeader = protocolProvider.getHeaderFactory()
            .createFromHeader(addressOfRecord,
                            SipMessageFactory.generateLocalTag());

        //CSeq Header
        CSeqHeader cSeqHeader = protocolProvider.getHeaderFactory()
            .createCSeqHeader(cSeqValue, Request.REGISTER);


        //To Header (In the case of SIP Communicator To and From are always
        //equal.
        ToHeader toHeader = protocolProvider.getHeaderFactory().createToHeader(
                addressOfRecord, null);

        //MaxForwardsHeader
        MaxForwardsHeader maxForwardsHeader = protocolProvider
            .getMaxForwardsHeader();

        //Request URI
        SipURI requestURI = protocolProvider.getRegistrarConnection()
            .getRegistrarURI();

        //Via
        ArrayList<ViaHeader> viaHeaders = protocolProvider
            .getLocalViaHeaders(requestURI);

        //Request
        Request request = createRequest(
                requestURI, Request.REGISTER, callIdHeader, cSeqHeader,
                fromHeader, toHeader, viaHeaders, maxForwardsHeader);

        //Expires Header - try to generate it twice in case there was something
        //wrong with the value we received from the provider or the server.
        ExpiresHeader expHeader = null;
        for (int retry = 0; retry < 2; retry++)
        {
            try
            {
                expHeader = protocolProvider.getHeaderFactory()
                    .createExpiresHeader(registrationsExpiration);
            }
            catch (InvalidArgumentException ex)
            {
                if (retry == 0)
                {
                    registrationsExpiration = 3600;
                    continue;
                }
                throw new IllegalArgumentException(
                    "Invalid registrations expiration parameter - "
                    + registrationsExpiration, ex);
            }
        }
        request.addHeader(expHeader);

        //Add an "expires" param to our Contact header.
        ContactHeader contactHeader
            = (ContactHeader)request.getHeader(ContactHeader.NAME);

        //add expires in the contact header as well in case server likes it
        //better there.
        contactHeader.setExpires(registrationsExpiration);

        request.setHeader(contactHeader);

        return request;

    }

    /**
     * Creates a request that would end a registration established with
     * <tt>registerRequest</tt>
     * @param registerRequest the request that established the registration
     * we are now about to terminate.
     * @param cSeqValue the value of the sequence number that the new
     * <tt>Request</tt> should list in its <tt>CSeq</tt> header.
     *
     * @return a <tt>Request</tt> built to terminate the registration
     * established with <tt>registerRequest</tt>
     * .
     * @throws InvalidArgumentException if we fail constructing the request for
     * some reason.
     */
    public Request createUnRegisterRequest(Request registerRequest,
                                           long    cSeqValue)
        throws InvalidArgumentException
    {
        Request unregisterRequest = (Request) registerRequest.clone();

        unregisterRequest.getExpires().setExpires(0);
        CSeqHeader cSeqHeader = (CSeqHeader) unregisterRequest
            .getHeader(CSeqHeader.NAME);

        //[issue 1] - increment registration cseq number
        //reported by - Roberto Tealdi <roby.tea@tin.it>
        cSeqHeader.setSeqNumber(cSeqValue);

        //remove the branch id.
        ViaHeader via
            = (ViaHeader)unregisterRequest.getHeader(ViaHeader.NAME);
        if(via != null)
            via.removeParameter("branch");


        //also set the expires param in the contact header in case server
        //prefers it this way.
        ContactHeader contact
            = (ContactHeader)unregisterRequest.getHeader(ContactHeader.NAME);

        contact.setExpires(0);

        attachScSpecifics(unregisterRequest);

        return unregisterRequest;
    }
}
