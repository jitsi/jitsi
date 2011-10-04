/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.sdes;

import java.util.*;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.srtp.*;
import net.java.sip.communicator.util.Logger;

/**
 * PacketTransformer for SDES based SRTP encryption.
 * 
 * @author Ingo Bauersachs
 */
public class SDesTransformEngine
    implements TransformEngine, PacketTransformer
{
    private final static Logger logger = Logger
        .getLogger(SDesTransformEngine.class);

    private SrtpCryptoAttribute inAttribute;
    private SrtpCryptoAttribute outAttribute;

    /**
     * All the known SSRC's corresponding SRTPCryptoContexts
     */
    private Hashtable<Long, SRTPCryptoContext> contexts;

    /**
     * Creates a new instance of this class.
     * @param sDesControl The control that supplies the key material.
     */
    public SDesTransformEngine(SDesControlImpl sDesControl)
    {
        reset(sDesControl);
    }

    public void reset(SDesControlImpl sDesControl)
    {
        inAttribute = sDesControl.getInAttribute();
        outAttribute = sDesControl.getOutAttribute();
        contexts = new Hashtable<Long, SRTPCryptoContext>();
    }

    private long getKdr(SrtpCryptoAttribute attribute)
    {
        if (attribute.getSessionParams() != null)
        {
            for (SrtpSessionParam param : attribute.getSessionParams())
            {
                if (param instanceof KdrSessionParam)
                    return ((KdrSessionParam) param)
                        .getKeyDerivationRateExpanded();
            }
        }
        return 0;
    }

    private byte[] getKey(SrtpCryptoAttribute attribute)
    {
        int length = attribute.getCryptoSuite().getEncKeyLength() / 8;
        byte[] key = new byte[length];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), 0, key, 0,
            length);
        return key;
    }

    private byte[] getSalt(SrtpCryptoAttribute attribute)
    {
        int keyLength = attribute.getCryptoSuite().getEncKeyLength() / 8;
        int saltLength = attribute.getCryptoSuite().getSaltKeyLength() / 8;
        byte[] salt = new byte[keyLength];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), keyLength, salt,
            0, saltLength);
        return salt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.impl.neomedia.transform.TransformEngine#
     * getRTPTransformer()
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.impl.neomedia.transform.TransformEngine#
     * getRTCPTransformer()
     */
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    private SRTPCryptoContext createContext(long ssrc,
        SrtpCryptoAttribute attribute)
    {
        int encType;
        SrtpCryptoSuite cs = attribute.getCryptoSuite();
        switch (cs.getEncryptionAlgorithm())
        {
            case SrtpCryptoSuite.ENCRYPTION_AES128_CM:
                encType = SRTPPolicy.AESCM_ENCRYPTION;
                break;
            case SrtpCryptoSuite.ENCRYPTION_AES128_F8:
                encType = SRTPPolicy.AESF8_ENCRYPTION;
                break;
            default:
                throw new IllegalArgumentException("Unsupported cipher");
        }
        int authType;
        switch (cs.getHashAlgorithm())
        {
            case SrtpCryptoSuite.HASH_HMAC_SHA1:
                authType = SRTPPolicy.HMACSHA1_AUTHENTICATION;
                break;
            default:
                throw new IllegalArgumentException("Unsupported hash");
        }
        SRTPPolicy policy =
            new SRTPPolicy(
                encType, cs.getEncKeyLength() / 8,
                authType, cs.getSrtpAuthKeyLength() / 8,
                cs.getSrtpAuthTagLength() / 8,
                cs.getSaltKeyLength() / 8
            );
        return new SRTPCryptoContext(
            ssrc, 0, getKdr(attribute),
            getKey(attribute),
            getSalt(attribute),
            policy
        );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.impl.neomedia.transform.PacketTransformer
     * #transform(net.java.sip.communicator.impl.neomedia.RawPacket)
     */
    public RawPacket transform(RawPacket pkt)
    {
        long ssrc = pkt.getSSRC();
        SRTPCryptoContext context = contexts.get(ssrc);
        if (context == null)
        {
            logger.debug("OutContext created for SSRC=" + ssrc);
            context = createContext(ssrc, outAttribute);
            context.deriveSrtpKeys(0);
            contexts.put(ssrc, context);
        }

        context.transformPacket(pkt);
        return pkt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.impl.neomedia.transform.PacketTransformer#
     * reverseTransform(net.java.sip.communicator.impl.neomedia.RawPacket)
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        // only accept RTP version 2 (SNOM phones send weird packages when on
        // hold, ignore them with this check)
        if((pkt.readByte(0) & 0xC0) != 0x80)
            return null;

        long ssrc = pkt.getSSRC();
        int seqNum = pkt.getSequenceNumber();

        SRTPCryptoContext context = contexts.get(ssrc);
        if (context == null)
        {
            logger.debug("InContext created for SSRC=" + ssrc);
            context = createContext(ssrc, inAttribute);
            context.deriveSrtpKeys(seqNum);
            contexts.put(ssrc, context);
        }

        boolean validPacket = context.reverseTransformPacket(pkt);
        if (!validPacket)
        {
            return null;
        }

        return pkt;
    }
}
