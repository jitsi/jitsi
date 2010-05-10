/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

/**
 * The <tt>MissedCall</tt> class wraps a missed call in order to provide
 * information about the call later.
 *
 * @author Yana Stamcheva
 */
public class MissedCall
{
    private final String callName;

    private final Date callTime;

    /**
     * Creates an instance of <tt>MissedCall</tt> by specifying the call name
     * and time.
     * @param callName the name associated to this call
     * @param callTime the time of the missed call
     */
    public MissedCall(String callName, Date callTime)
    {
        this.callName = callName;
        this.callTime = callTime;
    }

    /**
     * Returns the name associated with this call.
     * @return the name associated with this call
     */
    public String getCallName()
    {
        return callName;
    }

    /**
     * Returns the time the call was made.
     * @return the time the call was made
     */
    public Date getCallTime()
    {
        return callTime;
    }
}
