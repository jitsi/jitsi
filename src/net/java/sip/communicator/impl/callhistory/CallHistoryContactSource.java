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
     * Queries this contact source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, 50);
    }

    /**
     * Queries this contact source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString, int contactCount)
    {
        if (queryString != null && queryString.length() > 0)
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findByPeer(queryString, contactCount));
        else
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findLast(contactCount));
    }

    /**
     * The <tt>CallHistoryContactQuery</tt> contains information about a current
     * query to the contact source.
     */
    private class CallHistoryContactQuery
        implements ContactQuery
    {
        /**
         * A list of all registered query listeners.
         */
        private final List<ContactQueryListener> queryListeners
            = new LinkedList<ContactQueryListener>();

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
            this.callHistoryQuery = callHistoryQuery;

            callHistoryQuery.addQueryListener(new CallHistoryQueryListener()
            {
                public void callRecordReceived(CallRecordEvent event)
                {
                    if (getStatus() == ContactQuery.QUERY_CANCELED)
                        return;

                    SourceContact contact = new CallHistorySourceContact(
                                                CallHistoryContactSource.this,
                                                event.getCallRecord());
                    sourceContacts.add(contact);
                    fireQueryEvent(contact);
                }

                public void queryStatusChanged(
                    CallHistoryQueryStatusEvent event)
                {
                    status = event.getEventType();
                    fireQueryStatusEvent(status);
                }
            });

            Iterator<CallRecord> callRecords
                = callHistoryQuery.getCallRecords().iterator();

            while (callRecords.hasNext())
            {
                SourceContact contact = new CallHistorySourceContact(
                    CallHistoryContactSource.this,
                    callRecords.next());
                sourceContacts.add(contact);
            }
        }

        /**
         * Adds the given <tt>ContactQueryListener</tt> to the list of query
         * listeners.
         * @param l the <tt>ContactQueryListener</tt> to add
         */
        public void addContactQueryListener(ContactQueryListener l)
        {
            synchronized (queryListeners)
            {
                queryListeners.add(l);
            }
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
         * Removes the given <tt>ContactQueryListener</tt> from the list of
         * query listeners.
         * @param l the <tt>ContactQueryListener</tt> to remove
         */
        public void removeContactQueryListener(ContactQueryListener l)
        {
            synchronized (queryListeners)
            {
                queryListeners.remove(l);
            }
        }

        /**
         * Returns a list containing the results of this query.
         * @return a list containing the results of this query
         */
        public List<SourceContact> getQueryResults()
        {
            return sourceContacts;
        }

        /**
         * Returns the <tt>ContactSourceService</tt>, where this query was first
         * initiated.
         * @return the <tt>ContactSourceService</tt>, where this query was first
         * initiated
         */
        public ContactSourceService getContactSource()
        {
            return CallHistoryContactSource.this;
        }

        /**
         * Notifies all registered <tt>ContactQueryListener</tt>s that a new
         * contact has been received.
         * @param contact the <tt>SourceContact</tt> this event is about
         */
        private void fireQueryEvent(SourceContact contact)
        {
            ContactReceivedEvent event = new ContactReceivedEvent(this, contact);

            Collection<ContactQueryListener> listeners;
            synchronized (queryListeners)
            {
                listeners
                    = new ArrayList<ContactQueryListener>(queryListeners);
            }

            for (ContactQueryListener l : listeners)
                l.contactReceived(event);
        }

        /**
         * Notifies all registered <tt>ContactQueryListener</tt>s that a new
         * record has been received.
         * @param newStatus the new status
         */
        private void fireQueryStatusEvent(int newStatus)
        {
            ContactQueryStatusEvent event
                = new ContactQueryStatusEvent(this, newStatus);

            Collection<ContactQueryListener> listeners;
            synchronized (queryListeners)
            {
                listeners
                    = new ArrayList<ContactQueryListener>(queryListeners);
            }

            for (ContactQueryListener l : listeners)
                l.queryStatusChanged(event);
        }

        public String getQueryString()
        {
            return callHistoryQuery.getQueryString();
        }
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
}
