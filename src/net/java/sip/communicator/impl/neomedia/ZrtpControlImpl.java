/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import gnu.java.zrtp.*;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.zrtp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Controls zrtp in the MediaStream.
 *
 * @author Damian Minkov
 */
public class ZrtpControlImpl
    implements ZrtpControl
{
    /**
     * The listener interested in security events about zrtp.
     */
    private ZrtpListener zrtpListener = null;

    /**
     * Additional info codes for and data to support ZRTP4J.
     * These could be added to the library. However they are specific for this
     * implementation, needing them for various GUI changes.
     */
    public static enum ZRTPCustomInfoCodes
    {
        ZRTPNotEnabledByUser,
        ZRTPDisabledByCallEnd,
        ZRTPEngineInitFailure,
        ZRTPEnabledByDefault
    }

    /**
     * The zrtp engine control by this ZrtpControl.
     */
    private ZRTPTransformEngine zrtpEngine = null;

    /**
     * This is the connector, required to send ZRTP packets
     * via the DatagramSocket.
     */
    private RTPTransformConnector zrtpConnector = null;

    /**
     * Creates the control.
     */
    ZrtpControlImpl()
    {
    }

    /**
     * Cleans up the current zrtp control and its engine.
     */
    public void cleanup()
    {
        if(zrtpEngine != null)
        {
            zrtpEngine.stopZrtp();
            zrtpEngine.cleanup();
        }

        zrtpEngine = null;
        zrtpConnector = null;
    }

    /**
     * Sets a <tt>ZrtpListener</tt> that will listen for zrtp security events.
     *
     * @param zrtpListener the <tt>ZrtpListener</tt> to set
     */
    public void setZrtpListener(ZrtpListener zrtpListener)
    {
        this.zrtpListener = zrtpListener;
    }

    /**
     * Returns the <tt>ZrtpListener</tt> which listens for security events.
     *
     * @return the <tt>ZrtpListener</tt> which listens for  security events
     */
    public ZrtpListener getZrtpListener()
    {
        return this.zrtpListener;
    }

    /**
     * Method for getting the default secure status value for communication
     *
     * @return the default enabled/disabled status value for secure
     * communication
     */
    public boolean getSecureCommunicationStatus()
    {
        if(zrtpEngine != null)
            return zrtpEngine.getSecureCommunicationStatus();
        else
            return false;
    }

    /**
     * Sets the SAS verification
     *
     * @param verified the new SAS verification status
     */
    public void setSASVerification(boolean verified)
    {
        ZRTPTransformEngine engine = getZrtpEngine();

        if (verified)
        {
            engine.SASVerified();
        } else
        {
            engine.resetSASVerified();
        }
    }

    /**
     * Returns the zrtp engine currently used by this stream.
     * @return the zrtp engine
     */
    public ZRTPTransformEngine getZrtpEngine()
    {
        if(zrtpEngine == null)
        {
            zrtpEngine = new ZRTPTransformEngine();
            ZrtpConfigure config = ZrtpConfigureUtils.getZrtpConfiguration();
            zrtpEngine.initialize("GNUZRTP4J.zid", false, config);
        }

        return zrtpEngine;
    }

    /**
     * Starts and enables zrtp in the stream holding this control.
     * @param masterSession whether this stream is master for the current
     *        media session.
     */
    public void start(boolean masterSession)
    {
        // Create security user callback for each peer.
        SecurityEventManager securityEventManager
            = new SecurityEventManager(this);

        boolean zrtpAutoStart = false;

        // ZRTP engine initialization
        ZRTPTransformEngine engine = getZrtpEngine();

        // Decide if this will become the ZRTP Master session:
        // - Statement: audio media session will be started before video
        //   media session
        // - if no other audio session was started before then this will
        //   become
        //   ZRTP Master session
        // - only the ZRTP master sessions start in "auto-sensing" mode
        //   to immediately catch ZRTP communication from other client
        // - after the master session has completed its key negotiation
        //   it will start other media sessions (see SCCallback)
        if (masterSession)
        {
            zrtpAutoStart = true;
            securityEventManager.setDHSession(true);

            // we know that audio is considered as master for zrtp
            securityEventManager.setSessionType(
               SecurityEventManager.AUDIO_SESSION);
        }
        else
        {
            // check whether video was not already started
            // it may happen when using multistreams, audio has inited
            // and started video
            // initially engine has value enableZrtp = false
            zrtpAutoStart = zrtpEngine.isEnableZrtp();
            securityEventManager.setSessionType(
                SecurityEventManager.VIDEO_SESSION);
        }

        // tells the engine whether to autostart(enable)
        // zrtp communication, if false it just passes packets without
        // transformation
        engine.setEnableZrtp(zrtpAutoStart);

        engine.setConnector(zrtpConnector);

        engine.setUserCallback(securityEventManager);

        engine.sendInfo(
            ZrtpCodes.MessageSeverity.Info,
            EnumSet.of(
                    ZRTPCustomInfoCodes.ZRTPEnabledByDefault));
    }

    /**
     * Start multi-stream ZRTP sessions.
     *
     * After the ZRTP Master (DH) session reached secure state the SCCallback calls
     * this method to start the multi-stream ZRTP sessions.
     *
     * enable auto-start mode (auto-sensing) to the engine.
     * @param multiStreamData
     */
    public void setMultistream(byte[] multiStreamData)
    {
        ZRTPTransformEngine engine = getZrtpEngine();
        engine.setMultiStrParams(multiStreamData);
        engine.setEnableZrtp(true);
    }

    /**
     * Return the zrtp hello hash String.
     *
     * @return String the zrtp hello hash.
     */
    public String getHelloHash()
    {
        return getZrtpEngine().getHelloHash();
    }

    /**
     * Sets the RTP connector using this ZRTP engine
     *
     * @param connector the connector to set
     */
    public void setConnector(RTPTransformConnector connector)
    {
        zrtpConnector = connector;
    }
}
