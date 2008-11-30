/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.HashMap;

import net.java.sip.communicator.service.protocol.CallParticipant;


public class SecurityGUIEventZrtp extends SecurityGUIEvent {
    
    /**
     * Default
     */
    private static final long serialVersionUID = 1L;
    /**
     * ZRTP security state change actions
     */
    
    public final static String CIPHER = "cipherName";
    public final static String SECURITY_CHANGE = "secure";
    public final static String SAS = "sas";
    public final static String SAS_VERIFY = "sasVerify";
    public final static String SESSION_TYPE = "type";
    public final static String AUDIO = "Audio";
    public final static String VIDEO = "Video";
    
    private final HashMap<String, Object> states;
    
    public SecurityGUIEventZrtp(CallParticipant part,
            HashMap<String, Object> states) {
        super(part, SecurityGUIEvent.ZRTP, SecurityGUIEvent.STATUS_CHANGE);
        this.states = states;
    }

    /**
     * @return the states
     */
    public HashMap<String, Object> getStates() {
        return states;
    }

}
