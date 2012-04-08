/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import gnu.java.zrtp.*;
import gnu.java.zrtp.utils.*;

import java.util.*;

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
    private SrtpListener zrtpListener = null;

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
    private AbstractRTPConnector zrtpConnector = null;

    /**
     * Whether current is master session.
     */
    private boolean masterSession = false;

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
    public void setSrtpListener(SrtpListener zrtpListener)
    {
        this.zrtpListener = zrtpListener;
    }

    /**
     * Returns the <tt>ZrtpListener</tt> which listens for security events.
     *
     * @return the <tt>ZrtpListener</tt> which listens for  security events
     */
    public SrtpListener getSrtpListener()
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
        return
            (zrtpEngine != null) && zrtpEngine.getSecureCommunicationStatus();
    }

    /**
     * Sets the SAS verification
     *
     * @param verified the new SAS verification status
     */
    public void setSASVerification(boolean verified)
    {
        ZRTPTransformEngine engine = getTransformEngine();

        if (verified)
            engine.SASVerified();
        else
            engine.resetSASVerified();
    }

    /**
     * Returns the zrtp engine currently used by this stream.
     * @return the zrtp engine
     */
    public ZRTPTransformEngine getTransformEngine()
    {
        if(zrtpEngine == null)
        {
            zrtpEngine = new ZRTPTransformEngine();
            zrtpEngine.initialize(
                    "GNUZRTP4J.zid",
                    false,
                    ZrtpConfigureUtils.getZrtpConfiguration());
            zrtpEngine.setUserCallback(new SecurityEventManager(this));
        }
        return zrtpEngine;
    }

    /**
     * When in multistream mode, enables the master session.
     * @param masterSession whether current control, controls the master session.
     */
    public void setMasterSession(boolean masterSession)
    {
        // by default its not master, change only if set to be master
        // sometimes (jingle) streams are re-initing and
        // we must reuse old value (true) event that false is submitted
        if(masterSession)
            this.masterSession = masterSession;
    }

    /**
     * Starts and enables zrtp in the stream holding this control.
     * @param mediaType the media type of the stream this control controls.
     */
    public void start(MediaType mediaType)
    {
        boolean zrtpAutoStart;

        // ZRTP engine initialization
        ZRTPTransformEngine engine = getTransformEngine();
        // Create security user callback for each peer.
        SecurityEventManager securityEventManager = engine.getUserCallback();

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
                mediaType.equals(MediaType.AUDIO) ?
                    SecurityEventManager.AUDIO_SESSION
                    : SecurityEventManager.VIDEO_SESSION
            );
        }
        else
        {
            // check whether video was not already started
            // it may happen when using multistreams, audio has inited
            // and started video
            // initially engine has value enableZrtp = false
            zrtpAutoStart = zrtpEngine.isEnableZrtp();
            securityEventManager.setSessionType(
                mediaType.equals(MediaType.AUDIO) ?
                    SecurityEventManager.AUDIO_SESSION
                    : SecurityEventManager.VIDEO_SESSION);
        }

        // tells the engine whether to autostart(enable)
        // zrtp communication, if false it just passes packets without
        // transformation
        engine.setEnableZrtp(zrtpAutoStart);

        engine.setConnector(zrtpConnector);

        securityEventManager.setSrtpListener(zrtpListener);

        engine.sendInfo(
            ZrtpCodes.MessageSeverity.Info,
            EnumSet.of(
                    ZRTPCustomInfoCodes.ZRTPEnabledByDefault));
    }

    /**
     * Start multi-stream ZRTP sessions.
     *
     * After the ZRTP Master (DH) session reached secure state the SCCallback
     * calls this method to start the multi-stream ZRTP sessions.
     *
     * enable auto-start mode (auto-sensing) to the engine.
     * @param master master SRTP data
     */
    public void setMultistream(SrtpControl master)
    {
        if(master == null || master == this)
            return;

        if(!(master instanceof ZrtpControlImpl))
            throw new IllegalArgumentException("master is no ZRTP control");

        ZRTPTransformEngine engine = getTransformEngine();

        engine.setMultiStrParams(((ZrtpControlImpl) master)
            .getTransformEngine().getMultiStrParams());
        engine.setEnableZrtp(true);
    }

    /**
     * Return the zrtp hello hash String.
     *
     * @return String the zrtp hello hash.
     */
    public String getHelloHash()
    {
        return getTransformEngine().getHelloHash();
    }

    /**
     * Get the ZRTP Hello Hash data - separate strings.
     *
     * @return String array containing the version string at offset 0, the Hello
     *         hash value as hex-digits at offset 1. Hello hash is available
     *         immediately after class instantiation. Returns <code>null</code>
     *         if ZRTP is not available.
     */
    public String[] getHelloHashSep()
    {
        return getTransformEngine().getHelloHashSep();
    }

    /**
     * Sets the <tt>RTPConnector</tt> which is to use or uses this ZRTP engine.
     *
     * @param connector the <tt>RTPConnector</tt> which is to use or uses this
     * ZRTP engine
     */
    public void setConnector(AbstractRTPConnector connector)
    {
        zrtpConnector = connector;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.neomedia.ZrtpControl#getSecurityString
     * ()
     */
    public String getSecurityString()
    {
        return getTransformEngine().getUserCallback().getSecurityString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.neomedia.ZrtpControl#isSecurityVerified
     * ()
     */
    public boolean isSecurityVerified()
    {
        return getTransformEngine().getUserCallback().isSecurityVerified();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.neomedia.ZrtpControl#getPeerZid
     * ()
     */
    public byte[] getPeerZid()
    {
        return getTransformEngine().getPeerZid();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.neomedia.ZrtpControl#getPeerZidString
     * ()
     */
    public String getPeerZidString()
    {
        byte[] zid = getPeerZid();
        String s = new String(ZrtpUtils.bytesToHexString(zid, zid.length));
        return s;
    }

    /**
     * Returns false, ZRTP exchanges is keys over the media path.
     *
     * @return false
     */
    public boolean requiresSecureSignalingTransport()
    {
        return false;
    }

    /**
     * Returns the timeout value that will we will wait
     * and fire timeout secure event if call is not secured.
     * The value is in milliseconds.
     * @return the timeout value that will we will wait
     *  and fire timeout secure event if call is not secured.
     */
    public long getTimeoutValue()
    {
        // this is the default value as mentioned in rfc6189
        // we will later grab this setting from zrtp
        return 3750;
    }
}
