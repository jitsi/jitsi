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

import org.bouncycastle.crypto.engines.*;
import org.bouncycastle.crypto.params.*;

/**
 * AESCipher encapsulate basic AES encryption / decryption algorithm.
 * It encrypts and decrypts data in 128bit blocks. This implementation is based
 * on Bouncy Castle (http://www.bouncycastle.org). 
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class AESCipher
{
    /**
     * AES cipher block size in bytes 
     */
    public final static int BLOCK_SIZE = 16;

    /**
     * The encryption key we are using
     */
    private KeyParameter keyParam;
    
    /**
     * Bouncy Castle's AES cipher object
     */
    private AESFastEngine aesCipher;

    /**
     * Construct an AESCipher using specified key, which is 128bit long
     *
     * @param key encryption key for this AESCipher
     */
    public AESCipher(byte[] key)
    {
        // Currently we only support 128 bit AES encryption
        if (key == null || key.length != 16)
        {
            throw new RuntimeException("Invalid key length (BUG).");
        }

        this.aesCipher = new AESFastEngine();
        this.keyParam  = new KeyParameter(key);
    }

    /**
     * Encrypt a 128bit block using AES cipher
     * 
     * @param in byte array containing the block to encrypted
     * @param inOff start offset of the block inside byte array
     * @param out output byte array storing the encrypted block
     * @param outOff start offset of encrypted block inside output array
     */
    public void encryptBlock(byte[] in, int inOff, byte[] out, int outOff)
    {
        this.aesCipher.init(true, this.keyParam);
        this.aesCipher.processBlock(in, inOff, out, outOff);
    }

    /**
     * Decrypt a 128bit block using AES cipher
     * 
     * @param in byte array containing the block to be decrypted
     * @param inOff start offset of the block inside byte array
     * @param out output byte array holding the decrypted block
     * @param outOff start offset of decrypted block inside output array
     */
    public void decryptBlock(byte[] in, int inOff, byte[] out, int outOff)
    {
        this.aesCipher.init(false, this.keyParam);
        this.aesCipher.processBlock(in, inOff, out, outOff);
    }
}
