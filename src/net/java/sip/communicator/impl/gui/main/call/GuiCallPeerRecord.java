/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

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

    private Date callDuration;

    public GuiCallPeerRecord(String peerName,
            String direction,
            Date startTime,
            Date callDuration)
    {
        this.direction = direction;

        this.peerName = peerName;

        this.startTime = startTime;

        this.callDuration = callDuration;
    }

    public GuiCallPeerRecord(CallPeerRecord peerRecord,
            String direction)
    {
        this.direction = direction;

        this.peerName = peerRecord.getPeerAddress();

        this.startTime = peerRecord.getStartTime();

        this.callDuration = GuiUtils.substractDates(
                peerRecord.getEndTime(), startTime);
    }

    public String getDirection()
    {
        return direction;
    }

    /**
     * Returns the duration of the contained peer call.
     *
     * @return the duration of the contained peer call
     */
    public Date getDuration()
    {
        return callDuration;
    }

    public String getPeerName()
    {
        return peerName;
    }

    public Date getStartTime()
    {
        return startTime;
    }
}
