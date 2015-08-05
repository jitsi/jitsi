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

import java.awt.*;
import java.text.*;
import java.util.*;

import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.service.protocol.event.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;

/**
 * Implements all desktop streaming related functions for SIP.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopStreamingSipImpl
    extends OperationSetVideoTelephonySipImpl
    implements OperationSetDesktopStreaming
{
    /**
     * Dimension of the local desktop streamed.
     */
    protected Dimension size = null;

    /**
     * Origin (x, y) of the zone streamed. This apply only in case of partial
     * desktop streaming.
     */
    protected Point origin = null;

    /**
     * Whether handling desktop control out of dialog is enabled.
     */
    private boolean desktopControlOutOfDialogEnabled = false;

    /**
     * Initializes a new <tt>OperationSetDesktopStreamingSipImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonySipImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonySipImpl</tt> the
     * new extension should build upon
     */
    public OperationSetDesktopStreamingSipImpl(
            OperationSetBasicTelephonySipImpl basicTelephony)
    {
        super(basicTelephony);

        desktopControlOutOfDialogEnabled
            = SipActivator.getConfigurationService().getBoolean(
            DesktopSharingCallSipImpl
                .ENABLE_OUTOFDIALOG_DESKTOP_CONTROL_PROP,
            false);
    }

    /**
     * Get the <tt>MediaUseCase</tt> of a desktop streaming operation set.
     *
     * @return <tt>MediaUseCase.DESKTOP</tt>
     */
    @Override
    public MediaUseCase getMediaUseCase()
    {
        return MediaUseCase.DESKTOP;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param mediaDevice the media device to use for the desktop streaming
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
    public Call createVideoCall(String uri, MediaDevice mediaDevice)
        throws OperationFailedException, ParseException
    {
        return createVideoCall(
            parentProvider.parseAddressString(uri), mediaDevice);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param mediaDevice the media device to use for the desktop streaming
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    public Call createVideoCall(Contact callee, MediaDevice mediaDevice)
        throws OperationFailedException
    {
        Address toAddress;

        try
        {
            toAddress = parentProvider.parseAddressString(callee.getAddress());
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(ex.getMessage());
        }

        return createVideoCall(toAddress, mediaDevice);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param toAddress the address of the callee that we should invite to a new
     * call.
     * @param mediaDevice the media device to use for the desktop streaming
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    private Call createVideoCall(Address toAddress, MediaDevice mediaDevice)
        throws OperationFailedException
    {
        basicTelephony.assertRegistered();

        CallSipImpl call;
        if(desktopControlOutOfDialogEnabled)
        {
            call = new DesktopSharingCallSipImpl(basicTelephony)
            {
                @Override
                protected void processExtraHeaders(
                      javax.sip.message.Message message)
                    throws ParseException
                {
                    addDesktopShareHeader(message);
                }
            };
        }
        else
        {
            call = new CallSipImpl(basicTelephony)
            {
                @Override
                protected void processExtraHeaders(
                    javax.sip.message.Message message)
                    throws
                    ParseException
                {
                    addDesktopShareHeader(message);
                }
            };
        }

        MediaUseCase useCase = getMediaUseCase();

        call.setVideoDevice(mediaDevice, useCase);
        call.setLocalVideoAllowed(true, useCase);
        call.invite(toAddress, null);
        origin = getOriginForMediaDevice(mediaDevice);

        return call;
    }

    /**
     * A place where we can handle any headers we need for requests
     * and responses.
     * @param message the SIP <tt>Message</tt> in which a header change
     * is to be reflected
     * @throws java.text.ParseException if modifying the specified SIP
     * <tt>Message</tt> to reflect the header change fails
     */
    protected void addDesktopShareHeader(javax.sip.message.Message message)
        throws ParseException
    {
        Header customDesktopShareHeader = parentProvider.getHeaderFactory()
            .createHeader(CallSipImpl.DS_SHARING_HEADER, "true");
        message.setHeader(customDesktopShareHeader);
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
    @Override
    public Call createVideoCall(String uri)
        throws OperationFailedException, ParseException
    {
        CallSipImpl call = (CallSipImpl) super.createVideoCall(uri);
        MediaDevice device = call.getDefaultDevice(MediaType.VIDEO);

        size = (((VideoMediaFormat)device.getFormat()).getSize());
        origin = getOriginForMediaDevice(device);
        return call;
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
    @Override
    public Call createVideoCall(Contact callee) throws OperationFailedException
    {
        CallSipImpl call = (CallSipImpl) super.createVideoCall(callee);
        MediaDevice device = call.getDefaultDevice(MediaType.VIDEO);

        size = (((VideoMediaFormat)device.getFormat()).getSize());
        origin = getOriginForMediaDevice(device);
        return call;
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
     * @throws OperationFailedException if video initialization fails.
     */
    @Override
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        CallSipImpl callImpl = (CallSipImpl) call;
        MediaUseCase useCase = MediaUseCase.DESKTOP;

        callImpl.setLocalVideoAllowed(allowed, useCase);
        callImpl.setVideoDevice(null, useCase);

        MediaDevice device = callImpl.getDefaultDevice(MediaType.VIDEO);

        if(device.getFormat() != null)
            size = ((VideoMediaFormat)device.getFormat()).getSize();
        origin = getOriginForMediaDevice(device);

        /* reinvite all peers */
        callImpl.reInvite();
    }

    /**
     * Implements OperationSetVideoTelephony#isLocalVideoAllowed(Call). Modifies
     * the local media setup to reflect the requested setting for the streaming
     * of the local video.
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     *
     * @return <tt>true</tt> if the streaming of local video for the specified
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    @Override
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((CallSipImpl)call).isLocalVideoAllowed(MediaUseCase.DESKTOP);
    }

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to allow/disallow the streaming of local
     * video for
     * @param mediaDevice the media device to use for the desktop streaming
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     * the specified <tt>Call</tt>; <tt>false</tt> to disallow it
     *
     * @throws OperationFailedException if initializing local video fails.
     */
    public void setLocalVideoAllowed(Call call,
                                     MediaDevice mediaDevice,
                                     boolean allowed)
        throws OperationFailedException
    {
        CallSipImpl sipCall = (CallSipImpl) call;
        MediaUseCase useCase = MediaUseCase.DESKTOP;

        sipCall.setVideoDevice(mediaDevice, useCase);
        sipCall.setLocalVideoAllowed(allowed, useCase);
        size
            = (((VideoMediaFormat)
                        sipCall.getDefaultDevice(MediaType.VIDEO).getFormat())
                    .getSize());
        origin = getOriginForMediaDevice(mediaDevice);

        /* reinvite all peers */
        sipCall.reInvite();
    }

    /**
     * If the streaming is partial (not the full desktop).
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     * @return true if streaming is partial, false otherwise
     */
    public boolean isPartialStreaming(Call call)
    {
        CallSipImpl callImpl = (CallSipImpl)call;
        MediaDevice device = callImpl.getDefaultDevice(MediaType.VIDEO);

        return
            (device == null)
                ? false
                : SipActivator.getMediaService().isPartialStreaming(device);
    }

    /**
     * Move origin of a partial desktop streaming.
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     * @param x new x coordinate origin
     * @param y new y coordinate origin
     */
    public void movePartialDesktopStreaming(Call call, int x, int y)
    {
        CallSipImpl callImpl = (CallSipImpl)call;
        VideoMediaStream videoStream = (VideoMediaStream)
            callImpl.getCallPeers().next().getMediaHandler().getStream(
                MediaType.VIDEO);

        if(videoStream != null)
        {
            videoStream.movePartialDesktopStreaming(x, y);

            if(origin != null)
            {
                origin.x = x;
                origin.y = y;
            }
            else
            {
                origin = new Point(x, y);
            }
        }
    }

    /**
     * Get origin of streamed zone.
     *
     * @return origin of streamed zone or null if we stream the full desktop
     */
    public Point getOrigin()
    {
        return origin;
    }

    /**
     * Get origin of the screen.
     *
     * @param device media device
     * @return origin
     */
    protected static Point getOriginForMediaDevice(MediaDevice device)
    {
        return
            SipActivator.getMediaService().getOriginForDesktopStreamingDevice(
                    device);
    }
}
