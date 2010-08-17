/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.format;

import java.awt.*;

/**
 * The interface represents a video format. Video formats characterize video
 * streams and the <tt>VideoMediaFormat</tt> interface gives access to some of
 * their properties such as encoding and clock rate.
 *
 * @author Emil Ivov
 */
public interface VideoMediaFormat
    extends MediaFormat
{
    /**
     * Returns the size of the image that this <tt>VideoMediaFormat</tt>
     * describes.
     *
     * @return a <tt>java.awt.Dimension</tt> instance indicating the image size
     * (in pixels) of this <tt>VideoMediaFormat</tt>.
     */
    public Dimension getSize();
}
