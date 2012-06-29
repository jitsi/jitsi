/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.event.*;

/**
 * Represents an <tt>OperationSet</tt> giving access to video-specific
 * functionality in telephony such as visual <tt>Component</tt>s displaying
 * video and listening to dynamic availability of such <tt>Component</tt>s.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public interface OperationSetVideoTelephony
    extends OperationSet
{

    /**
     * Adds a specific <tt>VideoListener</tt> to this telephony in order to
     * receive notifications when visual/video <tt>Component</tt>s are being
     * added and removed for a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video the specified listener
     * is to be notified about
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed for
     * <tt>peer</tt>
     */
    public void addVideoListener( CallPeer peer, VideoListener listener);

    /**
     * Creates a visual <tt>Component</tt> which depicts the local video
     * being streamed to a specific <tt>CallPeer</tt>. The returned
     * visual <tt>Component</tt> should be disposed when it is no longer
     * required through {@link #disposeLocalVisualComponent(CallPeer,
     * Component) disposeLocalVisualComponent}.
     *
     * @param peer the <tt>CallPeer</tt> to whom the local video which is to be
     * depicted by the returned visual <tt>Component</tt> is being streamed
     * @param listener if not <tt>null</tt>, a <tt>VideoListener</tt> to track
     * the progress of the creation in case this telephony chooses to perform it
     * asynchronously and to not return the created visual <tt>Component</tt>
     * immediately/as the result of this method call
     *
     * @return a visual <tt>Component</tt> which depicts the local video being
     * streamed to the specified <tt>CallPeer</tt> if this telephony chooses to
     * carry out the creation synchronously; <tt>null</tt> if this telephony
     * chooses to create the requested visual <tt>Component</tt> asynchronously.
     *
     * @throws OperationFailedException if creating the component fails for
     * whatever reason.
     */
    public Component createLocalVisualComponent(CallPeer      peer,
                                                VideoListener listener)
        throws OperationFailedException;

    /**
     * Disposes of a visual <tt>Component</tt> depicting the local video for
     * a specific <tt>CallPeer</tt> (previously obtained through
     * {@link #createLocalVisualComponent(CallPeer, VideoListener)
     * createLocalVisualComponent}).
     * The disposal may include, but is not limited to, releasing the
     * <tt>Player</tt> which provides the <tt>component</tt> and renders
     * the local video into it, disconnecting from the video capture device.
     *
     * @param peer the <tt>CallPeer</tt> for whom the visual <tt>Component</tt>
     * depicts the local video
     * @param component the visual <tt>Component</tt> depicting the local video
     * to be disposed
     */
    public void disposeLocalVisualComponent(CallPeer peer, Component component);

    /**
     * Gets the visual/video <tt>Component</tt> available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video is to be retrieved
     * @return the visual/video <tt>Component</tt> available in this telephony
     * for the specified <tt>peer</tt> if any; otherwise, <tt>null</tt>
     */
    @Deprecated
    public Component getVisualComponent(CallPeer peer);

    /**
     * Gets the visual/video <tt>Component</tt>s available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose videos are to be retrieved
     * @return the visual/video <tt>Component</tt>s available in this telephony
     * for the specified <tt>peer</tt>
     */
    public List<Component> getVisualComponents(CallPeer peer);

    /**
     * Removes a specific <tt>VideoListener</tt> from this telephony in
     * order to no longer have it receive notifications when visual/video
     * <tt>Component</tt>s are being added and removed for a specific
     * <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video the specified listener is
     * to no longer be notified about
     * @param listener the <tt>VideoListener</tt> to no longer be notified
     * when visual/video <tt>Component</tt>s are being added or removed for
     * <tt>peer</tt>
     */
    public void removeVideoListener(CallPeer peer, VideoListener listener);

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to allow/disallow the streaming of local
     * video for
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     * the specified <tt>Call</tt>; <tt>false</tt> to disallow it
     *
     * @throws OperationFailedException if initializing local video fails.
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException;

    /**
     * Gets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to get the indicator of
     * @return <tt>true</tt> if the streaming of local video for the specified
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoAllowed(Call call);

    /**
     * The property which indicates whether a specific <tt>Call</tt> is
     * currently streaming the local video (to a remote destination).
     */
    public static final String LOCAL_VIDEO_STREAMING = "LOCAL_VIDEO_STREAMING";

    /**
     * Gets the indicator which determines whether a specific <tt>Call</tt>
     * is currently streaming the local video (to a remote destination).
     *
     * @param call the <tt>Call</tt> to get the indicator of
     * @return <tt>true</tt> if the specified <tt>Call</tt> is currently
     * streaming the local video (to a remote destination); otherwise,
     * <tt>false</tt>
     */
    public boolean isLocalVideoStreaming(Call call);

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to start listening to the changes of
     * the property values of
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with the specified <tt>Call</tt> change
     * their values
     */
    public void addPropertyChangeListener(Call                   call,
                                          PropertyChangeListener listener);

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to stop listening to the changes of the
     * property values of
     * @param listener the <tt>PropertyChangeListener</tt> to no longer be
     * notified when the properties associated with the specified <tt>Call</tt>
     * change their values
     */
    public void removePropertyChangeListener(Call                   call,
                                             PropertyChangeListener listener);

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
        throws OperationFailedException, ParseException;

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
        throws OperationFailedException;

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
    public Call createVideoCall(String uri, QualityPreset qualityPreferences)
        throws OperationFailedException, ParseException;

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
                                QualityPreset qualityPreferences)
        throws OperationFailedException;

    /**
     * Indicates a user request to answer an incoming call with video enabled
     * from the specified CallPeer.
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void answerVideoCallPeer(CallPeer peer)
        throws OperationFailedException;

    /**
     * Returns the quality control for video calls if any. It can be null if we
     * were able to successfully determine that other party does not support it.
     * @param peer the peer which this control operates on.
     * @return the implemented quality control.
     */
    public QualityControl getQualityControl(CallPeer peer);

    /**
     * Adds a specific <tt>VisualComponentResolveListener</tt> to this telephony
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being resolved to correspond to a particular
     * <tt>ConferenceMember</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt>, which visual components we're
     * listening to
     * @param listener the <tt>VisualComponentResolveListener</tt> to be
     * notified when visual/video <tt>Component</tt>s are being resolved to
     * correspond to a particular <tt>ConferenceMember</tt>.
     */
    public void addVisualComponentResolveListener(
        CallPeer callPeer,
        VisualComponentResolveListener listener);

    /**
     * Removes a <tt>VisualComponentResolveListener</tt> from this video
     * telephony operation set, which was previously added in order to receive
     * notifications when visual/video <tt>Component</tt>s are being resolved to
     * be corresponding to a particular <tt>ConferenceMember</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt>, which visual components we're
     * listening to
     * @param listener the <tt>VisualComponentResolveListener</tt> to be
     * removed
     */
    public void removeVisualComponentResolveListener(
        CallPeer callPeer,
        VisualComponentResolveListener listener);

    /**
     * Returns the <tt>ConferenceMember</tt> corresponding to the given
     * <tt>visualComponent</tt>.
     * 
     * @param peer the parent <tt>CallPeer</tt>
     * @param visualComponent the visual <tt>Component</tt>, which corresponding
     * <tt>ConferenceMember</tt> we're looking for
     * @return the <tt>ConferenceMember</tt> corresponding to the given
     * <tt>visualComponent</tt>.
     */
    public ConferenceMember getConferenceMember(CallPeer peer,
                                                Component visualComponent);
}
