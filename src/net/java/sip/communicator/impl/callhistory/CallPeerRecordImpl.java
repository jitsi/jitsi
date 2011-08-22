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
