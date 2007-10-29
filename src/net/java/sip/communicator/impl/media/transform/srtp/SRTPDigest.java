/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.srtp;

import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.macs.*;
import org.bouncycastle.crypto.params.*;

/**
 * SRTPDigest is used to calculate the digest of a specified byte stream using
 * HMC SHA1 algorithm.
 * 
 * This class uses bouncy castle's digest algorithm.
 *  
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTPDigest
{
    /**
     * Bouncy Castle's HMAC algorithm provider
     */
    private HMac hmac;
    
    /**
     * Construct a SRTPDigest based on given authentication key
     *
     * @param key the authentication key
     */
    public SRTPDigest(byte[] key)
    {
        this.hmac = new HMac(new SHA1Digest());
        this.hmac.init(new KeyParameter(key));
    }
    
    /**
     * Calculate HMC SHA1 digests for a set of byte streams. These byte streams
     * are considered to be continuous and the digest is calculated based on all
     * these byte streams.
     * 
     * @param chunks an array holding the byte streams to be calculated
     * @param chunkLengths lengths of the byte streams to be calculated
     * @return byte array holding the digest result
     */
    public byte[] authHMACSHA1(byte[][] chunks, int[] chunkLengths)
    {
        this.hmac.reset();
        
        byte[] result = new byte[this.hmac.getMacSize()];
        for (int i = 0; i < chunks.length; i++)
        {
            this.hmac.update(chunks[i], 0, chunkLengths[i]);
        }
        this.hmac.doFinal(result, 0);

        return result;
    }
}
