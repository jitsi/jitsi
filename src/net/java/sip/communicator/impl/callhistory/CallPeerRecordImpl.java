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
 * Added some setters to CallPeerRecord
 * @author Damian Minkov
 */
public class CallPeerRecordImpl
    extends CallPeerRecord
{
    /**
     * Creates CallPeerRecord
     * @param peerAddress String
     * @param startTime Date
     * @param endTime Date
     */
    public CallPeerRecordImpl(
        String peerAddress,
        Date startTime,
        Date endTime)
    {
        super(peerAddress, startTime, endTime);
    }

    /**
     * Sets the time the peer joined the call
     * @param startTime Date
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Sets the particiapnts address
     * @param peerAddress String
     */
    public void setPeerAddress(String peerAddress)
    {
        this.peerAddress = peerAddress;
    }

    /**
     * Sets the display name of the call peer in this record.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Sets the time peer leaves the call
     * @param endTime Date
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    /**
     * Sets the peer state
     * @param state CallPeerState
     */
    public void setState(CallPeerState state)
    {
        this.state = state;
    }
}
