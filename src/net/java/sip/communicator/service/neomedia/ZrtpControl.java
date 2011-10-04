/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
}
