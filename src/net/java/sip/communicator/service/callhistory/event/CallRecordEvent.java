/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;

/**
 * The <tt>CallRecordEvent</tt> indicates that a <tt>CallRecord</tt> has been
 * received as a result of a <tt>CallHistoryQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class CallRecordEvent
    extends EventObject
{
    /**
     * The <tt>CallRecord</tt> this event is about.
     */
    private final CallRecord callRecord;

    /**
     * Creates a <tt>CallRecordEvent</tt> by specifying the parent <tt>query</tt>
     * and the <tt>callRecord</tt> this event is about.
     * @param query the source that triggered this event
     * @param callRecord the <tt>CallRecord</tt> this event is about
     */
    public CallRecordEvent(CallHistoryQuery query,
                           CallRecord callRecord)
    {
        super(query);

        this.callRecord = callRecord;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public CallHistoryQuery getQuerySource()
    {
        return (CallHistoryQuery) source;
    }

    /**
     * Returns the <tt>CallRecord</tt>s this event is about.
     * @return the <tt>CallRecord</tt>s this event is about
     */
    public CallRecord getCallRecord()
    {
        return callRecord;
    }
}
