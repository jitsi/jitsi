/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
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

import java.util.*;

import net.java.sip.communicator.impl.media.transform.*;
import java.security.*;
import javax.crypto.spec.*;
import javax.crypto.*;

/**
 * SRTPCryptoContext class is the core class of SRTP implementation.
 * There can be multiple SRTP sources in one SRTP session. And each SRTP stream
 * has a corresponding SRTPCryptoContext object, identified by SSRC. In this
 * way, different sources can be protected independently.
 * 
 * SRTPCryptoContext class acts as a manager class and maintains all the
 * information used in SRTP transformation. It is responsible for deriving
 * encryption keys / salting keys / authentication keys from master keys. And 
 * it will invoke certain class to encrypt / decrypt (transform / reverse
 * transform) RTP packets. It will hold a replay check db and do replay check
 * against incoming packets.
 * 
 * Refer to section 3.2 in RFC3711 for detailed description of cryptographic
 * context.
 * 
 * Cryptographic related parameters, i.e. encryption mode / authentication mode,
 * master encryption key and master salt key are determined outside the scope
 * of SRTP implementation. They can be assigned manually, or can be assigned
 * automatically using some key management protocol, such as MIKEY (RFC3880) or
 * Phil Zimmermann's ZRTP protocol (draft-zimmermann-avt-zrtp-01).
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTPCryptoContext
{
    /**
     * The replay check windows size
     */
    private static final long REPLAY_WINDOW_SIZE = 64;
    
    /**
     * RTP SSRC of this cryptographic context
     */
    private long ssrc;
    
    /**
     * Master key identifier
     */
    private byte[] mki;

    /**
     * Roll-Over-Counter, see RFC3711 section 3.2.1 for detailed description 
     */
    private int roc;
    
    /**
     * Roll-Over-Counter guessed from packet
     */
    private int guessedROC;
    
    /**
     * RTP sequence number of the packet current processing 
     */
    private int seqNum;
    
    /**
     * Whether we have the sequence number of current packet
     */
    private boolean seqNumSet;
    
    /**
     * Key Derivation Rate, used to derive session keys from master keys
     */
    private long keyDerivationRate;

    /**
     * Bit mask for replay check
     */
    private long replayWindow;

    /**
     * Master encryption key
     */
    private byte[] masterKey;
    
    /**
     * Master salting key
     */
    private byte[] masterSalt;

    /**
     * Derived session encryption key
     */
    private byte[] encKey;

    /**
     * Derived session authentication key
     */
    private byte[] authKey;
    
    /**
     * Derived session salting key
     */
    private byte[] saltKey;

    /**
     * Encryption / Authentication policy for this session
     */
    private final SRTPPolicy policy;
    
    /**
     * The SRTPDigest object we used to do packet authentication
     */
    private SRTPDigest digest;
    
    /**
     * The cryptographic services provider
     */
    private Provider cryptoProvider;
    
    /**
     * Used for various HMAC computations
     */
    private Mac hmacSha1;
    
    /**
     * The AES cipher for counter mode
     */
    private Cipher AEScipher = null;
    
    /**
     * The AES cipher for F8 mode
     */
    private Cipher AEScipherF8 = null;

    /**
     * Construct an empty SRTPCryptoContext using ssrc.
     * The other parameters are set to default null value.
     * 
     * @param ssrc SSRC of this SRTPCryptoContext
     */
    public SRTPCryptoContext(long ssrc)
    {
        this.ssrc = ssrc;
        this.mki = null;
        this.roc = 0;
        this.guessedROC = 0;
        this.seqNum = 0;
        this.keyDerivationRate = 0;
        this.masterKey = null;
        this.masterSalt = null;
        this.encKey = null;
        this.authKey = null;
        this.saltKey = null;
        this.seqNumSet = false;
        this.policy = null;
        this.cryptoProvider = null;
    }

    /**
     * Construct a normal SRTPCryptoContext based on the given parameters.
     *
     * @param ssrc the RTP SSRC that this SRTP cryptographic context protects.
     * @param roc the initial Roll-Over-Counter according to RFC 3711.
     * These are the upper 32 bit of the overall 48 bit SRTP packet index.
     * Refer to chapter 3.2.1 of the RFC.
     * @param keyDerivationRate the key derivation rate defines when to recompute
     * the SRTP session keys. Refer to chapter 4.3.1 in the RFC.
     * @param masterKey byte array holding the master key for this SRTP
     * cryptographic context. Refer to chapter 3.2.1 of the RFC about the
     * role of the master key.
     * @param masterSalt byte array holding the master salt for this SRTP 
     * cryptographic context. It is used to computer the initialization vector
     * that in turn is input to compute the session key, session
     * authentication key and the session salt.
     * @param policy SRTP policy for this SRTP cryptographic context, defined 
     * the encryption algorithm, the authentication algorithm, etc 
     * @param cryptoProvider cryptographic services provider
     */
    public SRTPCryptoContext(long ssrc,
                             int roc,
                             long keyDerivationRate,
                             byte[] masterKey,
                             byte[] masterSalt,
                             SRTPPolicy policy,
                             Provider cryptoProvider)
    throws GeneralSecurityException
    {
        this.ssrc = ssrc;
        this.mki = null;
        this.roc = roc;
        this.guessedROC = 0;
        this.seqNum = 0;
        this.keyDerivationRate = keyDerivationRate;
        this.seqNumSet = false;
        this.cryptoProvider = cryptoProvider;

        this.policy = policy;

        this.masterKey = new byte[this.policy.getEncKeyLength()];
        System.arraycopy(masterKey, 0, this.masterKey, 0, this.policy.getEncKeyLength());

        this.masterSalt = new byte[this.policy.getSaltKeyLength()];
        System.arraycopy(masterSalt, 0, this.masterSalt, 0, this.policy.getSaltKeyLength());
         

        switch (policy.getEncType())
        {
            case SRTPPolicy.NULL_ENCRYPTION:
                this.encKey = null;
                this.saltKey = null;
                break;

            case SRTPPolicy.AESCM_ENCRYPTION:
                hmacSha1 = Mac.getInstance("HMACSHA1", cryptoProvider);
                AEScipher = Cipher.getInstance("AES/ECB/NOPADDING", cryptoProvider);
                this.encKey  = new byte[this.policy.getEncKeyLength()];
                this.saltKey = new byte[this.policy.getSaltKeyLength()];
                break;
                
            case SRTPPolicy.AESF8_ENCRYPTION:
                hmacSha1 = Mac.getInstance("HMACSHA1", cryptoProvider);
                AEScipher = Cipher.getInstance("AES/ECB/NOPADDING", cryptoProvider);
                AEScipherF8 = Cipher.getInstance("AES/ECB/NOPADDING", cryptoProvider);
                this.encKey  = new byte[this.policy.getEncKeyLength()];
                this.saltKey = new byte[this.policy.getSaltKeyLength()];
                break;
        }

        switch (policy.getAuthType())
        {
            case SRTPPolicy.NULL_AUTHENTICATION:
                this.authKey = null;
                break;

            case SRTPPolicy.HMACSHA1_AUTHENTICATION:
                this.authKey = new byte[policy.getAuthKeyLength()];
                break;
        }
    }

    /**
     * Get the authentication tag length of this SRTP cryptographic context
     *
     * @return the authentication tag length of this SRTP cryptographic context
     */
    public int getAuthTagLength()
    {
        return this.policy.getAuthTagLength();
    }

    /**
     * Get the MKI length of this SRTP cryptographic context
     *
     * @return the MKI length of this SRTP cryptographic context
     */
    public int getMKILength()
    {
        if (this.mki != null)
        {
            return this.mki.length;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Get the SSRC of this SRTP cryptographic context
     *
     * @return the SSRC of this SRTP cryptographic context
     */
    public long getSSRC()
    {
        return this.ssrc;
    }
    
    /**
     * Get the Roll-Over-Counter of this SRTP cryptographic context
     *
     * @return the Roll-Over-Counter of this SRTP cryptographic context
     */
    public int getROC()
    {
        return this.roc;
    }

    /**
     * Set the Roll-Over-Counter of this SRTP cryptographic context
     *
     * @param roc the Roll-Over-Counter of this SRTP cryptographic context
     */
    public void setROC(int roc)
    {
        this.roc = roc;
    }

    /**
     * Transform a RTP packet into a SRTP packet. 
     * This method is called when a normal RTP packet ready to be sent.
     * 
     * Operations done by the transformation may include: encryption, using
     * either Counter Mode encryption, or F8 Mode encryption, adding
     * authentication tag, currently HMC SHA1 method.
     * 
     * Both encryption and authentication functionality can be turned off
     * as long as the SRTPPolicy used in this SRTPCryptoContext is requires no
     * encryption and no authentication. Then the packet will be sent out
     * untouched. However this is not encouraged. If no SRTP feature is enabled,
     * then we shall not use SRTP TransformConnector. We should use the original
     * method (RTPManager managed transportation) instead.  
     * 
     * @param pkt the RTP packet that is going to be sent out
     */
    public void transformPacket(RawPacket pkt)
    {
        /* Encrypt the packet using Counter Mode encryption */
        if (this.policy.getEncType() == SRTPPolicy.AESCM_ENCRYPTION)
        {
            processPacketAESCM(pkt);
        }

        /* Encrypt the packet using F8 Mode encryption */
        else if (this.policy.getEncType() == SRTPPolicy.AESF8_ENCRYPTION)
        {
            processPacketAESF8(pkt);
        }

        /* Authenticate the packet */
        if (this.policy.getAuthType() == SRTPPolicy.HMACSHA1_AUTHENTICATION)
        {
            byte[] tag = authenticatePacketHMCSHA1(pkt);
            pkt.append(tag, policy.getAuthTagLength());
        }

        /* Update the ROC if necessary */
        int seqNum = PacketManipulator.GetRTPSequenceNumber(pkt);
        if (seqNum == 0xFFFF)
        {
            this.roc++;
        }
    }

    /**
     * Transform a SRTP packet into a RTP packet.
     * This method is called when a SRTP packet is received.
     * 
     * Operations done by the this operation include:
     * Authentication check, Packet replay check and decryption.
     * 
     * Both encryption and authentication functionality can be turned off
     * as long as the SRTPPolicy used in this SRTPCryptoContext is requires no
     * encryption and no authentication. Then the packet will be sent out
     * untouched. However this is not encouraged. If no SRTP feature is enabled,
     * then we shall not use SRTP TransformConnector. We should use the original
     * method (RTPManager managed transportation) instead.  
     * 
     * @param pkt the RTP packet that is just received
     * @return true if the packet can be accepted
     *         false if the packet failed authentication or failed replay check 
     */
    public boolean reverseTransformPacket(RawPacket pkt)
    {
        /* Authenticate the packet */
        if (this.policy.getAuthType() == SRTPPolicy.HMACSHA1_AUTHENTICATION)
        {
            int tagLength = this.policy.getAuthTagLength();

            byte[] originalTag =
                pkt.readRegion(pkt.getLength() - tagLength, tagLength);
            
            pkt.shrink(tagLength);
            
            byte[] calculatedTag = authenticatePacketHMCSHA1(pkt);

            for (int i = 0; i < tagLength; i++) {
                if ((originalTag[i]&0xff) == (calculatedTag[i]&0xff))
                    continue;
                else 
                    return false;
            }
        }

        int seqNum = PacketManipulator.GetRTPSequenceNumber(pkt);

        /* Replay control */
        if (!checkReplay(seqNum))
        {
            return false;
        }
        
        /* Decrypt the packet using Counter Mode encryption*/
        if (this.policy.getEncType() == SRTPPolicy.AESCM_ENCRYPTION)
        {
            processPacketAESCM(pkt);
        }

        /* Decrypt the packet using F8 Mode encryption*/
        else if (this.policy.getEncType() == SRTPPolicy.AESF8_ENCRYPTION)
        {
            processPacketAESF8(pkt);
        }

        update(seqNum);

        return true;
    }

    /**
     * Perform Counter Mode AES encryption / decryption 
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESCM(RawPacket pkt)
    {
        long  ssrc   = PacketManipulator.GetRTPSSRC(pkt);
        int   seqNum = PacketManipulator.GetRTPSequenceNumber(pkt);
        long  index  = ((long) this.roc << 16) | (long) seqNum;

        byte[] iv = new byte[16];
        System.arraycopy(this.saltKey, 0, iv, 0, 4);

        int i;
        for (i = 4; i < 8; i++)
        {
            iv[i] = (byte)((0xFF & (ssrc >> ((7 - i) * 8))) ^ this.saltKey[i]);
        }

        for (i = 8; i < 14; i++)
        {
            iv[i] =
                (byte)((0xFF & (byte)(index >> ((13 - i) * 8))) ^ this.saltKey[i]);
        }

        iv[14] = iv[15] = 0;

        final int payloadOffset = PacketManipulator.GetRTPHeaderLength(pkt);
        final int payloadLength = PacketManipulator.GetRTPPayloadLength(pkt);
        
        SRTPCipherCTR.process(AEScipher, pkt.getBuffer(), pkt.getOffset() + payloadOffset,
                              payloadLength, iv);
    }
    
    /**
     * Perform F8 Mode AES encryption / decryption
     *
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESF8(RawPacket pkt)
    {
        //long    ssrc     = PacketManipulator.GetRTPSSRC(pkt);
        //boolean isMarked = PacketManipulator.IsPacketMarked(pkt);
        //int     seqNum   = PacketManipulator.GetRTPSequenceNumber(pkt);
        //byte    payload  = PacketManipulator.GetRTPPayloadType(pkt);

        byte[] iv = new byte[16];
        
        //iv[0] = 0;
        //iv[1] = (byte) (isMarked ? 0x80 : 0x00);
        //iv[1] |= payload & 0x7f;
        //iv[2] = (byte) (seqNum >> 8);
        //iv[3] = (byte) seqNum;

        // set the TimeStamp in network order into IV
        //byte[] timeStamp = PacketManipulator.ReadTimeStampIntoByteArray(pkt);
        //System.arraycopy(timeStamp, 0, iv, 4, 4);
        
        // set the SSRC in network order into IV
        //iv[8]  = (byte) (ssrc >> 24);
        //iv[9]  = (byte) (ssrc >> 16);
        //iv[10] = (byte) (ssrc >> 8);
        //iv[11] = (byte) ssrc;
        
        // set the ROC in network order into IV
        // 11 bytes of the RTP header are the 11 bytes of the iv
        // the first byte of the RTP header is not used.
        System.arraycopy(pkt.getBuffer(), pkt.getOffset(), iv, 0, 12);
        iv[0] = 0;
        
        iv[12] = (byte) (this.roc >> 24);
        iv[13] = (byte) (this.roc >> 16);
        iv[14] = (byte) (this.roc >>  8);
        iv[15] = (byte) this.roc;
        
        final int payloadOffset = PacketManipulator.GetRTPHeaderLength(pkt);
        final int payloadLength = PacketManipulator.GetRTPPayloadLength(pkt);

        SRTPCipherF8.process(AEScipher, pkt.getBuffer(), pkt.getOffset() + payloadOffset,
                             payloadLength, iv, encKey, saltKey, AEScipherF8);
    }

    /**
     * Authenticate a packet using HMC SHA1 method.
     * Calculated authentication tag is returned.
     *
     * @param pkt the RTP packet to be authenticated
     * @return authentication tag of pkt
     */
    private byte[] authenticatePacketHMCSHA1(RawPacket pkt)
    {
        hmacSha1.update(pkt.getBuffer(), 0, pkt.getLength());
        byte[] rb = new byte[4];
        rb[0] = (byte) (this.roc >> 24);
        rb[1] = (byte) (this.roc >> 16);
        rb[2] = (byte) (this.roc >> 8);
        rb[3] = (byte) this.roc;
        hmacSha1.update(rb);
        
        return hmacSha1.doFinal();
    }
    
    /**
     * Checks if a packet is a replayed on based on its sequence number.
     * 
     * This method supports a 64 packet history relative the the given
     * sequence number.
     *
     * Sequence Number is guaranteed to be real (not faked) through 
     * authentication.
     * 
     * @param seqNum sequence number of the packet
     * @return true if this sequence number indicates the packet is not a
     * replayed one, false if not
     */
    boolean checkReplay(int seqNum)
    {
        /*
         * Initialize the sequences number on first call that uses the
         * sequence number. Either guessIndex() or checkReplay().
         */
        if (!this.seqNumSet)
        {
            this.seqNumSet = true;
            this.seqNum = seqNum;
        }

        long guessedIndex = guessIndex( seqNum );
        long localIndex = (((long)this.roc) << 16 & 0xFFFF) | this.seqNum;

        long delta = guessedIndex - localIndex;
        if (delta > 0)
        {
            /* Packet not yet received */
            return true;
        }
        else
        {
            if( -delta > REPLAY_WINDOW_SIZE )
            {
                /* Packet too old */
                return false;
            }
            else
            {
                if(((this.replayWindow >> (-delta)) & 0x1) != 0)
                {
                    /* Packet already received ! */
                    return false;
                }
                else
                {
                    /* Packet not yet received */
                    return true;
                }
            }
        }
    }


    /**
     * Compute the initialization vector, used later by encryption algorithms,
     * based on the lable, the packet index, key derivation rate and master
     * salt key. 
     * 
     * @param iv calculated initialization vector 
     * @param label label specified for each type of iv 
     * @param index 48bit RTP packet index
     * @param kdv key derivation rate of this SRTPCryptoContext
     * @param masterSalt master salt key
     */
    private static void computeIv(byte[] iv, long label, long index,
                                  long kdv, byte[] masterSalt)
    {
        long key_id;

        if (kdv == 0)
        {
            key_id = label << 48;
        }
        else
        {
            key_id = ((label << 48) | (index / kdv));
        }

        for (int i = 0; i < 7; i++)
        {
            iv[i] = masterSalt[i];
        }

        for (int i = 7; i < 14; i++)
        {
            iv[i] = (byte)
                   ((byte)(0xFF & (key_id >> (8 * (13 - i)))) ^ masterSalt[i]);
        }

        iv[14] = iv[15] = 0;
    }

    /**
     * Derives the srtp session keys from the master key
     * @param index the 48 bit SRTP packet index
     */
    public void deriveSrtpKeys(long index)
    {
        byte[] iv = new byte[16];

        // compute the session encryption key
        long label = 0;
        computeIv(iv, label, index, this.keyDerivationRate, this.masterSalt);
        
        SecretKey encryptionKey = new SecretKeySpec(masterKey, 0, policy.getEncKeyLength(), "AES");
        
        try 
        {
            AEScipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        } 
        catch (InvalidKeyException e1) 
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        SRTPCipherCTR.getCipherStream(AEScipher, encKey, policy.getEncKeyLength(), iv);


        // compute the session authentication key
        if (this.authKey != null)
        {
            label = 0x01;
            computeIv(iv, label, index, this.keyDerivationRate, this.masterSalt);
            
            SRTPCipherCTR.getCipherStream(AEScipher, authKey, policy.getAuthKeyLength(), iv);

            SecretKey key = new SecretKeySpec(authKey, "HMAC");
            try 
            {
                hmacSha1.init(key);
            } 
            catch (InvalidKeyException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        // compute the session salt
        label = 0x02;
        computeIv(iv, label, index, this.keyDerivationRate, this.masterSalt);
        
        SRTPCipherCTR.getCipherStream(AEScipher, saltKey, policy.getSaltKeyLength(), iv);
        
        // As last step: initialize AES cipher with derived encryption key.
        encryptionKey = new SecretKeySpec(encKey, 0, policy.getEncKeyLength(), "AES");
        try 
        {
            AEScipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        } 
        catch (InvalidKeyException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Compute (guess) the new SRTP index based on the sequence number of
     * a received RTP packet.
     * 
     * @param seqNum sequence number of the received RTP packet
     * @return the new SRTP packet index
     */
    private long guessIndex(int seqNum)
    {
        if (!this.seqNumSet)
        {
            this.seqNumSet = true;
            this.seqNum = seqNum;
        }

        if (this.seqNum < 32768)
        {
            if (seqNum - this.seqNum > 32768)
            {
                this.guessedROC = this.roc - 1;
            }
            else
            {
                this.guessedROC = this.roc;
            }
        }
        else
        {
            if (this.seqNum - 32768 > seqNum)
            {
                this.guessedROC = this.roc + 1;
            }
            else
            {
                this.guessedROC = this.roc;
            }
        }

        return ((long) this.guessedROC) << 16 | seqNum;
    }

    /**
     * Update the SRTP packet index.
     * 
     * This method is called after all checks were successful. 
     * See section 3.3.1 in RFC3711 for detailed description.
     * 
     * @param seqNum sequence number of the accepted packet
     */
    private void update(int seqNum)
    {
        guessIndex(seqNum);

        if (seqNum > this.seqNum)
        {
            this.seqNum = seqNum;
        }
        if (this.guessedROC > this.roc)
        {
            this.roc = this.guessedROC;
            this.seqNum = seqNum;
        }
    }

    /**
     * Derive a new SRTPCryptoContext for use with a new SSRC
     *
     * This method returns a new SRTPCryptoContext initialized with the data
     * of this SRTPCryptoContext. Replacing the SSRC, Roll-over-Counter, and
     * the key derivation rate the application cab use this SRTPCryptoContext
     * to encrypt / decrypt a new stream (Synchronization source) inside
     * one RTP session.
     *
     * Before the application can use this SRTPCryptoContext it must call the
     * deriveSrtpKeys method.
     *
     * @param ssrc The SSRC for this context
     * @param roc The Roll-Over-Counter for this context
     * @param deriveRate The key derivation rate for this context
     * @return a new SRTPCryptoContext with all relevant data set.
     */
    public SRTPCryptoContext deriveContext(long ssrc, int roc, long deriveRate)
    {
        SRTPCryptoContext pcc = null;
        try 
        {
            pcc = new SRTPCryptoContext(ssrc, roc, deriveRate,
                                        this.masterKey, this.masterSalt, this.policy,
                                        this.cryptoProvider);
        } 
        catch (GeneralSecurityException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pcc;

    }
}
