/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.extensions.*;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

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
     * and will just forward method calls to <tt>factory_</tt> (besides marking
     * messages).
     */
    private final MessageFactory wrappedFactory;

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
     * @return a <tt>Request</tt> marked with its service
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                Object content)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return request;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @return a <tt>Request</tt> marked with its service
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                byte[] content)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return request;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @return a <tt>Request</tt> marked with its service
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return request;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Request</tt> marked with its service.
     *
     * @return a <tt>Request</tt> marked with its service
     */
    public Request createRequest(String requestParam)
        throws ParseException
    {
        Request request = this.wrappedFactory.createRequest(requestParam);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return request;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                Object content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards, ContentTypeHeader contentType,
                byte[] content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, CallIdHeader callId,
            CSeqHeader cSeq, FromHeader from, ToHeader to, List via,
                MaxForwardsHeader maxForwards)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                callId, cSeq, from, to, via, maxForwards);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, Request request,
            ContentTypeHeader contentType, Object content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                request, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, Request request,
            ContentTypeHeader contentType, byte[] content)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(statusCode,
                request, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(int statusCode, Request request)
        throws ParseException
    {
        Response response
            = this.wrappedFactory.createResponse(statusCode, request);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    /**
     * Calls the same method in the internal wrapped factory
     * and returns a <tt>Response</tt> marked with its service.
     *
     * @return a <tt>Response</tt> marked with its service
     */
    public Response createResponse(String responseParam)
        throws ParseException
    {
        Response response = this.wrappedFactory.createResponse(responseParam);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.protocolProvider);
        return response;
    }

    //---------------- higher level methods ----------------------------------
    /**
     * Creates an invite request destined for <tt>callee</tt>.
     *
     * @param toAddress the sip address of the callee that the request is meant
     *            for.
     *
     * @return a newly created sip <tt>Request</tt> destined for <tt>callee</tt>
     *         .
     * @throws OperationFailedException with the corresponding code if creating
     *             the request fails.
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
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR,
                ex,
                logger);
        }
        catch (ParseException exc)
        {
            // shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, exc,
                logger);
        }

        // ReplacesHeader
        Header replacesHeader = stripReplacesHeader(toAddress);

        // FromHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            // FromHeader
            fromHeader =
                headerFactory.createFromHeader(
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
                OperationFailedException.INTERNAL_ERROR,
                ex,
                logger);
        }

        // ViaHeaders
        ArrayList<ViaHeader> viaHeaders =
            protocolProvider.getLocalViaHeaders(toAddress);

        // MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();

        // Contact
        ContactHeader contactHeader = null;
        try
        {
            contactHeader
                = protocolProvider.getContactHeader(toHeader.getAddress());
        }
        catch (IllegalArgumentException exc)
        {
            // encapsulate the illegal argument exception into an OpFailedExc
            // so that the UI would notice it.
            throw new OperationFailedException(
                            exc.getMessage(),
                            OperationFailedException.ILLEGAL_ARGUMENT,
                            exc);
        }

        Request invite = null;
        try
        {
            invite =
                protocolProvider.getMessageFactory().createRequest(
                    toHeader.getAddress().getURI(), Request.INVITE,
                    callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders,
                    maxForwards);

        }
        catch (ParseException ex)
        {
            // shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create invite Request!",
                OperationFailedException.INTERNAL_ERROR,
                ex,
                logger);
        }

        // User Agent
        UserAgentHeader userAgentHeader
                    = protocolProvider.getSipCommUserAgentHeader();
        if (userAgentHeader != null)
            invite.setHeader(userAgentHeader);

        // add the contact header.
        invite.setHeader(contactHeader);

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
     *            to be copied
     * @param effect the SIP <tt>Message</tt> into which the information is
     *            to be copied
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
     * @param address the <tt>Address</tt> which is to have its
     *            <tt>URI</tt> examined and modified
     *
     * @return a <tt>Header</tt> which represents the Replaces header
     *         contained in the <tt>URI</tt> of the specified
     *         <tt>address</tt>; <tt>null</tt> if no such header is
     *         present
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
                    replacesHeader =
                        protocolProvider.getHeaderFactory().createHeader(
                            ReplacesHeader.NAME, replacesHeaderValue);
                }
                catch (ParseException ex)
                {
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
     * Verifies wither we have already authenticated requests with the same
     * <tt>Call-ID</tt> as <tt>request</tt> and attaches the corresponding
     * credentials in an effort to avoid receiving an authentication challenge
     * from the server and having to resend the request. This method has no
     * effect if the <tt>Call-ID</tt> has not been seen by our security manager.
     *
     * @param request the request that we'd like to try pre-authenticating.
     * @param service_ the provider where the request originated.
     */
    public void preAuthenticateRequest( Request request )
    {
        //check whether there's a cached authorization header for this
        // call id and if so - attach it to the request.
        // add authorization header
        CallIdHeader callIdHeader
            = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        String callid = callIdHeader.getCallId();

        AuthorizationHeader authorization =
            protocolProvider.getSipSecurityManager()
                .getCachedAuthorizationHeader(callid);

        if (authorization != null)
            request.setHeader(authorization);

    }
}
