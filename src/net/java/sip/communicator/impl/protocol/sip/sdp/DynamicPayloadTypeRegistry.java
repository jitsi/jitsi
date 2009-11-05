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
 *
 * @author Emil Ivov
 */
public class DynamicPayloadTypeRegistry
{
    /**
     * A table mapping <tt>MediaFormat</tt> instances to the dynamic payload
     * type number they have obtained for the lifetime of this registry.
     */
    private Map<MediaFormat, Integer> payloadMappings
        = new Hashtable<MediaFormat, Integer>();


}
