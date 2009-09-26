/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.dummy;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.transform.*;

/**
 * DummyTransformEngine does nothing, its sole purpose is to test the
 * TransformConnector related classes.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class DummyTransformEngine
    implements TransformEngine, PacketTransformer
{
    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.
     * TransformEngine#getRTCPTransformer()
     */
    public PacketTransformer getRTCPTransformer()
    {
        return this; 
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.
     * TransformEngine#getRTPTransformer()
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.PacketTransformer#
     * transform(net.java.sip.communicator.impl.media.transform.RawPacket)
     */
    public RawPacket transform(RawPacket pkt)
    {
        return pkt;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.PacketTransformer#
     * reverseTransform(net.java.sip.communicator.impl.media.transform.
     * RawPacket)
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        return pkt;
    }
}
