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
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface MediaFormat
{

    /**
     * The constant returned by {@link #getRTPPayloadType()} when the
     * <tt>MediaFormat</tt> instance describes a format without an RTP payload
     * type (number) known in RFC 3551 "RTP Profile for Audio and Video
     * Conferences with Minimal Control".
     */
    public static final byte RTP_PAYLOAD_TYPE_UNKNOWN = -1;

    /**
     * The minimum integer that is allowed for use in dynamic payload type
     * assignment.
     */
    public static final int MIN_DYNAMIC_PAYLOAD_TYPE = 96;

    /**
     * The maximum integer that is allowed for use in dynamic payload type
     * assignment.
     */
    public static final int MAX_DYNAMIC_PAYLOAD_TYPE = 127;

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
     * @return The clock rate associated with this format.
     */
    public double getClockRate();

    /**
     * Returns a <tt>String</tt> representation of the clock rate associated
     * with this <tt>MediaFormat</tt> making sure that the value appears as
     * an integer (i.e. contains no decimal point) unless it is actually a non
     * integer.
     *
     * @return a <tt>String</tt> representation of the clock rate associated
     * with this <tt>MediaFormat</tt>.
     */
    public String getClockRateString();

    /**
     * Determines whether this <tt>MediaFormat</tt> is equal to
     * <tt>mediaFormat</tt> i.e. they have the same encoding, clock rate, format
     * parameters, advanced attributes, etc.
     *
     * @param mediaFormat the <tt>MediaFormat</tt> to compare to this instance
     * @return <tt>true</tt> if <tt>mediaFormat</tt> is equal to this format and
     * <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object mediaFormat);

    /**
     * Determines whether the format parameters of this <tt>MediaFormat</tt>
     * match a specific set of format parameters.
     *
     * @param fmtps the set of format parameters to match to the format
     * parameters of this <tt>MediaFormat</tt>
     * @return <tt>true</tt> if this <tt>MediaFormat</tt> considers
     * <tt>fmtps</tt> matching its format parameters; otherwise, <tt>false</tt>
     */
    public boolean formatParametersMatch(Map<String, String> fmtps);

    /**
     * Returns a <tt>Map</tt> containing advanced parameters specific to this
     * particular <tt>MediaFormat</tt>. The parameters returned here are meant
     * for use in SIP/SDP or XMPP session descriptions.
     *
     * @return a <tt>Map</tt> containing advanced parameters specific to this
     * particular <tt>MediaFormat</tt>
     */
    public Map<String, String> getAdvancedAttributes();

    /**
     * Returns a <tt>Map</tt> containing parameters specific to this particular
     * <tt>MediaFormat</tt>. The parameters returned here are meant for use in
     * SIP/SDP or XMPP session descriptions where they get transported through
     * the "fmtp:" attribute or <parameter/> tag respectively.
     *
     * @return a <tt>Map</tt> containing parameters specific to this particular
     * <tt>MediaFormat</tt>.
     */
    public Map<String, String> getFormatParameters();

    /**
     * Gets the RTP payload type (number) of this <tt>MediaFormat</tt> as it is
     * known in RFC 3551 "RTP Profile for Audio and Video Conferences with
     * Minimal Control".
     *
     * @return the RTP payload type of this <tt>MediaFormat</tt> if it is known
     * in RFC 3551 "RTP Profile for Audio and Video Conferences with Minimal
     * Control"; otherwise, {@link #RTP_PAYLOAD_TYPE_UNKNOWN}
     */
    public byte getRTPPayloadType();

    /**
     * Sets additional codec settings.
     *
     * @param settings additional settings represented by a map.
     */
    public void setAdditionalCodecSettings(Map<String, String> settings);

    /**
     * Returns additional codec settings.
     *
     * @return additional settings represented by a map.
     */
    public Map<String, String> getAdditionalCodecSettings();

    /**
     * Returns a <tt>String</tt> representation of this <tt>MediaFormat</tt>
     * containing important format attributes such as the encoding for example.
     *
     * @return a <tt>String</tt> representation of this <tt>MediaFormat</tt>.
     */
    @Override
    public String toString();
}
