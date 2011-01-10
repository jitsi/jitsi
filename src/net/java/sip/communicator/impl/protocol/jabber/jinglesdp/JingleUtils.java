/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.jinglesdp;

import java.net.*;
import java.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.CandidateType;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.NetworkUtils; //disambiguates with ice4j's network utils.
import static net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.*;

/**
 * The class contains a number of utility methods that are meant to facilitate
 * creating and parsing jingle media rtp description descriptions and
 * transports.
 *
 * @author Emil Ivov
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
     * @param payloadType the {@link PayloadTypePacketExtension} that we'd like
     * to parse into a {@link MediaFormat}.
     * @param ptRegistry the {@link DynamicPayloadTypeRegistry} that we would
     * use for the registration of possible dynamic payload types.
     *
     * @return the {@link MediaFormat} described in the <tt>payloadType</tt>
     * extension or <tt>null</tt> if we don't recognize the format.
     */
    public static MediaFormat payloadTypeToMediaFormat(
                    PayloadTypePacketExtension payloadType,
                    DynamicPayloadTypeRegistry ptRegistry)
    {
        byte pt = (byte)payloadType.getID();

        //convert params to a name:value map
        List<ParameterPacketExtension> params = payloadType.getParameters();
        Map<String, String> paramsMap = new HashMap<String, String>();

        for(ParameterPacketExtension param : params)
            paramsMap.put(param.getName(), param.getValue());

        //now create the format.
        MediaFormat format
            = JabberActivator.getMediaService().getFormatFactory()
                    .createMediaFormat(
                            pt,
                            payloadType.getName(),
                            (double)payloadType.getClockrate(),
                            payloadType.getChannels(),
                            paramsMap,
                            null);

        //we don't seem to know anything about this format
        if(format == null)
        {
            format =
                JabberActivator.getMediaService().getFormatFactory().
                    createUnknownMediaFormat(MediaType.AUDIO);
        }

        /*
         * We've just created a MediaFormat for the specified payloadType
         * so we have to remember the mapping between the two so that we
         * don't, for example, map the same payloadType to a different
         * MediaFormat at a later time when we do automatic generation
         * of payloadType in DynamicPayloadTypeRegistry.
         */
        /*
         * TODO What is expected to happen when the remote peer tries to
         * re-map a payloadType in its answer to a different MediaFormat
         * than the one we've specified in our offer?
         */
        if ((pt >= MediaFormat.MIN_DYNAMIC_PAYLOAD_TYPE)
                && (pt <= MediaFormat.MAX_DYNAMIC_PAYLOAD_TYPE)
                && (ptRegistry.findFormat(pt) == null))
            ptRegistry.addMapping(format, pt);

        return format;
    }

    /**
     * Extracts and returns the list of <tt>RTPExtension</tt>s advertised in
     * <tt>desc</tt> and registers newly encountered ones into the specified
     * <tt>extMap</tt>. The method returns an empty list in case there were no
     * <tt>extmap</tt> advertisements in <tt>desc</tt>.
     *
     * @param desc the {@link RtpDescriptionPacketExtension} that we'd like to
     * probe for a list of {@link RTPExtension}s
     * @param extMap a reference to the <tt>DynamycRTPExtensionsRegistry</tt>
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

        List<ExtmapPacketExtension> extmapList = desc.getExtmapList();

        for (ExtmapPacketExtension extmap : extmapList)
        {
            RTPExtension rtpExtension
                    = new RTPExtension(
                            extmap.getUri(),
                            MediaDirection.parseString(extmap.getDirection()),
                            extmap.getAttributes());

            if(rtpExtension != null)
                extensionsList.add( rtpExtension );
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
        //extract the default rtp candidate:
        CandidatePacketExtension rtpCand = getFirstCandidate(content, 1);

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
        CandidatePacketExtension rtcpCand = getFirstCandidate(content, 2);

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
                                            int                    componentID)
    {
        //passing IceUdp would also return RawUdp transports as one extends
        //the other.
        IceUdpTransportPacketExtension transport
            = content.getFirstChildOfType(IceUdpTransportPacketExtension.class);

        if ( transport == null)
            return null;

        for(CandidatePacketExtension cand : transport.getCandidateList())
        {
            //we don't care about remote candidates!
            if (cand instanceof RemoteCandidatePacketExtension )
                continue;
            if (cand.getComponent() == componentID)
                return cand;
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

        //senders - ony if we have them and if they are different from default
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

        return content;
    }

    /**
     * Converts <tt>format</tt> into a {@link PayloadTypePacketExtension} instance.
     *
     * @param format the {@link MediaFormat} we'd like to convert.
     * @param ptRegistry the {@link DynamicPayloadTypeRegistry} to use for
     * formats that don't have a static pt number.
     *
     * @return the newly created {@link PayloadTypePacketExtension} that
     * contains <tt>format</tt>'s parameters.
     */
    public static PayloadTypePacketExtension formatToPayloadType(
                                        MediaFormat               format,
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

        /* add parameters */
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
     * Converts the ICE media <tt>stream</tt> and its local candidates into a
     * {@link IceUdpTransportPacketExtension}.
     *
     * @param stream the {@link IceMediaStream} that we'd like to describe in
     * XML.
     *
     * @return the {@link IceUdpTransportPacketExtension} that we
     */
    public static IceUdpTransportPacketExtension createTransport(
                                                        IceMediaStream stream)
    {
        IceUdpTransportPacketExtension trans
            = new IceUdpTransportPacketExtension();
        Agent iceAgent = stream.getParentAgent();

        trans.setUfrag(iceAgent.getLocalUfrag());
        trans.setPassword(iceAgent.getLocalPassword());

        for(Component component : stream.getComponents())
        {
            for(Candidate candidate : component.getLocalCandidates())
                trans.addCandidate(createCandidate(candidate));
        }

        return trans;
    }

    /**
     * Creates a {@link CandidatePacketExtension} and initializes it so that it
     * would describe the state of <tt>candidate</tt>
     *
     * @param candidate the ICE4J {@link Candidate} that we'd like to convert
     * into an XMPP packet extension.
     *
     * @return a new {@link CandidatePacketExtension} corresponding to the state
     * of the <tt>candidate</tt> candidate.
     */
    private static CandidatePacketExtension createCandidate(Candidate candidate)
    {
        CandidatePacketExtension packet = new CandidatePacketExtension();

        //TODO: XMPP expects int values as foundations. Luckily that's exactly
        //what ice4j is using under the hood at this time. still, we'd need to
        //make sure that doesn't change ... possibly by setting a property there
        packet.setFoundation(Integer.parseInt( candidate.getFoundation()));

        Component component = candidate.getParentComponent();

        packet.setComponent(component.getComponentID());
        packet.setProtocol(candidate.getTransport().toString());
        packet.setPriority(candidate.getPriority());
        packet.setGeneration(
                component.getParentStream().getParentAgent().getGeneration());

        TransportAddress transportAddress = candidate.getTransportAddress();

        packet.setIP(transportAddress.getHostAddress());
        packet.setPort(transportAddress.getPort());

        packet.setType(CandidateType.valueOf(candidate.getType().toString()));

        TransportAddress relAddr = candidate.getRelatedAddress();

        if(relAddr != null)
        {
            packet.setRelAddr(relAddr.getHostAddress());
            packet.setRelPort(relAddr.getPort());
        }

        /*
         * FIXME The XML schema of XEP-0176: Jingle ICE-UDP Transport Method
         * specifies the network attribute as required.
         */
        packet.setNetwork(0);

        return packet;
    }
}
