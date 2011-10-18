/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Abstract base implementation of <tt>MediaStream</tt> to ease the
 * implementation of the interface.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractMediaStream
    implements MediaStream
{
    /**
     * The name of this stream, that some protocols may use for diagnostic
     * purposes.
     */
    private String streamName;

    /**
     * The delegate of this instance which implements support for property
     * change notifications for its
     * {@link #addPropertyChangeListener(PropertyChangeListener)} and
     * {@link #removePropertyChangeListener(PropertyChangeListener)}.
     */
    private final PropertyChangeSupport propertyChangeSupport
        = new PropertyChangeSupport(this);

    /**
     * Adds a <tt>PropertyChangelistener</tt> to this stream which is to be
     * notified upon property changes such as a SSRC ID which becomes known.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to register for
     * <tt>PropertyChangeEvent</tt>s
     * @see MediaStream#addPropertyChangeListener(PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this instance in order
     * to notify about a change in the value of a specific property which had
     * its old value modified to a specific new value.
     *
     * @param property the name of the property of this instance which had its
     * value changed
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
     */
    protected void firePropertyChange(
        String property,
        Object oldValue,
        Object newValue)
    {
        propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
    }

    /**
     * Determines whether a specific <tt>MediaFormat</tt> has specific values
     * for its properties <tt>mediaType</tt>, <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt> for <tt>MediaFormat</tt>s with
     * <tt>mediaType</tt> equal to {@link MediaType#AUDIO}.
     *
     * @param format the {@link MediaFormat} whose properties we'd like to
     * examine
     * @param mediaType the type we expect {@link MediaFormat} to have
     * @param encoding the encoding we are looking for.
     * @param clockRate the clock rate that we'd like the format to have.
     * @param channels the number of channels that expect to find in this format
     * @param formatParameters the format parameters expected to match these of
     * the specified <tt>format</tt>
     * @return <tt>true</tt> if the specified <tt>format</tt> has specific
     * values for its properties <tt>mediaType</tt>, <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt>; otherwise, <tt>false</tt>
     */
    public static boolean matches(
            MediaFormat format,
            MediaType mediaType,
            String encoding, double clockRate,  int channels,
            Map<String, String> formatParameters)
    {
        // mediaType
        // encoding
        if (!format.getMediaType().equals(mediaType)
                || !format.getEncoding().equals(encoding))
            return false;

        // clockRate
        if (clockRate != MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED)
        {
            double formatClockRate = format.getClockRate();

            if ((formatClockRate != MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED)
                    && (formatClockRate != clockRate))
                return false;
        }

        // channels
        if (MediaType.AUDIO.equals(mediaType))
        {
            if (channels == MediaFormatFactory.CHANNELS_NOT_SPECIFIED)
                channels = 1;

            int formatChannels = ((AudioMediaFormat) format).getChannels();

            if (formatChannels == MediaFormatFactory.CHANNELS_NOT_SPECIFIED)
                formatChannels = 1;
            if (formatChannels != channels)
                return false;
        }

        // formatParameters
        return format.formatParametersMatch(formatParameters);
    }

    /**
     * Returns the name of this stream or <tt>null</tt> if no name has been
     * set. A stream name is used by some protocols, for diagnostic purposes
     * mostly. In XMPP for example this is the name of the content element that
     * describes a stream.
     *
     * @return the name of this stream or <tt>null</tt> if no name has been
     * set.
     */
    public String getName()
    {
        return streamName;
    }

    /**
     * Sets the name of this stream. Stream names are used by some protocols,
     * for diagnostic purposes mostly. In XMPP for example this is the name of
     * the content element that describes a stream.
     *
     * @param streamName the name of this stream or <tt>null</tt> if no name has
     * been set.
     */
    public void setName(String streamName)
    {
        this.streamName = streamName;
    }

    /**
     * Removes the specified <tt>PropertyChangeListener</tt> from this stream so
     * that it won't receive further property change events.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to remove
     * @see MediaStream#removePropertyChangeListener(PropertyChangeListener)
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
