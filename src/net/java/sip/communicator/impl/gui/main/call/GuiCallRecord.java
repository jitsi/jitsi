/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>GuiCallRecord</tt> is meant to be used in the call history to
 * represent a history call record. It wraps a <tt>Call</tt> or a
 * <tt>CallRecord</tt> object.
 * 
 * @author Yana Stamcheva
 */
public class GuiCallRecord
{
    private Vector participants; 
    
    private Date startTime;
    
    private Date endTime;
    
    /**
     * 
     * @param guiParticipantRecords
     * @param direction
     * @param startTime
     * @param endTime
     */
    public GuiCallRecord(Vector guiParticipantRecords,
            Date startTime,
            Date endTime)
    {   
        this.startTime = startTime;
        
        this.endTime = endTime;
        
        participants = guiParticipantRecords;
    }
    
    /**
     * Creates a <tt>GuiCallRecord</tt> from a <tt>CallRecord</tt>. The
     * <tt>GuiCallRecord</tt> will be used in the call history.
     * 
     * @param callRecord the <tt>CallParticipantRecord</tt>
     */
    public GuiCallRecord(CallRecord callRecord)
    {   
        this.startTime = callRecord.getStartTime();
        
        this.endTime = callRecord.getEndTime();
        
        this.participants = new Vector();
        
        Iterator records = callRecord.getParticipantRecords().iterator();
        
        while(records.hasNext()) {
            CallParticipantRecord record
                = (CallParticipantRecord)records.next();
            
            GuiCallParticipantRecord newRecord
                = new GuiCallParticipantRecord(
                        record, callRecord.getDirection());
            
            this.participants.add(newRecord);
        }
    }
    
    public Date getEndTime()
    {
        return endTime;
    }

    public Iterator getParticipants()
    {
        return participants.iterator();
    }

    public int getParticipantsCount()
    {
        return participants.size();
    }
    
    public Date getStartTime()
    {
        return startTime;
    }
}
