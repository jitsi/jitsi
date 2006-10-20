package net.java.sip.communicator.service.callhistory;

import java.util.*;

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

    protected String direction = null;
    protected Vector participantRecords = new Vector();
    protected Date startTime = null;
    protected Date endTime = null;

    /**
     * Creates CallRecord
     */
    public CallRecord()
    {
    }

    /**
     * Creates Call Record
     * @param direction String
     * @param startTime Date
     * @param endTime Date
     */
    public CallRecord(
        String direction,
        Date startTime,
        Date endTime)
    {
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
     * The time when the call has began
     * @return Date
     */
    public Date getStartTime()
    {
        return startTime;
    }
}
