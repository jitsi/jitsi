/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
}
