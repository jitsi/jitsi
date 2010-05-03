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
 * The <tt>CallRecordsEvent</tt> indicates that one or more
 * <tt>CallRecord</tt>s has been received as a result of a
 * <tt>CallHistoryQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class CallRecordsEvent
    extends EventObject
{
    /**
     * A collection of call records received as a result of a given
     * <tt>query</tt>.
     */
    private final Collection<CallRecord> callRecords;

    /**
     * Creates a <tt>ContactReceivedEvent</tt> by specifying the contact search
     * source and the received <tt>searchContact</tt>.
     * @param query the source that triggered this event
     * @param callRecords the call records received as a result from the given
     * <tt>query</tt>
     */
    public CallRecordsEvent(CallHistoryQuery query,
                            Collection<CallRecord> callRecords)
    {
        super(query);

        this.callRecords = callRecords;
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
     * Returns the collection of <tt>CallRecord</tt>s this event is about.
     * @return the collection of <tt>CallRecord</tt>s this event is about
     */
    public Collection<CallRecord> getCallRecords()
    {
        return callRecords;
    }
}
