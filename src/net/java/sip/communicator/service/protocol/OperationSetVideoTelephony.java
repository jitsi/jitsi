/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an <code>OperationSet</code> giving access to video-specific
 * functionality in telephony such as visual <code>Component</code>s displaying
 * video and listening to dynamic availability of such <code>Component</code>s.
 *
 * @author Lubomir Marinov
 */
public interface OperationSetVideoTelephony
    extends OperationSet
{

    /**
     * Adds a specific <code>VideoListener</code> to this telephony in order to
     * receive notifications when visual/video <code>Component</code>s are being
     * added and removed for a specific <code>CallPeer</code>.
     *
     * @param peer the <code>CallPeer</code> whose video the
     *            specified listener is to be notified about
     * @param listener the <code>VideoListener</code> to be notified when
     *            visual/video <code>Component</code>s are being added or
     *            removed for <code>peer</code>
     */
    public void addVideoListener( CallPeer peer, VideoListener listener);

    /**
     * Creates a visual <code>Component</code> which depicts the local video
     * being streamed to a specific <code>CallPeer</code>. The returned
     * visual <code>Component</code> should be disposed when it is no longer
     * required through {@link #disposeLocalVisualComponent(CallPeer, Component) disposeLocalVisualComponent}.
     *
     * @param peer the <code>CallPeer</code> to whom the local
     *            video which is to be depicted by the returned visual
     *            <code>Component</code> is being streamed
     * @param listener if not <tt>null</tt>, a <code>VideoListener</code> to
     *            track the progress of the creation in case this telephony
     *            chooses to perform it asynchronously and to not return the
     *            created visual <code>Component</code> immediately/as the
     *            result of this method call
     * @return a visual <code>Component</code> which depicts the local video
     *         being streamed to the specified <code>CallPeer</code> if
     *         this telephony chooses to carry out the creation synchronously;
     *         <tt>null</tt> if this telephony chooses to create the requested
     *         visual <code>Component</code> asynchronously.
     */
    public Component createLocalVisualComponent(
            CallPeer peer, VideoListener listener)
        throws OperationFailedException;

    /**
     * Disposes of a visual <code>Component</code> depicting the local video for
     * a specific <code>CallPeer</code> (previously obtained through
     * {@link #createLocalVisualComponent(CallPeer, VideoListener) createLocalVisualComponent}).
     * The disposal may include, but is not limited to, releasing the
     * <code>Player</code> which provides the <code>component</code> and renders
     * the local video into it, disconnecting from the video capture device.
     *
     * @param peer the <code>CallPeer</code> for whom the visual
     *            <code>Component</code> depicts the local video
     * @param component the visual <code>Component</code> depicting the local
     *            video to be disposed
     */
    public void disposeLocalVisualComponent(
            CallPeer peer, Component component);

    /**
     * Gets the visual/video <code>Component</code>s available in this telephony
     * for a specific <code>CallPeer</code>.
     *
     * @param peer the <code>CallPeer</code> whose videos are to
     *            be retrieved
     * @return an array of the visual <code>Component</code>s available in this
     *         telephony for the specified <code>peer</code>
     */
    public Component[] getVisualComponents(CallPeer peer);

    /**
     * Removes a specific <code>VideoListener</code> from this telephony in
     * order to no longer have it receive notifications when visual/video
     * <code>Component</code>s are being added and removed for a specific
     * <code>CallPeer</code>.
     *
     * @param peer the <code>CallPeer</code> whose video the
     *            specified listener is to no longer be notified about
     * @param listener the <code>VideoListener</code> to no longer be notified
     *            when visual/video <code>Component</code>s are being added or
     *            removed for <code>peer</code>
     */
    public void removeVideoListener(
            CallPeer peer, VideoListener listener);

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in a specific <code>Call</code> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <code>Call</code> to allow/disallow the streaming of
     *            local video for
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     *            the specified <code>Call</code>; <tt>false</tt> to disallow it
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException;

    /**
     * Gets the indicator which determines whether the streaming of local video
     * in a specific <code>Call</code> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <code>Call</code> to get the indicator of
     * @return <tt>true</tt> if the streaming of local video for the specified
     *         <code>Call</code> is allowed; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoAllowed(Call call);

    /**
     * The property which indicates whether a specific <code>Call</code> is
     * currently streaming the local video (to a remote destination).
     */
    public static final String LOCAL_VIDEO_STREAMING
        = CallSession.LOCAL_VIDEO_STREAMING;

    /**
     * Gets the indicator which determines whether a specific <code>Call</code>
     * is currently streaming the local video (to a remote destination).
     *
     * @param call the <code>Call</code> to get the indicator of
     * @return <tt>true</tt> if the specified <code>Call</code> is currently
     *         streaming the local video (to a remote destination); otherwise,
     *         <tt>false</tt>
     */
    public boolean isLocalVideoStreaming(Call call);

    /**
     * Adds a specific <code>PropertyChangeListener</code> to the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <code>Call</code> change their values.
     *
     * @param call the <code>Call</code> to start listening to the changes of
     *            the property values of
     * @param listener the <code>PropertyChangeListener</code> to be notified
     *            when the properties associated with the specified
     *            <code>Call</code> change their values
     */
    public void addPropertyChangeListener(
            Call call, PropertyChangeListener listener);

    /**
     * Removes a specific <code>PropertyChangeListener</code> from the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <code>Call</code> change their values.
     *
     * @param call the <code>Call</code> to stop listening to the changes of the
     *            property values of
     * @param listener the <code>PropertyChangeListener</code> to no longer be
     *            notified when the properties associated with the specified
     *            <code>Call</code> change their values
     */
    public void removePropertyChangeListener(
            Call call, PropertyChangeListener listener);
}
