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
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.records.*;

/**
 *
 * @author Yana Stamcheva
 */
public class CallHistoryQueryImpl
    implements CallHistoryQuery
{
    private final Collection<CallHistoryQueryListener> queryListeners
        = new LinkedList<CallHistoryQueryListener>();

    private final Collection<CallRecord> callRecords = new Vector<CallRecord>();

    private final HistoryQuery historyQuery;

    /**
     * Creates an instance of <tt>CallHistoryQueryImpl</tt> by specifying the
     * underlying <tt>HistoryQuery</tt>.
     * @param query the underlying <tt>HistoryQuery</tt> this query is based on
     */
    public CallHistoryQueryImpl(HistoryQuery query)
    {
        this.historyQuery = query;

        historyQuery.addHistoryRecordsListener(new HistoryQueryListener()
        {
            public void historyRecordReceived(HistoryRecordEvent event)
            {
                CallRecord callRecord
                    = CallHistoryServiceImpl.convertHistoryRecordToCallRecord(
                        event.getHistoryRecord());

                callRecords.add(callRecord);
                fireQueryEvent(callRecord);
            }

            public void queryStatusChanged(HistoryQueryStatusEvent event)
            {
                fireQueryStatusEvent(event.getEventType());
            }
        });

        Iterator<HistoryRecord> historyRecords
            = historyQuery.getHistoryRecords().iterator();

        while (historyRecords.hasNext())
        {
            CallRecord callRecord
                = CallHistoryServiceImpl.convertHistoryRecordToCallRecord(
                    historyRecords.next());

            callRecords.add(callRecord);
        }
    }

    /**
     * Cancels this query.
     */
    public void cancel()
    {
        historyQuery.cancel();
    }

    /**
     * Returns a collection of the results for this query. It's up to
     * the implementation to determine how and when to fill this list of
     * results.
     * <p>
     * This method could be used in order to obtain first fast initial
     * results and then obtain the additional results through the
     * <tt>CallHistoryQueryListener</tt>, which should improve user experience
     * when waiting for results.
     *
     * @return a collection of the initial results for this query
     */
    public Collection<CallRecord> getCallRecords()
    {
        return new Vector<CallRecord>(callRecords);
    }

    /**
     * Adds the given <tt>CallHistoryQueryListener</tt> to the list of
     * listeners interested in query result changes.
     * @param l the <tt>CallHistoryQueryListener</tt> to add
     */
    public void addQueryListener(CallHistoryQueryListener l)
    {
        synchronized (queryListeners)
        {
            queryListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>CallHistoryQueryListener</tt> from the list of
     * listeners interested in query result changes.
     * @param l the <tt>CallHistoryQueryListener</tt> to remove
     */
    public void removeQueryListener(CallHistoryQueryListener l)
    {
        synchronized (queryListeners)
        {
            queryListeners.remove(l);
        }
    }

    /**
     * Notifies all registered <tt>HistoryQueryListener</tt>s that a new record
     * has been received.
     * @param record the <tt>HistoryRecord</tt>
     */
    private void fireQueryEvent(CallRecord record)
    {
        CallRecordEvent event = new CallRecordEvent(this, record);

        synchronized (queryListeners)
        {
            for (CallHistoryQueryListener l : queryListeners)
                l.callRecordReceived(event);
        }
    }

    /**
     * Notifies all registered <tt>HistoryQueryListener</tt>s that a new record
     * has been received.
     * @param newStatus the new status
     */
    private void fireQueryStatusEvent(int newStatus)
    {
        CallHistoryQueryStatusEvent event
            = new CallHistoryQueryStatusEvent(this, newStatus);

        synchronized (queryListeners)
        {
            for (CallHistoryQueryListener l : queryListeners)
                l.queryStatusChanged(event);
        }
    }

    /**
     * Returns the query string, this query was created for.
     *
     * @return the query string, this query was created for
     */
    public String getQueryString()
    {
        return historyQuery.getQueryString();
    }
}
