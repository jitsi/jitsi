/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.sdp;

import java.util.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The class contains a number of utility methods that are meant to facilitate
 * creating and parsing SDP descriptions.
 *
 * @author Emil Ivov
 */
public class SdpUtils
{
    /**
     * Our class logger.
     */
    private static Logger logger = Logger.getLogger(SdpUtils.class);

    /**
     * A reference to the currently valid SDP factory instance.
     */
    private static final SdpFactory sdpFactory = SdpFactory.getInstance();

    /**
     * Creates an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized  <tt>s</tt>, <tt>v</tt>, and <tt>t</tt> parameters.
     *
     * @return an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized <tt>s</tt>, <tt>v</tt>, and <tt>t</tt> parameters.
     */
    public static SessionDescription createSessionDescription()
    {
        SessionDescription sessDescr = null;

        try
        {
            sessDescr = sdpFactory.createSessionDescription();

            //"v=0"
            Version v = sdpFactory.createVersion(0);

            sessDescr.setVersion(v);

            //"s=-"
            sessDescr.setSessionName(sdpFactory.createSessionName("-"));

            //"t=0 0"
            TimeDescription t = sdpFactory.createTimeDescription();
            Vector<TimeDescription> timeDescs = new Vector<TimeDescription>();
            timeDescs.add(t);

            sessDescr.setTimeDescriptions(timeDescs);

            return sessDescr;
        }
        catch (SdpException exc)
        {
            //the jain-sip implementations of the above methods do not throw
            //exceptions in the cases we are using them so falling here is
            //quite unlikely. we are logging out of mere decency :)
            logger.error("Failed to crate an SDP SessionDescription.", exc);
        }

        return sessDescr;
    }

    private static MediaDescription createMediaDescription(
                    List<MediaFormat> formats,
                    StreamConnector   connector,
                    MediaDirection    direction)
        throws OperationFailedException
    {
        int[] payloadTypesArray = new int[formats.size()];
        Vector<Attribute> mediaAttributes = new Vector<Attribute>(
                        2 * payloadTypesArray.length + 1);
        MediaType mediaType = null;

        // a=sendonly|sendrecv|recvonly|inactive
        mediaAttributes.add(createDirectionAttribute(direction));

        for (int i = 0; i < payloadTypesArray.length; i++)
        {
            MediaFormat format = formats.get(i);
            MediaType fmtMediaType = format.getMediaType();

            // determine whether we are dealing with audio or video.
            if (mediaType != null)
            {
                mediaType = fmtMediaType;
            }

            int payloadType = formats.get(i).getRTPPayloadType();

            // is this a dynamic payload type.
            if (payloadType == MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN)
            {
                Integer dynamicPT = dynamicPayloadTypes.get(format);
                if (dynamicPT == null)
                {
                    // this is the first time we see this fmt in this session
                    payloadType = nextDynamicPayloadType++;
                    dynamicPayloadTypes.put(format, new Integer(payloadType));
                } else
                {
                    // we have already registered this format for this session.
                    payloadType = dynamicPT.intValue();
                }
            }

            // a=rtpmap:
            String numChannelsStr = "";
            if (format instanceof AudioMediaFormat)
            {
                int channels = ((AudioMediaFormat) format).getChannels();
                if (channels > 1)
                    numChannelsStr = "/" + channels;
            }

            Attribute rtpmap = sdpFactory.createAttribute(SdpConstants.RTPMAP,
                            payloadType + " " + format.getEncoding() + "/"
                                            + format.getClockRate()
                                            + numChannelsStr);

            mediaAttributes.add(rtpmap);

            // a=fmtp:
            Attribute fmtp = sdpFactory.createAttribute(SdpConstants.FMTP + ":"
                            + payloadType, encodeFmtp(format));

            mediaAttributes.add(fmtp);

            payloadTypesArray[i] = payloadType;
        }

        // rtcp:
        int rtpPort = connector.getDataSocket().getLocalPort();
        int rtcpPort = connector.getControlSocket().getLocalPort();

        if ((rtpPort + 1) != rtcpPort)
        {
            Attribute rtcpAttr = sdpFactory.createAttribute("rtcp:", Integer
                            .toString(rtcpPort));
            mediaAttributes.add(rtcpAttr);
        }

        MediaDescription mediaDesc = null;
        try
        {
            mediaDesc = sdpFactory.createMediaDescription(mediaType.name(),
                            connector.getDataSocket().getLocalPort(), 1,
                            SdpConstants.RTP_AVP, payloadTypesArray);

            // add all the attributes we have created above
            mediaDesc.setAttributes(mediaAttributes);
        } catch (Exception cause)
        {
            // this is very unlikely to happen but we should still re-throw
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                            "Failed to create a media description",
                            OperationFailedException.INTERNAL_ERROR, cause,
                            logger);
        }

        // dtmf
        return mediaDesc;
    }

    private static String encodeFmtp(MediaFormat format)
    {
        Iterator<Map.Entry<String, String>> formatParamsIter = format
                        .getFormatParameters().entrySet().iterator();

        StringBuffer fmtpBuff = new StringBuffer();

        while (formatParamsIter.hasNext())
        {
            Map.Entry<String, String> ntry = formatParamsIter.next();
            fmtpBuff.append(ntry.getKey()).append("=").append(ntry.getValue());

            // add a separator in case we'd need to add more parameters
            if (formatParamsIter.hasNext())
                fmtpBuff.append(";");
        }

        return fmtpBuff.toString();
    }

    private static Attribute createDirectionAttribute(MediaDirection direction)
    {
        String dirStr;

        if(MediaDirection.SENDONLY.equals(direction))
        {
            dirStr = "sendonly";
        }
        else if(MediaDirection.RECVONLY.equals(direction))
        {
            dirStr = "recvonly";
        }
        else if(MediaDirection.SENDRECV.equals(direction))
        {
            dirStr = "sendrecv";
        }
        else
        {
            dirStr = "inactive";
        }

        return sdpFactory.createAttribute(dirStr, null);
    }
}
