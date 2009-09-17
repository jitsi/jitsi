/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * The <tt>MediaDirections</tt> enumeration contains a list of media directions
 * that indicate read/write capabilities of different entities in this
 * <tt>MediaService</tt> such as for example devices.
 *
 * @author Emil Ivov
 */
public enum MediaDirection
{
    /**
     * Represents a direction from the entity that this direction pertains to
     * to the outside. When applied to a <tt>MediaDevice</tt> the direction
     * indicates that the device is a read-only one. In the case of a stream
     * a <tt>SENDONLY</tt> direction indicates that the stream is only sending
     * data to the remote party without receiving.
     */
    SENDONLY("sendonly"),

    /**
     * Represents a direction pointing to the entity that this object pertains
     * to and from the outside. When applied to a <tt>MediaDevice</tt> the
     * direction indicates that the device is a write-only one. In the case of a
     * <tt>MediaStream</tt> a <tt>RECVONLY</tt> direction indicates that the
     * stream is only receiving data from the remote party without sending
     * any.
     */
    RECVONLY("recvonly"),

    /**
     * Indicates that the related entity supports both input and output (send
     * and receive) operations.
     */
    SENDRECV("sendrecv");

    /**
     * The name of this direction.
     */
    private final String directionName;

    /**
     * Creates a <tt>MediaDirection</tt> instance with the specified name.
     *
     * @param directionName the name of the <tt>MediaDirections</tt> we'd like
     * to create.
     */
    private MediaDirection(String directionName)
    {
        this.directionName = directionName;
    }

    /**
     * Returns the name of this <tt>MediaDirection</tt> (e.g. "sendonly" or
     * "sendrecv"). The name returned by this method is meant for use by
     * session description mechanisms such as SIP/SDP or XMPP/Jingle.
     *
     * @return the name of this <tt>MediaDirection</tt> (e.g. "sendonly",
     * "recvonly", "sendrecv").
     */
    public String toString()
    {
        return directionName;
    }

    /**
     * Returns a <tt>MediaDirection</tt> value corresponding to the specified
     * <tt>mediaDirectionName</tt> or in other words <tt>MediaType.SENDONLY</tt>
     * for "sendonly", <tt>MediaType.RECVONLY</tt> for "recvonly", and
     * <tt>MediaType.SENDRECV</tt> for "sendrecv".
     *
     * @param mediaDirectionName the name that we'd like to parse.
     * @return a <tt>MediaDirection</tt> value corresponding to the specified
     * <tt>mediaDirectionName</tt>.
     *
     * @throws a <tt>java.lang.IllegalArgumentException</tt> in case
     * <tt>mediaDirectionName</tt> is not a valid or currently supported media
     * direction.
     */
    public static MediaDirection parseString(String mediaDirectionName)
        throws IllegalArgumentException
    {
        if(SENDONLY.toString().equals(mediaDirectionName))
            return SENDONLY;

        if(RECVONLY.toString().equals(mediaDirectionName))
            return RECVONLY;

        if(SENDRECV.toString().equals(mediaDirectionName))
            return SENDRECV;

        throw new IllegalArgumentException(mediaDirectionName
                        + " is not a currently supported MediaDirection");
    }
}
