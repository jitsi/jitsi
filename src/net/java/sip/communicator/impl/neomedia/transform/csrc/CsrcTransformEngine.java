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
     * transform packets fro.
     */
    private final MediaStreamImpl mediaStream;

    /**
     * Creates
     * @param stream
     */
    public CsrcTransformEngine(MediaStreamImpl stream)
    {
        this.mediaStream = stream;
    }

    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    public RawPacket reverseTransform(RawPacket pkt)
    {
        return pkt;
    }

    public RawPacket transform(RawPacket pkt)
    {
        return null;
    }

}
