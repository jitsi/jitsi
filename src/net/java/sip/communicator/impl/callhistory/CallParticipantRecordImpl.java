/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Added some setters to CallParticipantRecord
 * @author Damian Minkov
 */
public class CallParticipantRecordImpl
    extends CallParticipantRecord
{
    /**
     * Creates CallParticipantRecord
     * @param participantAddress String
     * @param startTime Date
     * @param endTime Date
     */
    public CallParticipantRecordImpl(
        String participantAddress,
        Date startTime,
        Date endTime)
    {
        super(participantAddress, startTime, endTime);
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

    /**
     * Sets the participant state
     * @param state CallParticipantState
     */
    public void setState(CallParticipantState state)
    {
        this.state = state;
    }
}
