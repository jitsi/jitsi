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

import net.java.sip.communicator.service.protocol.*;

import gov.nist.javax.sip.header.*;
import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * The SIP implementation of {@link OperationSetJitsiMeetTools}.
 *
 * @author Pawel Domas
 * @author Cristian Florin Ghita
 */
public class OperationSetJitsiMeetToolsSipImpl
    extends MethodProcessorAdapter
    implements OperationSetJitsiMeetTools
{
    /**
     * The protocol provider that created this operation set.
     */
    private final ProtocolProviderServiceSipImpl parentProvider;

    /**
     * Parameter key used to complement handling of send/receive
     * implementation for a protocol.
     */
    private static final String VIA_PARAMETER = "VIA";

    /**
     * Parameter value for VIA_PARAMETER that represents SIP
     * INFO method to send/receive using SIP.
     */
    private static final String VIA_SIP_INFO = "SIP.INFO";

    /**
    * Default encoding for incoming messages.
    */
    public static final String DEFAULT_MIME_ENCODING = "UTF-8";

    /**
     * Name of extra INVITE header which specifies name of MUC room that is
     * hosting the Jitsi Meet conference.
     */
    public String JITSI_MEET_ROOM_HEADER = "Jitsi-Conference-Room";

    /**
     * Property name of extra INVITE header which specifies name of MUC room
     * that is hosting the Jitsi Meet conference.
     */
    private static final String JITSI_MEET_ROOM_HEADER_PROPERTY
        = "JITSI_MEET_ROOM_HEADER_NAME";

    /**
     * Property name to enable answering SIP Info messages that has no content type.
     */
    private static final String JITSI_MEET_ANSWER_EMPTY_INFO_PROPERTY = "JITSI_MEET_ANSWER_EMPTY_INFO";

    /**
     * Whether to answer SIP Info messages that has no content type.
     */
    private final boolean enableAnswerEmptySIPInfo;

    /**
     * The logger used by this class.
     */
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetJitsiMeetToolsSipImpl.class);

    /**
     * The list of {@link JitsiMeetRequestListener}.
     */
    private final List<JitsiMeetRequestListener> requestHandlers
        = new CopyOnWriteArrayList<>();

    /**
     * Constructs new OperationSetJitsiMeetToolsSipImpl.
     * @param parentProvider the parent provider.
     */
    public OperationSetJitsiMeetToolsSipImpl(
        ProtocolProviderServiceSipImpl parentProvider)
    {
        AccountID account = parentProvider.getAccountID();
        // Specify custom header names
        JITSI_MEET_ROOM_HEADER = account.getAccountPropertyString(
            JITSI_MEET_ROOM_HEADER_PROPERTY, JITSI_MEET_ROOM_HEADER);
        enableAnswerEmptySIPInfo = account.getAccountPropertyBoolean(
            JITSI_MEET_ANSWER_EMPTY_INFO_PROPERTY, false);

        this.parentProvider = parentProvider;
        this.parentProvider.registerMethodProcessor(Request.INFO, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.add(requestHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.remove(requestHandler);
    }

    /**
     * Notifies all registered {@link JitsiMeetRequestListener} about incoming
     * call that contains name of the MUC room which is hosting Jitsi Meet
     * conference.
     * @param call the incoming {@link Call} instance.
     * @param callHeaders map of all the sip headers (<name,value>)
     */
    public void notifyJoinJitsiMeetRoom(
        Call call, Map<String, String> callHeaders)
    {
        // Parses Jitsi Meet room name header
        String jitsiMeetRoom = callHeaders.get(JITSI_MEET_ROOM_HEADER);

        // nothing to handle
        if (jitsiMeetRoom == null)
            return;

        boolean handled = false;
        for (JitsiMeetRequestListener l : requestHandlers)
        {
            l.onJoinJitsiMeetRequest(call, jitsiMeetRoom, callHeaders);
            handled = true;
        }
        if (!handled)
        {
            logger.warn(
                "Unhandled join Jitsi Meet request R:" + jitsiMeetRoom
                    + " C: " + call);
        }
    }

    /**
     * Sends a JSON to the specified <tt>callPeer</tt>.
     *
     * @param callPeer the CallPeer to which we send the JSONObject to.
     * @param jsonObject the JSONObject that we send to the CallPeer.
     *
     * @param params a map which is used to set specific parameters
     * for the protocol used to send the jsonObject.
     */
    @Override
    public void sendJSON(CallPeer callPeer,
                        JSONObject jsonObject,
                        Map<String, Object> params)
                        throws OperationFailedException
    {
        try
        {
            boolean bViaParam = params.containsKey(VIA_PARAMETER);

            if (!bViaParam)
            {
                throw new OperationFailedException(
                    "Unspecified " + VIA_PARAMETER + " parameter!",
                    OperationFailedException.ILLEGAL_ARGUMENT);
            }

            String viaParam = (String) params.get(VIA_PARAMETER);

            if (viaParam.equalsIgnoreCase(VIA_SIP_INFO))
            {
                CallPeerSipImpl peer = (CallPeerSipImpl) callPeer;

                Request info = this.parentProvider
                                    .getMessageFactory()
                                    .createRequest( peer.getDialog(),
                                                    Request.INFO);

                ContentType ct = new ContentType("application", "json");

                String content = jsonObject.toString();

                ContentLength cl = new ContentLength(content.length());

                info.setContentLength(cl);

                info.setContent(content.getBytes(), ct);

                ClientTransaction clientTransaction =
                    peer.getJainSipProvider()
                        .getNewClientTransaction(info);

                if (peer.getDialog().getState() == DialogState.TERMINATED)
                {
                    //this is probably because the call has just ended, so don't
                    //throw an exception. simply log and get lost.
                    logger.warn(
                        "Trying to send a request using a TERMINATED dialog.");
                    return;
                }

                peer.getDialog().sendRequest(clientTransaction);
                logger.trace("Request {} sent", info);
            }
            else
            {
                throw new OperationFailedException(
                    "Unsupported " + VIA_PARAMETER + " parameter by protocol!",
                    OperationFailedException.ILLEGAL_ARGUMENT);
            }
        }
        catch(Exception ex)
        {
            throw new OperationFailedException(
                ex.getMessage(),
                OperationFailedException.GENERAL_ERROR);
        }
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     *
     * @param requestEvent requestEvent fired from the SipProvider to the
     * <tt>SipListener</tt> representing a Request received from the network.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    @Override
    public boolean processRequest(RequestEvent requestEvent)
    {

        boolean requestHandled = false;

        try
        {
            Request request = requestEvent.getRequest();

            ContentTypeHeader contentTypeHeader
                = (ContentTypeHeader)
                    request.getHeader(ContentTypeHeader.NAME);

            if (contentTypeHeader != null)
            {
                if (contentTypeHeader.getContentType()
                    .equalsIgnoreCase("application")
                    &&
                        contentTypeHeader.getContentSubType()
                            .equalsIgnoreCase("json"))
                {

                    requestHandled = true;

                    String charset = contentTypeHeader.getParameter("charset");

                    if (charset == null)
                        charset = DEFAULT_MIME_ENCODING;

                    String contentString = new String(
                        request.getRawContent(),
                        charset);

                    JSONObject receivedJson =
                        (JSONObject) (new JSONParser()).parse(contentString);

                    OperationSetBasicTelephonySipImpl telephony
                    = (OperationSetBasicTelephonySipImpl)parentProvider
                            .getOperationSet(OperationSetBasicTelephony.class);

                    Dialog dialog = requestEvent.getDialog();

                    // Find call peer
                    CallPeerSipImpl callPeer = null;
                    for (Iterator<CallSipImpl> activeCalls
                            = telephony.getActiveCalls();
                        activeCalls.hasNext();)
                    {
                        CallSipImpl call = activeCalls.next();
                        callPeer
                            = call.findCallPeer(dialog);
                        if(callPeer != null)
                        {
                            break;
                        }
                    }

                    if (callPeer == null)
                    {
                        if (logger.isTraceEnabled())
                        {
                            logger.trace("Could not find call peer for " +
                                request);
                        }
                    }

                    HashMap<String, Object> params =
                        new HashMap<String, Object>() {{
                            put(VIA_PARAMETER, VIA_SIP_INFO);
                    }};

                    boolean handled = false;

                    for (JitsiMeetRequestListener l : requestHandlers)
                    {
                        l.onJSONReceived(callPeer, receivedJson, params);
                        handled = true;
                    }

                    if (!handled)
                    {
                        logger.warn(
                            "Unhandled onJSONReceived Jitsi Meet Request!");
                    }

                    sendSipInfoResponse(requestEvent);
                }
            }
            else if (enableAnswerEmptySIPInfo)
            {
                requestHandled = true;

                sendSipInfoResponse(requestEvent);
            }
        }
        catch(Exception exception)
        {
            logger.error(exception.getMessage());
        }

        return requestHandled;
    }

    private void sendSipInfoResponse(RequestEvent requestEvent)
        throws Exception
    {
        // Handle response
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();

        if (serverTransaction == null)
        {
            serverTransaction = SipStackSharing.getOrCreateServerTransaction(requestEvent);

            if (serverTransaction == null)
            {
                logger.warn("No valid server transaction to send response!");
                return;
            }
        }

        Response response = this.parentProvider.getMessageFactory()
                .createResponse(Response.OK, serverTransaction.getRequest());

        serverTransaction.sendResponse(response);

        if (logger.isTraceEnabled())
        {
            logger.trace("Response " + response.toString() + " sent.");
        }
    }
}
