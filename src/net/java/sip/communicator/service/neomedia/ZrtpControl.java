/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Controls zrtp in the MediaStream.
 *
 * @author Damian Minkov
 */
public interface ZrtpControl
{
    /**
     * Cleans up the current zrtp control and its engine.
     */
    public void cleanup();

    /**
     * Sets a <tt>ZrtpListener</tt> that will listen for
     * zrtp security events.
     *
     * @param zrtpListener the <tt>ZrtpListener</tt> to set
     */
    public void setZrtpListener(ZrtpListener zrtpListener);

    /**
     * Returns the <tt>ZrtpListener</tt> which listens for security events.
     *
     * @return the <tt>ZrtpListener</tt> which listens for  security events
     */
    public ZrtpListener getZrtpListener();

    /**
     * Gets the default secure/unsecure communication status for the supported
     * call sessions.
     *
     * @return default secure communication status for the supported
     *          call sessions.
     */
    public boolean getSecureCommunicationStatus();

    /**
     * Sets the SAS verification
     *
     * @param verified the new SAS verification status
     */
    public void setSASVerification(boolean verified);

    /**
     * Starts and enables zrtp in the stream holding this control.
     * @param masterSession whether this stream is master for the current
     *        media session.
     */
    public void start(boolean masterSession);

    /**
     * Sets the multistream data, which means that the master stream
     * has successfully started and this will start all other streams
     * in this session.
     * @param multiStreamData the multistream data comming from master stream
     *        needed to start rest of the streams in the session.
     */
    public void setMultistream(byte[] multiStreamData);

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
}
