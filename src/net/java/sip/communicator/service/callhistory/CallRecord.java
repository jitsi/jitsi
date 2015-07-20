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
package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also these records are used for returning data
 * from the Call History Service.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CallRecord
{
    /**
     * The outgoing call direction.
     */
    public final static String OUT = "out";

    /**
     * The incoming call direction.
     */
    public final static String IN = "in";

    /**
     * Indicates the direction of the call - IN or OUT.
     */
    protected String direction = null;

    /**
     * A list of all peer records corresponding to this call record.
     */
    protected final List<CallPeerRecord> peerRecords =
        new Vector<CallPeerRecord>();

    /**
     * The start call date.
     */
    protected Date startTime = null;

    /**
     * The end call date.
     */
    protected Date endTime = null;

    /**
     * The protocol provider through which the call was made.
     */
    protected ProtocolProviderService protocolProvider;

    /**
     * This is the end reason of the call if any. -1 default value for
     * no reason specified.
     */
    protected int endReason = -1;

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
     * Finds a CallPeer with the supplied address
     *
     * @param address String
     * @return CallPeerRecord
     */
    public CallPeerRecord findPeerRecord(String address)
    {
        for (CallPeerRecord item : peerRecords)
        {
            if (item.getPeerAddress().equals(address))
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
     * Return Vector of CallPeerRecords
     * @return Vector
     */
    public List<CallPeerRecord> getPeerRecords()
    {
        return peerRecords;
    }

    /**
     * The time when the call has began
     * @return Date
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * Returns the protocol provider used for the call. Could be null if the
     * record has not saved the provider.
     * @return the protocol provider used for the call
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * This is the end reason of the call if any. -1 the default value
     * for no reason specified.
     * @return end reason code if any.
     */
    public int getEndReason()
    {
        return endReason;
    }
}
