/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>GuiCallParticipantRecord</tt> is meant to be used in the call history
 * to represent a history call participant record. It wraps a
 * <tt>CallParticipant</tt> or a <tt>CallParticipantRecord</tt> object.
 * 
 * @author Yana Stamcheva
 */
public class GuiCallParticipantRecord
{
    public static final String INCOMING_CALL = "IncomingCall";
    
    public static final String OUTGOING_CALL = "OutgoingCall";
    
    private String direction; 
    
    private String participantName;
    
    private Date startTime;
    
    private Date callTime;    
    
    /**
     * Creates an instance of <tt>GuiCallParticipantRecord</tt> by specifying
     * the participant name, the call direction (incoming or outgoing), the
     * time at which the call has started and the duration of the call.
     * 
     * @param participantName the name of the call participant
     * @param direction the direction of the call - INCOMING_CALL
     * or OUTGOING_CALL
     * @param startTime the time at which the call has started
     * @param callTime the duration of the call
     */
    public GuiCallParticipantRecord(String participantName,
            String direction,
            Date startTime,
            Date callTime)
    {
        this.direction = direction;
        
        this.participantName = participantName;
        
        this.startTime = startTime;
        
        this.callTime = callTime;
    }
    
    /**
     * Creates an instance of <tt>GuiCallParticipantRecord</tt> by specifying
     * the corresponding <tt>CallParticipantRecord</tt>, which gives all the
     * information for the participant and the call duration.
     * 
     * @param participantRecord the corresponding <tt>CallParticipantRecord</tt>
     * @param direction the call direction - INCOMING_CALL or OUTGOING_CALL
     */
    public GuiCallParticipantRecord(CallParticipantRecord participantRecord,
            String direction)
    {   
        this.direction = direction;
        
        this.participantName = participantRecord.getParticipantAddress();
        
        this.startTime = participantRecord.getStartTime();
        
        this.callTime = GuiUtils.substractDates(
                participantRecord.getEndTime(), startTime);
    }
    
    /**
     * Returns the call direction - INCOMING_CALL or OUTGOING_CALL.
     * 
     * @return the call direction - INCOMING_CALL or OUTGOING_CALL.
     */
    public String getDirection()
    {
        return direction;
    }
    
    /**
     * Returns the duration of the call.
     * 
     * @return the duration of the call
     */
    public Date getCallTime()
    {
        return callTime;
    }

    /**
     * Returns the name of the participant.
     * 
     * @return the name of the participant
     */
    public String getParticipantName()
    {
        return participantName;
    }

    /**
     * Returns the time at which the call has started.
     * 
     * @return the time at which the call has started
     */
    public Date getStartTime()
    {
        return startTime;
    }
}
