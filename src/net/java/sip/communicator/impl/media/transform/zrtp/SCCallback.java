/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import java.util.*;

import net.java.sip.communicator.impl.media.CallSessionImpl;
import net.java.sip.communicator.impl.media.MediaActivator;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import net.java.sip.communicator.service.protocol.event.*;

import gnu.java.zrtp.*;

/**
 * The user callback class for ZRTP4J.
 * 
 * This class constructs and sends events to the ZRTP GUI implementation. The
 * <code>showMessage()<code> function implements a specific check to start 
 * associated ZRTP multi-stream sessions.
 * 
 * Coordinate this callback class with the associated GUI implementation class
 * net.java.sip.communicator.impl.gui.main.call.ZrtpPanel
 *
 * @see net.java.sip.communicator.impl.gui.main.call.ZrtpPanel
 *
 * @author Emanuel Onica
 * @author Werner Dittmann
 *
 */
public class SCCallback extends ZrtpUserCallback {
    private static final Logger logger = Logger.getLogger(SCCallback.class);

    public static final String WARNING_NO_RS_MATCH = MediaActivator
            .getResources().getI18NString(
                    "impl.media.security.WARNING_NO_RS_MATCH");

    public static final String WARNING_NO_EXPECTED_RS_MATCH = MediaActivator
            .getResources().getI18NString(
                    "impl.media.security.WARNING_NO_EXPECTED_RS_MATCH");

    private CallSession callSession = null;

    private SecurityGUIListener guiListener = null;

    private CallParticipant participant;

    /*
     * Is this a ZRTP DH (Master) session?
     */
    private boolean dhSession = false;

    /*
     * Type of session. See class SecurityGUIEventZrtp which types are
     * supported.
     */
    private String sessionType = null;

    /*
     * Remember the cipher name
     */
    private String cipherName = null;

    /*
     * Remember the SAS
     */
    private String sasData = null;

    /*
     * Remember SAS verification state
     */
    private boolean sasVerification = false;

    /**
     * The class constructor.
     */
    public SCCallback(CallSession callSession) {

        this.callSession = callSession;

        guiListener = callSession.getCall().getSecurityGUIListener("zrtp");
        Iterator<CallParticipant> participants = callSession.getCall()
                .getCallParticipants();
        if (participants.hasNext())
            participant = participants.next();

    }

    public void init() {
        SecurityGUIEvent evt = new SecurityGUIEvent(participant,
                SecurityGUIEvent.NONE, SecurityGUIEvent.SECURITY_ENABLED);

        if (logger.isInfoEnabled())
            logger.info(sessionType + ": initialize SCCallback");

        fireStateChangedEvent(evt);
    }

    private void fireStateChangedEvent(SecurityGUIEvent evt) {
        if (guiListener != null) {
            guiListener.securityStatusChanged(evt);
        } else {
            guiListener = callSession.getCall().getSecurityGUIListener("zrtp");
            if (guiListener != null) {
                guiListener.securityStatusChanged(evt);
            }
        }

    }

    /**
     * Set the type of this session.
     * 
     * @param type
     */
    public void setType(String type) {
        sessionType = type;
    }

    /**
     * Set the DH session flag.
     * 
     * @param yesNo
     */
    public void setDHSession(boolean yesNo) {
        dhSession = yesNo;
    }

    /*
     * The following methods implement the ZrtpUserCallback interface
     */

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOn(java.lang.String)
     */
    public void secureOn(String cipher) {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": cipher enabled: " + cipher);

        cipherName = cipher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#showSAS(java.lang.String, boolean)
     */
    public void showSAS(String sas, boolean verified) {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": SAS is: " + sas);

