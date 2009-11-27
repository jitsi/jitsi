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
 * @author Emil Ivov
 */
public class CsrcTransformEngine
    implements TransformEngine,
               PacketTransformer
{

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.neomedia.transform.TransformEngine#getRTCPTransformer()
     */
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.neomedia.transform.TransformEngine#getRTPTransformer()
     */
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
