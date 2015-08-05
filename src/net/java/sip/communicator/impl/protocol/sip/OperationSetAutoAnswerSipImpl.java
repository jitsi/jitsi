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

import gov.nist.javax.sip.header.*;

import java.util.*;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.util.*;
// Disambiguation.

/**
 * An Operation Set defining options to auto answer/forward incoming calls.
 * Forward calls to specified number using same provider.
 * Auto answering calls unconditional, on existence of certain header name, or
 * on existence of specified header name and value.
 *
 * @author Damian Minkov
 * @author Vincent Lucas
 */
public class OperationSetAutoAnswerSipImpl
    extends AbstractOperationSetBasicAutoAnswer
    implements OperationSetAdvancedAutoAnswer
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * Should we answer on existence of some header and/or name.
     */
    private boolean answerConditional = false;

    /**
     * The header name to look for.
     */
    private String headerName = null;

    /**
     * The header value to look for, if specified.
     */
    private String headerValue = null;

    /**
     * The call number to use for forwarding calls.
     */
    private String callFwdTo = null;

    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param protocolProvider the parent Protocol Provider.
     */
    public OperationSetAutoAnswerSipImpl(
            ProtocolProviderServiceSipImpl protocolProvider)
    {
        super(protocolProvider);

        this.load();
    }

    /**
     * Load values from account properties.
     */
    @Override
    protected void load()
    {
        super.load();

        AccountID acc = protocolProvider.getAccountID();

        headerName =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_NAME_PROP);
        headerValue =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_VALUE_PROP);
        if(!StringUtils.isNullOrEmpty(headerName))
            answerConditional = true;

        callFwdTo =
            acc.getAccountPropertyString(AUTO_ANSWER_FWD_NUM_PROP);
    }

    /**
     * Saves values to account properties.
     */
    @Override
    protected void save()
    {
        AccountID acc = protocolProvider.getAccountID();
        Map<String, String> accProps = acc.getAccountProperties();

        // lets clear anything before saving :)
        accProps.put(AUTO_ANSWER_UNCOND_PROP, null);
        accProps.put(AUTO_ANSWER_COND_NAME_PROP, null);
        accProps.put(AUTO_ANSWER_COND_VALUE_PROP, null);
        accProps.put(AUTO_ANSWER_FWD_NUM_PROP, null);

        if(answerUnconditional)
        {
            accProps.put(AUTO_ANSWER_UNCOND_PROP, Boolean.TRUE.toString());
        }
        else if(answerConditional)
        {
            accProps.put(AUTO_ANSWER_COND_NAME_PROP, headerName);

            if(!StringUtils.isNullOrEmpty(headerValue))
                accProps.put(AUTO_ANSWER_COND_VALUE_PROP, headerValue);
        }
        else if(!StringUtils.isNullOrEmpty(callFwdTo))
        {
            accProps.put(AUTO_ANSWER_FWD_NUM_PROP, callFwdTo);
        }
        accProps.put(
                AUTO_ANSWER_WITH_VIDEO_PROP,
                Boolean.toString(answerWithVideo));

        acc.setAccountProperties(accProps);
        SipActivator.getProtocolProviderFactory().storeAccount(acc);
    }

    /**
     * Sets a specified header and its value if they exist in the incoming
     * call packet this will activate auto answer.
     * If value is empty or null it will be considered as any (will search
     * only for a header with that name and ignore the value)
     *
     * @param headerName the name of the header to search
     * @param value the value for the header, can be null.
     */
    public void setAutoAnswerCondition(String headerName, String value)
    {
        clearLocal();

        this.answerConditional = true;
        this.headerName = headerName;
        this.headerValue = value;

        save();
    }

    /**
     * Is the auto answer option set to conditionally
     * answer all incoming calls.
     *
     * @return is auto answer set to conditional.
     */
    public boolean isAutoAnswerConditionSet()
    {
        return answerConditional;
    }

    /**
     * Set to automatically forward all calls to the specified
     * number using the same provider.
     *
     * @param numberTo number to use for forwarding
     */
    public void setCallForward(String numberTo)
    {
        clearLocal();

        this.callFwdTo = numberTo;

        save();
    }

    /**
     * Get the value for automatically forward all calls to the specified
     * number using the same provider.
     *
     * @return numberTo number to use for forwarding
     */
    public String getCallForward()
    {
        return this.callFwdTo;
    }

    /**
     * Clear local settings.
     */
    @Override
    protected void clearLocal()
    {
        super.clearLocal();

        this.answerConditional = false;
        this.headerName = null;
        this.headerValue = null;
        this.callFwdTo = null;
    }

    /**
     * Returns the name of the header if conditional auto answer is set.
     *
     * @return the name of the header if conditional auto answer is set.
     */
    public String getAutoAnswerHeaderName()
    {
        return headerName;
    }

    /**
     * Returns the value of the header for the conditional auto answer.
     *
     * @return the value of the header for the conditional auto answer.
     */
    public String getAutoAnswerHeaderValue()
    {
        return headerValue;
    }

    /**
     * Makes a check before locally creating call, should we just forward it.
     *
     * @param invite the current invite to check.
     * @param serverTransaction the transaction.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean forwardCall(
            Request invite,
            ServerTransaction serverTransaction)
    {
        if(StringUtils.isNullOrEmpty(callFwdTo))
            return false;

        Response response;
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("will send moved temporally response: ");

            response = ((ProtocolProviderServiceSipImpl) protocolProvider)
                .getMessageFactory()
                .createResponse(Response.MOVED_TEMPORARILY, invite);

            ContactHeader contactHeader =
                (ContactHeader)response.getHeader(ContactHeader.NAME);
            AddressFactory addressFactory =
                ((ProtocolProviderServiceSipImpl) protocolProvider)
                .getAddressFactory();

            String destination = getCallForward();
            if(!destination.startsWith("sip"))
                destination = "sip:" + destination;

            contactHeader.setAddress(addressFactory.createAddress(
                addressFactory.createURI(destination)));

            serverTransaction.sendResponse(response);
            if (logger.isDebugEnabled())
                logger.debug("sent a moved temporally response: "
                    + response);
        }
        catch (Throwable ex)
        {
            logger.error("Error while trying to send a request", ex);
            return false;
        }

        return true;
    }

    /**
     * Checks if the call satisfy the auto answer conditions.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return <tt>true</tt> if the call satisfy the auto answer conditions.
     * <tt>False</tt> otherwise.
     */
    @Override
    protected boolean satisfyAutoAnswerConditions(Call call)
    {
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        CallPeer peer;

        /*
         * Check if the specific Call should be auto-answered.
         */
        if (call.isAutoAnswer())
            return true;

        // lets check for headers
        if(answerConditional)
        {
            while(peers.hasNext())
            {
                peer = peers.next();
                Transaction transaction = ((CallPeerSipImpl) peer)
                    .getLatestInviteTransaction();
                if(transaction != null)
                {
                    Request invite = transaction.getRequest();
                    SIPHeader callAnswerHeader = (SIPHeader)
                        invite.getHeader(headerName);

                    if(callAnswerHeader != null)
                    {
                        if(!StringUtils.isNullOrEmpty(headerValue))
                        {
                            String value = callAnswerHeader.getHeaderValue();

                            if(value != null && headerValue.equals(value))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Makes a check after creating call locally, should we answer it.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean autoAnswer(Call call)
    {
        if(answerUnconditional || satisfyAutoAnswerConditions(call))
        {
            boolean isVideoCall = doesRequestContainsActiveVideoMediaType(call);
            this.answerCall(call, isVideoCall);
            return true;
        }
        return false;
    }

    /**
     * Detects if the incoming call is a video call. Parses the SDP from the SIP
     * request to determine if the video is active.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return True if the incoming call is a video call. False, otherwise.
     */
    private boolean doesRequestContainsActiveVideoMediaType(Call call)
    {
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        while(peers.hasNext())
        {
            Transaction transaction = ((CallPeerSipImpl) peers.next())
                .getLatestInviteTransaction();
            if(transaction != null)
            {
                Request inviteReq = transaction.getRequest();

                if(inviteReq != null && inviteReq.getRawContent() != null)
                {
                    String sdpStr = SdpUtils.getContentAsString(inviteReq);
                    SessionDescription sesDescr
                        = SdpUtils.parseSdpString(sdpStr);
                    List<MediaDescription> remoteDescriptions
                        = SdpUtils.extractMediaDescriptions(sesDescr);

                    for (MediaDescription mediaDescription :
                            remoteDescriptions)
                    {
                        if(SdpUtils.getMediaType(mediaDescription)
                                == MediaType.VIDEO)
                        {
                            if(SdpUtils.getDirection(mediaDescription)
                                    == MediaDirection.SENDRECV)
                            {
                                return true;
                            }

                        }
                    }
                }
            }
        }

        return false;
    }
}
