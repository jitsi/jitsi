/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.impl.neomedia.AbstractRTPConnector;
import net.java.sip.communicator.impl.neomedia.transform.TransformEngine;
import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Controls SRTP encryption in the MediaStream.
 *
 * @author Damian Minkov
 */
public interface SrtpControl
{
    /**
     * Cleans up the current SRTP control and its engine.
     */
    public void cleanup();

    /**
     * Sets a <tt>SrtpListener</tt> that will listen for security events.
     * 
     * @param srtpListener the <tt>SrtpListener</tt> that will receive the
     *            events
     */
    public void setSrtpListener(SrtpListener srtpListener);

    /**
     * Returns the <tt>SrtpListener</tt> which listens for security events.
     *
     * @return the <tt>SrtpListener</tt> which listens for security events
     */
    public SrtpListener getSrtpListener();

    /**
     * Gets the default secure/insecure communication status for the supported
     * call sessions.
     *
     * @return default secure communication status for the supported
     *          call sessions.
     */
    public boolean getSecureCommunicationStatus();

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
     * @param master The security control of the master stream.
     */
    public void setMultistream(SrtpControl master);

    /**
     * Returns the transform engine currently used by this stream.
     * 
     * @return the RTP stream transformation engine
     */
    public TransformEngine getTransformEngine();

    /**
     * Sets the <tt>RTPConnector</tt> which is to use or uses this SRTP engine.
     *
     * @param connector the <tt>RTPConnector</tt> which is to use or uses this
     * SRTP engine
     */
    public void setConnector(AbstractRTPConnector newValue);

    /**
     * Indicates if the key exchange method is dependent on secure transport of
     * the signaling channel.
     * 
     * @return True when secure signaling is required to make the encryption
     *         secure, false otherwise.
     */
    public boolean requiresSecureSignalingTransport();
}
