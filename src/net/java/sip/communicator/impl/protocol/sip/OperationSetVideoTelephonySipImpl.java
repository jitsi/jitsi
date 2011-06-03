/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;

import javax.sip.address.Address;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

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
 * @author Lubomir Marinov
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
    public Call createVideoCall(String uri, QualityPresets qualityPreferences)
        throws OperationFailedException, ParseException
    {
        Address toAddress = parentProvider.parseAddressString(uri);

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
    public Call createVideoCall(Contact callee,
                                QualityPresets qualityPreferences)
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

        CallSipImpl call = basicTelephony.createOutgoingCall();
        call.setLocalVideoAllowed(true, getMediaUseCase());
        call.setInitialQualityPreferences(qualityPreferences);
        call.invite(toAddress, null);

        return call;
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
    public QualityControls getQualityControls(CallPeer peer)
    {
        return ((CallPeerSipImpl) peer).getMediaHandler().getQualityControls();
    }
}
