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
package net.java.sip.communicator.impl.neomedia.transform.srtp;

import net.java.sip.communicator.impl.neomedia.*;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.*;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersForSkein;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.Mac;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;


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
 * Phil Zimmermann's ZRTP protocol.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTCPCryptoContext
{
    /**
     * The replay check windows size
     */
    private static final long REPLAY_WINDOW_SIZE = 64;
    
    /**
     * RTCP SSRC of this cryptographic context
     */
    private long ssrc;
    
    /**
     * Master key identifier
     */
    private byte[] mki;
   
    /**
     * Index received so far
     */
    private int receivedIndex = 0;
    
    /**
     * Index sent so far
     */
    private int sentIndex = 0;
    
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
     * The HMAC object we used to do packet authentication
     */
    private Mac mac;             // used for various HMAC computations
    
    // The symmetric cipher engines we need here
    private BlockCipher cipher = null;
    private BlockCipher cipherF8 = null; // used inside F8 mode only
    
    // implements the counter cipher mode for RTP according to RFC 3711
    private final SRTPCipherCTR cipherCtr = new SRTPCipherCTR();

    // Here some fields that a allocated here or in constructor. The methods
    // use these fields to avoid too many new operations
    
    private final byte[] tagStore;
    private final byte[] ivStore = new byte[16];
    private final byte[] rbStore = new byte[4];
    
    // this is some working store, used by some methods to avoid new operations
    // the methods must use this only to store some reults for immediate processing
    private final byte[] tempStore = new byte[100];

    /**
     * Construct an empty SRTPCryptoContext using ssrc.
     * The other parameters are set to default null value.
     * 
     * @param ssrc SSRC of this SRTPCryptoContext
     */
    public SRTCPCryptoContext(long ssrcIn)
    {
        ssrc = ssrcIn;
        mki = null;
        masterKey = null;
        masterSalt = null;
        encKey = null;
        authKey = null;
        saltKey = null;
        policy = null;
        tagStore = null;
    }

    /**
     * Construct a normal SRTPCryptoContext based on the given parameters.
     * 
     * @param ssrc
     *            the RTP SSRC that this SRTP cryptographic context protects.
     * @param masterKey
     *            byte array holding the master key for this SRTP cryptographic
     *            context. Refer to chapter 3.2.1 of the RFC about the role of
     *            the master key.
     * @param masterSalt
     *            byte array holding the master salt for this SRTP cryptographic
     *            context. It is used to computer the initialization vector that
     *            in turn is input to compute the session key, session
     *            authentication key and the session salt.
     * @param policy
     *            SRTP policy for this SRTP cryptographic context, defined the
     *            encryption algorithm, the authentication algorithm, etc
     */
    @SuppressWarnings("fallthrough")
    public SRTCPCryptoContext(long ssrcIn,
            byte[] masterK, byte[] masterS, SRTPPolicy policyIn) 
    {
        ssrc = ssrcIn;
        mki = null;

        policy = policyIn;

        masterKey = new byte[policy.getEncKeyLength()];
        System.arraycopy(masterK, 0, masterKey, 0, policy
                .getEncKeyLength());

        masterSalt = new byte[policy.getSaltKeyLength()];
        System.arraycopy(masterS, 0, masterSalt, 0, policy
                .getSaltKeyLength());

        switch (policy.getEncType()) {
        case SRTPPolicy.NULL_ENCRYPTION:
            encKey = null;
            saltKey = null;
            break;

        case SRTPPolicy.AESCM_ENCRYPTION:
            cipher = new AESFastEngine();
            encKey = new byte[this.policy.getEncKeyLength()];
            saltKey = new byte[this.policy.getSaltKeyLength()];
            
        case SRTPPolicy.AESF8_ENCRYPTION:
            cipherF8 = new AESFastEngine();
            break;

        case SRTPPolicy.TWOFISH_ENCRYPTION:
            cipher = new TwofishEngine();
            encKey = new byte[this.policy.getEncKeyLength()];
            saltKey = new byte[this.policy.getSaltKeyLength()];
            
        case SRTPPolicy.TWOFISHF8_ENCRYPTION:
            cipherF8 = new TwofishEngine();
            break;
        }
        
        switch (policy.getAuthType()) {
        case SRTPPolicy.NULL_AUTHENTICATION:
            authKey = null;
            tagStore = null;
            break;

        case SRTPPolicy.HMACSHA1_AUTHENTICATION:
            mac = new HMac(new SHA1Digest());
            authKey = new byte[policy.getAuthKeyLength()];
            tagStore = new byte[mac.getMacSize()];
            break;
            
        case SRTPPolicy.SKEIN_AUTHENTICATION:
            mac = new SkeinMac();
            authKey = new byte[policy.getAuthKeyLength()];
            tagStore = new byte[policy.getAuthTagLength()];
            break;

        default:
            tagStore = null;
        }
    }

    /**
     * Get the authentication tag length of this SRTP cryptographic context
     * 
     * @return the authentication tag length of this SRTP cryptographic context
     */
    public int getAuthTagLength() {
        return policy.getAuthTagLength();
    }

    /**
     * Get the MKI length of this SRTP cryptographic context
     * 
     * @return the MKI length of this SRTP cryptographic context
     */
    public int getMKILength() {
        if (mki != null) {
            return mki.length;
        } else {
            return 0;
        }
    }

    /**
     * Get the SSRC of this SRTP cryptographic context
     *
     * @return the SSRC of this SRTP cryptographic context
     */
    public long getSSRC() {
        return ssrc;
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
    public void transformPacket(RawPacket pkt) {
        
        boolean encrypt = false;
        /* Encrypt the packet using Counter Mode encryption */
        if (policy.getEncType() == SRTPPolicy.AESCM_ENCRYPTION || 
                policy.getEncType() == SRTPPolicy.TWOFISH_ENCRYPTION) {
            processPacketAESCM(pkt, sentIndex);
            encrypt = true;
        }

        /* Encrypt the packet using F8 Mode encryption */
        else if (policy.getEncType() == SRTPPolicy.AESF8_ENCRYPTION ||
                policy.getEncType() == SRTPPolicy.TWOFISHF8_ENCRYPTION) {
            processPacketAESF8(pkt, sentIndex);
            encrypt = true;
        }
        int index = 0;
        if (encrypt)
            index = sentIndex | 0x80000000;
        
        // Grow packet storage in one step
        pkt.grow(4 + policy.getAuthTagLength());
        
        // Authenticate the packet
        // The authenticate method gets the index via parameter and stores
        // it in network order in rbStore variable. 
        if (policy.getAuthType() != SRTPPolicy.NULL_AUTHENTICATION) {
            authenticatePacket(pkt, index);
            pkt.append(rbStore, 4);
            pkt.append(tagStore, policy.getAuthTagLength());
        }
        sentIndex++;
        sentIndex &= ~0x80000000;       // clear possible overflow
    }

    /**
     * Transform a SRTCP packet into a RTCP packet.
     * This method is called when a SRTCP packet was received.
     * 
     * Operations done by the this operation include:
     * Authentication check, Packet replay check and decryption.
     * 
     * Both encryption and authentication functionality can be turned off
     * as long as the SRTPPolicy used in this SRTPCryptoContext requires no
     * encryption and no authentication. Then the packet will be sent out
     * untouched. However this is not encouraged. If no SRTCP feature is enabled,
     * then we shall not use SRTP TransformConnector. We should use the original
     * method (RTPManager managed transportation) instead.  
     * 
     * @param pkt the received RTCP packet 
     * @return true if the packet can be accepted
     *         false if authentication or replay check failed 
     */
    public boolean reverseTransformPacket(RawPacket pkt) {

        boolean decrypt = false;
        int tagLength = policy.getAuthTagLength();
        int indexEflag = pkt.getSRTCPIndex(tagLength);

        if ((indexEflag & 0x80000000) == 0x80000000)
            decrypt = true;

        int index = indexEflag & ~0x80000000;
        
        /* Replay control */
        if (!checkReplay(index)) {
            return false;

        }
        /* Authenticate the packet */
        if (policy.getAuthType() != SRTPPolicy.NULL_AUTHENTICATION) {

            // get original authentication data and store in tempStore
            pkt.readRegionToBuff(pkt.getLength() - tagLength, tagLength,
                    tempStore);
            
            // Shrink packet to remove the authentication tag and index
            // because this is part of authenicated data
            pkt.shrink(tagLength + 4);

            // compute, then save authentication in tagStore
            authenticatePacket(pkt, indexEflag);

            for (int i = 0; i < tagLength; i++) {
                if ((tempStore[i] & 0xff) == (tagStore[i] & 0xff))
                    continue;
                else
                    return false;
            }
        }

        if (decrypt) {
            /* Decrypt the packet using Counter Mode encryption */
            if (policy.getEncType() == SRTPPolicy.AESCM_ENCRYPTION
                    || policy.getEncType() == SRTPPolicy.TWOFISH_ENCRYPTION) {
                processPacketAESCM(pkt, index);
            }

            /* Decrypt the packet using F8 Mode encryption */
            else if (policy.getEncType() == SRTPPolicy.AESF8_ENCRYPTION
                    || policy.getEncType() == SRTPPolicy.TWOFISHF8_ENCRYPTION) {
                processPacketAESF8(pkt, index);
            }
        }
        update(index);

        return true;
    }

    /**
     * Perform Counter Mode AES encryption / decryption 
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESCM(RawPacket pkt, int index) {
        long ssrc = pkt.GetRTCPSSRC();

        /* Compute the CM IV (refer to chapter 4.1.1 in RFC 3711):
        *
        * k_s   XX XX XX XX XX XX XX XX XX XX XX XX XX XX
        * SSRC              XX XX XX XX
        * index                               XX XX XX XX
        * ------------------------------------------------------XOR
        * IV    XX XX XX XX XX XX XX XX XX XX XX XX XX XX 00 00
        *        0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
        */
        ivStore[0] = saltKey[0];
        ivStore[1] = saltKey[1];
        ivStore[2] = saltKey[2];
        ivStore[3] = saltKey[3];

        // The shifts transform the ssrc and index into network order
        ivStore[4] = (byte) (((ssrc >> 24) & 0xff) ^ this.saltKey[4]);
        ivStore[5] = (byte) (((ssrc >> 16) & 0xff) ^ this.saltKey[5]);
        ivStore[6] = (byte) (((ssrc >> 8) & 0xff) ^ this.saltKey[6]);
        ivStore[7] = (byte) ((ssrc & 0xff) ^ this.saltKey[7]);

        ivStore[8] = saltKey[8];
        ivStore[9] = saltKey[9];

        ivStore[10] = (byte) (((index >> 24) & 0xff) ^ this.saltKey[10]);
        ivStore[11] = (byte) (((index >> 16) & 0xff) ^ this.saltKey[11]);
        ivStore[12] = (byte) (((index >> 8) & 0xff) ^ this.saltKey[12]);
        ivStore[13] = (byte) ((index & 0xff) ^ this.saltKey[13]);

        ivStore[14] = ivStore[15] = 0;

        // Encrypted part excludes fixed header (8 bytes)  
        final int payloadOffset = 8;
        final int payloadLength = pkt.getLength() - payloadOffset;

        cipherCtr.process(cipher, pkt.getBuffer(), pkt.getOffset() + payloadOffset,
                payloadLength, ivStore);
    }

    /**
     * Perform F8 Mode AES encryption / decryption
     *
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESF8(RawPacket pkt, int index) {
        // byte[] iv = new byte[16];

        // 4 bytes of the iv are zero
        // the first byte of the RTP header is not used.
        ivStore[0] = 0;
        ivStore[1] = 0;
        ivStore[2] = 0;
        ivStore[3] = 0;
      
        // Need the encryption flag
        index = index | 0x80000000;

        // set the index and the encrypt flag in network order into IV
        ivStore[4] = (byte) (index >> 24);
        ivStore[5] = (byte) (index >> 16);
        ivStore[6] = (byte) (index >> 8);
        ivStore[7] = (byte) index;
        
        // The fixed header follows and fills the rest of the IV
        System.arraycopy(pkt.getBuffer(), pkt.getOffset(), ivStore, 8, 8);

        // Encrypted part excludes fixed header (8 bytes), index (4 bytes), and
        // authentication tag (variable according to policy)  
        final int payloadOffset = 8;
        final int payloadLength = pkt.getLength() - (4 + policy.getAuthTagLength());

        SRTPCipherF8.process(cipher, pkt.getBuffer(), pkt.getOffset() + payloadOffset,
                payloadLength, ivStore, encKey, saltKey, cipherF8);
    }

    /**
     * Authenticate a packet.
     * 
     * Calculated authentication tag is stored in tagStore area.
     *
     * @param pkt the RTP packet to be authenticated
     */
    private void authenticatePacket(RawPacket pkt, int index) {

        mac.update(pkt.getBuffer(), 0, pkt.getLength());
        // byte[] rb = new byte[4];
        rbStore[0] = (byte) (index >> 24);
        rbStore[1] = (byte) (index >> 16);
        rbStore[2] = (byte) (index >> 8);
        rbStore[3] = (byte) index;
        mac.update(rbStore, 0, rbStore.length);
        mac.doFinal(tagStore, 0);
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
     * @param index index number of the SRTCP packet
     * @return true if this sequence number indicates the packet is not a
     * replayed one, false if not
     */
    boolean checkReplay(int index) {
        // compute the index of previously received packet and its
        // delta to the new received packet
        long delta = index - receivedIndex;
        
        if (delta > 0) {
            /* Packet not yet received */
            return true;
        } else {
            if (-delta > REPLAY_WINDOW_SIZE) {
                /* Packet too old */
                return false;
            } else {
                if (((this.replayWindow >> (-delta)) & 0x1) != 0) {
                    /* Packet already received ! */
                    return false;
                } else {
                    /* Packet not yet received */
                    return true;
                }
            }
        }
    }

    /**
     * Compute the initialization vector, used later by encryption algorithms,
     * based on the label.
     * 
     * @param label label specified for each type of iv 
     */
    private void computeIv(byte label) {

        for (int i = 0; i < 14; i++) {
            ivStore[i] = masterSalt[i];
        }
        ivStore[7] ^= label;
        ivStore[14] = ivStore[15] = 0;
    }

    /**
     * Derives the srtcp session keys from the master key.
     * 
     */
    public void deriveSrtcpKeys() {
        // compute the session encryption key
        byte label = 3;
        computeIv(label);

        KeyParameter encryptionKey = new KeyParameter(masterKey);
        cipher.init(true, encryptionKey);
        cipherCtr.getCipherStream(cipher, encKey, policy.getEncKeyLength(), ivStore);

        if (authKey != null) {
            label = 4;
            computeIv(label);
            cipherCtr.getCipherStream(cipher, authKey, policy.getAuthKeyLength(), ivStore);

            switch ((policy.getAuthType())) {
            case SRTPPolicy.HMACSHA1_AUTHENTICATION:
                KeyParameter key =  new KeyParameter(authKey);
                mac.init(key);
                break;

            case SRTPPolicy.SKEIN_AUTHENTICATION:
                // Skein MAC uses number of bits as MAC size, not just bytes
                ParametersForSkein pfs = new ParametersForSkein(new KeyParameter(authKey),
                        ParametersForSkein.Skein512, tagStore.length*8);
                mac.init(pfs);
                break;
            }
        }
        // compute the session salt
        label = 5;
        computeIv(label);
        cipherCtr.getCipherStream(cipher, saltKey, policy.getSaltKeyLength(), ivStore);
        
        // As last step: initialize cipher with derived encryption key.
        encryptionKey = new KeyParameter(encKey);
        cipher.init(true, encryptionKey);
    }


    /**
     * Update the SRTP packet index.
     * 
     * This method is called after all checks were successful. 
     * 
     * @param index index number of the accepted packet
     */
    private void update(int index) {
        int delta = receivedIndex - index;

        /* update the replay bit mask */
        if( delta > 0 ){
          replayWindow = replayWindow << delta;
          replayWindow |= 1;
        }
        else {
          replayWindow |= ( 1 << delta );
        }

        receivedIndex = index;
    }

    /**
     * Derive a new SRTPCryptoContext for use with a new SSRC
     * 
     * This method returns a new SRTPCryptoContext initialized with the data of
     * this SRTPCryptoContext. Replacing the SSRC, Roll-over-Counter, and the
     * key derivation rate the application cab use this SRTPCryptoContext to
     * encrypt / decrypt a new stream (Synchronization source) inside one RTP
     * session.
     * 
     * Before the application can use this SRTPCryptoContext it must call the
     * deriveSrtpKeys method.
     * 
     * @param ssrc
     *            The SSRC for this context
     * @return a new SRTPCryptoContext with all relevant data set.
     */
    public SRTCPCryptoContext deriveContext(long ssrc) {
        SRTCPCryptoContext pcc = null;
        pcc = new SRTCPCryptoContext(ssrc, masterKey,
                masterSalt, policy);
        return pcc;
    }
}
