/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;
import java.text.*;

import javax.sip.address.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;

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
        Address toAddress = parentProvider.parseAddressString(uri);

        CallSipImpl call = basicTelephony.createOutgoingCall();
        call.setVideoDevice(mediaDevice);
        call.setLocalVideoAllowed(true, getMediaUseCase());
        call.invite(toAddress, null);
        return call;
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

        CallSipImpl call = basicTelephony.createOutgoingCall();
        call.setLocalVideoAllowed(true, getMediaUseCase());
        call.setVideoDevice(mediaDevice);
        call.invite(toAddress, null);

        return call;
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
        Call call = super.createVideoCall(uri);
        size = (((VideoMediaFormat)((CallSipImpl)call).
                getDefaultDevice(MediaType.VIDEO).
                getFormat()).getSize());
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
        Call call = super.createVideoCall(callee);
        size = (((VideoMediaFormat)((CallSipImpl)call).
                getDefaultDevice(MediaType.VIDEO).
                getFormat()).getSize());
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
        ((CallSipImpl)call).setLocalVideoAllowed(allowed, MediaUseCase.DESKTOP);

        size = (((VideoMediaFormat)((CallSipImpl)call).
                getDefaultDevice(MediaType.VIDEO).
                getFormat()).getSize());

        /* reinvite all peers */
        ((CallSipImpl)call).reInvite();
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
        ((CallSipImpl)call).setVideoDevice(mediaDevice);
        size = (((VideoMediaFormat)((CallSipImpl)call).
                getDefaultDevice(MediaType.VIDEO).
                getFormat()).getSize());
        super.setLocalVideoAllowed(call, allowed);
    }
}
