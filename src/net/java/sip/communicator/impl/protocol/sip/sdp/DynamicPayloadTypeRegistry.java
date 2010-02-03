/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.sdp;

import java.util.*;

import net.java.sip.communicator.service.neomedia.format.*;

/**
 * The RTP Audio/Video Profile [RFC 3551] specifies a number of static payload
 * types for use with RTP and reserves the 96-127 field for use with dynamic
 * payload types.
 * <p>
 * Mappings of dynamic payload types are handled with SDP. They are created for
 * a particular session and remain the same for its entire lifetime. They may
 * however change in following sessions.
 * </p>
 * <p>
 * We use this class as a utility for easily creating and tracking dynamic
 * payload mappings for the lifetime of a particular session. One instance of
 * this registry is supposed to be mapped to one media session. They should
 * have pretty much the same life cycle.
 * </p>
 * @author Emil Ivov
 */
public class DynamicPayloadTypeRegistry
{
    /**
     * A field that we use to track dynamic payload numbers that we allocate.
     */
    private byte nextDynamicPayloadType = MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE;

    /**
     * A table mapping <tt>MediaFormat</tt> instances to the dynamic payload
     * type number they have obtained for the lifetime of this registry.
     */
    private Map<MediaFormat, Byte> payloadTypeMappings
        = new Hashtable<MediaFormat, Byte>();

    /**
     * Returns the dynamic payload type that has been allocated for
     * <tt>format</tt>. A mapping for the specified <tt>format</tt> would be
     * created even if it did not previously exist. The method is meant for use
     * primarily during generation of SDP descriptions.
     *
     * @param format the <tt>MediaFormat</tt> instance that we'd like to obtain
     * a payload type number for..
     *
     * @return the (possibly newly allocated) payload type number corresponding
     * to the specified <tt>format</tt> instance for the lifetime of the media
     * session.
     *
     * @throws IllegalStateException if we have already registered more dynamic
     * formats than allowed for by RTP.
     */
    public byte obtainPayloadTypeNumber(MediaFormat format)
        throws IllegalStateException
    {
        Byte payloadType = payloadTypeMappings.get(format);

        //hey, we already had this one, let's return it ;)
        if( payloadType == null)
        {
            payloadType = nextPayloadTypeNumber();
            payloadTypeMappings.put(format, payloadType);
        }

        return payloadType;
    }

    /**
     * Adds the specified <tt>format</tt> to <tt>payloadType</tt> mapping to
     * the list of mappings known to this registry. The method is meant for
     * use primarily when handling incoming media descriptions, methods
     * generating local SDP should use the <tt>obtainPayloadTypeNumber</tt>
     * instead.
     *
     * @param payloadType the payload type number that we'd like to allocated
     * to <tt>format</tt>.
     * @param format the <tt>MediaFormat</tt> that we'd like to create a
     * dynamic mapping for.
     *
     * @throws IllegalArgumentException in case <tt>payloadType</tt> has
     * already been assigned to another format.
     */
    public void addMapping(MediaFormat format, byte payloadType)
        throws IllegalArgumentException
    {
        MediaFormat alreadyMappedFmt = findFormat(payloadType);

        if(alreadyMappedFmt != null)
        {
            throw new IllegalArgumentException(payloadType
                    + " has already been allocated to " + alreadyMappedFmt);
        }

        if( payloadType < MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE)
        {
            throw new IllegalArgumentException(payloadType
                + " is not a valid dynamic payload type number."
                + " (must be between " + MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE
                + " and " + MediaFormat.MAX_DYNAMIC_PAYLOAD_TYPE);
        }

        payloadTypeMappings.put(format, Byte.valueOf(payloadType));
    }

    /**
     * Returns a reference to the <tt>MediaFormat</tt> with the specified
     * mapping or <tt>null</tt> if the number specified by <tt>payloadType</tt>
     * has not been allocated yet.
     *
     * @param payloadType the number of the payload type that we are trying to
     * get a format for.
     *
     * @return the <tt>MediaFormat</tt> that has been mapped to
     * <tt>payloadType</tt> in this registry or <tt>null</tt> if it hasn't been
     * allocated yet.
     */
    public MediaFormat findFormat(byte payloadType)
    {
        for (Map.Entry<MediaFormat, Byte> entry
                : payloadTypeMappings.entrySet())
        {
            byte fmtPayloadType = entry.getValue();

            if(fmtPayloadType == payloadType)
                return entry.getKey();
        }
        return null;
    }

    /**
     * Returns the first non-allocated dynamic payload type number.
     *
     * @return the first non-allocated dynamic payload type number.
     *
     * @throws IllegalStateException if we have already registered more dynamic
     * formats than allowed for by RTP.
     */
    private byte nextPayloadTypeNumber()
        throws IllegalStateException
    {
        while (true)
        {
            if (nextDynamicPayloadType < 0)
            {
                throw new IllegalStateException(
                    "Impossible to allocate more than the already 32 mapped "
                    +"dynamic payload type numbers");
            }

            byte payloadType = nextDynamicPayloadType++;

            if(findFormat(payloadType) == null)
                return payloadType;

            //if we get here then that means that the number we obtained by
            //incrementing our PT counter was already occupied (probably by an
            //incoming SDP). continue bravely and get the next free one.
        }
    }

    /**
     * Returns a copy of all mappings currently registered in this registry.
     *
     * @return a copy of all mappings currently registered in this registry.
     */
    public Map<MediaFormat, Byte> getMappings()
    {
        return new Hashtable<MediaFormat, Byte>(payloadTypeMappings);
    }
}
