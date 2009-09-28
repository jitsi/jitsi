/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;

import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

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
    private final ProtocolProviderServiceSipImpl service_;

    /**
     * The wrapped factory. <tt>SipMessageFactory</tt> does nothing by itself
     * and will just forward method calls to <tt>factory_</tt> (besides marking
     * messages).
     */
    private final MessageFactory factory_;

    /**
     * The contructor for this class.
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

        this.service_ = service;
        this.factory_ = wrappedFactory;
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
        Request request = this.factory_.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Request request = this.factory_.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Request request = this.factory_.createRequest(requestURI, method,
                callId, cSeq, from, to, via, maxForwards);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Request request = this.factory_.createRequest(requestParam);
        SipApplicationData.setApplicationData(request,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, callId,
                cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, callId,
                cSeq, from, to, via, maxForwards, contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, callId,
                cSeq, from, to, via, maxForwards);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, request,
                contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, request,
                contentType, content);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(statusCode, request);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
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
        Response response = this.factory_.createResponse(responseParam);
        SipApplicationData.setApplicationData(response,
                SipApplicationData.KEY_SERVICE, this.service_);
        return response;
    }

    //---------------- higher level methods ----------------------------------
}
