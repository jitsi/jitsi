/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * SecureEvent class extends EventObject
 * This is the event type sent to current call sessions running
 * when the user changes the secure state of communication in the GUI,
 * to inform about the go secure / go clear change in communication.
 * The event is actual triggered by a modification of the usingSRTP
 * static secure communication status in the CallSessionImpl class.
 *     
 * @author Emanuel Onica (eonica@info.uaic.ro)
 */
public class SecureEvent 
    extends EventObject 
{    
    /**
     * Constant value defining that the user triggered secure communication.
     */
    public static final int SECURE_COMMUNICATION = 1;
    
    /**
     * Constant value defining that the user triggered unsecure communication.
     */
    public static final int UNSECURE_COMMUNICATION = 2;
    
    /**
     * The actual event value - secure or unsecure, set at one of the above constants
     */
    private final int eventID;
    
    /**
     * The source that triggered the event - local or remote peer
     */
    private final OperationSetSecureTelephony.SecureStatusChangeSource source;
    
    /**
     * The event constructor
     * 
     * @param callSession the event source - the call session for which this event applies
     * @param eventID the change value - going secure or stopping secure communication
     */
    public SecureEvent(CallSession callSession, 
    				   int eventID, 
    				   OperationSetSecureTelephony.SecureStatusChangeSource source)
    {
        super(callSession);
        this.eventID = eventID;
        this.source = source;
    }

    /**
     * Retrieves the value of change - secure or unsecure
     * 
     * @return the actual event value
     */
    public int getEventID() 
    {
        return eventID;
    }
    
    /**
     * Retrieves the source that triggered the event
     * (change by local peer or remote peer or reverting a previous change)
     */
    public OperationSetSecureTelephony.SecureStatusChangeSource getSource()
    {
    	return source;
    }
}
