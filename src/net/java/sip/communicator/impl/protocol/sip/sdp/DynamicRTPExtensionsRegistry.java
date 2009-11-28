/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.sdp;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * RFC [RFC 5285] defines a mechanism for attaching multiple extensions to
 * RTP packets. Part of this mechanism consists in negotiating their
 * identifiers using <tt>extmap</tt> attributes pretty much the same way one
 * would negotiate payload types with <tt>rtpmap</tt> attributes.
 * <p>
 * Mappings of extension IDs are handled with SDP. They are created for
 * a particular session and remain the same for its entire lifetime. They may
 * however change in following sessions.
 * </p>
 * <p>
 * We use this class as a utility for easily creating and tracking extension
 * mappings for the lifetime of a particular session. One instance of this
 * registry is supposed to be mapped to one media session and they should
 * have the same life cycle.
 * </p>
 * @author Emil Ivov
 */
public class DynamicRTPExtensionsRegistry
{
    /**
     * The minimum integer that is allowed for use when mapping extensions using
     * the one-byte header.
     */
    public static final int MIN_HEADER_ID = 1;

    /**
     * The maximum integer that is allowed for use when mapping extensions using
     * the one-byte header. Note that 15 is reserved for future use by 5285
     */
    public static final int MAX_ONE_BYTE_HEADER_ID = 14;

    /**
     * The maximum integer that is allowed for use when mapping extensions using
     * the two-byte header.
     */
    public static final int MAX_TWO_BYTE_HEADER_ID = 255;

    /**
     * A field that we use to track mapping IDs.
     */
    private byte nextExtensionMapping = MIN_HEADER_ID;

    /**
     * A table mapping <tt>RTPExtension</tt> instances to the dynamically
     * allocated ID they have obtained for the lifetime of this registry.
     */
    private Map<RTPExtension, Byte> extMap
        = new Hashtable<RTPExtension, Byte>();

    /**
     * Returns the ID that has been allocated for <tt>extension</tt>. A mapping
     * for the specified <tt>extension</tt> would be created even if it did not
     * previously exist. The method is meant for use primarily during generation
     * of SDP descriptions.
     *
     * @param extension the <tt>RTPExtension</tt> instance that we'd like to
     * obtain a dynamic ID for.
     *
     * @return the (possibly newly allocated) ID corresponding to the specified
     * <tt>extension</tt> and valid for the lifetime of the media session.
     *
     * @throws IllegalStateException if we have already registered more RTP
     * extensions than allowed for by RTP.
     */
    public byte obtainExtensionMapping(RTPExtension extension)
        throws IllegalStateException
    {
        Byte extID = extMap.get(extension);

        //hey, we already had this one, let's return it ;)
        if( extID == null)
        {
            extID = nextExtensionID();
            extMap.put(extension, extID);
        }

        return extID;
    }

    /**
     * Adds the specified <tt>extension</tt> to <tt>extID</tt> mapping to
     * the list of mappings known to this registry. The method is meant for
     * use primarily when handling incoming media descriptions, methods
     * generating local SDP should use the <tt>obtainExtensionMapping</tt>
     * instead.
     *
     * @param extID the extension ID that we'd like to allocated to
     * <tt>extension</tt>.
     * @param extension the <tt>RTPExtension</tt> that we'd like to create a
     * dynamic mapping for.
     *
     * @throws IllegalArgumentException in case <tt>extID</tt> has already been
     * assigned to another <tt>RTPExtension</tt>.
     */
    public void addMapping(RTPExtension extension, byte extID)
        throws IllegalArgumentException
    {
        RTPExtension alreadyMappedExt = findExtension(extID);

        if(alreadyMappedExt != null)
        {
            throw new IllegalArgumentException(extID
                    + " has already been allocated to " + alreadyMappedExt);
        }

        if( extID < MIN_HEADER_ID)
        {
            throw new IllegalArgumentException(extID
                + " is not a valid RTP extensino header ID."
                + " (must be between " + MIN_HEADER_ID
                + " and " + MAX_TWO_BYTE_HEADER_ID);
        }

        extMap.put(extension, Byte.valueOf(extID));
    }

    /**
     * Returns a reference to the <tt>RTPExtension</tt> with the specified
     * mapping or <tt>null</tt> if the number specified by <tt>extID</tt>
     * has not been allocated yet.
     *
     * @param extID the ID whose <tt>RTPExtension</tt> we are trying to
     * discover.
     *
     * @return the <tt>RTPExtension</tt> that has been mapped to
     * <tt>extID</tt> in this registry or <tt>null</tt> if it hasn't been
     * allocated yet.
     */
    public RTPExtension findExtension(byte extID)
    {
        for (Map.Entry<RTPExtension, Byte> entry
                : extMap.entrySet())
        {
            byte currentExtensionID = entry.getValue();

            if(currentExtensionID == extID)
                return entry.getKey();
        }
        return null;
    }

    /**
     * Returns the first non-allocated dynamic extension ID number.
     *
     * @return the first non-allocated dynamic extension ID number..
     *
     * @throws IllegalStateException if we have already registered more RTP
     * extension headers than allowed for by RTP.
     */
    private byte nextExtensionID()
        throws IllegalStateException
    {
        while (true)
        {
            if (nextExtensionMapping < 0)
            {
                throw new IllegalStateException(
                    "Impossible to map more than the 255 already mapped "
                    +" RTP extensions");
            }

            byte extID = nextExtensionMapping++;

            if(findExtension(extID) == null)
                return extID;

            //if we get here then that means that the number we obtained by
            //incrementing our ID counter was already occupied (probably by an
            //incoming SDP). continue bravely and get the next free one.
        }
    }

    /**
     * Returns a copy of all mappings currently registered in this registry.
     *
     * @return a copy of all mappings currently registered in this registry.
     */
    public Map<RTPExtension, Byte> getMappings()
    {
        return new Hashtable<RTPExtension, Byte>(extMap);
    }
}
