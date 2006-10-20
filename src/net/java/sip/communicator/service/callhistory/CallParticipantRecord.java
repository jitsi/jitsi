package net.java.sip.communicator.service.callhistory;

import java.util.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also These records are uesd for returning data
 * from the Call History Service
 *
 * @author Damian Minkov
 */
public class CallParticipantRecord
{
    protected String participantAddress = null;
    protected Date startTime = null;
    protected Date endTime = null;

    /**
     * Creates CallParticipantRecord
     * @param participantAddress String
     * @param startTime Date
     * @param endTime Date
     */
    public CallParticipantRecord(
        String participantAddress,
        Date startTime,
        Date endTime)
    {
        this.participantAddress = participantAddress;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * When participant diconnected from the call
     *
     * @return Date
     */
    public Date getEndTime()
    {
        return endTime;
    }

    /**
     * The participant address
     * @return String
     */
    public String getParticipantAddress()
    {
        return participantAddress;
    }

    /**
     * When participant connected to the call
     * @return Date
     */
    public Date getStartTime()
    {
        return startTime;
    }
}
