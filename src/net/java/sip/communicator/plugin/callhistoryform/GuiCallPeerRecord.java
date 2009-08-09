/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.callhistoryform;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>GuiCallPeerRecord</tt> is meant to be used in the call history
 * to represent a history call peer record. It wraps a
 * <tt>CallPeer</tt> or a <tt>CallPeerRecord</tt> object.
 *
 * @author Yana Stamcheva
 */
public class GuiCallPeerRecord
{
    public static final String INCOMING_CALL = "IncomingCall";

    public static final String OUTGOING_CALL = "OutgoingCall";

    private String direction;

    private String peerName;

    private Date startTime;

    private Date callTime;

    /**
     * Creates an instance of <tt>GuiCallPeerRecord</tt> by specifying
     * the peer name, the call direction (incoming or outgoing), the
     * time at which the call has started and the duration of the call.
     *
     * @param peerName the name of the call peer
     * @param direction the direction of the call - INCOMING_CALL
     * or OUTGOING_CALL
     * @param startTime the time at which the call has started
     * @param callTime the duration of the call
     */
    public GuiCallPeerRecord(String peerName,
            String direction,
            Date startTime,
            Date callTime)
    {
        this.direction = direction;

        this.peerName = peerName;

        this.startTime = startTime;

        this.callTime = callTime;
    }

    /**
     * Creates an instance of <tt>GuiCallPeerRecord</tt> by specifying
     * the corresponding <tt>CallPeerRecord</tt>, which gives all the
     * information for the peer and the call duration.
     *
     * @param peerRecord the corresponding <tt>CallPeerRecord</tt>
     * @param direction the call direction - INCOMING_CALL or OUTGOING_CALL
     */
    public GuiCallPeerRecord(CallPeerRecord peerRecord,
            String direction)
    {
        this.direction = direction;

        this.peerName = peerRecord.getPeerAddress();

        this.startTime = peerRecord.getStartTime();

        this.callTime = GuiUtils.substractDates(
                peerRecord.getEndTime(), startTime);
    }

    /**
     * Returns the call direction - INCOMING_CALL or OUTGOING_CALL.
     *
     * @return the call direction - INCOMING_CALL or OUTGOING_CALL.
     */
    public String getDirection()
    {
        return direction;
    }

    /**
     * Returns the duration of the call.
     *
     * @return the duration of the call
     */
    public Date getCallTime()
    {
        return callTime;
    }

    /**
     * Returns the name of the peer.
     *
     * @return the name of the peer
     */
    public String getPeerName()
    {
        return peerName;
    }

    /**
     * Returns the time at which the call has started.
     *
     * @return the time at which the call has started
     */
    public Date getStartTime()
    {
        return startTime;
    }
}
