package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also These records are uesd for returning data
 * from the Call History Service
 *
 * @author Damian Minkov
 */
public class CallRecord
{
    /**
     * Possible directions of the call
     */
    public final static String OUT = "out";
    public final static String IN = "in";

    private Call sourceCall = null;
    private String direction = null;
    private Vector participantRecords = new Vector();
    private Date startTime = null;
    private Date endTime = null;

    /**
     * Creates CallRecord
     */
    public CallRecord()
    {
    }

    /**
     * Creates Call Record
     * @param sourceCall Call
     * @param direction String
     * @param startTime Date
     * @param endTime Date
     */
    public CallRecord(
        Call sourceCall,
        String direction,
        Date startTime,
        Date endTime)
    {
        this.sourceCall = sourceCall;
        this.direction = direction;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Finds a Participant with the supplied address
     * @param address String
     * @return CallParticipantRecord
     */
    public CallParticipantRecord findParticipantRecord(String address)
    {
        Iterator iter = participantRecords.iterator();
        while (iter.hasNext())
        {
            CallParticipantRecord item = (CallParticipantRecord) iter.next();
            if (item.getParticipantAddress().equals(address))
                return item;
        }

        return null;
    }

    /**
     * Set the time when the call finishes
     * If some participant has no end Time set we set it also
     * @param endTime Date
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;

        Iterator iter = participantRecords.iterator();
        while (iter.hasNext())
        {
            CallParticipantRecord item = (CallParticipantRecord) iter.next();
            if(item.getEndTime() == null)
                item.setEndTime(endTime);
        }
    }

    /**
     * Sets the time when the call begins
     * @param startTime Date
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * The source call which this record servers
     * @param sourceCall Call
     */
    public void setSourceCall(Call sourceCall)
    {
        this.sourceCall = sourceCall;
    }

    /**
     * Sets the direction of the call
     * IN or OUT
     * @param direction String
     */
    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    /**
     * Returns the direction of the call
     * IN or OUT
     * @return String
     */
    public String getDirection()
    {
        return direction;
    }

    /**
     * Returns the time when the call has finished
     * @return Date
     */
    public Date getEndTime()
    {
        return endTime;
    }

    /**
     * Return Vector of CallParticipantRecords
     * @return Vector
     */
    public Vector getParticipantRecords()
    {
        return participantRecords;
    }

    /**
     * The Call source of this record
     * @return Call
     */
    public Call getSourceCall()
    {
        return sourceCall;
    }

    /**
     * The time when the call has began
     * @return Date
     */
    public Date getStartTime()
    {
        return startTime;
    }
}
