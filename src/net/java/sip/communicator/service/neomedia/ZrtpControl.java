/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * ZRTP based SRTP MediaStream encryption control.
 *
 * @author Damian Minkov
 */
public interface ZrtpControl
    extends SrtpControl
{
    /**
     * Return the zrtp hello hash String.
     *
     * @return String the zrtp hello hash.
     */
    public String getHelloHash();

    /**
     * Get the ZRTP Hello Hash data - separate strings.
     *
     * @return String array containing the version string at offset 0, the Hello
     *         hash value as hex-digits at offset 1. Hello hash is available
     *         immediately after class instantiation. Returns <code>null</code>
     *         if ZRTP is not available.
     */
    public String[] getHelloHashSep();

    /**
     * Gets the SAS for the current media stream.
     * 
     * @return the four character ZRTP SAS.
     */
    public String getSecurityString();

    /**
     * Gets the cipher information for the current media stream.
     * 
     * @return the cipher information string.
     */
    public String getCipherString();

    /**
     * Gets the status of the SAS verification.
     * 
     * @return true when the SAS has been verified.
     */
    public boolean isSecurityVerified();

    /**
     * Sets the SAS verification
     *
     * @param verified the new SAS verification status
     */
    public void setSASVerification(boolean verified);

    /**
     * Returns the timeout value that will we will wait
     * and fire timeout secure event if call is not secured.
     * The value is in milliseconds.
     * @return the timeout value that will we will wait
     *  and fire timeout secure event if call is not secured.
     */
    public long getTimeoutValue();
    
    /**
     * Get other party's ZID (ZRTP Identifier) data
     * 
     * This functions returns the other party's ZID that was receivied during
     * ZRTP processing.
     *
     * The ZID data can be retrieved after ZRTP receive the first Hello packet
     * from the other party.
     * 
     * @return the ZID data as byte array.
     */
    public byte[] getPeerZid();

    /**
     * Get other party's ZID (ZRTP Identifier) data as String
     * 
     * This functions returns the other party's ZID that was receivied during
     * ZRTP processing.
     *
     * The ZID data can be retrieved after ZRTP receive the first Hello packet
     * from the other party.
     *
     * @return the ZID data as String.
     */
    public String getPeerZidString();
}
