/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.neomedia.control.*;
import net.java.sip.communicator.util.event.*;

/**
 * Extends the <tt>MediaStream</tt> interface and adds methods specific to
 * video streaming.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface VideoMediaStream
    extends MediaStream
{
    /**
     * Gets local visual <tt>Component</tt> of the local peer.
     *
     * @param flipLocalVideoDisplay Tells to flip the local video display (true,
     * in case of diplaying the webcam as a mirror), or not (false, for desktop
     * streaming).
     *
     * @return visual <tt>Component</tt>
     */
    public Component createLocalVisualComponent(boolean flipLocalVideoDisplay);

    /**
     * Disposes of a specific local visual <tt>Component</tt> of the local peer.
     *
     * @param component the local visual <tt>Component</tt> of the local peer to
     * dispose of
     */
    public void disposeLocalVisualComponent(Component component);

    /**
     * Gets the visual <tt>Component</tt> where video from the remote peer is
     * being rendered or <tt>null</tt> if no video is currently being rendered.
     *
     * @return the visual <tt>Component</tt> where video from the remote peer is
     * being rendered or <tt>null</tt> if no video is currently being rendered
     */
    @Deprecated
    public Component getVisualComponent();

    /**
     * Gets a list of the visual <tt>Component</tt>s where video from the remote
     * peer is being rendered.
     *
     * @return a list of the visual <tt>Component</tt>s where video from the
     * remote peer is being rendered
     */
    public List<Component> getVisualComponents();

    /**
     * Gets the visual <tt>Component</tt>s rendering the <tt>ReceiveStream</tt>
     * corresponding to the given ssrc.
     *
     * @param ssrc the src-id of the receive stream, which visual
     * <tt>Component</tt> we're looking for
     * @return the visual <tt>Component</tt> rendering the
     * <tt>ReceiveStream</tt> corresponding to the given ssrc
     */
    public Component getVisualComponent(long ssrc);

    /**
     * Adds a specific <tt>VideoListener</tt> to this <tt>VideoMediaStream</tt>
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void addVideoListener(VideoListener listener);

    /**
     * Removes a specific <tt>VideoListener</tt> from this
     * <tt>VideoMediaStream</tt> in order to have to no longer receive
     * notifications when visual/video <tt>Component</tt>s are being added and
     * removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void removeVideoListener(VideoListener listener);

    /**
     * Gets the <tt>KeyFrameControl</tt> of this <tt>VideoMediaStream</tt>.
     *
     * @return the <tt>KeyFrameControl</tt> of this <tt>VideoMediaStream</tt>
     */
    public KeyFrameControl getKeyFrameControl();

    /**
     * Gets the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>.
     *
     * @return the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>
     */
    public QualityControl getQualityControl();

    /**
     * Updates the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>.
     *
     * @param advancedParams parameters of advanced attributes that may affect
     * quality control
     */
    public void updateQualityControl(
        Map<String, String> advancedParams);

    /**
     * Move origin of a partial desktop streaming <tt>MediaDevice</tt>.
     *
     * @param x new x coordinate origin
     * @param y new y coordinate origin
     */
    public void movePartialDesktopStreaming(
            int x, int y);
}
