/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.sdp;

import java.net.*;
import java.util.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.media.*;
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

    /**
     * Extracts and returns the list of <tt>MediaFormat</tt>s advertised in
     * <tt>mediaDesc</tt> preserving their oder and registering dynamic payload
     * type numbers in the specified <tt>ptRegistry</tt>.
     *
     * @param mediaDesc the <tt>MediaDescription</tt> that we'd like to probe
     * for a list of <tt>MediaFormat</tt>s
     * @param ptRegistry a reference to the <tt>DynamycPayloadTypeRegistry</tt>
     * where we should be registering newly added payloat type number to format
     * mappings.
     *
     * @return an ordered list of <tt>MediaFormat</tt>s as advertised in the
     * <tt>mediaDesc</tt> description.
     */
    @SuppressWarnings("unchecked")//legacy code from jain-sdp
    public static List<MediaFormat> extractFormats(
                                         MediaDescription mediaDesc,
                                         DynamicPayloadTypeRegistry ptRegistry)
    {
        List<MediaFormat> mediaFmts = new ArrayList();

        Vector<String> formatStrings = null;
        try
        {
            formatStrings
                = (Vector<String>)mediaDesc.getMedia().getMediaFormats(true);
        }
        catch (SdpParseException exc)
        {
            //this is never thrown by the implementation because it doesn't
            //do lazy parsing ... and whose idea was it to have an exception
            //here anyway ?!?
            logger.debug("A funny thing just happened ...", exc);
            return mediaFmts;
        }

        Iterator<String> fmtStringsIter = formatStrings.iterator();

        while( fmtStringsIter.hasNext() )
        {
            String ptStr = fmtStringsIter.next();
            byte pt = -1;

            try
            {
                pt = Byte.parseByte(ptStr);
            }
            catch (NumberFormatException e)
            {
                //weird payload type. contact is sending rubbish. try to ignore
                logger.debug(ptStr + " is not a valid payload type", e);
                continue;
            }

            Attribute rtpmap = null;
            try
            {
                rtpmap = findPayloadTypeSpecificAttribute(
                    mediaDesc.getAttributes(false), SdpConstants.RTPMAP, pt);
            }
            catch (SdpException e)
            {
                //there was a problem parsing the rtpmap. try to ignore.
                logger.debug(
                   rtpmap + " does not seem like a valid rtpmap: attribute", e);
            }

            Attribute fmtp = null;
            try
            {
                fmtp = findPayloadTypeSpecificAttribute(
                    mediaDesc.getAttributes(false), SdpConstants.FMTP, pt);
            }
            catch (SdpException exc)
            {
                //there was a problem parsing the fmtp: try to ignore.
                logger.debug(
                   fmtp + " does not seem like a valid fmtp: attribute", exc);
            }

            MediaFormat mediaFormat = null;
            try
            {
                mediaFormat = createFormat(pt, rtpmap, fmtp);
            }
            catch (SdpException e)
            {
                //this is never thrown by the implementation because it doesn't
                //do lazy parsing ... and whose idea was it to have an exception
                //here anyway ?!?
                logger.debug("A funny thing just happened ...");
                continue;
            }

            mediaFmts.add(mediaFormat);

        }

        return mediaFmts;
    }

    /**
     * Creates and returns <tt>MediaFormat</tt> instance corresponding to the
     * specified <tt>payloadType</tt> and the parameters in the <tt>rtpmap</tt>
     * and <tt>fmtp</tt> <tt>Attribute</tt>s. The method would only return
     * <tt>MediaFormat</tt> instances for formats known to our media service
     * implementation and returns <tt>null</tt> otherwise.
     *
     * @param payloadType a static or dynamic payload type number that
     * determines the encoding of the format we'd like to create.
     * @param rtpmap an SDP <tt>Attribute</tt> mapping the <tt>payloadType</tt>
     * to an encoding name.
     * @param fmtp a list of format specific parameters
     *
     * @return a <tt>MediaForamt</tt> instance corresponding to the specified
     * <tt>payloadType</tt> and <tt>rtpmap</tt>, and <tt>fmtp</tt> attributes
     * or <tt>null</tt> if such a format is not currently supported by our
     * media service implementation.
     *
     * @throws SdpException never, the exception is only there because the
     * jain-sdp API declares exceptions in case of impls using lazy parsing but
     * the one in the jain-sip-ri isn't doing it.
     */
    private static MediaFormat createFormat(byte      payloadType,
                                            Attribute rtpmap,
                                            Attribute fmtp)
        throws SdpException
    {
        //default values in case rtpmap is null.
        String encoding = null;
        int clockRate = -1;
        int numChannels = 1;

        if (rtpmap != null)
        {
            String rtpmapValue = rtpmap.getValue();

            //rtpmapValue looks sth like this: "98 H264/90000" or
            //"97 speex/16000/2" we need to extract the encoding name, the clock
            // rate and the number of channels if any
            // if at any point we determine there's something wrong with the
            // rtpmap we bail out and try to create a format based on the
            // payloadType only.

            //first strip the payload type
            StringTokenizer tokenizer
                = new StringTokenizer(rtpmapValue, " /", false);

            //skip payload type number (mandatory)
            if(tokenizer.hasMoreTokens())
            {
                tokenizer.nextToken();
            }

            //encoding name (mandatory)
            if(tokenizer.hasMoreTokens())
            {
                encoding = tokenizer.nextToken();
            }

            //clock rate (mandatory)
            if(tokenizer.hasMoreTokens())
            {
                clockRate = Integer.parseInt(tokenizer.nextToken());
            }

            //number of channels (optional)
            if(tokenizer.hasMoreTokens())
            {
                String nChansStr = tokenizer.nextToken();

                try
                {
                    numChannels = Integer.parseInt(nChansStr);
                }
                catch(NumberFormatException exc)
                {
                    logger.debug(nChansStr
                                    + " is not a valid number of channels.");
                }
            }
        }

        //Format parameters
        Map<String, String> fmtParamsMap = parseFmtpAttribute(fmtp);

        //now create the format.
        MediaFormat format = SipActivator.getMediaService().getFormatFactory()
            .createMediaFormat(payloadType, encoding, clockRate,
                               numChannels, fmtParamsMap);

        return format;
    }

    /**
     * Parses the value of the <tt>fmtpAttr</tt> attribute into a format
     * parameters <tt>Map</tt> and returns it.
     *
     * @param fmtpAttr the SDP attribute containing the format params that we'd
     * like to parse.
     *
     * @return a (possibly empty) <tt>Map</tt> containing the format parameters
     * resulting from parsing <tt>fmtpAttr</tt>'s value.
     *
     * @throws SdpException never, the exception is only there because the
     * jain-sdp API declares exceptions in case of impls using lazy parsing but
     * the one in the jain-sip-ri isn't doing it.
     */
    private static Map<String, String> parseFmtpAttribute(Attribute fmtpAttr)
        throws SdpException
    {
        /* a few examples of what format params may look like:
         *
         * //ilbc
         * a=fmtp:97 mode=20
         *
         * //H264
         * a=fmtp:98 profile-level-id=42A01E;
         *       sprop-parameter-sets=Z0IACpZTBYmI,aMljiA==
         *
         * a=fmtp:100 profile-level-id=42A01E; packetization-mode=2;
         *       sprop-parameter-sets=Z0IACpZTBYmI,aMljiA==;
         *       sprop-interleaving-depth=45; sprop-deint-buf-req=64000;
         *       sprop-init-buf-time=102478; deint-buf-cap=128000
         *
         * //speex
         * a=fmtp:97 mode="1,any";vbr=on
         *
         * //yes ... it's funny how sometimes there are spaces after the colons
         * //and sometimes not
         */

        Map<String, String> fmtParamsMap = new Hashtable<String, String>();
        String fmtpValue = fmtpAttr.getValue();

        StringTokenizer tokenizer
            = new StringTokenizer(fmtpValue, " ;", false);

        //skip payload type number (mandatory)
        if(! tokenizer.hasMoreTokens())
            return null;

        while (tokenizer.hasMoreTokens())
        {
            //every token looks sth like "name=value". nb: value may contain
            //other "=" signs so only tokenize by semicolons and use the 1st one
            String token = tokenizer.nextToken();
            int indexOfEq = token.indexOf("=");

            if (indexOfEq == -1 || indexOfEq == token.length() -1)
                continue; // there's something wrong with this param - move on.

            String paramName = token.substring(0, indexOfEq );
            String paramValue = token.substring(indexOfEq + 1, token.length());


            fmtParamsMap.put(paramName, paramValue);
        }

        return fmtParamsMap;
    }

    /**
     * Tries to find an attribute with the specified <tt>attibuteName</tt>
     * pertaining to the specified  <tt>payloadType</tt> in the
     * <tt>mediaAttributes</tt> list  and returns it if it exists.
     *
     * @param mediaAttributes the list of <tt>Attribute</tt> fields where we
     * are to look for the attribute.
     * @param payloadType the payloadType that we are trying to find.
     * @param attributeName the name of the attribute we are looking for.
     *
     * @return the first Attribute whose name matches <tt>attributeName</tt>
     * and whose value pertains to <tt>payloadType</tt> or <tt>null</tt> if no
     * such attribute was found.
     *
     * @throws SdpException when ... well never really, it's there just for ...
     * fun?
     */
    private static Attribute findPayloadTypeSpecificAttribute(
                                    Vector<Attribute> mediaAttributes,
                                    String            attributeName,
                                    byte              payloadType)
        throws SdpException
    {
        if( mediaAttributes == null || mediaAttributes.size() == 0)
            return null;

        Iterator<Attribute> attIter = mediaAttributes.iterator();
        String ptStr = Byte.toString(payloadType);

        while(attIter.hasNext())
        {
            Attribute attr = attIter.next();

            if(!attributeName.equals(attr.getName()))
                continue;

            String attrValue = attr.getValue();

            if(attrValue == null)
                continue;

            attrValue = attrValue.trim();

            if(!attrValue.startsWith(ptStr + " "))
                continue;

            //that's it! we have the attribute we are looking for.
            return attr;
        }

        return null;
    }

    /**
     * Returns a <tt>MediaStreamTarget</tt> instance reflecting the address
     * pair (RTP + RTCP) where we should send media in this stream. The method
     * takes into account the possibility to have a connection (i.e. c=)
     * parameter in either the media description (i.e. <tt>mediaDesc</tt>) or
     * (i.e. <tt>sessDesc</tt>), or both and handles their priority as defined
     * by the SDP spec [RFC 4566].
     *
     * @param mediaDesc the media description that we'd like to extract our
     * RTP and RTCP destination addresses.
     * @param sessDesc the session description that we received
     * <tt>mediaDesc</tt> in.
     *
     * @return a <tt>MediaStreamTarget</tt> containing the RTP and RTCP
     * destinations that our interlocutor has specified for this media stream.
     *
     * @throws IllegalArgumentException in case we couldn't find connection
     * data or stumble upon other problems while analyzing the SDP.
     */
    public static MediaStreamTarget extractDefaultTarget(
                                         MediaDescription mediaDesc,
                                         SessionDescription sessDesc)
        throws IllegalArgumentException
    {
        //first check if there's a "c=" field in the media description
        Connection conn = mediaDesc.getConnection();

        if ( conn == null)
        {
            //no "c=" in the media description. check the session level.
            conn = sessDesc.getConnection();

            if (conn == null)
            {
                throw new IllegalArgumentException(
                    "No \"c=\" field in the following media description nor "
                    +"in the enclosing session:\n"+ mediaDesc.toString());
            }
        }

        String address;
        try
        {
            address = conn.getAddress();
        }
        catch (SdpParseException exc)
        {
            //this can't actually happen as there's no parsing here. the
            //exception is just inherited from the jain-sdp api. we are
            //rethrowing only because there's nothing else we could do.
            throw new IllegalArgumentException(
                            "Couldn't extract connection address.", exc);
        }

        InetAddress inetAddress = null;
        try
        {
            inetAddress = NetworkUtils.getInetAddress(address);
        }
        catch (UnknownHostException exc)
        {
            throw new IllegalArgumentException(
                "Failed to parse address " + address, exc);
        }

        //ip address (media or session level c)

        //rtp port
        //rtcp port ( and address? )
        return null;
    }

    public static MediaDirection getDirection( MediaDescription mediaDesc )
    {
        return null;
    }

    public static URL getCallInfoURL(SessionDescription sessDesc)
    {
        return null;
    }


    /**
     * Creates a new <tt>MediaDescription</tt> instance according to the
     * specified <tt>formats</tt>, <tt>connector</tt> and <tt>direction</tt>,
     * and using the <tt>dynamicPayloadTypes</tt> registry to handle dynamic
     * payload type registrations. The type (e.g. audio/video) of the media
     * description is determined via from the type of the first
     * <tt>MediaFormat</tt> in the <tt>formats</tt> list.
     *
     * @param formats the list of formats that should be advertised in the newly
     * created <tt>MediaDescription</tt>.
     * @param connector the socket couple that will be used for the media stream
     * which we are advertising with the media description created here.
     * @param direction the direction of the media stream that we are describing
     * here.
     * @param dynamicPayloadTypes a reference to the
     * <tt>DynamicPayloadTypeRegistry</tt>
     *
     * @return the newly create SDP <tt>MediaDescription</tt>.
     *
     * @throws OperationFailedException in case we fail to get payload type
     * numbers for dynamic payload types or in case our SDP generation fails for
     * some other reason.
     */
    public static MediaDescription createMediaDescription(
                    List<MediaFormat>          formats,
                    StreamConnector            connector,
                    MediaDirection             direction,
                    DynamicPayloadTypeRegistry dynamicPayloadTypes)
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

            int payloadType = format.getRTPPayloadType();

            // is this a dynamic payload type.
            if (payloadType == MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN)
            {
                try
                {
                    payloadType
                        = dynamicPayloadTypes.obtainPayloadTypeNumber(format);
                }
                catch (IllegalStateException exception)
                {
                    //means we ran out of dynamic rtp payload types.
                    throw new OperationFailedException(
                          "Failed to allocate a new dynamic PT number.",
                          OperationFailedException.INTERNAL_ERROR,
                          exception);
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
                + format.getClockRate() + numChannelsStr);

            mediaAttributes.add(rtpmap);

            // a=fmtp:
            Attribute fmtp = sdpFactory.createAttribute(SdpConstants.FMTP,
                            payloadType + " " + encodeFmtp(format));

            mediaAttributes.add(fmtp);

            payloadTypesArray[i] = payloadType;
        }

        // rtcp: (only include it if different from the default (i.e. rtp + 1)
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
        }
        catch (Exception cause)
        {
            // this is very unlikely to happen but we should still re-throw
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                            "Failed to create a media description",
                            OperationFailedException.INTERNAL_ERROR, cause,
                            logger);
        }

        return mediaDesc;
    }

    /**
     * Encodes in an SDP string all <tt>format</tt> specific codec parameters.
     *
     * @param format a reference to the <tt>MediaFormat</tt> instance whose
     * parameters we'd like to encode.
     *
     * @return a String representation of the <tt>format</tt>s codec parameters.
     */
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

    /**
     * Creates an <tt>Attribute</tt> instance reflecting the value of the
     * <tt>direction</tt> parameter (e.g. a=sendrecv, a=recvonly, etc.).
     *
     * @param direction the direction that we'd like to convert to an SDP
     * <tt>Attribute</tt>.
     *
     * @return an SDP <tt>Attribute</tt> field translating the value of the
     * <tt>direction</tt> parameter.
     */
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
