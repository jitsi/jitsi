/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.EventObject;

import net.java.sip.communicator.service.protocol.CallParticipant;

public class SecurityGUIEvent extends EventObject {

    /**
     * Default
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constant value defining that security status changed.
     */
    public static final int STATUS_CHANGE = 1;
    
    /**
     * Constant value defining that security is enabled.
     */
    public static final int SECURITY_ENABLED = 2;

    /**
     * The actual event value
     */
    private final int eventID;

    /**
     * Constant value defining the key share provider .
     */
    public static final int NONE = 0;

    public static final int ZRTP = 1;

    private final int provider;
    

    /**
     * The event constructor
     * 
     * @param callSession
     *            the event source - the call session for which this event
     *            applies
     * @param eventID
     *            the change value - going secure or stopping secure
     *            communication
     */
    public SecurityGUIEvent(CallParticipant part,
            int prov, int eventID) {
        super(part);
        this.eventID = eventID;
        this.provider = prov;
    }

    /**
     * @return the eventID
     */
    public int getEventID() {
        return eventID;
    }

    /**
     * @return the provider
     */
    public int getProvider() {
        return provider;
    }
}
