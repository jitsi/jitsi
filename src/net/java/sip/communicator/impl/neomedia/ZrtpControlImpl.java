/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import gnu.java.zrtp.*;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.keyshare.*;
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
    * Toggles default (from the call start) activation
    * of secure communication
    */
    private boolean usingZRTP = false;

        /**
     * Vector used to hold references of the various key management solutions
     * that are implemented. For now only ZRTP and dummy (hardcoded keys) are
     * present.
     */
    private static Vector<KeyProviderAlgorithm> keySharingAlgorithms = null;

    /**
     * Insert ZRTPKeyProvider at index 0 which is priority 0
     * and the dummy provider with priority 1.
     */
    static
    {
        keySharingAlgorithms = new Vector<KeyProviderAlgorithm>();

        keySharingAlgorithms.add(KeyProviderAlgorithm.ZRTP_PROVIDER);
        keySharingAlgorithms.add(KeyProviderAlgorithm.DUMMY_PROVIDER);
    }

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
     * The stream holding the control.
     */
    private MediaStreamImpl stream = null;

    /**
     * Creates the control.
     * @param stream the stream holding the control.
     */
    ZrtpControlImpl(MediaStreamImpl stream)
    {
        this.stream = stream;
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
        return usingZRTP;
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
     * @return
     */
    public ZRTPTransformEngine getZrtpEngine()
    {
        return stream.getZrtpEngine();
    }

    /**
     * Starts and enables zrtp in the stream holding this control.
     * @param masterSession whether this stream is master for the current
     *        media session.
     */
    public void start(boolean masterSession)
    {
        usingZRTP = true;

        /* Select a key management type from the present ones to use
         * for now using the zero - top priority solution (ZRTP);
         * TODO: should be extended to a selection algorithm to choose the
         * key management type
         */
        KeyProviderAlgorithm selectedKeyProviderAlgorithm =
            keySharingAlgorithms.get(0);

        // Selected key management type == ZRTP branch
        if (selectedKeyProviderAlgorithm != null &&
            selectedKeyProviderAlgorithm
                == KeyProviderAlgorithm.ZRTP_PROVIDER)
        {
            // Create security user callback for each peer.
            SecurityEventManager securityEventManager
                = new SecurityEventManager(stream);

            boolean zrtpAutoStart = false;

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

                // we now that audio is considered as master for zrtp
               securityEventManager.setSessionType(
                   SecurityEventManager.AUDIO_SESSION);
            }
            else
            {
                securityEventManager.setSessionType(
                    SecurityEventManager.VIDEO_SESSION);
            }

            // ZRTP engine initialization
            ZRTPTransformEngine zrtpEngine = stream.getZrtpEngine();
            zrtpEngine.initialize("GNUZRTP4J.zid", zrtpAutoStart);
            
            zrtpEngine.setConnector(stream.getRtpConnector());

            zrtpEngine.setUserCallback(securityEventManager);

            usingZRTP = true;
            zrtpEngine.sendInfo(
                ZrtpCodes.MessageSeverity.Info,
                EnumSet.of(
                        ZRTPCustomInfoCodes.ZRTPEnabledByDefault));
        }
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
        if(usingZRTP)
        {
            ZRTPTransformEngine engine = getZrtpEngine();
            engine.setMultiStrParams(multiStreamData);
            engine.setEnableZrtp(true);
        }
    }
}
