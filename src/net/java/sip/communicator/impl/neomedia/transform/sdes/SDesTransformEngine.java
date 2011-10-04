package net.java.sip.communicator.impl.neomedia.transform.sdes;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.srtp.*;

public class SDesTransformEngine
    implements TransformEngine, PacketTransformer
{
    private SRTPCryptoContext inContext;
    private SRTPCryptoContext outContext;
    private SrtpCryptoAttribute inAttribute;
    private SrtpCryptoAttribute outAttribute;

    public SDesTransformEngine(SDesControlImpl sDesControl)
    {
        inAttribute = sDesControl.getInAttribute();
        outAttribute = sDesControl.getOutAttribute();
    }

    private long getKdr(SrtpCryptoAttribute attribute)
    {
        if(attribute.getSessionParams() != null)
        {
            for (SrtpSessionParam param : attribute.getSessionParams())
            {
                if (param instanceof KdrSessionParam)
                    return ((KdrSessionParam) param).getKeyDerivationRateExpanded();
            }
        }
        return 0;
    }

    private byte[] getKey(SrtpCryptoAttribute attribute)
    {
        int length = attribute.getCryptoSuite().getEncKeyLength()/8;
        byte[] key = new byte[length];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), 0, key, 0, length);
        return key;
    }

    private byte[] getSalt(SrtpCryptoAttribute attribute)
    {
        int keyLength = attribute.getCryptoSuite().getEncKeyLength()/8;
        int saltLength = attribute.getCryptoSuite().getSaltKeyLength()/8;
        byte[] salt = new byte[keyLength];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), keyLength, salt, 0,
            saltLength);
        return salt;
    }

    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    private SRTPCryptoContext createContext(long ssrc, SrtpCryptoAttribute attribute)
    {
        int encType;
        SrtpCryptoSuite cs = attribute.getCryptoSuite();
        switch(cs.getEncryptionAlgorithm())
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
        switch(cs.getHashAlgorithm())
        {
            case SrtpCryptoSuite.HASH_HMAC_SHA1:
                authType = SRTPPolicy.HMACSHA1_AUTHENTICATION;
                break;
            default:
                throw new IllegalArgumentException("Unsupported hash");
        }
        SRTPPolicy policy = new SRTPPolicy(
            encType, cs.getEncKeyLength()/8,
            authType, cs.getSrtpAuthKeyLength()/8, cs.getSrtpAuthTagLength()/8,
            cs.getSaltKeyLength()/8
        );
        return new SRTPCryptoContext(
            ssrc, 0, getKdr(attribute),
            getKey(attribute),
            getSalt(attribute),
            policy
        );
    }

    public RawPacket transform(RawPacket pkt)
    {
        if (outContext == null)
        {
            outContext = createContext(pkt.getSSRC(), outAttribute);
            outContext.deriveSrtpKeys(0);
        }

        outContext.transformPacket(pkt);
        return pkt;
    }

    public RawPacket reverseTransform(RawPacket pkt)
    {
        long ssrc  = pkt.getSSRC();
        int seqNum = pkt.getSequenceNumber();

        boolean isNewContext = false;
        if (inContext == null)
        {
            inContext = createContext(ssrc, inAttribute);
            inContext.deriveSrtpKeys(seqNum);
            isNewContext = true;
        }
        else if(ssrc != inContext.getSSRC())
        {
            // invalid packet, don't even try to decode
            return null;
        }

        boolean validPacket = inContext.reverseTransformPacket(pkt);
        if(!validPacket && isNewContext)
            inContext = null;

        if (!validPacket)
        {
            return null;
        }

        return pkt;
    }
}
