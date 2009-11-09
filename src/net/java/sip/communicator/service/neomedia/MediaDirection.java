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
     * Indicates that the related entity does not support neither input
     * nor output (i.e. neither send nor receive) operations.
     */
    INACTIVE("inactive"),

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
    @Override
    public String toString()
    {
        return directionName;
    }

    /**
     * Applies an extra direction constraint to this <tt>MediaDirection</tt>
     * or in other words performs an <tt>and</tt> operation. This method is
     * primarily meant for use by the
     * <tt>getReverseMediaDirection(MediaDirection)</tt> method while working
     * on Offer/Answer media negotiation..
     *
     * @param direction that direction constraint that we'd like to apply to
     * this <tt>MediaDirection</tt>
     *
     * @return the new <tt>MediaDirection</tt> obtained after applying the
     * <tt>direction</tt> constraint to this <tt>MediaDirection</tt>.
     */
    public MediaDirection and(MediaDirection direction)
    {
        if (this == SENDRECV)
        {
            return direction;
        }
        else if( this == SENDONLY)
        {
            if (direction == SENDONLY || direction == SENDRECV)
                return SENDONLY;
            else
                return INACTIVE;
        }
        else if( this == RECVONLY)
        {
            if (direction == RECVONLY || direction == SENDRECV)
                return RECVONLY;
            else
                return INACTIVE;
        }
        else
            return INACTIVE;
    }

    /**
     * Reverses a direction constraint on this <tt>MediaDirection</tt>
     * or in other words performs an <tt>or</tt> operation. This method is
     * meant for use in cases like putting a stream off hold or in other words
     * reversing the <tt>SENDONLY</tt> constraint.
     *
     * @param direction the direction that we'd like to enable (i.e. add) to
     * this <tt>MediaDirection</tt>
     *
     * @return the new <tt>MediaDirection</tt> obtained after adding the
     * specified <tt>direction</tt> this <tt>MediaDirection</tt>.
     */
    public MediaDirection or(MediaDirection direction)
    {
        if (this == SENDRECV)
        {
            return this;
        }
        else if( this == SENDONLY)
        {
            if( direction.allowsReceiving())
                return SENDRECV;
            else
                return this;
        }
        else if( this == RECVONLY)
        {
            if( direction.allowsSending())
                return SENDRECV;
            else
                return this;
        }
        else // INACTIVE
            return direction;
    }

    /**
     * Returns the <tt>MediaDirection</tt> value corresponding to a remote
     * party's perspective of this <tt>MediaDirection</tt>. In other words,
     * if I say I'll be sending only, for you this means that you'll be
     * receiving only. If however, I say I'll be both sending and receiving
     * (i.e. <tt>SENDRECV</tt>) then it means you'll be doing the same (i.e.
     * again <tt>SENDRECV</tt>).
     *
     * @return the <tt>MediaDirection</tt> value corresponding to a remote
     * party's perspective of this <tt>MediaDirection</tt>.
     */
    public MediaDirection getReverseDirection()
    {
        if (this == SENDRECV)
        {
            return SENDRECV;
        }
        else if(this == SENDONLY)
        {
            return RECVONLY;
        }
        else if (this == RECVONLY)
        {
            return SENDONLY;
        }
        else
        {
            return INACTIVE;
        }
    }

    /**
     * Returns the <tt>MediaDirection</tt> value corresponding to a remote
     * party's perspective of this <tt>MediaDirection</tt> applying a remote
     * party constraint. In other words, if I say I'll only be sending media
     * (i.e. <tt>SENDONLY</tt>) and you know that you can both send and receive
     * (i.e. <tt>SENDRECV</tt>) then to you this mean that you'll be only
     * receiving media (i.e. <tt>RECVONLY</tt>). If however I say that I can
     * only receive a particular media type (i.e. <tt>RECVONLY</tt>) and you
     * are in the same situation then this means that neither of us would be
     * sending nor receiving and the stream would appear <tt>INACTIVE</tt> to
     * you (and me for that matter). The method is meant for use during
     * Offer/Answer SDP negotiation.
     *
     * @param remotePartyDir the remote party <tt>MediaDirection</tt> constraint
     * that we'd have to consider when trying to obtain a
     * <tt>MediaDirection</tt> corresponding to remoteParty's constraint.
     *
     * @return the <tt>MediaDirection</tt> value corresponding to a remote
     * party's perspective of this <tt>MediaDirection</tt> applying a remote
     * party constraint.
     */
    public MediaDirection getDirectionForAnswer(MediaDirection remotePartyDir)
    {
        return this.and(remotePartyDir.getReverseDirection());
    }

    /**
     * Determines whether the directions specified by this
     * <tt>MediaDirection</tt> instance allow for outgoing (i.e. sending)
     * streams or in other words whether this is a <tt>SENDONLY</tt> or a
     * <tt>SENDRECV</tt> instance
     *
     * @return <tt>true</tt> if this <tt>MediaDirection</tt> instance includes
     * the possibility of sending and <tt>false</tt> otherwise.
     */
    public boolean allowsSending()
    {
        return this == SENDONLY || this == SENDRECV;
    }

    /**
     * Determines whether the directions specified by this
     * <tt>MediaDirection</tt> instance allow for incoming (i.e. receiving)
     * streams or in other words whether this is a <tt>RECVONLY</tt> or a
     * <tt>SENDRECV</tt> instance
     *
     * @return <tt>true</tt> if this <tt>MediaDirection</tt> instance includes
     * the possibility of receiving and <tt>false</tt> otherwise.
     */
    public boolean allowsReceiving()
    {
        return this == RECVONLY || this == SENDRECV;
    }
}
