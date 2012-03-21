/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;
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
 *
 * @author Emil Ivov
 */
public class DynamicPayloadTypeRegistry
{
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
     * An override mappings of <tt>MediaFormat</tt> instances to the
     * dynamic payload type numbers.
     */
    private Map<Byte, String> overridePayloadTypeMappings = null;

    /**
     * Payload types mapping from <tt>MediaService</tt>.
     */
    private Map<MediaFormat, Byte> mediaMappings = null;

    /**
     * Sets the override payload type numbers.
     *
     * @param mappings the override payload-type mappings.
     */
    public void setOverridePayloadTypeMappings(Map<Byte, String> mappings)
    {
        overridePayloadTypeMappings = mappings;
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
        MediaType mediaType = format.getMediaType();
        String encoding = format.getEncoding();
        double clockRate = format.getClockRate();
        int channels
            = MediaType.AUDIO.equals(mediaType)
                ? ((AudioMediaFormat) format).getChannels()
                : MediaFormatFactory.CHANNELS_NOT_SPECIFIED;
        Byte payloadType = null;
        Map<String, String> formatParameters
            = format.getFormatParameters();

        for (Map.Entry<MediaFormat, Byte> payloadTypeMapping
                : payloadTypeMappings.entrySet())
        {
            if (AbstractMediaStream.matches(
                    payloadTypeMapping.getKey(),
                    mediaType, encoding, clockRate, channels, formatParameters))
            {
                payloadType = payloadTypeMapping.getValue();
                break;
            }
        }

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
            Map<MediaFormat, Byte> mappings = new HashMap<MediaFormat, Byte>(
                ProtocolMediaActivator.getMediaService()
                    .getDynamicPayloadTypePreferences());

            if(overridePayloadTypeMappings == null)
                return mappings;

            // if we have specific payload type preferences from
            // CallPeerMediaHandler, replace them here

            for(Map.Entry<Byte, String> e :
                this.overridePayloadTypeMappings.entrySet())
            {
                Byte key = e.getKey();
                String fmt = e.getValue();
                MediaFormat saveFmt = null;
                Byte saveKey = null;
                Byte replaceKey = null;
                MediaFormat replaceFmt = null;

                for(Map.Entry<MediaFormat, Byte> e2 : mappings.entrySet())
                {
                    if(e2.getKey().getEncoding().equals(fmt))
                    {
                        saveFmt = e2.getKey();
                        saveKey = e2.getValue();

                        if(replaceKey != null)
                            break;
                    }

                    if(e2.getValue().byteValue() == key.byteValue())
                    {
                        replaceFmt = e2.getKey();
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
        return getDynamicPayloadTypePreferences().get(format);
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
            throw new IllegalArgumentException(
                    payloadType + " has already been allocated to "
                        + alreadyMappedFmt);
        }

        if( payloadType < MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE)
        {
            throw new IllegalArgumentException(
                    payloadType + " is not a valid dynamic payload type number."
                        + " (must be between "
                        + MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE + " and "
                        + MediaFormat.MAX_DYNAMIC_PAYLOAD_TYPE);
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
     * @return the {@link MediaFormat} with the specified
     * <tt>payloadTypePreference</tt> or <tt>null</tt> if no {@link MediaFormat}
     * has claimed this payload type number as preferred.
     */
    private MediaFormat findFormatWithPreference(Byte payloadTypePreference)
    {
        for(Map.Entry<MediaFormat, Byte> entry
                : getDynamicPayloadTypePreferences().entrySet())
        {
            if(entry.getValue() == payloadTypePreference)
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
}
