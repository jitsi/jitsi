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
        this.displayName = displayName;
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
}
