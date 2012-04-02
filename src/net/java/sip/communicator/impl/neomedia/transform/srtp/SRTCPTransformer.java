/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.srtp;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * SRTCPTransformer implements PacketTransformer.
 * It encapsulate the encryption / decryption logic for SRTCP packets
 * 
 * @author Bing SU (nova.su@gmail.com)
 * @author Werner Dittmann &lt;Werner.Dittmann@t-online.de>
 */
public class SRTCPTransformer
    implements PacketTransformer
{
    private final SRTPTransformEngine forwardEngine;
    private final SRTPTransformEngine reverseEngine;

    /**
     * All the known SSRC's corresponding SRTCPCryptoContexts
     */
    private final Hashtable<Long,SRTCPCryptoContext> contexts;

    /**
     * Constructs a SRTCPTransformer object.
     * 
     * @param engine The associated SRTPTransformEngine object for both
     *            transform directions.
     */
    public SRTCPTransformer(SRTPTransformEngine engine)
    {
        this(engine, engine);
    }

    /**
     * Constructs a SRTCPTransformer object.
     * 
     * @param forwardEngine The associated SRTPTransformEngine object for
     *            forward transformations.
     * @param reverseEngine The associated SRTPTransformEngine object for
     *            reverse transformations.
     */
    public SRTCPTransformer(
            SRTPTransformEngine forwardEngine,
            SRTPTransformEngine reverseEngine)
    {
        this.forwardEngine = forwardEngine;
        this.reverseEngine = reverseEngine;
        this.contexts = new Hashtable<Long, SRTCPCryptoContext>();
    }

    /**
     * Closes this <tt>SRTCPTransformer</tt> and the underlying transform
     * engine. It closes all stored crypto contexts. It deletes key data and
     * forces a cleanup of the crypto contexts.
     */
    public void close() 
    {
        synchronized (contexts)
        {
            forwardEngine.close();
            if (reverseEngine != forwardEngine)
                reverseEngine.close();

            Iterator<Map.Entry<Long, SRTCPCryptoContext>> iter
                = contexts.entrySet().iterator();

            while (iter.hasNext()) 
            {
                Map.Entry<Long, SRTCPCryptoContext> entry = iter.next();
                SRTCPCryptoContext context = entry.getValue();
    
                iter.remove();
                if (context != null)
                    context.close();
            }
        }
    }

    private SRTCPCryptoContext getContext(
            RawPacket pkt,
            SRTPTransformEngine engine)
    {
        long ssrc = pkt.getRTCPSSRC();
        SRTCPCryptoContext context;

        synchronized (contexts)
        {
            context = contexts.get(ssrc);
            if (context == null)
            {
                context = engine.getDefaultContextControl();
                if (context != null)
                {
                    context = context.deriveContext(ssrc);
                    context.deriveSrtcpKeys();
                    contexts.put(ssrc, context);
                }
            }
        }

        return context;
    }

    /**
     * Decrypts a SRTCP packet
     * 
     * @param pkt encrypted SRTCP packet to be decrypted
     * @return decrypted SRTCP packet
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        SRTCPCryptoContext context = getContext(pkt, reverseEngine);

        return
            ((context != null) && context.reverseTransformPacket(pkt))
                ? pkt
                : null;
    }

    /**
     * Encrypts a SRTCP packet
     * 
     * @param pkt plain SRTCP packet to be encrypted
     * @return encrypted SRTCP packet
     */
    public RawPacket transform(RawPacket pkt)
    {
        SRTCPCryptoContext context = getContext(pkt, forwardEngine);

        context.transformPacket(pkt);
        return pkt;
    }
}
