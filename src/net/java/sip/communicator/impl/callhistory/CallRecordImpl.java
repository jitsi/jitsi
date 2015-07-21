/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    /**
     * The <tt>Call</tt> source of this record.
     */
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

    /**
     * Sets the given <tt>ProtocolProviderService</tt> used for the call.
     * @param pps the <tt>ProtocolProviderService</tt> to set
     */
    public void setProtocolProvider(ProtocolProviderService pps)
    {
        this.protocolProvider = pps;
    }

    /**
     * This is the end reason of the call if any.
     * @param endReason the reason code.
     */
    public void setEndReason(int endReason)
    {
        this.endReason = endReason;
    }
}
