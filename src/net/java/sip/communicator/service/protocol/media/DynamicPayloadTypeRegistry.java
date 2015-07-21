/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.media;

import java.util.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.format.*;

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
 *
 * @author Emil Ivov
 */
public class DynamicPayloadTypeRegistry
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(DynamicPayloadTypeRegistry.class);

    /**
     * A field that we use to track dynamic payload numbers that we allocate.
     */
    private byte nextDynamicPayloadType = MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE;

    /**
     * The mappings of <tt>MediaFormat</tt> instances to the dynamic payload
     * type numbers they have obtained for the lifetime of this registry.
     */
    private final Map<MediaFormat, Byte> payloadTypeMappings
        = new HashMap<MediaFormat, Byte>();

    /**
     * Maps locally defined payload types to payload type numbers that the
     * remote party wants to use.
     */
    private final Map<Byte, Byte> payloadTypeOverrides
        = new HashMap<Byte, Byte>();

    /**
     * An override mappings of <tt>MediaFormat</tt> instances to the
     * dynamic payload type numbers.
     */
    private Map<Byte, String> localPayloadTypePreferences = null;

    /**
     * Payload types mapping from <tt>MediaService</tt>.
     */
    private Map<MediaFormat, Byte> mediaMappings = null;

    /**
     * Sets the override payload type numbers.
     *
     * @param mappings the override payload-type mappings.
     */
    public void setLocalPayloadTypePreferences(Map<Byte, String> mappings)
    {
        localPayloadTypePreferences = mappings;
    }

    /**
     * Returns the dynamic payload type that has been allocated for
     * <tt>format</tt>. A mapping for the specified <tt>format</tt> would be
     * created even if it did not previously exist. The method is meant for use
     * primarily during generation of SDP descriptions.
     *
     * @param format the <tt>MediaFormat</tt> instance that we'd like to obtain
     * a payload type number for.
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
        Byte payloadType = getPayloadType(format);

        //seems like we haven't allocated a payload type for this format yet.
        //lets try to do so now.
        if (payloadType == null)
        {
            //first, let's check whether there's a particular PT number that
            //this format would like to have (e.g. "telephone-event" generally
            //loves to be called "101").
            Byte preferredPT = getPreferredDynamicPayloadType(format);

            if(preferredPT != null && findFormat(preferredPT) == null)
            {
                //the format has a preference and it's free
                payloadType = preferredPT;
            }
            else
            {
                //the format does not have a preferred PT number or it isn't
                //free.
                payloadType = nextPayloadTypeNumber();
            }
            payloadTypeMappings.put(format, payloadType);
        }

        return payloadType;
    }

    /**
     * Returns the dynamic payload type preferences mappings.
     *
     * @return the dynamic payload type preferences mappings.
     */
    private Map<MediaFormat, Byte> getDynamicPayloadTypePreferences()
    {
        if(mediaMappings == null)
        {
            Map<MediaFormat, Byte> mappings
                = new HashMap<MediaFormat, Byte>(
                        ProtocolMediaActivator
                            .getMediaService()
                                .getDynamicPayloadTypePreferences());

            if(localPayloadTypePreferences == null)
                return mappings;

            // If we have specific payload type preferences from
            // CallPeerMediaHandler, replace them here.

            for(Map.Entry<Byte, String> e
                    : localPayloadTypePreferences.entrySet())
            {
                Byte key = e.getKey();
                String fmt = e.getValue();
                MediaFormat saveFmt = null;
                Byte saveKey = null;
                Byte replaceKey = null;
                MediaFormat replaceFmt = null;

                for(Map.Entry<MediaFormat, Byte> e2 : mappings.entrySet())
                {
                    MediaFormat fmt2 = e2.getKey();
                    Byte key2 = e2.getValue();

                    if(fmt2.getEncoding().equals(fmt))
                    {
                        saveFmt = fmt2;
                        saveKey = key2;

                        if(replaceKey != null)
                            break;
                    }

                    if(key2.equals(key))
                    {
                        replaceFmt = fmt2;
                        replaceKey = key;

                        if(saveKey != null)
                            break;
                    }
                }

                if(saveFmt != null)
                {
                    mappings.remove(saveFmt);

                    if(replaceFmt != null)
                        mappings.remove(replaceFmt);

                    mappings.put(saveFmt, key);

                    if(replaceFmt != null)
                        mappings.put(replaceFmt, saveKey);
                }
            }
            mediaMappings = mappings;
        }
        return mediaMappings;
    }

    /**
     * Returns the payload type number that <tt>format</tt> would like to use if
     * possible and <tt>null</tt> if there is no such preference.
     *
     * @param format the {@link MediaFormat} whose preferred dynamic PT number
     * we are trying to obtain.
     *
     * @return the payload type number that <tt>format</tt> would like to use if
     * possible and <tt>null</tt> if there is no such preference.
     */
    private Byte getPreferredDynamicPayloadType(MediaFormat format)
    {
        Map<MediaFormat, Byte> ptPreferences
                = getDynamicPayloadTypePreferences();

        return getPayloadTypeFromMap(ptPreferences, format);
    }

    /**
     * Adds the specified <tt>format</tt> to <tt>payloadType</tt> mapping to
     * the list of mappings known to this registry. If the mapping already
     * exists in the registry then we will use the new value to create an
     * overriding mapping. This basically means that we will expect packets to
     * be streamed to us with the original PT but we will be streaming with
     * The method is meant for
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
            if(alreadyMappedFmt.matches(format))
            {
                //we already have the exact same mapping, so no need to
                //create a new one override the old PT number. However, there
                //might be a leftover override from a previous mapping so let's
                //remove it if this is the case.
                payloadTypeOverrides.remove(payloadType);
                return;
            }
            //else:
            //welcome to hackland: the remote party is trying to re-map a
            //payload type we already use. we will try to respect their choice
            //and create an overriding mapping but we also need to make sure
            //that the format itself actually has a PT we can override.
            byte newlyObtainedPT = obtainPayloadTypeNumber(format);

            logger.warn("Remote party is trying to remap payload type "
                        + payloadType + " and reassign it from "
                        + alreadyMappedFmt + " to " + format
                        + ". We'll go along but there might be issues because"
                        + " of this. We'll also expect to receive " + format
                        + " with PT=" + newlyObtainedPT);

        }

        if( payloadType < MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE)
        {
            throw new IllegalArgumentException(
                    payloadType + " is not a valid dynamic payload type number."
                        + " (must be between "
                        + MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE + " and "
                        + MediaFormat.MAX_DYNAMIC_PAYLOAD_TYPE);
        }

        // if the format is already mapped to a PT then we'll keep it and use
        // the new one as an override value for sending. we'd still expect to
        // receive packets with the value that we had first selected.
        Byte originalPayloadType = getPayloadType(format);

        if( originalPayloadType != null && originalPayloadType != payloadType)
        {
            payloadTypeOverrides.put(originalPayloadType, payloadType);
        }
        else
        {
            //we are just adding a new mapping. nothing out of the ordinary
            payloadTypeMappings.put(format, Byte.valueOf(payloadType));
        }
    }

    /**
     * Return   s a reference to the <tt>MediaFormat</tt> with the specified
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
                        "Impossible to allocate more than the already 32 "
                            + "mapped dynamic payload type numbers");
            }

            byte payloadType = nextDynamicPayloadType++;

            if ((findFormat(payloadType) == null)
                    && (findFormatWithPreference(payloadType) == null))
                return payloadType;

            /*
             * If we get here, then that means that the number we obtained by
             * incrementing our PT counter was already occupied (probably by an
             * incoming SDP). Continue bravely and get the next free one.
             */
        }
    }

    /**
     * Returns the {@link MediaFormat} with the specified
     * <tt>payloadTypePreference</tt> or <tt>null</tt> if no {@link MediaFormat}
     * has claimed this payload type number as preferred.
     *
     * @param payloadTypePreference the dynamic payload type number that we
     * are trying to determine as being claimed as preferred or not by a
     * media format.
     *
     * @return the {@link MediaFormat} with the null
     * <tt>payloadTypePreference</tt> or <tt>null</tt> if no {@link MediaFormat}
     * has claimed this payload type number as preferred.
     */
    private MediaFormat findFormatWithPreference(byte payloadTypePreference)
    {
        for(Map.Entry<MediaFormat,Byte> entry
                : getDynamicPayloadTypePreferences().entrySet())
        {
            Byte value = entry.getValue();

            if((value != null) && (value.byteValue() == payloadTypePreference))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Returns a copy of all mappings currently registered in this registry.
     *
     * @return a copy of all mappings currently registered in this registry.
     */
    public Map<MediaFormat, Byte> getMappings()
    {
        return new HashMap<MediaFormat, Byte>(payloadTypeMappings);
    }

    /**
     * Returns a copy of all mapping overrides  currently registered in this
     * registry.
     *
     * @return a copy of all mapping overrides  currently registered in this
     * registry.
     */
    public Map<Byte, Byte> getMappingOverrides()
    {
        return new HashMap<Byte, Byte>(payloadTypeOverrides);
    }

    /**
     * Returns the payload type that is currently mapped to <tt>format</tt> or
     * <tt>null</tt> if there is currently no such payload type.
     *
     * @param format the {@link MediaFormat} whose mapping we are looking for
     * @return the payload type that is currently mapped to <tt>format</tt> or
     * <tt>null</tt> if there is currently no such payload type.
     */
    public Byte getPayloadType(MediaFormat format)
    {
        return getPayloadTypeFromMap(payloadTypeMappings, format);
    }

    /**
     * Iterates through <tt>formatMap</tt> and returns the payload type that it
     * maps to <tt>format</tt> or <tt>null</tt> if there is currently no such
     * payload type.
     *
     * @param format the {@link MediaFormat} whose mapping we are looking for
     * @return the payload type that is currently mapped to <tt>format</tt> or
     * <tt>null</tt> if there is currently no such payload type.
     */
    private static Byte getPayloadTypeFromMap(Map<MediaFormat, Byte> formatMap,
                                              MediaFormat format)
    {
        for (Map.Entry<MediaFormat, Byte> mapping : formatMap.entrySet())
        {
            if (mapping.getKey().matches(format))
            {
                return mapping.getValue();
            }
        }

        return null;
    }

}
