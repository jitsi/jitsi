/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * The <tt>MediaType</tt> enumeration contains a list of media types
 * currently known to and handled by the <tt>MediaService</tt>.
 *
 * @author Emil Ivov
 */
public enum MediaType
{
    /**
     * Represents an AUDIO media type.
     */
    AUDIO("audio"),

    /**
     * Represents a VIDEO media type.
     */
    VIDEO("video");

    /**
     * The name of this <tt>MediaType</tt>.
     */
    private final String mediaTypeName;

    /**
     * Creates a <tt>MediaType</tt> instance with the specified name.
     *
     * @param mediaTypeName the name of the <tt>MediaType</tt> we'd like to
     * create.
     */
    private MediaType(String mediaTypeName)
    {
        this.mediaTypeName = mediaTypeName;
    }

    /**
     * Returns the name of this MediaType (e.g. "audio" or "video"). The name
     * returned by this method is meant for use by session description
     * mechanisms such as SIP/SDP or XMPP/Jingle.
     *
     * @return the name of this MediaType (e.g. "audio" or "video").
     */
    @Override
    public String toString()
    {
        return mediaTypeName;
    }

    /**
     * Returns a <tt>MediaType</tt> value corresponding to the specified
     * <tt>mediaTypeName</tt> or in other words <tt>MediaType.AUDIO</tt> for
     * "audio" and <tt>MediaType.VIDEO</tt> for "video".
     *
     * @param mediaTypeName the name that we'd like to parse.
     * @return a <tt>MediaType</tt> value corresponding to the specified
     * <tt>mediaTypeName</tt>.
     *
     * @throws IllegalArgumentException in case <tt>mediaTypeName</tt> is not a
     * valid or currently supported media type.
     */
    public static MediaType parseString(String mediaTypeName)
        throws IllegalArgumentException
    {
        if(AUDIO.toString().equals(mediaTypeName))
            return AUDIO;

        if(VIDEO.toString().equals(mediaTypeName))
            return VIDEO;

        throw new IllegalArgumentException(
            mediaTypeName + " is not a currently supported MediaType");
    }
}
