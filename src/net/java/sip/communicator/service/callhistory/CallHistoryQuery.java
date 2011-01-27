/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.event.*;

/**
 * The <tt>CallHistoryQuery</tt> corresponds to a query made to the
 * <tt>CallHistoryService</tt>. It allows to be canceled, to listen for changes
 * in the results and to obtain initial results if available.
 *
 * @author Yana Stamcheva
 */
public interface CallHistoryQuery
{
    /**
     * Cancels this query.
     */
    public void cancel();

    /**
     * Returns the query string, this query was created for.
     * @return the query string, this query was created for
     */
    public String getQueryString();

    /**
     * Returns a collection of the initial results for this query. It's up to
     * the implementation to determine, which and how many the initial results
     * would be.
     * <p>
     * This method is meant to be used in order to return first fast initial
     * results and then notify interested parties of additional results through
     * the <tt>CallHistoryQueryListener</tt>, which should improve user
     * experience when waiting for results.
     * @return a collection of the initial results for this query
     */
    public Collection<CallRecord> getCallRecords();

    /**
     * Adds the given <tt>CallHistoryQueryListener</tt> to the list of
     * listeners interested in query result changes.
     * @param l the <tt>CallHistoryQueryListener</tt> to add
     */
    public void addQueryListener(CallHistoryQueryListener l);

    /**
     * Removes the given <tt>CallHistoryQueryListener</tt> from the list of
     * listeners interested in query result changes.
     * @param l the <tt>CallHistoryQueryListener</tt> to remove
     */
    public void removeQueryListener(CallHistoryQueryListener l);
}
