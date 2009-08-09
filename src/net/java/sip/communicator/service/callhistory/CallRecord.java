package net.java.sip.communicator.service.callhistory;

import java.util.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also These records are uesd for returning data
 * from the Call History Service
 *
 * @author Damian Minkov
 */
public class CallRecord
{
    /**
     * Possible directions of the call
     */
    public final static String OUT = "out";
    public final static String IN = "in";

    protected String direction = null;

    protected final List<CallPeerRecord> peerRecords =
        new Vector<CallPeerRecord>();

    protected Date startTime = null;
    protected Date endTime = null;

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
}
