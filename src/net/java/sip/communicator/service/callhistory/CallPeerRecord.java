package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also These records are uesd for returning data
 * from the Call History Service
 *
 * @author Damian Minkov
 */
public class CallPeerRecord
{
    protected String peerAddress = null;
    protected Date startTime = null;
    protected Date endTime = null;
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
}
