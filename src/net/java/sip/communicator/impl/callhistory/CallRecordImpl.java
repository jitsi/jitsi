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
 * Add Source call to the CallRecord
 * @author Damian Minkov
 */
public class CallRecordImpl
    extends CallRecord
{
    private Call sourceCall = null;

    /**
     * Creates CallRecord
     */
    public CallRecordImpl()
    {
        super();
    }

    /**
     * Creates Call Record
     * @param direction String
     * @param startTime Date
     * @param endTime Date
     */
    public CallRecordImpl(
        String direction,
        Date startTime,
        Date endTime)
    {
        super(direction, startTime, endTime);
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
     * Set the time when the call finishes
     * If some peer has no end Time set we set it also
     * @param endTime Date
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;

        for (CallPeerRecord item : peerRecords)
        {
            CallPeerRecordImpl itemImpl =
                (CallPeerRecordImpl) item;
            if (item.getEndTime() == null)
                itemImpl.setEndTime(endTime);
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
}
