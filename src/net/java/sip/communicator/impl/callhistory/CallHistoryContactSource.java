/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
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
        if (queryString != null && queryString.length() > 0)
            return new CallHistoryQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findByPeer(queryString));
        else
            return new CallHistoryQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findLast(50));
    }

    /**
     * The <tt>CallHistoryQuery</tt> contains information about a current query
     * to the contact source.
     */
    private class CallHistoryQuery implements ContactQuery
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
         * Creates a <tt>CallHistoryQuery</tt>.
         * @param callRecords a collection of the result call records
         */
        public CallHistoryQuery(Collection<CallRecord> callRecords)
        {
            Iterator<CallRecord> recordsIter = callRecords.iterator();

            while (recordsIter.hasNext())
            {
                sourceContacts.add(
                    new CallHistorySourceContact(
                        CallHistoryContactSource.this,
                        recordsIter.next()));
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
