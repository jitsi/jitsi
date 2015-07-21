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
package net.java.sip.communicator.impl.protocol.jabber.jinglesdp;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.CreatorEnum;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.SendersEnum;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.*;

/**
 * The class contains a number of utility methods that are meant to facilitate
 * creating and parsing jingle media rtp description descriptions and
 * transports.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class JingleUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>JingleUtils</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(JingleUtils.class);

    /**
     * Extracts and returns an {@link RtpDescriptionPacketExtension} provided
     * with <tt>content</tt> or <tt>null</tt> if there is none.
     *
     * @param content the media content that we'd like to extract the
     * {@link RtpDescriptionPacketExtension} from.
     *
     * @return an {@link RtpDescriptionPacketExtension} provided with
     * <tt>content</tt> or <tt>null</tt> if there is none.
     */
    public static RtpDescriptionPacketExtension getRtpDescription(
                                        ContentPacketExtension content)
    {
        return content.getFirstChildOfType(RtpDescriptionPacketExtension.class);
    }

    /**
     * Extracts and returns the list of <tt>MediaFormat</tt>s advertised in
     * <tt>description</tt> preserving their oder and registering dynamic payload
     * type numbers in the specified <tt>ptRegistry</tt>. Note that this method
     * would only include in the result list <tt>MediaFormat</tt> instances
     * that are currently supported by our <tt>MediaService</tt> implementation
     * and enabled in its configuration. This means that the method could
     * return an empty list even if there were actually some formats in the
     * <tt>mediaDesc</tt> if we support none of them or if all these we support
     * are not enabled in the <tt>MediaService</tt> configuration form.
     *
     * @param description the <tt>MediaDescription</tt> that we'd like to probe
     * for a list of <tt>MediaFormat</tt>s
     * @param ptRegistry a reference to the <tt>DynamycPayloadTypeRegistry</tt>
     * where we should be registering newly added payload type number to format
     * mappings.
     *
     * @return an ordered list of <tt>MediaFormat</tt>s that are both advertised
     * in the <tt>description</tt> and supported by our <tt>MediaService</tt>
     * implementation.
     */
    public static List<MediaFormat> extractFormats(
                                     RtpDescriptionPacketExtension description,
                                     DynamicPayloadTypeRegistry    ptRegistry)
    {
        List<MediaFormat> mediaFmts = new ArrayList<MediaFormat>();

        if (description == null)
        {
            return mediaFmts;
        }

        List<PayloadTypePacketExtension> payloadTypes
                                            = description.getPayloadTypes();

        for(PayloadTypePacketExtension ptExt : payloadTypes)
        {
            MediaFormat format = payloadTypeToMediaFormat(ptExt, ptRegistry);

            //continue if our media service does not know this format
            if(format == null)
            {
                if(logger.isTraceEnabled())
                    logger.trace("Unsupported remote format: " + ptExt.toXML());
            }
            else
                mediaFmts.add(format);
        }

        return mediaFmts;
    }

    /**
     * Returns the {@link MediaFormat} described in the <tt>payloadType</tt>
     * extension or <tt>null</tt> if we don't recognize the format.
     *
     * @param payloadType the {@link PayloadTypePacketExtension} which is to be
     * parsed into a {@link MediaFormat}.
     * @param ptRegistry the {@link DynamicPayloadTypeRegistry} that we would
     * use for the registration of possible dynamic payload types or
     * <tt>null</tt> the returned <tt>MediaFormat</tt> is to not be registered
     * into a <tt>DynamicPayloadTypeRegistry</tt>.
     *
     * @return the {@link MediaFormat} described in the <tt>payloadType</tt>
     * extension or <tt>null</tt> if we don't recognize the format.
     */
    public static MediaFormat payloadTypeToMediaFormat(
            PayloadTypePacketExtension payloadType,
            DynamicPayloadTypeRegistry ptRegistry)
    {
        return
            payloadTypeToMediaFormat(
                    payloadType,
                    JabberActivator.getMediaService(),
                    ptRegistry);
    }

    /**
     * Returns the {@link MediaFormat} described in the <tt>payloadType</tt>
     * extension or <tt>null</tt> if we don't recognize the format.
     *
     * @param payloadType the {@link PayloadTypePacketExtension} which is to be
     * parsed into a {@link MediaFormat}.
     * @param mediaService the <tt>MediaService</tt> implementation which is to
     * be used for <tt>MediaFormat</tt>-related factory methods
     * @param ptRegistry the {@link DynamicPayloadTypeRegistry} that we would
     * use for the registration of possible dynamic payload types or
     * <tt>null</tt> the returned <tt>MediaFormat</tt> is to not be registered
     * into a <tt>DynamicPayloadTypeRegistry</tt>.
     *
     * @return the {@link MediaFormat} described in the <tt>payloadType</tt>
     * extension or <tt>null</tt> if we don't recognize the format.
     */
    public static MediaFormat payloadTypeToMediaFormat(
                    PayloadTypePacketExtension payloadType,
                    MediaService mediaService,
                    DynamicPayloadTypeRegistry ptRegistry)
    {
        byte pt = (byte)payloadType.getID();
        boolean unknown = false;

        //convert params to a name:value map
        List<ParameterPacketExtension> params = payloadType.getParameters();
        Map<String, String> paramsMap = new HashMap<String, String>();
        Map<String, String> advancedMap = new HashMap<String, String>();

        for(ParameterPacketExtension param : params)
        {
            String paramName = param.getName();

            if("imageattr".equals(paramName))
                advancedMap.put(paramName, param.getValue());
            else
                paramsMap.put(paramName, param.getValue());
        }

        // video-related attributes in payload-type element
        for(String attr : payloadType.getAttributeNames())
        {
            if(attr.equals("width") || attr.equals("height"))
                paramsMap.put(attr, payloadType.getAttributeAsString(attr));
        }

        //now create the format.
        MediaFormatFactory formatFactory = mediaService.getFormatFactory();
        MediaFormat format
            = formatFactory.createMediaFormat(
                    pt,
                    payloadType.getName(),
                    payloadType.getClockrate(),
                    payloadType.getChannels(),
                    -1,
                    paramsMap,
                    advancedMap);

        //we don't seem to know anything about this format
        if(format == null)
        {
            unknown = true;
            format = formatFactory.createUnknownMediaFormat(MediaType.AUDIO);
        }

        /*
         * We've just created a MediaFormat for the specified payloadType
         * so we have to remember the mapping between the two so that we
         * don't, for example, map the same payloadType to a different
         * MediaFormat at a later time when we do automatic generation
         * of payloadType in DynamicPayloadTypeRegistry. If the remote peer
         * tries to remap a payloadType in its answer to a different MediaFormat
         * than the one we've specified in our offer, then the dynamic paylaod
         * type registry will keep the original value for receiving and also
         * add an overriding value for the new one. The overriding value will
         * be streamed to our peer.
         */
        if ((ptRegistry != null)
                && (pt >= MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE)
                && (pt <= MediaFormat.MAX_DYNAMIC_PAYLOAD_TYPE))
                //some systems will violate 3264 by reusing previously defined
                //payload types for new formats. we try and salvage that
                //situation by creating an overriding mapping in such cases
                //we therefore don't do the following check.
                //&& (ptRegistry.findFormat(pt) == null))
        {
            ptRegistry.addMapping(format, pt);
        }

        return unknown ? null : format;
    }

    /**
     * Extracts and returns the list of <tt>RTPExtension</tt>s advertised in
     * <tt>desc</tt> and registers newly encountered ones into the specified
     * <tt>extMap</tt>. The method returns an empty list in case there were no
     * <tt>extmap</tt> advertisements in <tt>desc</tt>.
     *
     * @param desc the {@link RtpDescriptionPacketExtension} that we'd like to
     * probe for a list of {@link RTPExtension}s
     * @param extMap a reference to the <tt>DynamicRTPExtensionsRegistry</tt>
     * where we should be registering newly added extension mappings.
     *
     * @return a <tt>List</tt> of {@link RTPExtension}s advertised in the
     * <tt>mediaDesc</tt> description.
     */
    public static List<RTPExtension> extractRTPExtensions(
                                         RtpDescriptionPacketExtension desc,
                                         DynamicRTPExtensionsRegistry  extMap)
    {
        List<RTPExtension> extensionsList = new ArrayList<RTPExtension>();

        if (desc == null)
        {
            return extensionsList;
        }

        List<RTPHdrExtPacketExtension> extmapList = desc.getExtmapList();

        for (RTPHdrExtPacketExtension extmap : extmapList)
        {
            RTPExtension rtpExtension
                    = new RTPExtension(
                            extmap.getURI(),
                            getDirection(extmap.getSenders(), false),
                            extmap.getAttributes());

            extensionsList.add(rtpExtension);
        }

        return extensionsList;
    }

    /**
     * Converts the specified media <tt>direction</tt> into the corresponding
     * {@link SendersEnum} value so that we could add it to a content element.
     * The <tt>initiatorPerspectice</tt> allows callers to specify whether the
     * direction is to be considered from the session initator's perspective
     * or that of the responder.
     * <p>
     * Example: A {@link MediaDirection#SENDONLY} value would be translated to
     * {@link SendersEnum#initiator} from the initiator's perspective and to
     * {@link SendersEnum#responder} otherwise.
     *
     * @param direction the {@link MediaDirection} that we'd like to translate.
     * @param initiatorPerspective <tt>true</tt> if the <tt>direction</tt> param
     * is to be considered from the initiator's perspective and <tt>false</tt>
     * otherwise.
     *
     * @return one of the <tt>MediaDirection</tt> values indicating the
     * direction of the media steam described by <tt>content</tt>.
     */
    public static SendersEnum getSenders(MediaDirection direction,
                                         boolean   initiatorPerspective)
    {
        if (direction ==  MediaDirection.SENDRECV)
            return SendersEnum.both;
        if (direction ==  MediaDirection.INACTIVE)
            return SendersEnum.none;

        if(initiatorPerspective)
        {
            if(direction == MediaDirection.SENDONLY)
                return SendersEnum.initiator;
            else // recvonly
                return SendersEnum.responder;
        }
        else
        {
            if(direction == MediaDirection.SENDONLY)
                return SendersEnum.responder;
            else // recvonly
                return SendersEnum.initiator;
        }
    }

    /**
     * Determines the direction of the media stream that <tt>content</tt>
     * describes and returns the corresponding <tt>MediaDirection</tt> enum
     * entry. The method looks for a direction specifier attribute (i.e. the
     * content 'senders' attribute) or the absence thereof and returns the
     * corresponding <tt>MediaDirection</tt> entry. The
     * <tt>initiatorPerspectice</tt> allows callers to specify whether the
     * direction is to be considered from the session initiator's perspective
     * or that of the responder.
     * <p>
     * Example: An <tt>initiator</tt> value would be translated to
     * {@link MediaDirection#SENDONLY} from the initiator's perspective and to
     * {@link MediaDirection#RECVONLY} from the responder's.
     *
     * @param content the description of the media stream whose direction
     * we are trying to determine.
     * @param initiatorPerspective <tt>true</tt> if the senders argument is to
     * be translated into a direction from the initiator's perspective and
     * <tt>false</tt> for the sender's.
     *
     * @return one of the <tt>MediaDirection</tt> values indicating the
     * direction of the media steam described by <tt>content</tt>.
     */
    public static MediaDirection getDirection(
                                            ContentPacketExtension content,
                                            boolean   initiatorPerspective)
    {
        SendersEnum senders = content.getSenders();

        return getDirection(senders, initiatorPerspective);
    }

    /**
     * Determines the direction of the media stream that <tt>content</tt>
     * describes and returns the corresponding <tt>MediaDirection</tt> enum
     * entry. The method looks for a direction specifier attribute (i.e. the
     * content 'senders' attribute) or the absence thereof and returns the
     * corresponding <tt>MediaDirection</tt> entry. The
     * <tt>initiatorPerspectice</tt> allows callers to specify whether the
     * direction is to be considered from the session initiator's perspective
     * or that of the responder.
     *
     * @param senders senders direction
     * @param initiatorPerspective <tt>true</tt> if the senders argument is to
     * be translated into a direction from the initiator's perspective and
     * <tt>false</tt> for the sender's.
     *
     * @return one of the <tt>MediaDirection</tt> values indicating the
     * direction of the media steam described by <tt>content</tt>.
     */
    public static MediaDirection getDirection(SendersEnum senders,
            boolean initiatorPerspective)
    {
        if(senders == null)
            return MediaDirection.SENDRECV;

        if (senders == SendersEnum.initiator)
        {
            if(initiatorPerspective)
                return MediaDirection.SENDONLY;
            else
                return MediaDirection.RECVONLY;
        }
        else if (senders == SendersEnum.responder)
        {
            if(initiatorPerspective)
                return MediaDirection.RECVONLY;
            else
                return MediaDirection.SENDONLY;
        }
        else if (senders == SendersEnum.both)
            return MediaDirection.SENDRECV;
        else //if (senders == SendersEnum.none)
            return MediaDirection.INACTIVE;
    }

    /**
     * Returns the default candidate for the specified content <tt>content</tt>.
     * The method is used when establishing new calls and we need a default
     * candidate to initiate our stream with before we've discovered the one
     * that ICE would pick.
     *
     * @param content the stream whose default candidate we are looking for.
     *
     * @return a {@link MediaStreamTarget} containing the default
     * <tt>candidate</tt>s for the stream described in <tt>content</tt> or
     * <tt>null</tt>, if for some reason, the packet does not contain any
     * candidates.
     */
    public static MediaStreamTarget extractDefaultTarget(
            ContentPacketExtension content)
    {
        IceUdpTransportPacketExtension transport
            = content.getFirstChildOfType(IceUdpTransportPacketExtension.class);

        return (transport == null) ? null : extractDefaultTarget(transport);
    }

    public static MediaStreamTarget extractDefaultTarget(
            IceUdpTransportPacketExtension transport)
    {
        //extract the default rtp candidate:
        CandidatePacketExtension rtpCand
            = getFirstCandidate(
                    transport,
                    CandidatePacketExtension.RTP_COMPONENT_ID);

        if (rtpCand == null)
            return null;

        InetAddress rtpAddress = null;

        try
        {
            rtpAddress = NetworkUtils.getInetAddress(rtpCand.getIP());
        }
        catch (UnknownHostException exc)
        {
            throw new IllegalArgumentException(
                    "Failed to parse address " + rtpCand.getIP(),
                    exc);
        }

        //rtp port
        int rtpPort = rtpCand.getPort();
        InetSocketAddress rtpTarget
            = new InetSocketAddress(rtpAddress, rtpPort);

        //extract the RTCP candidate
        CandidatePacketExtension rtcpCand
            = getFirstCandidate(
                    transport,
                    CandidatePacketExtension.RTCP_COMPONENT_ID);
        InetSocketAddress rtcpTarget;

        if( rtcpCand == null)
        {
            rtcpTarget = new InetSocketAddress(rtpAddress, rtpPort + 1);
        }
        else
        {
            InetAddress rtcpAddress = null;

            try
            {
                rtcpAddress = NetworkUtils.getInetAddress(rtcpCand.getIP());
            }
            catch (UnknownHostException exc)
            {
                throw new IllegalArgumentException(
                        "Failed to parse address " + rtcpCand.getIP(),
                        exc);
            }

            //rtcp port
            int rtcpPort = rtcpCand.getPort();

            rtcpTarget = new InetSocketAddress(rtcpAddress, rtcpPort);
        }

        return new MediaStreamTarget(rtpTarget, rtcpTarget);
    }

    /**
     * Returns the first candidate for the specified <tt>componentID</tt> or
     * null if no such component exists.
     *
     * @param content the {@link ContentPacketExtension} that we'll be searching
     * for a component.
     * @param componentID the id of the component that we are looking for
     * (e.g. 1 for RTP, 2 for RTCP);
     *
     * @return the first candidate for the specified <tt>componentID</tt> or
     * null if no such component exists.
     */
    public static CandidatePacketExtension getFirstCandidate(
            ContentPacketExtension content,
            int componentID)
    {
        //passing IceUdp would also return RawUdp transports as one extends
        //the other.
        IceUdpTransportPacketExtension transport
            = content.getFirstChildOfType(IceUdpTransportPacketExtension.class);

        return
            (transport == null)
                ? null
                : getFirstCandidate(transport, componentID);
    }

    public static CandidatePacketExtension getFirstCandidate(
            IceUdpTransportPacketExtension transport,
            int componentID)
    {
        for(CandidatePacketExtension cand : transport.getCandidateList())
        {
            //we don't care about remote candidates!
            if (!(cand instanceof RemoteCandidatePacketExtension)
                    && (cand.getComponent() == componentID))
            {
                return cand;
            }
        }
        return null;
    }

    /**
     * Creates a new {@link ContentPacketExtension} instance according to the
     * specified <tt>formats</tt>, <tt>connector</tt> and <tt>direction</tt>,
     * and using the <tt>dynamicPayloadTypes</tt> registry to handle dynamic
     * payload type registrations. The type (e.g. audio/video) of the media
     * description is determined via from the type of the first
     * {@link MediaFormat} in the <tt>formats</tt> list.
     *
     * @param creator indicates whether the person who originally created this
     * content was the initiator or the responder of the jingle session
     * @param contentName the name of the content element as indicator by the
     * creator or, in case we are the creators: as we'd like it to be.
     * @param formats the list of formats that should be advertised in the newly
     * created content extension.
     * @param senders indicates the direction of the media in this stream.
     * @param rtpExtensions a list of <tt>RTPExtension</tt>s supported by the
     * <tt>MediaDevice</tt> that we will be advertising.
     * @param dynamicPayloadTypes a reference to the
     * <tt>DynamicPayloadTypeRegistry</tt> that we should be using to lookup
     * and register dynamic RTP mappings.
     * @param rtpExtensionsRegistry a reference to the
     * <tt>DynamicRTPExtensionRegistry</tt> that we should be using to lookup
     * and register URN to ID mappings.
     *
     * @return the newly create SDP <tt>MediaDescription</tt>.
     */
    public static ContentPacketExtension createDescription(
                            CreatorEnum                  creator,
                            String                       contentName,
                            SendersEnum                  senders,
                            List<MediaFormat>            formats,
                            List<RTPExtension>           rtpExtensions,
                            DynamicPayloadTypeRegistry   dynamicPayloadTypes,
                            DynamicRTPExtensionsRegistry rtpExtensionsRegistry)
    {
        ContentPacketExtension content = new ContentPacketExtension();
        RtpDescriptionPacketExtension description
                                    = new RtpDescriptionPacketExtension();

        content.setCreator(creator);
        content.setName(contentName);

        //senders - only if we have them and if they are different from default
        if(senders != null && senders != SendersEnum.both)
            content.setSenders(senders);

        //RTP description
        content.addChildExtension(description);
        description.setMedia(formats.get(0).getMediaType().toString());

        //now fill in the RTP description
        for(MediaFormat fmt : formats)
        {
            description.addPayloadType(
                            formatToPayloadType(fmt, dynamicPayloadTypes));
        }

        // extmap attributes
        if (rtpExtensions != null && rtpExtensions.size() > 0)
        {
            for (RTPExtension extension : rtpExtensions)
            {
                byte extID
                    = rtpExtensionsRegistry.obtainExtensionMapping(extension);
                URI uri = extension.getURI();
                MediaDirection extDirection = extension.getDirection();
                String attributes = extension.getExtensionAttributes();
                SendersEnum sendersEnum = getSenders(extDirection,
                        false);
                RTPHdrExtPacketExtension ext = new RTPHdrExtPacketExtension();

                ext.setURI(uri);
                ext.setSenders(sendersEnum);
                ext.setID(Byte.toString(extID));
                ext.setAttributes(attributes);

                description.addChildExtension(ext);
            }
        }
        return content;
    }

    /**
     * Converts a specific {@link MediaFormat} into a new
     * {@link PayloadTypePacketExtension} instance.
     *
     * @param format the <tt>MediaFormat</tt> we'd like to convert.
     * @param ptRegistry the {@link DynamicPayloadTypeRegistry} to use for
     * formats that don't have a static pt number.
     *
     * @return the new <tt>PayloadTypePacketExtension</tt> which contains
     * <tt>format</tt>'s parameters.
     */
    public static PayloadTypePacketExtension formatToPayloadType(
            MediaFormat format,
            DynamicPayloadTypeRegistry ptRegistry)
    {
        PayloadTypePacketExtension ptExt = new PayloadTypePacketExtension();

        int payloadType = format.getRTPPayloadType();

        if (payloadType == MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN)
                payloadType = ptRegistry.obtainPayloadTypeNumber(format);

        ptExt.setId(payloadType);
        ptExt.setName(format.getEncoding());

        if(format instanceof AudioMediaFormat)
            ptExt.setChannels(((AudioMediaFormat)format).getChannels());

        ptExt.setClockrate((int)format.getClockRate());

        /*
         * Add the format parameters and the advanced attributes (as parameter
         * packet extensions).
         */
        for(Map.Entry<String, String> entry :
            format.getFormatParameters().entrySet())
        {
            ParameterPacketExtension ext = new ParameterPacketExtension();
            ext.setName(entry.getKey());
            ext.setValue(entry.getValue());
            ptExt.addParameter(ext);
        }
        for(Map.Entry<String, String> entry :
            format.getAdvancedAttributes().entrySet())
        {
            ParameterPacketExtension ext = new ParameterPacketExtension();
            ext.setName(entry.getKey());
            ext.setValue(entry.getValue());
            ptExt.addParameter(ext);
        }

        return ptExt;
    }

    /**
     * Returns the <tt>MediaType</tt> for <tt>content</tt> by looking for it
     * in the <tt>content</tt>'s <tt>description</tt>, if any.
     *
     * @param content the content to return the <tt>MediaType</tt> of
     * @return the <tt>MediaType</tt> for <tt>content</tt> by looking for it
     * in the <tt>content</tt>'s <tt>description</tt>, if any.
     * <tt>contentName</tt>
     */
    public static MediaType getMediaType(ContentPacketExtension content)
    {
        if (content == null)
            return null;

        // We will use content name for determining media type
        // if no RTP description is present(SCTP connection case)
        String mediaTypeName = content.getName();

        RtpDescriptionPacketExtension desc = getRtpDescription(content);
        if (desc != null)
        {
            String rtpMedia = desc.getMedia().toLowerCase();
            if (!StringUtils.isNullOrEmpty(rtpMedia))
            {
                mediaTypeName = rtpMedia;
            }
        }
        if ("application".equals(mediaTypeName))
        {
            return MediaType.DATA;
        }
        return MediaType.parseString(mediaTypeName);
    }

}
