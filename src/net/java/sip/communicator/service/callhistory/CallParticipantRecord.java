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
    private String participantAddress = null;
    private Date startTime = null;
    private Date endTime = null;

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

    /**
     * Sets the time the participant joined the call
     * @param startTime Date
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Sets the particiapnts address
     * @param participantAddress String
     */
    public void setParticipantAddress(String participantAddress)
    {
        this.participantAddress = participantAddress;
    }

    /**
     * Sets the time participant leaves the call
     * @param endTime Date
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }
}
