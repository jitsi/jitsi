/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import java.util.*;

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
        
        logger.info("initialize SCCallback");

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
    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOn(java.lang.String)
     */
    public void secureOn(String cipher)
    {
        logger.info("Cipher: " + cipher);
        HashMap<String, Object> state = new HashMap<String, Object>(3);
        
        state.put(SecurityGUIEventZrtp.SESSION_TYPE, SecurityGUIEventZrtp.AUDIO);
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
        logger.info("SAS: " + sas);
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
    public void showMessage(ZrtpCodes.MessageSeverity sev, EnumSet<?> subCode)
    {
        Iterator<?> ii = subCode.iterator();
        Object msgCode = ii.next();
        logger.info("Show message sub code: " + msgCode);
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
        logger.warn("Negotiation failed sub code: " + msgCode);
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOff()
     */
    public void secureOff()
    {
        logger.info("Security off");

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
        logger.info("ZRTP not supported");
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#confirmGoClear()
     */
    public void confirmGoClear()
    {
        logger.info("GoClear confirmation requested");
    }
}
