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

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;

/**
 * Implements <tt>OperationSetVideoTelephony</tt> in order to give access to
 * video-specific functionality in the SIP protocol implementation such as
 * visual <tt>Component</tt>s displaying video and listening to dynamic
 * availability of such <tt>Component</tt>s. Because the video in the SIP
 * protocol implementation is provided by the <tt>CallSession</tt>, this
 * <tt>OperationSetVideoTelephony</tt> just delegates to the
 * <tt>CallSession</tt> while hiding the <tt>CallSession</tt> as the
 * provider of the video and pretending this
 * <tt>OperationSetVideoTelephony</tt> is the provider because other
 * implementation may not provider their video through the
 * <tt>CallSession</tt>.
 *
 * @author Lyubomir Marinov
 */
public class OperationSetVideoTelephonySipImpl
    extends AbstractOperationSetVideoTelephony<
        OperationSetBasicTelephonySipImpl,
        ProtocolProviderServiceSipImpl,
        CallSipImpl,
        CallPeerSipImpl>
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetTelephonyConferencingSipImpl</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetVideoTelephonySipImpl.class);

    /**
     * Initializes a new <tt>OperationSetVideoTelephonySipImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonySipImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonySipImpl</tt>
     *            the new extension should build upon
     */
    public OperationSetVideoTelephonySipImpl(
        OperationSetBasicTelephonySipImpl basicTelephony)
    {
        super(basicTelephony);

        parentProvider.registerMethodProcessor(
            Request.INFO,
            new PictureFastUpdateMethodProcessor());
    }

    /**
     * Implements OperationSetVideoTelephony#setLocalVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     *
     * @param call the call where we'd like to allow sending local video.
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    @Override
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        super.setLocalVideoAllowed(call, allowed);

        /* reinvite all peers */
        ((CallSipImpl)call).reInvite();
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    public Call createVideoCall(String uri)
        throws OperationFailedException, ParseException
    {
        return createVideoCall(uri, null);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    public Call createVideoCall(Contact callee)
        throws OperationFailedException
    {
        return createVideoCall(callee, null);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    @Override
    public Call createVideoCall(String uri, QualityPreset qualityPreferences)
        throws OperationFailedException, ParseException
    {
        return createVideoCall(
            parentProvider.parseAddressString(uri), qualityPreferences);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param toAddress the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    private Call createVideoCall(Address toAddress,
                                 QualityPreset qualityPreferences)
        throws OperationFailedException
    {
        CallSipImpl call = basicTelephony.createOutgoingCall();
        call.setLocalVideoAllowed(true, getMediaUseCase());
        call.setInitialQualityPreferences(qualityPreferences);
        call.invite(toAddress, null);

        return call;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    @Override
    public Call createVideoCall(Contact callee,
                                QualityPreset qualityPreferences)
        throws OperationFailedException
    {
        Address toAddress;

        try
        {
            toAddress = parentProvider.parseAddressString(callee.getAddress());
        }
        catch (ParseException ex)
        {
            // couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        return createVideoCall(toAddress, qualityPreferences);
    }

    /**
     * Indicates a user request to answer an incoming call with video enabled
     * from the specified CallPeer.
     *
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void answerVideoCallPeer(CallPeer peer)
        throws OperationFailedException
    {
        CallPeerSipImpl callPeer = (CallPeerSipImpl) peer;

        /* answer with video */
        callPeer.getCall().setLocalVideoAllowed(true, getMediaUseCase());
        callPeer.answer();
    }

    /**
     * Returns the quality control for video calls if any.
     * @param peer the peer which this control operates on.
     * @return the implemented quality control.
     */
    @Override
    public QualityControl getQualityControl(CallPeer peer)
    {
        return ((CallPeerSipImpl) peer).getMediaHandler().getQualityControl();
    }

    /**
     * Implements a <tt>MethodProcessor</tt> which processes
     * <tt>picture_fast_update</tt> <tt>Response</tt>s.
     *
     * @author Lyubomir Marinov
     */
    private class PictureFastUpdateMethodProcessor
        extends MethodProcessorAdapter
    {
        /**
         * Determines whether a specific <tt>Request</tt> is a
         * <tt>picture_fast_update</tt> one.
         *
         * @param request the <tt>Request</tt> to check
         * @return <tt>true</tt> if the specified <tt>request</tt> is a
         * <tt>picture_fast_update</tt> one; otherwise, <tt>false</tt>
         */
        private boolean isPictureFastUpdate(Request request)
        {
            ContentTypeHeader contentTypeHeader
                = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
            if (contentTypeHeader == null)
                return false;
            if (!"application".equalsIgnoreCase(
                    contentTypeHeader.getContentType()))
                return false;
            if (!CallPeerSipImpl.PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE
                    .equalsIgnoreCase(
                            contentTypeHeader.getContentSubType()))
                return false;

            Object content = request.getContent();
            if (content == null)
                return false;

            String xml;
            if (content instanceof String)
                xml = content.toString();
            else if (content instanceof byte[])
            {
                byte[] bytes = (byte[]) content;
                try
                {
                    xml = new String(bytes, "UTF-8");
                }
                catch (UnsupportedEncodingException uee)
                {
                    xml = new String(bytes);
                }
            }
            else
                return false;
            if (xml == null)
                return false;
            return xml.contains("picture_fast_update");
        }

        /**
         * Delivers <tt>picture_fast_update</tt> <tt>Request</tt>s to the
         * targeted <tt>CallPeerSipImpl</tt>.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean processRequest(RequestEvent requestEvent)
        {
            if (requestEvent == null)
                return false;

            Request request = requestEvent.getRequest();
            if (request == null)
                return false;
            if (!isPictureFastUpdate(request))
                return false;

            ServerTransaction serverTransaction;
            try
            {
                serverTransaction
                    = SipStackSharing.getOrCreateServerTransaction(
                            requestEvent);
            }
            catch (Exception e)
            {
                logger.error("Error creating server transaction" , e);
                return false;
            }
            if (serverTransaction == null)
                return false;

            CallPeerSipImpl callPeer
                = basicTelephony.getActiveCallsRepository().findCallPeer(
                    serverTransaction.getDialog());
            if (callPeer == null)
                return false;

            try
            {
                return
                    callPeer.processPictureFastUpdate(
                            serverTransaction,
                            request);
            }
            catch (OperationFailedException ofe)
            {
                return false;
            }
        }

        /**
         * Delivers <tt>Response</tt>s to <tt>picture_fast_update</tt>
         * <tt>Request</tt>s to the originating <tt>CallPeerSipImpl</tt>.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean processResponse(ResponseEvent responseEvent)
        {
            if (responseEvent == null)
                return false;

            Response response = responseEvent.getResponse();
            if (response == null)
                return false;

            ClientTransaction clientTransaction
                = responseEvent.getClientTransaction();
            if (clientTransaction == null)
                return false;

            Request request = clientTransaction.getRequest();
            if (request == null)
                return false;
            if (!isPictureFastUpdate(request))
                return false;

            CallPeerSipImpl callPeer
                = basicTelephony.getActiveCallsRepository().findCallPeer(
                        clientTransaction.getDialog());
            if (callPeer == null)
                return false;

            callPeer.processPictureFastUpdate(clientTransaction, response);
            return true;
        }
    }
}
