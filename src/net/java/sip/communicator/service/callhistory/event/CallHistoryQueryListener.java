/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory.event;

/**
 * The <tt>CallHistoryQueryListener</tt> listens for changes in the result of
 * a given <tt>CallHistoryQuery</tt>. When a query to the call history is
 * started, this listener would be notified every time new results are available
 * for this query.
 *
 * @author Yana Stamcheva
 */
public interface CallHistoryQueryListener
{
    /**
     * Indicates that new <tt>CallRecord</tt> is received as a result of the
     * query.
     * @param event the <tt>CallRecordsEvent</tt> containing information about
     * the query results.
     */
    public void callRecordReceived(CallRecordEvent event);

    /**
     * Indicates that the status of the history has changed.
     * @param event the <tt>HistoryQueryStatusEvent</tt> containing information
     * about the status change
     */
    public void queryStatusChanged(CallHistoryQueryStatusEvent event);
}
