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
package net.java.sip.communicator.impl.history;

import java.util.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * The <tt>HistoryQueryImpl</tt> is an implementation of the
 * <tt>HistoryQuery</tt> interface. It corresponds to a query made through the
 * <tt>InteractiveHistoryReader</tt>. It allows to be canceled, to listen for
 * changes in the results and to obtain initial results if available.
 *
 * @author Yana Stamcheva
 */
public class HistoryQueryImpl implements HistoryQuery
{
    /**
     * The list of query listeners registered in this query.
     */
    private final Collection<HistoryQueryListener> queryListeners
        = new LinkedList<HistoryQueryListener>();

    /**
     * The list of history records, which is the result of this query.
     */
    private final Collection<HistoryRecord> historyRecords
        = new Vector<HistoryRecord>();

    /**
     * Indicates if this query has been canceled.
     */
    private boolean isCanceled = false;

    /**
     * The query string we're looking for in this query.
     */
    private final String queryString;

    /**
     * Creates an instance of <tt>HistoryQueryImpl</tt> by specifying the query
     * string it was created for.
     *
     * @param queryString the query string we're looking for in this query
     */
    public HistoryQueryImpl(String queryString)
    {
        this.queryString = queryString;
    }

    /**
     * Cancels this query.
     */
    public void cancel()
    {
        isCanceled = true;
    }

    /**
     * Indicates if this query has been canceled.
     * @return <tt>true</tt> if this query has been canceled, otherwise returns
     * <tt>false</tt>
     */
    boolean isCanceled()
    {
        return isCanceled;
    }

    /**
     * Returns a collection of the results for this query. It's up to
     * the implementation to determine how and when to fill this list of
     * results.
     * <p>
     * This method could be used in order to obtain first fast initial
     * results and then obtain the additional results through the
     * <tt>HistoryQueryListener</tt>, which should improve user experience when
     * waiting for results.
     *
     * @return a collection of the initial results for this query
     */
    public Collection<HistoryRecord> getHistoryRecords()
    {
        return new Vector<HistoryRecord>(historyRecords);
    }

    /**
     * Adds the given <tt>HistoryQueryListener</tt> to the list of
     * listeners interested in query result changes.
     * @param l the <tt>HistoryQueryListener</tt> to add
     */
    public void addHistoryRecordsListener(HistoryQueryListener l)
    {
        synchronized (queryListeners)
        {
            queryListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>HistoryQueryListener</tt> from the list of
     * listeners interested in query result changes.
     * @param l the <tt>HistoryQueryListener</tt> to remove
     */
    public void removeHistoryRecordsListener(HistoryQueryListener l)
    {
        synchronized (queryListeners)
        {
            queryListeners.remove(l);
        }
    }

    /**
     * Adds the given <tt>HistoryRecord</tt> to the result list of this query
     * and notifies all interested listeners that a new record is received.
     * @param record the <tt>HistoryRecord</tt> to add
     */
    void addHistoryRecord(HistoryRecord record)
    {
        historyRecords.add(record);

        fireQueryEvent(record);
    }

    /**
     * Sets this query status to the given <tt>queryStatus</tt> and notifies
     * all interested listeners of the change.
     * @param queryStatus the new query status to set
     */
    void setStatus(int queryStatus)
    {
        fireQueryStatusEvent(queryStatus);
    }

    /**
     * Notifies all registered <tt>HistoryQueryListener</tt>s that a new record
     * has been received.
     * @param record the <tt>HistoryRecord</tt>
     */
    private void fireQueryEvent(HistoryRecord record)
    {
        HistoryRecordEvent event = new HistoryRecordEvent(this, record);

        synchronized (queryListeners)
        {
            for (HistoryQueryListener l : queryListeners)
            {
                l.historyRecordReceived(event);
            }
        }
    }

    /**
     * Notifies all registered <tt>HistoryQueryListener</tt>s that a new record
     * has been received.
     * @param newStatus the new status
     */
    private void fireQueryStatusEvent(int newStatus)
    {
        HistoryQueryStatusEvent event
            = new HistoryQueryStatusEvent(this, newStatus);

        synchronized (queryListeners)
        {
            for (HistoryQueryListener l : queryListeners)
            {
                l.queryStatusChanged(event);
            }
        }
    }

    /**
     * Returns the query string, this query was created for.
     *
     * @return the query string, this query was created for
     */
    public String getQueryString()
    {
        return queryString;
    }
}
