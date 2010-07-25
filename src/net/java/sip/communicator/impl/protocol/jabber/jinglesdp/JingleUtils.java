/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.jinglesdp;

import java.net.*;
import java.util.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
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
     * The <tt>Logger</tt> used by the <tt>JingleUtils</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(JingleUtils.class
                    .getName());

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
        return (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);
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
            byte pt = (byte)ptExt.getID();
            List<ParameterPacketExtension> params = ptExt.getParameters();

            //convert params to a name:value map
            Map<String, String> paramsMap = new Hashtable<String, String>();

            for(ParameterPacketExtension param : params)
                paramsMap.put(param.getName(), param.getValue());

            //now create the format.
            MediaFormat format = JabberActivator.getMediaService()
                .getFormatFactory().createMediaFormat(
                    pt, ptExt.getName(), (double)ptExt.getClockrate(),
                    ptExt.getChannels(), paramsMap, null);

            //continue if our media service does not know this format
            if(format == null)
                continue;

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

            mediaFmts.add(format);
        }

        return mediaFmts;
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

        if(extmapList.size() == 0)
            return null;

        for (ExtmapPacketExtension extmap : extmapList)
        {
            RTPExtension rtpExtension = new RTPExtension(extmap.getUri(),
                            MediaDirection.parseString(extmap.getDirection()),
                            extmap.getAttributes());;

            if(rtpExtension != null)
                extensionsList.add( rtpExtension );
        }

        return extensionsList;
    }

    /**
     * Determines the direction of the media stream that <tt>content</tt>
     * describes and returns the corresponding <tt>MediaDirection</tt> enum
     * entry. The method looks for a direction specifier attribute (i.e. the
     * content 'senders' attribute) or the absence thereof and returns the
     * corresponding <tt>MediaDirection</tt> entry. The
     * <tt>initiatorPerspectice</tt> allows callers to specify whether the
     * direction is to be considered from the session initator's perspective
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
     * Determines the direction of the media stream that <tt>content</tt>
     * describes and returns the corresponding <tt>MediaDirection</tt> enum
     * entry from the initiator perspective. This means that an
     * <tt>initiator</tt> value would be translated to {@link
     * MediaDirection#SENDONLY} and not {@link MediaDirection#RECVONLY} which
     * would be the case if we were considering the direction from the
     * responder's perspective.
     *
     * @param content the description of the media stream whose direction
     * we are trying to determine.
     *
     * @return one of the <tt>MediaDirection</tt> values indicating the
     * direction of the media steam described by <tt>content</tt>.
     *
     * @see JingleUtils#getDirection(ContentPacketExtension, boolean)
     */
    public static MediaDirection getDirection(ContentPacketExtension content)
    {
        return getDirection(content, true);
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
                "Failed to parse address " + rtpCand.getIP(), exc);
        }

        //rtp port
        int rtpPort = rtpCand.getPort();

        InetSocketAddress rtpTarget
                                = new InetSocketAddress(rtpAddress, rtpPort);

        //extract the RTCP candidate
        CandidatePacketExtension rtcpCand = getFirstCandidate(content, 2);

        InetSocketAddress rtcpTarget = null;
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
                    "Failed to parse address " + rtcpCand.getIP(), exc);
            }

            //rtcp port
            int rtcpPort = rtcpCand.getPort();
            rtpTarget = new InetSocketAddress(rtcpAddress, rtcpPort);
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
            = (IceUdpTransportPacketExtension)content.getFirstChildOfType(
                                IceUdpTransportPacketExtension.class);

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

}
