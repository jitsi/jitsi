/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.callhistory.*;

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
    
    public GuiCallParticipantRecord(CallParticipantRecord participantRecord,
            String direction)
    {   
        this.direction = direction;
        
        this.participantName = participantRecord.getParticipantAddress();
        
        this.startTime = participantRecord.getStartTime();
        
        this.callTime = GuiUtils.substractDates(
                participantRecord.getEndTime(), startTime);
    }
    
    public String getDirection()
    {
        return direction;
    }
    
    public Date getCallTime()
    {
        return callTime;
    }

    public String getParticipantName()
    {
        return participantName;
    }

    public Date getStartTime()
    {
        return startTime;
    }
}
