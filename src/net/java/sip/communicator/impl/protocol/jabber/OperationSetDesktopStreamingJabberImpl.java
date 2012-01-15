/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.awt.*;
import java.text.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * Implements all desktop streaming related functions for XMPP.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopStreamingJabberImpl
    extends OperationSetVideoTelephonyJabberImpl
    implements OperationSetDesktopStreaming
{
    /**
     * Video panel size.
     */
    protected Dimension size = null;

    /**
     * Origin (x, y) of the zone streamed. This apply only in case of partial
     * desktop streaming.
     */
    protected Point origin = null;

    /**
     * Initializes a new <tt>OperationSetDesktopStreamingJabberImpl</tt>
     * instance which builds upon the telephony-related functionality of a
     * specific <tt>OperationSetBasicTelephonyJabberImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonyJabberImpl</tt>
     * the new extension should build upon
     */
    public OperationSetDesktopStreamingJabberImpl(
            OperationSetBasicTelephonyJabberImpl basicTelephony)
    {
        super(basicTelephony);
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
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid Jabber address
     * string.
     */
    public Call createVideoCall(String uri, MediaDevice mediaDevice)
        throws OperationFailedException, ParseException
    {
        return createOutgoingVideoCall(uri, mediaDevice);
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
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    public Call createVideoCall(Contact callee, MediaDevice mediaDevice)
        throws OperationFailedException
    {
        return createOutgoingVideoCall(callee.getAddress(), mediaDevice);
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
     */
    @Override
    public Call createVideoCall(String uri)
        throws OperationFailedException
    {
        Call call = createOutgoingVideoCall(uri);
        MediaDevice device = ((CallJabberImpl)call).getDefaultDevice(
                MediaType.VIDEO);
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
        Call call = createOutgoingVideoCall(callee.getAddress());
        MediaDevice device = ((CallJabberImpl)call).getDefaultDevice(
                MediaType.VIDEO);
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
     *  @throws OperationFailedException if video initialization fails.
     */
    @Override
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        setLocalVideoAllowed(call, null, allowed);
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
     * @param mediaDevice the media device to use for the desktop streaming.
     * If the device is null, the default device is used.
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
        if(mediaDevice == null)
            mediaDevice = ((CallJabberImpl)call).getDefaultDevice(
                MediaType.VIDEO);

        ((CallJabberImpl)call).setLocalVideoAllowed(allowed,
                MediaUseCase.DESKTOP);
        ((CallJabberImpl)call).setVideoDevice(mediaDevice);
        if(((VideoMediaFormat)mediaDevice.getFormat()) != null)
            size = ((VideoMediaFormat)mediaDevice.getFormat()).getSize();
        else
            size = null;
        ((CallJabberImpl)call).modifyVideoContent(allowed);
        origin = getOriginForMediaDevice(mediaDevice);
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
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((MediaAwareCall<?, ?, ?>)call).
            isLocalVideoAllowed(MediaUseCase.DESKTOP);
    }

    /**
     * Check if the remote part supports Jingle video.
     *
     * @param calleeAddress Contact address
     * @param videoDevice <tt>MediaDevice</tt> used
     * @return true if contact support Jingle video, false otherwise
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    protected Call createOutgoingVideoCall(String calleeAddress,
                                           MediaDevice videoDevice)
        throws OperationFailedException
    {
        if (parentProvider.getConnection() == null)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "we don't have a valid XMPPConnection."
                    , OperationFailedException.INTERNAL_ERROR);
        }

        CallJabberImpl call = new CallJabberImpl(basicTelephony);

        /* enable video */
        call.setVideoDevice(videoDevice);
        call.setLocalVideoAllowed(true, getMediaUseCase());

        basicTelephony.createOutgoingCall(call, calleeAddress);
        origin = getOriginForMediaDevice(videoDevice);
        return call;
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
        CallJabberImpl callImpl = (CallJabberImpl)call;
        MediaDevice device = callImpl.getDefaultDevice(MediaType.VIDEO);

        if(device != null)
        {
            MediaService mediaService = JabberActivator.getMediaService();
            return mediaService.isPartialStreaming(device);
        }

        return false;
    }

    /**
     * Move origin of a partial desktop streaming.
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     * @param x new x coordinate origin
     * @param y new y coordinate origin
     */
    public void movePartialDesktopStreaming(Call call, int x,
            int y)
    {
        CallJabberImpl callImpl = (CallJabberImpl)call;
        MediaDevice device = callImpl.getDefaultDevice(MediaType.VIDEO);

        if(device != null)
        {
            MediaService mediaService = JabberActivator.getMediaService();
            mediaService.movePartialDesktopStreaming(device, x, y);

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
     * Get origin of the screen.
     *
     * @param device media device
     * @return origin
     */
    protected static Point getOriginForMediaDevice(MediaDevice device)
    {
        MediaService mediaService = JabberActivator.getMediaService();

        return mediaService.getOriginForDesktopStreamingDevice(device);
    }
}
