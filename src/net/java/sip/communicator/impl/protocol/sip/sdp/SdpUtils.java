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
     * The name of the SDP attribute that contains RTCP address and port.
     */
    private static final String RTCP_ATTR = "rtcp";

    /**
     * Parses the specified <tt>sdp String</tt> into a
     * <tt>SessionDescription</tt> and returns it;
     *
     * @param sdp the <tt>sdp String</tt> that we'd like to parse.
     *
     * @return the <tt>SessionDescription</tt> instance corresponding to the
     * specified <tt>sdp String</tt>.
     *
     * @throws IllegalArgumentException in case <tt>sdp</tt> is not a valid
     * SDP <tt>String</tt>.
     */
    public static SessionDescription parseSdpString(String sdp)
        throws IllegalArgumentException
    {
        try
        {
            return sdpFactory.createSessionDescription(sdp);
        }
        catch (SdpParseException ex)
        {
            throw new IllegalArgumentException(
                "Failed to parse the SDP description of the peer.", ex);
        }
    }
    /**
     * Creates an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized  <tt>s</tt>, <tt>v</tt>, <tt>c</tt>, <tt>o</tt> and
     * <tt>t</tt> parameters.
     *
     * @param localAddress the <tt>InetAddress</tt> corresponding to the local
     * address that we'd like to use when talking to the remote party.
     *
     * @return an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized <tt>s</tt>, <tt>v</tt>, and <tt>t</tt> parameters.
     */
    public static SessionDescription createSessionDescription(
                    InetAddress localAddress)
    {
        return createSessionDescription(localAddress, null, null);
    }

    /**
     * Creates an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized  <tt>s</tt>, <tt>v</tt>, <tt>c</tt>, <tt>o</tt> and
     * <tt>t</tt> parameters.
     *
     * @param localAddress the <tt>InetAddress</tt> corresponding to the local
     * address that we'd like to use when talking to the remote party.
     * @param userName the user name to use in the origin parameter or
     * <tt>null</tt> in case we'd like to use a default.
     * @param mediaDescriptions a <tt>Vector</tt> containing the list of
     * <tt>MediaDescription</tt>s that we'd like to advertise (leave
     * <tt>null</tt> if you'd like to add these later).
     *
     * @return an empty instance of a <tt>SessionDescription</tt> with
     * preinitialized <tt>s</tt>, <tt>v</tt>, and <tt>t</tt> parameters.
     */
    public static SessionDescription createSessionDescription(
                                   InetAddress              localAddress,
                                   String                   userName,
                                   Vector<MediaDescription> mediaDescriptions)
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

            String addrType = localAddress instanceof Inet6Address
                ? Connection.IP6
                : Connection.IP4;

            //o
            if (userName == null)
                userName = "sip-communicator";

            Origin o = sdpFactory.createOrigin(
                userName,
                0,
                0,
                "IN",
                addrType,
                localAddress.getHostAddress());

            sessDescr.setOrigin(o);

            //c=
            Connection c = sdpFactory.createConnection(
                "IN", addrType, localAddress.getHostAddress());

            sessDescr.setConnection(c);

            if ( mediaDescriptions != null)
                sessDescr.setMediaDescriptions(mediaDescriptions);

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
     * Creates and returns a new <tt>SessionDescription</tt> that is supposed
     * to update our previous <tt>descToUpdate</tt> and advertise the brand new
     * <tt>newMediaDescriptions</tt>. The method also respects other 3264
     * policies like reusing the origing field and augmenting its version number
     * for example.
     *
     * @param descToUpdate the <tt>SessionDescription</tt> that we'd like to
     * update.
     * @param newConnectionAddress the <tt>InetAddress</tt> that we'd like to
     * use in the new <tt>c=</tt> field.
     * @param newMediaDescriptions the descriptions of the new streams that we'd
     * like to have in the updated session.
     *
     * @return a new <tt>SessionDescription</tt> that updates
     * <tt>descToUpdate</tt>;
     */
    public static SessionDescription createSessionUpdateDescription(
                          SessionDescription       descToUpdate,
                          InetAddress              newConnectionAddress,
                          Vector<MediaDescription> newMediaDescriptions)
    {
        SessionDescription update = createSessionDescription(
                        newConnectionAddress, null, newMediaDescriptions);

        //extract the previous o= field.
        //RFC 3264 says we must use it in the update and only change the ver
        try
        {
            Origin o = (Origin)descToUpdate.getOrigin().clone();

            long version = o.getSessionVersion();
            o.setSessionVersion(version + 1);

            update.setOrigin(o);
        }
        catch (Exception e)
        {
            // can't happen, ignore
            logger.info("Something very odd just happened.", e);
        }

        //now, RFC 3264 says all previous m= fields must be present and new ones
        //added at the end. We should also disable all m= fields that are not
        //present in the new version. We therefore loop through the previous m=s
        //update them along the way and then add our new descs (if any).
        Vector<MediaDescription> prevMedias
            = extractMediaDescriptions(descToUpdate);

        Vector<MediaDescription> completeMediaDescList
            = new Vector<MediaDescription>();

        //we'll be modifying the newMediaDescs list so let's make sure we don't
        //cause any trouble and clone it.
        newMediaDescriptions
            = new Vector<MediaDescription>(newMediaDescriptions);

        for(MediaDescription medToUpdate : prevMedias)
        {
            MediaType type = getMediaType(medToUpdate);
            MediaDescription desc = removeMediaDesc(newMediaDescriptions, type);

            if (desc == null)
            {
                //obviously we don't want a stream of that type so make sure
                //the old one is disabled
                desc = createDisablingAnswer(medToUpdate);
            }

            completeMediaDescList.add(desc);
        }

        //now add whatever's left;
        for(MediaDescription medToAdd : newMediaDescriptions)
        {
            completeMediaDescList.add(medToAdd);
        }

        try
        {
            update.setMediaDescriptions(completeMediaDescList);
        }
        catch (SdpException e)
        {
            // never thrown, unless completeMediaDescList is null and that
            // can't be since we just created it.
            logger.info("A crazy thing just happened.", e);
        }


        return update;
    }

    /**
     * Iterates through the <tt>descs</tt> <tt>Vector</tt> looking for a
     * <tt>MediaDescription</tt> of the specified media <tt>type</tt> and
     * then removes and return the first one it finds. Returns <tt>null</tt> if
     * the <tt>descs</tt> <tt>Vector</tt> contains no <tt>MediaDescription</tt>
     * with the specified <tt>MediaType</tt>.
     *
     * @param descs the <tt>Vector</tt> that we'd like to search for a
     * <tt>MediaDescription</tt> of the specified <tt>MediaType</tt>.
     * @param type the <tt>MediaType</tt> that we're trying to find and remove
     * from the <tt>descs Vector</tt>
     *
     * @return the first <tt>MediaDescription</tt> of the specified
     * <tt>type</tt> or <tt>null</tt> if no such description was found in the
     * <tt>descs Vector</tt>.
     */
    private static MediaDescription removeMediaDesc(
                                            Vector<MediaDescription> descs,
                                            MediaType                type)
    {
        Iterator<MediaDescription> descsIter = descs.iterator();
        while( descsIter.hasNext())
        {
            MediaDescription mDesc = descsIter.next();

            if (getMediaType(mDesc) == type)
            {
                descsIter.remove();
                return mDesc;
            }
        }

        return null;
    }

    /**
     * Extracts and returns the list of <tt>MediaFormat</tt>s advertised in
     * <tt>mediaDesc</tt> preserving their oder and registering dynamic payload
     * type numbers in the specified <tt>ptRegistry</tt>. Note that this method
     * would only include in the result list <tt>MediaFormat</tt> instances
     * that are currently supported by our <tt>MediaService</tt> implementation
     * and enabled in its configuration. This means that the method could
     * return an empty list even if there were actually some formats in the
     * <tt>mediaDesc</tt> if we support none of them or if all these we support
     * are not enabled in the <tt>MediaService</tt> configuration form.
     *
     * @param mediaDesc the <tt>MediaDescription</tt> that we'd like to probe
     * for a list of <tt>MediaFormat</tt>s
     * @param ptRegistry a reference to the <tt>DynamycPayloadTypeRegistry</tt>
     * where we should be registering newly added payload type number to format
     * mappings.
     *
     * @return an ordered list of <tt>MediaFormat</tt>s that are both advertised
     * in the <tt>mediaDesc</tt> description and supported by our
     * <tt>MediaService</tt> implementation.
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
                    mediaDesc.getAttributes(false), "fmtp", pt);
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

            if (mediaFormat != null)
            {
                //only add the format if it non-null (i.e. if our MediaService
                //supports it
                mediaFmts.add(mediaFormat);
            }

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
        double clockRate = -1;
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
                clockRate = Double.parseDouble(tokenizer.nextToken());
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
        Map<String, String> fmtParamsMap = null;
        if ( fmtp != null)
            fmtParamsMap = parseFmtpAttribute(fmtp);

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

        InetAddress rtpAddress = null;

        try
        {
            rtpAddress = NetworkUtils.getInetAddress(address);
        }
        catch (UnknownHostException exc)
        {
            throw new IllegalArgumentException(
                "Failed to parse address " + address, exc);
        }

        //rtp port
        int rtpPort;
        try
        {
            rtpPort = mediaDesc.getMedia().getMediaPort();
        }
        catch (SdpParseException exc)
        {
            throw new IllegalArgumentException(
                "Couldn't extract port from a media description.", exc);
        }

        InetSocketAddress rtpTarget = new InetSocketAddress(
                        rtpAddress, rtpPort);


        //by default RTP and RTCP will be going to the same address (which is
        //actually going to be the case 99% of the time.
        InetAddress rtcpAddress = rtpAddress;
        int         rtcpPort    = rtpPort + 1;

        String rtcpAttributeValue;
        try
        {
            rtcpAttributeValue = mediaDesc.getAttribute(RTCP_ATTR);
        }
        catch (SdpParseException exc)
        {
            //this can't actually happen as there's no parsing here. the
            //exception is just inherited from the jain-sdp api. we are
            //rethrowing only because there's nothing else we could do.
            throw new IllegalArgumentException(
                            "Couldn't extract attribute value.", exc);
        }

        InetSocketAddress rtcpTarget = determineRtcpAddress(
                        rtcpAttributeValue, rtcpAddress, rtcpPort);

        return new MediaStreamTarget(rtpTarget, rtcpTarget);
    }

    /**
     * Determines the address and port where our interlocutor would like to
     * receive RTCP. The method uses the port, and possibly address, indicated
     * in the <tt>rtcpAttribute</tt> or returns the default
     * <tt>defaultAddr:defaultPort</tt> in case <tt>rtcpAttribute</tt> is null.
     * We also use <tt>defaultAddr</tt> in case <tt>rtcpAttribute</tt> only
     * contains a port number and no address.
     *
     * @param rtcpAttrValue the SDP <tt>Attribute</tt> where we are supposed
     * to look for an RTCP address and port (could be <tt>null</tt> if there
     * was no such field in the incoming SDP);
     * @param defaultAddr the address that we should use to construct the result
     * <tt>InetSocketAddress</tt> in case <tt>rtcpAttribute</tt> is
     * <tt>null</tt> or in case <tt>rtcpAttribute</tt> only contains a port
     * number.
     * @param defaultPort  the port that we should use to construct the result
     * <tt>InetSocketAddress</tt> in case <tt>rtcpAttribute</tt> is
     * <tt>null</tt>.
     *
     * @return an <tt>InetSocketAddress</tt> instance indicating the destination
     * where should be sending RTCP.
     *
     * @throws IllegalArgumentException if an error occurs while parsing the
     * <tt>rtcpAttribute</tt>.
     */
    private static InetSocketAddress determineRtcpAddress(
                                        String      rtcpAttrValue,
                                        InetAddress defaultAddr,
                                        int         defaultPort)
        throws IllegalArgumentException
    {
        if (rtcpAttrValue == null)
        {
            //no explicit RTCP attribute means RTCP goes to RTP.port + 1
            return new InetSocketAddress(defaultAddr, defaultPort);
        }

        if (rtcpAttrValue == null || rtcpAttrValue.trim().length() == 0)
        {
            //invalid attribute. return default port.
            return new InetSocketAddress(defaultAddr, defaultPort);
        }

        StringTokenizer rtcpTokenizer
            = new StringTokenizer(rtcpAttrValue.trim(), " ");

        // RTCP attribtutes are supposed to look this way:
        // rtcp-attribute =  "a=rtcp:" port  [nettype space addrtype space
        //                                    connection-address] CRLF
        // which gives us 2 cases: port only (1 token), or port+addr (4 tokens)

        int tokenCount = rtcpTokenizer.countTokens();

        //a single token means we only have a port number.
        int rtcpPort;
        try
        {
            rtcpPort = Integer.parseInt( rtcpTokenizer.nextToken() );
        }
        catch (NumberFormatException exc)
        {
            //rtcp attribute is messed up.
            throw new IllegalArgumentException(
                "Error while parsing rtcp attribute: " + rtcpAttrValue, exc);
        }

        if ( tokenCount == 1 )
        {
            //that was all then. ready to return.
            return new InetSocketAddress(defaultAddr, rtcpPort);
        }
        else if ( tokenCount == 4)
        {
            rtcpTokenizer.nextToken();//nettype
            rtcpTokenizer.nextToken();//addrtype

            //address
            String rtcpAddrStr = rtcpTokenizer.nextToken();

            InetAddress rtcpAddress = null;

            try
            {
                rtcpAddress = NetworkUtils.getInetAddress(rtcpAddrStr);
            }
            catch (UnknownHostException exc)
            {
                throw new IllegalArgumentException(
                    "Failed to parse address " + rtcpAddress, exc);
            }

            return new InetSocketAddress(rtcpAddress, rtcpPort);
        }
        else
        {
            //rtcp attribute is messed up: too many tokens.
            throw new IllegalArgumentException(
                "Error while parsing rtcp attribute: "
                + rtcpAttrValue + ". Too many tokens! ("
                + tokenCount + ")");
        }
    }

    /**
     * Determines the direction of the media stream that <tt>mediaDesc</tt>
     * describes and returns the corresponding <tt>MediaDirection</tt> enum
     * entry. The method looks for a direction specifier attribute (e.g.
     * sendrecv, recvonly, etc.) or the absence thereof and returns the
     * corresponding <tt>MediaDirection</tt> entry.
     *
     * @param mediaDesc the description of the media stream whose direction
     * we are trying to determine.
     *
     * @return one of the <tt>MediaDirection</tt> values indicating the
     * direction of the media steam described by <tt>mediaDesc</tt>.
     */
    @SuppressWarnings("unchecked")//legacy code from jain-sdp
    public static MediaDirection getDirection( MediaDescription mediaDesc )
    {
        Vector<Attribute> attributes  = mediaDesc.getAttributes(false);

        //default
        if (attributes == null)
            return MediaDirection.SENDRECV;

        for (Attribute attribute : attributes)
        {
            String attrName = null;
            try
            {
                attrName = attribute.getName();
            }
            catch (SdpParseException e)
            {
                //can't happen (checkout the jain-sdp code if you wish)
                logger.debug("The impossible has just occurred!", e);
            }

            if (MediaDirection.SENDONLY.name().equals(attrName))
                return MediaDirection.SENDONLY;
            else if (MediaDirection.RECVONLY.name().equals(attrName))
                return MediaDirection.RECVONLY;
            else if (MediaDirection.SENDRECV.name().equals(attrName))
                return MediaDirection.SENDRECV;
            else if (MediaDirection.INACTIVE.name().equals(attrName))
                return MediaDirection.INACTIVE;
        }

        return MediaDirection.SENDRECV;
    }

    /**
     * Returns a <tt>URL</tt> pointing to a location with more details (and
     * possibly call control utilities) about the session. This corresponds to
     * the <tt>"u="</tt> field of the SDP data.
     *
     * @param sessDesc the session description that we'd like to extract an
     * <tt>URL</tt> form.
     *
     * @return a <tt>URL</tt> pointing to a location with more details about
     * the session or <tt>null</tt> if the remote party did not provide one.
     */
    public static URL getCallInfoURL(SessionDescription sessDesc)
    {
        javax.sdp.URI sdpUriField = sessDesc.getURI();

        if (sdpUriField == null)
        {
            logger.trace("Call URI was null.");
            return null;
        }

        try
        {
            return sdpUriField.get();
        }
        catch (SdpParseException exc)
        {
            logger.warn("Failed to parse SDP URI.", exc);
            return null;
        }

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
        if( direction != MediaDirection.SENDRECV)
            mediaAttributes.add(createDirectionAttribute(direction));

        for (int i = 0; i < payloadTypesArray.length; i++)
        {
            MediaFormat format = formats.get(i);
            MediaType fmtMediaType = format.getMediaType();

            // determine whether we are dealing with audio or video.
            if (mediaType == null)
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
                + format.getClockRateString() + numChannelsStr);

            mediaAttributes.add(rtpmap);

            // a=fmtp:
            if( format.getFormatParameters().size() > 0)
            {
                Attribute fmtp = sdpFactory.createAttribute("fmtp",
                            payloadType + " " + encodeFmtp(format));

                mediaAttributes.add(fmtp);
            }

            payloadTypesArray[i] = payloadType;
        }

        // rtcp: (only include it if different from the default (i.e. rtp + 1)
        int rtpPort = connector.getDataSocket().getLocalPort();
        int rtcpPort = connector.getControlSocket().getLocalPort();

        if ((rtpPort + 1) != rtcpPort)
        {
            Attribute rtcpAttr = sdpFactory.createAttribute(RTCP_ATTR, Integer
                            .toString(rtcpPort));
            mediaAttributes.add(rtcpAttr);
        }

        MediaDescription mediaDesc = null;
        try
        {
            mediaDesc = sdpFactory.createMediaDescription(mediaType.toString(),
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

    /**
     * Returns the media type (e.g. audio or video) for the specified media
     * <tt>description</tt>.
     *
     * @param description the <tt>MediaDescription</tt> whose media type we'd
     * like to extract.
     *
     * @return the media type (e.g. audio or video) for the specified media
     * <tt>description</tt>.
     */
    public static MediaType getMediaType(MediaDescription description)
    {
        try
        {
            return MediaType.parseString(description.getMedia().getMediaType());
        }
        catch (SdpException exc)
        {
            // impossible to happen for reasons mentioned many times here :)
            logger.debug("Invalid media type in m= line: " + description, exc);
            throw new IllegalArgumentException(
                         "Invalid media type in m= line: " + description, exc);
        }
    }

    /**
     * Creates and returns a <tt>MediaDescription</tt> in answer of the
     * specified <tt>offer</tt> that disables the corresponding stream by
     * setting a <tt>0</tt> port and keeping the original list of formats and
     * eliminating all attributes.
     *
     * @param offer the <tt>MediaDescription</tt> of the stream that we'd like
     * to disable.
     *
     * @return a <tt>MediaDescription</tt> meant to disable the media stream
     * specified by the <tt>offer</tt> description.
     *
     * @throws IllegalArgumentException if the <tt>offer</tt> argument is so
     * in-parsable that there was no way we could create a meaningful answer.
     *
     */
    @SuppressWarnings("unchecked") // legacy jain-sdp code
    public static MediaDescription createDisablingAnswer(
                                                  MediaDescription offer)
        throws IllegalArgumentException
    {
        MediaType type = getMediaType(offer);
        try
        {
            Vector<String> formatsVec = offer.getMedia().getMediaFormats(true);

            if(formatsVec == null)
            {
                formatsVec = new Vector<String>();
                //add at least one format so that we could generate a valid offer.
                formatsVec.add(Integer.toString(0));
            }

            String[] formatsArray = new String[formatsVec.size()];

            return sdpFactory.createMediaDescription(type.toString(), 0, 1,
                SdpConstants.RTP_AVP, formatsVec.toArray(formatsArray));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not create an ");
        }
    }

    /**
     * Extracts and returns all <tt>MediaDescription</tt>s provided in
     * <tt>sessionDescription</tt>.
     *
     * @param sessionDescription the <tt>SessionDescription</tt> that we'd like
     * to extract <tt>MediaDescription</tt>s from.
     *
     * @return a non <tt>null</tt> <tt>Vector</tt> containing all media
     * descriptions from the <tt>sessionDescription</tt>.
     *
     * @throws IllegalArgumentException in case there were no media descriptions
     * in <tt>sessionDescription</tt>.
     */
    @SuppressWarnings("unchecked") // legacy jain-sdp code.
    public static Vector<MediaDescription> extractMediaDescriptions(
                    SessionDescription sessionDescription)
        throws IllegalArgumentException
    {
        Vector<MediaDescription> remoteDescriptions = null;
        try
        {
            remoteDescriptions = sessionDescription.getMediaDescriptions(false);
        }
        catch (SdpException e)
        {
            // ignoring as remoteDescriptions would remain null and we will
            // log and rethrow right underneath.
        }

        if(remoteDescriptions == null || remoteDescriptions.size() == 0)
        {
            throw new IllegalArgumentException(
                "Could not find any media descriptions.");
        }

        return remoteDescriptions;
    }
}
