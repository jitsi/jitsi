/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>CallHistoryContactSource</tt> is the contact source for the call
 * history.
 *
 * @author Yana Stamcheva
 */
public class CallHistoryContactSource implements ContactSourceService
{
    /**
     * The display name of this contact source.
     */
    private static final String CALL_HISTORY_NAME = "Call history";

    /**
     * Returns the display name of this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return CALL_HISTORY_NAME;
    }

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public String getIdentifier()
    {
        return CALL_HISTORY;
    }

    /**
     * Queries this contact source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        if (queryString != null && queryString.length() > 0)
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findByPeer(queryString, 50));
        else
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findLast(50));
    }

    /**
     * The <tt>CallHistoryContactQuery</tt> contains information about a current
     * query to the contact source.
     */
    private class CallHistoryContactQuery
        extends AbstractContactQuery<CallHistoryContactSource>
    {
        /**
         * A list of all source contact results.
         */
        private final List<SourceContact> sourceContacts
            = new LinkedList<SourceContact>();

        /**
         * The underlying <tt>CallHistoryQuery</tt>, on which this
         * <tt>ContactQuery</tt> is based.
         */
        private CallHistoryQuery callHistoryQuery;

        /**
         * Indicates the status of this query. When created this query is in
         * progress.
         */
        private int status = QUERY_IN_PROGRESS;

        /**
         * Creates an instance of <tt>CallHistoryContactQuery</tt> by specifying
         * the list of call records results.
         * @param callRecords the list of call records, which are the result
         * of this query
         */
        public CallHistoryContactQuery(Collection<CallRecord> callRecords)
        {
            super(CallHistoryContactSource.this);

            Iterator<CallRecord> recordsIter = callRecords.iterator();

            while (recordsIter.hasNext() && status != QUERY_CANCELED)
            {
                sourceContacts.add(
                    new CallHistorySourceContact(
                        CallHistoryContactSource.this,
                        recordsIter.next()));
            }

            if (status != QUERY_CANCELED)
                status = QUERY_COMPLETED;
        }

        /**
         * Creates an instance of <tt>CallHistoryContactQuery</tt> based on the
         * given <tt>callHistoryQuery</tt>.
         * @param callHistoryQuery the query used to track the call history
         */
        public CallHistoryContactQuery(CallHistoryQuery callHistoryQuery)
        {
            super(CallHistoryContactSource.this);

            this.callHistoryQuery = callHistoryQuery;

            callHistoryQuery.addQueryListener(new CallHistoryQueryListener()
            {
                public void callRecordReceived(CallRecordEvent event)
                {
                    SourceContact contact = new CallHistorySourceContact(
                                                CallHistoryContactSource.this,
                                                event.getCallRecord());
                    sourceContacts.add(contact);
                    fireContactReceived(contact);
                }

                public void queryStatusChanged(
                    CallHistoryQueryStatusEvent event)
                {
                    status = event.getEventType();
                    fireQueryStatusChanged(status);
                }
            });
        }

        /**
         * This query could not be canceled.
         */
        public void cancel()
        {
            status = QUERY_CANCELED;

            if (callHistoryQuery != null)
                callHistoryQuery.cancel();
        }

        /**
         * Returns the status of this query. One of the static constants defined
         * in this class.
         * @return the status of this query
         */
        public int getStatus()
        {
            return status;
        }

        /**
         * Returns a list containing the results of this query.
         * @return a list containing the results of this query
         */
        public List<SourceContact> getQueryResults()
        {
            return sourceContacts;
        }
    }
}
