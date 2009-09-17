/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.awt.*;

import net.java.sip.communicator.service.protocol.event.*;


/**
 * Extends the <tt>MediaStream</tt> interface and adds methods specific to
 * video streaming.
 *
 * @author Emil Ivov
 */
public interface VideoMediaStream
    extends MediaStream
{
    /**
     * Returns a reference to the visual component where video from the remote
     * participant is being rendered or <tt>null</tt> if no video is currently
     * rendered.
     *
     * @return a reference to the visual <tt>Component</tt> where video from
     * the remote party is being rendered or <tt>null</tt> if no video is
     * currently rendered.
     */
    public Component getVisualComponent();

    /**
     * Adds a specific <tt>VideoListener</tt> to this <tt>VideoMediaStream</tt>
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being added and removed.
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>CallSession</tt>
     */
    public void addVideoListener(VideoListener listener);
}
