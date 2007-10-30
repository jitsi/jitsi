/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * 
 * Some of the code in this class is derived from ccRtp's SRTP implementation,
 * which has the following copyright notice: 
 *
  Copyright (C) 2004-2006 the Minisip Team

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
*/
package net.java.sip.communicator.impl.media.transform.srtp;

/**
 * SRTPCipherCTR implements SRTP Counter Mode AES Encryption (AES-CM).
 * Counter Mode AES Encryption algorithm is defined in RFC3711, section 4.1.1.
 * 
 * Other than Null Cipher, RFC3711 defined two two encryption algorithms:
 * Counter Mode AES Encryption and F8 Mode AES encryption. Both encryption
 * algorithms are capable to encrypt / decrypt arbitrary length data, and the
 * size of packet data is not required to be a multiple of the AES block 
 * size (128bit). So, no padding is needed.
 * 
 * Please note: these two encryption algorithms are specially defined by SRTP.
 * They are not common AES encryption modes, so you will not be able to find a 
 * replacement implementation in common cryptographic libraries. 
 *
 * As defined by RFC3711: Counter Mode Encryption is mandatory..
 *
 *                        mandatory to impl     optional      default
 * -------------------------------------------------------------------------
 *   encryption           AES-CM, NULL          AES-f8        AES-CM
 *   message integrity    HMAC-SHA1                -          HMAC-SHA1
 *   key derivation       (PRF) AES-CM             -          AES-CM 
 *
 * We use AESCipher to handle basic AES encryption / decryption.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTPCipherCTR implements SRTPCipher
{
    /**
     * The AESCihper object we used to do basic AES encryption / decryption
     */
    private AESCipher aesCipher;
 
    /**
     * Construct a SRTPCipherCTR object using given encryption key
     *
     * @param key the encryption key for this session
     */
    public SRTPCipherCTR(byte[] key)
    {
        this.aesCipher = new AESCipher(key);
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.srtp.
     * SRTPCipher#process(byte[], int, int, byte[])
     */
    public void process(byte[] data, int off, int len, byte[] iv)
    {
        if (off + len > data.length)
        {
            // TODO this is invalid, need error handling
            return;
        }

        byte[] cipherStream = new byte[len];

        getCipherStream(cipherStream, len, iv);

        for (int i = 0; i < len; i++)
        {
            data[i + off] ^= cipherStream[i];
        }
    }

    /**
     * Computes the cipher stream for AES CM mode. 
     * See section 4.1.1 in RFC3711 for detailed description.
     * 
     * @param out byte array holding the output cipher stream
     * @param length length of the cipher stream to produce, in bytes
     * @param iv initialization vector used to generate this cipher stream
     */
    public void getCipherStream(byte[] out, int length, byte[] iv)
    {
        final int BLKLEN = AESCipher.BLOCK_SIZE;
        
        byte[] in  = new byte[BLKLEN];
        byte[] tmp = new byte[BLKLEN];

        System.arraycopy(iv, 0, in, 0, 14);

        int ctr;
        for (ctr = 0; ctr < length / BLKLEN; ctr++)
        {
            // compute the cipher stream
            in[14] = (byte) ((ctr & 0xFF00) >> 8);
            in[15] = (byte) ((ctr & 0x00FF));

            this.aesCipher.encryptBlock(in, 0, out, ctr * BLKLEN);
        }

        // Treat the last bytes:
        in[14] = (byte) ((ctr & 0xFF00) >> 8);
        in[15] = (byte) ((ctr & 0x00FF));

        this.aesCipher.encryptBlock(in, 0, tmp, 0);
        System.arraycopy(tmp, 0, out, ctr * BLKLEN, length % BLKLEN);
    }
}
