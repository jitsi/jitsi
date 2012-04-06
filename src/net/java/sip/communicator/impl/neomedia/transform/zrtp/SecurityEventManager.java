/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.zrtp;

import gnu.java.zrtp.*;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The user callback class for ZRTP4J.
 *
 * This class constructs and sends events to the ZRTP GUI implementation. The
 * <code>showMessage()<code> function implements a specific check to start
 * associated ZRTP multi-stream sessions.
 *
 * Coordinate this callback class with the associated GUI implementation class
 *
 * @see net.java.sip.communicator.impl.gui.main.call.ZrtpSecurityPanel
 *
 * @author Emanuel Onica
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public class SecurityEventManager extends ZrtpUserCallback
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(SecurityEventManager.class);

    /**
     * A warning <tt>String</tt> that we display to the user.
     */
    public static final String WARNING_NO_RS_MATCH =
        getI18NString("impl.media.security.WARNING_NO_RS_MATCH", null);

    /**
     * A warning <tt>String</tt> that we display to the user.
     */
    public static final String WARNING_NO_EXPECTED_RS_MATCH =
        getI18NString("impl.media.security.WARNING_NO_EXPECTED_RS_MATCH", null);

    /**
     * Indicates an audio session type.
     */
    public static final int AUDIO_SESSION =
        CallPeerSecurityStatusEvent.AUDIO_SESSION;

    /**
     * Indicates a video session type.
     */
    public static final int VIDEO_SESSION =
        CallPeerSecurityStatusEvent.VIDEO_SESSION;

    /**
     * The zrtp control that this manager is associated with.
     */
    private final ZrtpControlImpl zrtpControl;

    /**
     * A callback to the instance that created us.
     */
    private SrtpListener securityListener;

    /**
     * Is this a ZRTP DH (Master) session?
     */
    private boolean isDHSession = false;

    /**
     * Type of session
     */
    private int sessionType;

    /**
     * SAS string.
     */
    private String sas;

    /**
     * Cipher.
     */
    private String cipher;

    /**
     * Indicates if the SAS has already been verified in a previous session.
     */
    private boolean isSasVerified;

    /**
     * The class constructor.
     *
     * @param zrtpControl that this manager is to be associated with.
     */
    public SecurityEventManager(ZrtpControlImpl zrtpControl)
    {
        this.zrtpControl = zrtpControl;
        this.securityListener = zrtpControl.getSrtpListener();
    }

    /**
     * Set the type of this session.
     *
     * @param type the session type. The session type could be either
     * AUDIO_SESSION or VIDEO_SESSION.
     */
    public void setSessionType(int type)
    {
        sessionType = type;
    }

    /**
     * Set the DH session flag.
     *
     * @param isDHSession the DH session flag.
     */
    public void setDHSession(boolean isDHSession)
    {
        this.isDHSession = isDHSession;
    }

    /*
     * The following methods implement the ZrtpUserCallback interface
     */

    /**
     * Reports the security algorithm that the ZRTP protocol negotiated.
     *
     * @param cipher the cipher
     */
    @Override
    public void secureOn(String cipher)
    {
        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType)
            + ": cipher enabled: " + cipher);

        this.cipher = cipher;
    }

    /**
     * ZRTP computes the SAS string after nearly all the negotiation
     * and computations are done internally.
     *
     * @param sas     The string containing the SAS.
     * @param isVerified is sas verified.
     */
    @Override
    public void showSAS(String sas, boolean isVerified)
    {
        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType) + ": SAS is: " + sas);

        this.sas = sas;
        this.isSasVerified = isVerified;
    }

    /**
     * Show some information to user.
     * ZRTP calls this method to display some information to the user.
     * Along with the message ZRTP provides a severity indicator that defines:
     *  Info, Warning, Error, and Alert.
     * @param sev severity of the message.
     * @param subCode the message code.
     */
    @Override
    public void showMessage(ZrtpCodes.MessageSeverity sev,
                            EnumSet<?> subCode)
    {
        int multiStreams = 0;

        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        String message = null;
        String i18nMessage = null;
        int severity = 0;

        boolean sendEvent = true;

        if (msgCode instanceof ZrtpCodes.InfoCodes)
        {
            ZrtpCodes.InfoCodes inf = (ZrtpCodes.InfoCodes) msgCode;

            // Use the following fields if INFORMATION type messages shall be
            // shown to the user via SecurityMessageEvent, i.e. if
            // sendEvent is set to true
            // severity = CallPeerSecurityMessageEvent.INFORMATION;

            // Don't spam user with info messages, only internal processing
            // or logging.
            sendEvent = false;

            // If the ZRTP Master session (DH mode) signals "security on"
            // then start multi-stream sessions.
            // Signal SAS to GUI only if this is a DH mode session.
            // Multi-stream session don't have own SAS data
            if (inf == ZrtpCodes.InfoCodes.InfoSecureStateOn)
            {
                if (isDHSession)
                {
                    securityListener.securityTurnedOn(
                        sessionType, cipher,
                        zrtpControl);
                }
                else
                {
                    securityListener.securityTurnedOn(sessionType, cipher,
                                                zrtpControl);
                }
            }
        }
        else if (msgCode instanceof ZrtpCodes.WarningCodes)
        {
            // Warning codes usually do not affect encryption or security. Only
            // in few cases inform the user and ask to verify SAS.
            ZrtpCodes.WarningCodes warn = (ZrtpCodes.WarningCodes) msgCode;
            severity = CallPeerSecurityMessageEvent.WARNING;

            if (warn == ZrtpCodes.WarningCodes.WarningNoRSMatch)
            {
                message = "No retained shared secret available.";
                i18nMessage = WARNING_NO_RS_MATCH;
            }
            else if (warn == ZrtpCodes.WarningCodes.WarningNoExpectedRSMatch)
            {
                message = "An expected retained shared secret is missing.";
                i18nMessage = WARNING_NO_EXPECTED_RS_MATCH;
            }
            else if (warn == ZrtpCodes.WarningCodes.WarningCRCmismatch)
            {
                message = "Internal ZRTP packet checksum mismatch.";
                i18nMessage = getI18NString(
                    "impl.media.security.CHECKSUM_MISMATCH", null);
            }
            else
            {
                // Other warnings are  internal only, no user action required
                sendEvent = false;
            }
        }
        else if (msgCode instanceof ZrtpCodes.SevereCodes)
        {
            ZrtpCodes.SevereCodes severe = (ZrtpCodes.SevereCodes) msgCode;
            severity = CallPeerSecurityMessageEvent.SEVERE;

            if (severe == ZrtpCodes.SevereCodes.SevereCannotSend)
            {
                message = "Failed to send data."
                    + "Internet data connection or peer is down.";
                i18nMessage =getI18NString(
                    "impl.media.security.DATA_SEND_FAILED", msgCode.toString());
            }
            else if (severe == ZrtpCodes.SevereCodes.SevereTooMuchRetries)
            {
                message = "Too much retries during ZRTP negotiation.";
                i18nMessage = getI18NString(
                    "impl.media.security.RETRY_RATE_EXCEEDED",
                    msgCode.toString());
            }
            else if (severe == ZrtpCodes.SevereCodes.SevereProtocolError)
            {
                message = "Internal protocol error occured.";
                i18nMessage = getI18NString(
                    "impl.media.security.INTERNAL_PROTOCOL_ERROR",
                    msgCode.toString());
            }
            else
            {
                message = "General error has occurred.";
                i18nMessage =  getI18NString(
                    "impl.media.security.ZRTP_GENERIC_MSG",
                    msgCode.toString());
            }
        }
        else if (msgCode instanceof ZrtpCodes.ZrtpErrorCodes)
        {
            severity = CallPeerSecurityMessageEvent.ERROR;

            message =   "Indicates compatibility problems like for example:"
                        + "unsupported protocol version, unsupported hash type,"
                        + "cypher type, SAS scheme, etc.";
            i18nMessage =  getI18NString(
                "impl.media.security.ZRTP_GENERIC_MSG",
                msgCode.toString());
        }
        else
        {
            // Other warnings are  internal only, no user action required
            sendEvent = false;
        }

        if (sendEvent)
            securityListener
                .securityMessageReceived(message, i18nMessage, severity);

        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType) + ": "
            + "ZRTP message: severity: " + sev + ", sub code: " + msgCode
            + ", DH session: " + isDHSession + ", multi: " + multiStreams);
    }

    /**
     * Negotiation has failed.
     *
     * @param severity of the message.
     * @param subCode the message code.
     */
    @Override
    public void zrtpNegotiationFailed(  ZrtpCodes.MessageSeverity severity,
                                        EnumSet<?> subCode)
    {
        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType)
                + ": ZRTP key negotiation failed, sub code: " + msgCode);
    }

    /**
     * Inform user interface that security is not active any more.
     */
    @Override
    public void secureOff()
    {
        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType) + ": Security off");

        securityListener.securityTurnedOff(sessionType);
    }

    /**
     * The other part does not support zrtp.
     */
    @Override
    public void zrtpNotSuppOther()
    {
        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType)
            + ": Other party does not support ZRTP key negotiation protocol,"
            + " no secure calls possible.");

        securityListener.securityTimeout(sessionType);
    }

    /**
     * Inform the user that ZRTP received "go clear" message from its peer.
     */
    @Override
    public void confirmGoClear()
    {
        if (logger.isDebugEnabled())
            logger.debug(sessionTypeToString(sessionType)
            + ": GoClear confirmation requested.");
    }

    /**
     * Converts the <tt>sessionType</tt> into into a <tt>String</tt>.
     *
     * @param sessionType one of the
     * <tt>CallPeerSecurityStatusEvent.XXX_SESSION</tt> fields
     *
     * @return a <tt>String</tt> representation of <tt>sessionType</tt>.
     */
    private String sessionTypeToString(int sessionType)
    {
        switch (sessionType)
        {
            case AUDIO_SESSION:
                return "AUDIO_SESSION";
            case VIDEO_SESSION:
                return "VIDEO_SESSION";
            default:
                throw new IllegalArgumentException("sessionType");
        }
    }

    /**
     * Gets the localized message and replace the param. If the param is null
     * we ignore it.
     *
     * @param key the key for the localized message.
     * @param param the param to replace.
     * @return the i18n message.
     */
    private static String getI18NString(String key, String param)
    {
        String[] params = null;

        if(param != null)
            params = new String[]{param};

        return NeomediaActivator.getResources().getI18NString(key, params);
    }

    /**
     * Sets a new receiver of the security callback events.
     * @param securityListener An object that receives the security events.
     */
    public void setSrtpListener(SrtpListener securityListener)
    {
        this.securityListener = securityListener;
    }

    /**
     * Gets the SAS for the current media stream.
     * 
     * @return the four character ZRTP SAS.
     */
    public String getSecurityString()
    {
        return sas;
    }

    /**
     * Gets the status of the SAS verification.
     * 
     * @return true when the SAS has been verified.
     */
    public boolean isSecurityVerified()
    {
        return isSasVerified;
    }

    /**
     * Indicates that we started the process of securing the the connection.
     */
    public void securityNegotiationStarted()
    {
        // make sure we don't throw any exception
        try
        {
            this.securityListener.securityNegotiationStarted(sessionType);
        }
        catch(Throwable t)
        {
            logger.error("Error processing security started.");
        }
    }
}
