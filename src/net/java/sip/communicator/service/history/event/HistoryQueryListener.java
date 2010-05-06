/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.event;

/**
 * The <tt>HistoryQueryListener</tt> listens for changes in the result of
 * a given <tt>HistoryQuery</tt>. When a query to the history is started, this
 * listener would be notified every time new results are available
 * for this query.
 *
 * @author Yana Stamcheva
 */
public interface HistoryQueryListener
{
    /**
     * Indicates that new <tt>HistoryRecord</tt> has been received as a result
     * of the query.
     * @param event the <tt>HistoryRecordEvent</tt> containing information about
     * the query results.
     */
    public void historyRecordReceived(HistoryRecordEvent event);

    /**
     * Indicates that the status of the history has changed.
     * @param event the <tt>HistoryQueryStatusEvent</tt> containing information
     * about the status change
     */
    public void queryStatusChanged(HistoryQueryStatusEvent event);
}
