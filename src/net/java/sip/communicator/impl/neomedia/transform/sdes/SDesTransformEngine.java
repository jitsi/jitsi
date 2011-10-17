/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.sdes;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.transform.srtp.*;

/**
 * PacketTransformer for SDES based SRTP encryption.
 * 
 * @author Ingo Bauersachs
 */
public class SDesTransformEngine
    extends AsymmetricSRTPTransformer
{
    /**
     * Creates a new instance of this class.
     * @param sDesControl The control that supplies the key material.
     */
    public SDesTransformEngine(SDesControlImpl sDesControl)
    {
        super(
            getTransformEngine(sDesControl.getOutAttribute()),
            getTransformEngine(sDesControl.getInAttribute())
        );
    }

    private static SRTPTransformEngine getTransformEngine(
        SrtpCryptoAttribute attribute)
    {
        if (attribute.getSessionParams() != null
            && attribute.getSessionParams().length > 0)
        {
            throw new IllegalArgumentException(
                "session parameters are not supported");
        }

        SrtpCryptoSuite cs = attribute.getCryptoSuite();
        return new SRTPTransformEngine(
            getKey(attribute),
            getSalt(attribute),
            new SRTPPolicy(
                getEncryptionCipher(cs), cs.getEncKeyLength() / 8,
                getHashAlgorithm(cs), cs.getSrtpAuthKeyLength() / 8,
                cs.getSrtpAuthTagLength() / 8,
                cs.getSaltKeyLength() / 8
            ),
            new SRTPPolicy(
                getEncryptionCipher(cs), cs.getEncKeyLength() / 8,
                getHashAlgorithm(cs), cs.getSrtcpAuthKeyLength() / 8,
                cs.getSrtcpAuthTagLength() / 8,
                cs.getSaltKeyLength() / 8
            )
        );
    }

//    /**
//     * Get the key derivation parameter or the default from the SDES attribute.
//     * @param attribute The negotiated SDES attribute for the stream.
//     * @return The KDR parameter or 0 if not present.
//     */
//    private static long getKdr(SrtpCryptoAttribute attribute)
//    {
//        if (attribute.getSessionParams() != null)
//        {
//            for (SrtpSessionParam param : attribute.getSessionParams())
//            {
//                if (param instanceof KdrSessionParam)
//                    return ((KdrSessionParam) param)
//                        .getKeyDerivationRateExpanded();
//            }
//        }
//        return 0;
//    }

    private static byte[] getKey(SrtpCryptoAttribute attribute)
    {
        int length = attribute.getCryptoSuite().getEncKeyLength() / 8;
        byte[] key = new byte[length];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), 0, key, 0,
            length);
        return key;
    }

    private static byte[] getSalt(SrtpCryptoAttribute attribute)
    {
        int keyLength = attribute.getCryptoSuite().getEncKeyLength() / 8;
        int saltLength = attribute.getCryptoSuite().getSaltKeyLength() / 8;
        byte[] salt = new byte[keyLength];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), keyLength, salt,
            0, saltLength);
        return salt;
    }

    private static int getEncryptionCipher(SrtpCryptoSuite cs)
    {
        switch (cs.getEncryptionAlgorithm())
        {
            case SrtpCryptoSuite.ENCRYPTION_AES128_CM:
                return SRTPPolicy.AESCM_ENCRYPTION;
            case SrtpCryptoSuite.ENCRYPTION_AES128_F8:
                return SRTPPolicy.AESF8_ENCRYPTION;
            default:
                throw new IllegalArgumentException("Unsupported cipher");
        }
    }

    private static int getHashAlgorithm(SrtpCryptoSuite cs)
    {
        switch (cs.getHashAlgorithm())
        {
            case SrtpCryptoSuite.HASH_HMAC_SHA1:
                return SRTPPolicy.HMACSHA1_AUTHENTICATION;
            default:
                throw new IllegalArgumentException("Unsupported hash");
        }
    }
}
