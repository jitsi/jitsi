/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

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
     * Implements {@link Controls#getControlComponent()}. Returns <tt>null</tt>.
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
        /*
         * Determine whether the specified format is supported by this instance
         * because we have to return null if it is not supported. Or at least
         * that is what I gather from the respective javadoc.
         */
        boolean formatIsSupported = false;

        if (format != null)
            for (Format supportedFormat : getSupportedFormats())
                if (supportedFormat.matches(format))
                {
                    formatIsSupported = true;
                    break;
                }

        /*
         * We do not actually support setFormat so we have to return the
         * currently set format if the specified format is supported.
         */
        return (formatIsSupported) ? getFormat() : null;
    }
}
