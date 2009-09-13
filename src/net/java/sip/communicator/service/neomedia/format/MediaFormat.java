/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.format;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * The <tt>MediaFormat</tt> interface represents a generic (i.e. audio/video or
 * other) format used to represent media represent a media stream.
 * <p>
 * The interface contains utility methods for extracting common media format
 * properties such as the name of the underlying encoding, or clock rate or in
 * order comparing to compare formats. Extending interfaces representing audio
 * or video formats are likely to add other methods.
 * <p>
 * @author Emil Ivov
 */
public interface MediaFormat
{
    /**
     * Returns the type of this <tt>MediaFormat</tt> (e.g. audio or video).
     *
     * @return the <tt>MediaType</tt> that this format represents (e.g. audio
     * or video).
     */
    public MediaType getMediaType();

    /**
     * Returns the name of the encoding (i.e. codec) used by this
     * <tt>MediaFormat</tt>.
     *
     * @return The name of the encoding that this <tt>MediaFormat</tt> is using.
     */
    public String getEncoding();

    /**
     * Returns the clock rate associated with this <tt>MediaFormat</tt>.
     *
     * @return The clock rate associated with this format..
     */
    public float getClockRate();

    /**
     * Determines whether this <tt>MediaFormat</tt> is equal to
     * <tt>mediaFormat</tt>, or in other words that they have the same encoding
     * and attributes (e.g. refreshRate, channels, etc.).
     *
     * @param mediaFormat The <tt>MediaFormat</tt> that we would like to compare
     * with the current one.
     * @return <tt>true</tt> if <tt>mediaFormat</tt> is equal to this format and
     * <tt>false</tt> otherwise.
     */
    public boolean equals(Object mediaFormat);

    /**
     * Returns a <tt>Map</tt> containing parameters specific to this
     * particular <tt>MediaFormat</tt>. The parameters returned here are meant
     * for use in SIP/SDP or XMPP session descriptions where they get
     * transported through the "fmtp:" attribute or <parameter/> tag
     * respectively.
     *
     * @return Returns a <tt>Map</tt> containing parameters specific to this
     * particular <tt>MediaFormat</tt>.
     */
    public Map<String, String> getFormatParameters();

    /**
     * Returns a <tt>String</tt> representation of this <tt>MediaFormat</tt>
     * containing important format attributes such as the encoding for example.
     *
     * @return a <tt>String</tt> representation of this <tt>MediaFormat</tt>.
     */
    public String toString();
}
