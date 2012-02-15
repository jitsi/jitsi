/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.device;

/**
 * Represents a special-purpose <tt>MediaDevice</tt> which is effectively built
 * on top of and forwarding to another <tt>MediaDevice</tt>.
 *
 * @author Lyubomir Marinov
 */
public interface MediaDeviceWrapper
    extends MediaDevice
{
    /**
     * Gets the actual <tt>MediaDevice</tt> which this <tt>MediaDevice</tt> is
     * effectively built on top of and forwarding to.
     *
     * @return the actual <tt>MediaDevice</tt> which this <tt>MediaDevice</tt>
     * is effectively built on top of and forwarding to
     */
    public MediaDevice getWrappedDevice();
}
