/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.srtp;

import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * SRTPTransformEngine class implements TransformEngine interface.
 * It stores important information / objects regarding SRTP processing.
 * Through SRTPTransformEngine, we can get the needed PacketTransformer, which
 * will be used by abstract TransformConnector classes.
 *
 * @author Bing SU (nova.su@gmail.com)
 *
 */
public class SRTPTransformEngine
    implements TransformEngine
{
    /**
     * Master key of this SRTP session
     */
    private final byte[] masterKey;

    /**
     * Master salt key of this SRTP session
     */
    private final byte[] masterSalt;

    /**
     * SRTP processing policy
     */
    private final SRTPPolicy srtpPolicy;

    /**
     * SRTCP processing policy
     */
    private final SRTPPolicy srtcpPolicy;

    /**
     * The default SRTPCryptoContext, which will be used to derivate other
     * contexts.
     */
    private SRTPCryptoContext defaultContext;

    /**
     * The default SRTPCryptoContext, which will be used to derive other
     * contexts.
     */
    private SRTCPCryptoContext defaultContextControl;

    /**
     * Construct a SRTPTransformEngine based on given master encryption key,
     * master salt key and SRTP/SRTCP policy.
     *
     * @param masterKey the master encryption key
     * @param masterSalt the master salt key
     * @param srtpPolicy SRTP policy
     * @param srtcpPolicy SRTCP policy
     */
    public SRTPTransformEngine(byte[] masterKey, byte[] masterSalt,
                               SRTPPolicy srtpPolicy, SRTPPolicy srtcpPolicy)
    {
        this.masterKey = new byte[masterKey.length];
        System.arraycopy(masterKey, 0, this.masterKey, 0, masterKey.length);

        this.masterSalt = new byte[masterSalt.length];
        System.arraycopy(masterSalt, 0, this.masterSalt, 0, masterSalt.length);

        this.srtpPolicy  = srtpPolicy;
        this.srtcpPolicy = srtcpPolicy;

        this.defaultContext = new SRTPCryptoContext(0, 0, 0,
                                                    this.masterKey,
                                                    this.masterSalt,
                                                    this.srtpPolicy);
        this.defaultContextControl = new SRTCPCryptoContext(0, 
                                                    this.masterKey,
                                                    this.masterSalt, 
                                                    this.srtcpPolicy);
    }

    /**
     * Gets the <tt>PacketTransformer</tt> for RTCP packets.
     *
     * @return the <tt>PacketTransformer</tt> for RTCP packets
     */
    public PacketTransformer getRTCPTransformer()
    {
        return new SRTCPTransformer(this);
    }

    /*
     *  (non-Javadoc)
     * @see net.java.sip.communicator.impl.media.transform.
     * TransformEngine#getRTPTransformer()
     */
    public PacketTransformer getRTPTransformer()
    {
        return new SRTPTransformer(this);
    }

    /**
     * Get the master encryption key
     *
     * @return the master encryption key
     */
    public byte[] getMasterKey()
    {
        return this.masterKey;
    }

    /**
     * Get the master salt key
     *
     * @return the master salt key
     */
    public byte[] getMasterSalt()
    {
        return this.masterSalt;
    }

    /**
     * Get the SRTCP policy
     *
     * @return the SRTCP policy
     */
    public SRTPPolicy getSRTCPPolicy()
    {
        return this.srtcpPolicy;
    }

    /**
     * Get the SRTP policy
     *
     * @return the SRTP policy
     */
    public SRTPPolicy getSRTPPolicy()
    {
        return this.srtpPolicy;
    }

    /**
     * Get the default SRTPCryptoContext
     *
     * @return the default SRTPCryptoContext
     */
    public SRTPCryptoContext getDefaultContext()
    {
        return this.defaultContext;
    }

    /**
     * Get the default SRTPCryptoContext
     *
     * @return the default SRTPCryptoContext
     */
    public SRTCPCryptoContext getDefaultContextControl() {
        return this.defaultContextControl;
    }
}
