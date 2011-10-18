/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

import java.util.*;

import javax.media.*;
import javax.media.control.*;

/**
 * Provides an abstract implementation of <tt>FormatControl</tt> which
 * facilitates implementers by requiring them to implement just
 * {@link FormatControl#getSupportedFormats()} and
 * {@link FormatControl#getFormat()}.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractFormatControl
    implements FormatControl
{

    /**
     * The indicator which determines whether this track is enabled.
     */
    private boolean enabled;

    /**
     * Implements {@link Control#getControlComponent()}. Returns <tt>null</tt>.
     *
     * @return a <tt>Component</tt> which represents UI associated with this
     * instance if any; otherwise, <tt>null</tt>
     */
    public java.awt.Component getControlComponent()
    {
        // No Component is exported by this instance.
        return null;
    }

    /**
     * Gets an array of <tt>FormatControl</tt> instances from the list of
     * controls available for a specific <tt>Controls</tt> implementation.
     *
     * @param controlsImpl the <tt>Controls</tt> implementation from which the
     * <tt>FormatControl</tt> instances are to be retrieved
     * @return an array of <tt>FormatControl</tt> instances from the list of
     * controls available for the specified <tt>Controls</tt> implementation
     */
    public static FormatControl[] getFormatControls(Controls controlsImpl)
    {
        List<FormatControl> formatControls = new ArrayList<FormatControl>();

        for (Object control : controlsImpl.getControls())
            if (control instanceof FormatControl)
                formatControls.add((FormatControl) control);
        return formatControls.toArray(new FormatControl[formatControls.size()]);
    }

    /**
     * Implements {@link FormatControl#isEnabled()}.
     *
     * @return <tt>true</tt> if this track is enabled; otherwise, <tt>false</tt>
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Implements {@link FormatControl#setEnabled(boolean)}.
     *
     * @param enabled <tt>true</tt> if this track is to be enabled; otherwise,
     * <tt>false</tt>
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Implements {@link FormatControl#setFormat(Format)}. Not supported and
     * just returns the currently set format if the specified <tt>Format</tt> is
     * supported and <tt>null</tt> if it is not supported.
     *
     * @param format the <tt>Format</tt> to be set on this instance
     * @return the currently set <tt>Format</tt> after the attempt to set it on
     * this instance if <tt>format</tt> is supported by this instance and
     * regardless of whether it was actually set; <tt>null</tt> if
     * <tt>format</tt> is not supported by this instance
     */
    public Format setFormat(Format format)
    {
        return setFormat(this, format);
    }

    /**
     * Implements setting the <tt>Format</tt> of a specific
     * <tt>FormatControl</tt> as documented for
     * {@link FormatControl#setFormat(Format)} in the case of not supporting
     * <tt>Format</tt> setting.
     *
     * @param formatControl the <tt>FormatControl</tt> for which the
     * functionality is implemented
     * @param format the <tt>Format</tt> specified to be set to
     * <tt>formatControl</tt> and which will be ignored in accord with the
     * documentation of <tt>FormatControl#setFormat(Format)</tt>
     * @return the currently set <tt>Format</tt> of <tt>formatControl</tt> if
     * <tt>format</tt> is supported by <tt>formatControl</tt>; <tt>null</tt> if
     * <tt>format</tt> is not supported by <tt>formatControl</tt>
     */
    public static Format setFormat(FormatControl formatControl, Format format)
    {
        /*
         * Determine whether the specified format is supported by this instance
         * because we have to return null if it is not supported. Or at least
         * that is what I gather from the respective javadoc.
         */
        boolean formatIsSupported = false;

        if (format != null)
            for (Format supportedFormat : formatControl.getSupportedFormats())
                if (supportedFormat.matches(format))
                {
                    formatIsSupported = true;
                    break;
                }

        /*
         * We do not actually support setFormat so we have to return the
         * currently set format if the specified format is supported.
         */
        return (formatIsSupported) ? formatControl.getFormat() : null;
    }
}