        sasData = sas;
        sasVerification = verified;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#showMessage(
     * gnu.java.zrtp.ZrtpCodes.MessageSeverity, java.util.EnumSet)
     */
    public void showMessage(ZrtpCodes.MessageSeverity sev, EnumSet<?> subCode) {

        int multiStreams = 0;
        HashMap<String, Object> state = new HashMap<String, Object>(5);
        boolean fireEvent = false;

        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        if (sev == ZrtpCodes.MessageSeverity.Info) {
            if (msgCode instanceof ZrtpCodes.InfoCodes) {
                ZrtpCodes.InfoCodes inf = (ZrtpCodes.InfoCodes) msgCode;

                // If the ZRTP Master session (DH mode) signals "security on"
                // then start multi-stream sessions.
                // Signal SAS to GUI only if this is a DH mode session.
                // Multi-stream session don't have own SAS data
                if (inf == ZrtpCodes.InfoCodes.InfoSecureStateOn) {
                    if (dhSession) {
                        multiStreams = ((CallSessionImpl) callSession)
                                .startZrtpMultiStreams();
                        state.put(SecurityGUIEventZrtp.SAS, sasData);
                        if (sasVerification) {
                            state.put(SecurityGUIEventZrtp.SAS_VERIFY,
                                    Boolean.TRUE);
                        } else {
                            state.put(SecurityGUIEventZrtp.SAS_VERIFY,
                                    Boolean.FALSE);
                        }
                    }
                    state.put(SecurityGUIEventZrtp.SESSION_TYPE, sessionType);
                    state.put(SecurityGUIEventZrtp.SECURITY_CHANGE,
                            Boolean.TRUE);
                    state.put(SecurityGUIEventZrtp.CIPHER, cipherName);
                    fireEvent = true;
                }

                if (inf == ZrtpCodes.InfoCodes.InfoHelloReceived) {
                    state.put(SecurityGUIEventZrtp.SESSION_TYPE,
                            SecurityGUIEventZrtp.MSG_INFO);
                    fireEvent = true;
                }
            }
        }

        /*
         * Warning codes usually do not affect encryption or security. Onl
         * in few cases inform the user and ask to verify SAS
         */
        if (sev == ZrtpCodes.MessageSeverity.Warning) {
            if (msgCode instanceof ZrtpCodes.WarningCodes) {
                ZrtpCodes.WarningCodes warn = (ZrtpCodes.WarningCodes) msgCode;

                if (warn == ZrtpCodes.WarningCodes.WarningNoRSMatch) {
                    state.put(SecurityGUIEventZrtp.SESSION_TYPE,
                            SecurityGUIEventZrtp.MSG_WARN);
                    state.put(SecurityGUIEventZrtp.MSG_TEXT,
                            WARNING_NO_RS_MATCH);
                    fireEvent = true;
                }
                if (warn == ZrtpCodes.WarningCodes.WarningNoExpectedRSMatch) {
                    state.put(SecurityGUIEventZrtp.SESSION_TYPE,
                            SecurityGUIEventZrtp.MSG_WARN);
                    state.put(SecurityGUIEventZrtp.MSG_TEXT,
                            WARNING_NO_EXPECTED_RS_MATCH);
                    fireEvent = true;
                }
            }
        }

        /*
         * Severe or ZRTP error codes always signal that security cannot be established.
         * Inform the user about this.
         */
        if (sev == ZrtpCodes.MessageSeverity.Severe) {
            String[] param = new String[1];
            param[0] = msgCode.toString();
            String msg = MediaActivator
            .getResources().getI18NString(
                    "impl.media.security.SEVERE_GENERIC_MSG", param);
            
            state.put(SecurityGUIEventZrtp.SESSION_TYPE,
                    SecurityGUIEventZrtp.MSG_SEVERE);
            state.put(SecurityGUIEventZrtp.MSG_TEXT, msg);
            fireEvent = true;
        }
        
        
        if (sev == ZrtpCodes.MessageSeverity.ZrtpError) {
            String[] param = new String[1];
            param[0] = msgCode.toString();
            String msg = MediaActivator
            .getResources().getI18NString(
                    "impl.media.security.ZRTP_GENERIC_MSG", param);

            state.put(SecurityGUIEventZrtp.SESSION_TYPE,
                    SecurityGUIEventZrtp.MSG_ZRTP);
            state.put(SecurityGUIEventZrtp.MSG_TEXT, msg);
            fireEvent = true;
        }

        if (fireEvent) {
            SecurityGUIEventZrtp evt = new SecurityGUIEventZrtp(participant,
                    state);
            fireStateChangedEvent(evt);
        }

        if (logger.isInfoEnabled()) {
            logger.info(sessionType + ": " + "ZRTP message: severity: " + sev
                    + ", sub code: " + msgCode + ", DH session: " + dhSession
                    + ", multi: " + multiStreams);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNegotiationFailed(
     * gnu.java.zrtp.ZrtpCodes.MessageSeverity, java.util.EnumSet)
     */
    public void zrtpNegotiationFailed(ZrtpCodes.MessageSeverity severity,
            EnumSet<?> subCode) {
        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        if (logger.isInfoEnabled())
            logger.warn(sessionType
                    + ": ZRTP key negotiation failed, sub code: " + msgCode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOff()
     */
    public void secureOff() {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": Security off");

        HashMap<String, Object> state = new HashMap<String, Object>(2);

        state
                .put(SecurityGUIEventZrtp.SESSION_TYPE,
                        SecurityGUIEventZrtp.AUDIO);
        state.put(SecurityGUIEventZrtp.SECURITY_CHANGE, Boolean.FALSE);

        SecurityGUIEventZrtp evt = new SecurityGUIEventZrtp(participant, state);
        fireStateChangedEvent(evt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNotSuppOther()
     */
    public void zrtpNotSuppOther() {
        if (logger.isInfoEnabled())
            logger
                    .info(sessionType
                            + ": Other party does not support ZRTP key negotiation protocol, no secure calls possible");
    }

    /*
     * (non-Javadoc)
     * 
     * @see gnu.java.zrtp.ZrtpUserCallback#confirmGoClear()
     */
    public void confirmGoClear() {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": GoClear confirmation requested");
    }
}
