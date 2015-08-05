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
 * Call History Data. Also These records are uesd for returning data
 * from the Call History Service
 *
 * @author Damian Minkov
 * @author Hristo Terezov
 */
public class CallPeerRecord
{
    /**
     * The peer address.
     */
    protected String peerAddress = null;

    /**
     * The display name.
     */
    protected String displayName = null;

    /**
     * The start time of the record.
     */
    protected Date startTime = null;

    /**
     * The end time of the record.
     */
    protected Date endTime = null;

    /**
     * The secondary address of the peer.
     */
    protected String secondaryPeerAddress = null;

    /**
     * The state of <tt>CallPeer</tt>.
     */
    protected CallPeerState state = CallPeerState.UNKNOWN;

    /**
     * Creates CallPeerRecord
     * @param peerAddress String
     * @param startTime Date
     * @param endTime Date
     */
    public CallPeerRecord(  String peerAddress,
                            Date startTime,
                            Date endTime)
    {
        this.peerAddress = peerAddress;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * When peer diconnected from the call
     *
     * @return Date
     */
    public Date getEndTime()
    {
        return endTime;
    }

    /**
     * The peer address
     * @return String
     */
    public String getPeerAddress()
    {
        return peerAddress;
    }

    /**
     * Returns the display name of the call peer in this record.
     *
     * @return the call peer display name
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * When peer connected to the call
     * @return Date
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * Returns the actual state of the peer
     * @return CallPeerState
     */
    public CallPeerState getState()
    {
        return state;
    }

    /**
     * Sets secondary address to the <tt>CallPeerRecord</tt>
     * @param address the address to be set.
     */
    public void setPeerSecondaryAddress(String address)
    {
        secondaryPeerAddress = address;
    }

    /**
     * Returns the secondary address to the <tt>CallPeerRecord</tt>
     * @return the secondary address to the <tt>CallPeerRecord</tt>
     */
    public String getPeerSecondaryAddress()
    {
        return secondaryPeerAddress;
    }
}
