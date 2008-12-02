/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import java.util.*;

import net.java.sip.communicator.impl.media.CallSessionImpl;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import net.java.sip.communicator.service.protocol.event.*;

import gnu.java.zrtp.*;

/**
 * The user callback class used by the ZRTP4J.
 * This class provides means to communicate events to the user through
 * GUI elements, and also allows the user to control the ZRTP activation
 * and deactivation.
 *
 * @author Emanuel Onica
 *
 */
public class SCCallback
    extends ZrtpUserCallback
{
    private static final Logger logger
    = Logger.getLogger(SCCallback.class);

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
    /**
     * The class constructor.
     */
    public SCCallback(CallSession callSession)
    {

        this.callSession = callSession;
        
        guiListener = callSession.getCall().getSecurityGUIListener("zrtp");
        Iterator<CallParticipant> participants = callSession.getCall().getCallParticipants();
        if (participants.hasNext())
            participant = participants.next();

    }

    public void init() {
        SecurityGUIEvent evt = new SecurityGUIEvent(participant,
                SecurityGUIEvent.NONE,
                SecurityGUIEvent.SECURITY_ENABLED);
        
        if (logger.isInfoEnabled())
            logger.info(sessionType +": initialize SCCallback");

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
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOn(java.lang.String)
     */
    public void secureOn(String cipher)
    {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": cipher enabled: " + cipher);
        
        HashMap<String, Object> state = new HashMap<String, Object>(3);
        
        state.put(SecurityGUIEventZrtp.SESSION_TYPE, sessionType);
        state.put(SecurityGUIEventZrtp.SECURITY_CHANGE, Boolean.TRUE);
        state.put(SecurityGUIEventZrtp.CIPHER, cipher);

        SecurityGUIEventZrtp evt = new SecurityGUIEventZrtp(participant, state);
        fireStateChangedEvent(evt);     
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#showSAS(java.lang.String, boolean)
     */
    public void showSAS(String sas, boolean verified)
    {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": SAS is: " + sas);
        
        HashMap<String, Object> state = new HashMap<String, Object>(3);
        
        state.put(SecurityGUIEventZrtp.SESSION_TYPE, SecurityGUIEventZrtp.AUDIO);
        state.put(SecurityGUIEventZrtp.SAS, sas);
        if (verified) {
            state.put(SecurityGUIEventZrtp.SAS_VERIFY, Boolean.TRUE);
        }
        else {
            state.put(SecurityGUIEventZrtp.SAS_VERIFY, Boolean.FALSE);
        }

        SecurityGUIEventZrtp evt = new SecurityGUIEventZrtp(participant, state);
        fireStateChangedEvent(evt);     
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#showMessage(
     *                              gnu.java.zrtp.ZrtpCodes.MessageSeverity,
     *                              java.util.EnumSet)
     */
    public void showMessage(ZrtpCodes.MessageSeverity sev, EnumSet<?> subCode) {

        ZrtpCodes.InfoCodes inf;
        int multiStreams = 0;

        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        if (sev == ZrtpCodes.MessageSeverity.Info) {
            if (msgCode instanceof ZrtpCodes.InfoCodes) {
                inf = (ZrtpCodes.InfoCodes) msgCode;

                // If the ZRTP Master session (DH mode) signals "security on"
                // then start multi-stream sessions.
                if (dhSession && inf == ZrtpCodes.InfoCodes.InfoSecureStateOn) {
                    multiStreams = ((CallSessionImpl) callSession)
                            .startZrtpMultiStreams();
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(sessionType + ": " + "ZRTP message: severity: " + sev
                    + ", sub code: " + msgCode + ", DH session: " + dhSession
                    + ", multi: " + multiStreams);
        }

    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNegotiationFailed(
     *          gnu.java.zrtp.ZrtpCodes.MessageSeverity,
     *          java.util.EnumSet)
     */
    public void zrtpNegotiationFailed(ZrtpCodes.MessageSeverity severity,
                EnumSet<?> subCode)
    {
        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();

        if (logger.isInfoEnabled())
            logger.warn(sessionType + ": ZRTP key negotiation failed, sub code: " + msgCode);
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOff()
     */
    public void secureOff()
    {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": Security off");

        HashMap<String, Object> state = new HashMap<String, Object>(2);
        
        state.put(SecurityGUIEventZrtp.SESSION_TYPE, SecurityGUIEventZrtp.AUDIO);
        state.put(SecurityGUIEventZrtp.SECURITY_CHANGE, Boolean.FALSE);

        SecurityGUIEventZrtp evt = new SecurityGUIEventZrtp(participant, state);
        fireStateChangedEvent(evt);
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNotSuppOther()
     */
    public void zrtpNotSuppOther()
    {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": Other party does not support ZRTP key negotiation protocol, no secure calls possible");
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#confirmGoClear()
     */
    public void confirmGoClear()
    {
        if (logger.isInfoEnabled())
            logger.info(sessionType + ": GoClear confirmation requested");
    }
}
