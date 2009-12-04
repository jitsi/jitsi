/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

import javax.media.control.*;

/**
 * Represents a wrapper of a specific <tt>BufferControl</tt> which does not call
 * the setters of the wrapped instance and calls only the getters.
 *
 * @author Lubomir Marinov
 */
public class ReadOnlyBufferControlDelegate
        implements BufferControl
{

    /**
     * The <tt>BufferControl</tt> wrapped by this instance.
     */
    private final BufferControl bufferControl;

    /**
     * Initializes a new <tt>ReadOnlyBufferControlDelegate</tt> instance which
     * is to wrap a specific <tt>BufferControl</tt> and call only its getters.
     *
     * @param bufferControl the <tt>BufferControl</tt> to be wrapped by the new
     * instance
     */
    public ReadOnlyBufferControlDelegate(BufferControl bufferControl)
    {
        this.bufferControl = bufferControl;
    }

    /**
     * Implements {@link BufferControl#getBufferLength()}. Gets the length in
     * milliseconds of the buffering performed by the owner of the wrapped
     * <tt>BufferControl</tt>.
     *
     * @return the length in milliseconds of the buffering performed by the
     * owner of the wrapped <tt>BufferControl</tt>
     */
    public long getBufferLength()
    {
        return bufferControl.getBufferLength();
    }

    /**
     * Implements {@link Control#getControlComponent()}. Gets the UI
     * <tt>Component</tt> representing this instance and exported by the owner
     * of the wrapped <tt>BufferControl</tt>.
     *
     * @return the UI <tt>Component</tt> representing the wrapped
     * <tt>BufferControl</tt> and exported by its owner if such a
     * <tt>Component</tt> is available; otherwise, <tt>null</tt>
     */
    public java.awt.Component getControlComponent()
    {
        return bufferControl.getControlComponent();
    }

    /**
     * Implements {@link BufferControl#getEnabledThreshold()}. Gets the
     * indicator of the wrapped <tt>BufferControl</tt> which determines whether
     * threshold calculations are enabled.
     *
     * @return <tt>true</tt> if threshold calculations are enabled in the
     * wrapped <tt>BufferControl</tt>; otherwise, <tt>false</tt>
     */
    public boolean getEnabledThreshold()
    {
        return bufferControl.getEnabledThreshold();
    }

    /**
     * Implements {@link BufferControl#getMinimumThreshold()}. Gets the minimum
     * threshold in milliseconds for the buffering performed by the owner of the
     * wrapped <tt>BufferControl</tt>.
     *
     * @return the minimum threshold in milliseconds for the buffering performed
     * by the owner of the wrapped <tt>BufferControl</tt>
     */
    public long getMinimumThreshold()
    {
        return bufferControl.getMinimumThreshold();
    }

    /**
     * Implements {@link BufferControl#setBufferLength(long)}. Ignores the
     * request because this instance provides read-only support and returns the
     * value actually in effect.
     *
     * @param bufferLength the length in milliseconds of the buffering to be
     * performed by the owner of the wrapped <tt>BufferControl</tt>
     * @return the length in milliseconds of the buffering performed by the
     * owner of the wrapped <tt>BufferControl</tt> that is actually in effect
     */
    public long setBufferLength(long bufferLength)
    {
        return getBufferLength();
    }

    /**
     * Implements {@link BufferControl#setEnabledThreshold(boolean)}. Ignores
     * the set request because this instance provides read-only support.
     *
     * @param enabledThreshold <tt>true</tt> if threshold calculations are
     * to be enabled; otherwise, <tt>false</tt>
     */
    public void setEnabledThreshold(boolean enabledThreshold)
    {
    }

    /**
     * Implements {@link BufferControl#setMinimumThreshold(long)}. Ignores the
     * set request because this instance provides read-only support and returns
     * the value actually in effect.
     *
     * @param minimumThreshold the minimum threshold in milliseconds for the
     * buffering to be performed by the owner of the wrapped
     * <tt>BufferControl</tt>
     * @return the minimum threshold in milliseconds for the buffering performed
     * by the owner of the wrapped <tt>BufferControl</tt> that is actually in
     * effect
     */
    public long setMinimumThreshold(long minimumThreshold)
    {
        return getMinimumThreshold();
    }
}
