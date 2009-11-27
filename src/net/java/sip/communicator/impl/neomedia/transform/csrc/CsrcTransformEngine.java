/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.csrc;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * We use this engine to add the list of CSRC identifiers in RTP packets that
 * we send to conference participants during calls where we are the mixer.
 *
 * @author Emil Ivov
 */
public class CsrcTransformEngine
    implements TransformEngine,
               PacketTransformer
{
    /**
     * The <tt>MediaStreamImpl</tt> that this transform engine was created to
     * transform packets for.
     */
    private final MediaStreamImpl mediaStream;

    /**
     * Creates an engine instance that will be adding CSRC lists to the
     * specified <tt>stream</tt>.
     *
     * @param stream that <tt>MediaStream</tt> whose RTP packets we are going
     * to be adding CSRC lists. to
     */
    public CsrcTransformEngine(MediaStreamImpl stream)
    {
        this.mediaStream = stream;
    }

    /**
     * Always returns <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     *
     * @return <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     */
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    /**
     * Returns a reference to this class since it is performing RTP
     * transformations in here.
     *
     * @return a reference to <tt>this</tt> instance of the
     * <tt>CsrcTransformEngine</tt>.
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * Extracts the list of CSRC identifiers and passes it to the
     * <tt>MediaStream</tt> associated with this engine. Other than that the
     * method does not do any transformations since CSRC lists are part of
     * RFC 3550 and they shouldn't be disrupting the rest of the application.
     *
     * @param pkt the RTP <tt>RawPacket</tt> that we are to extract a CSRC list
     * from.
     *
     * @return the same <tt>RawPacket</tt> that was received as a parameter
     * since we don't need to worry about hiding the CSRC list from the rest
     * of the RTP stack.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        return pkt;
    }

    /**
     * Extracts the list of CSRC identifiers representing participants currently
     * contributing to the media being sent by the <tt>MediaStream</tt>
     * associated with this engine and (unless the list is empty) encodes them
     * into the <tt>RawPacket</tt>.
     *
     * @param pkt the RTP <tt>RawPacket</tt> that we need to add a CSRC list to.
     *
     * @return the updated <tt>RawPacket</tt> instance containing the list of
     * CSRC identifiers.
     */
    public RawPacket transform(RawPacket pkt)
    {
        long[] csrcList = mediaStream.getLocalContributingSourceIDs();

        if (csrcList != null && csrcList.length > 0)
            pkt.setCsrcList( csrcList);

        return pkt;
    }

}
