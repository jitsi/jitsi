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
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>CallHistoryContactSource</tt> is the contact source for the call
 * history.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class CallHistoryContactSource
    implements ContactSourceService
{
    /**
     * Returns the display name of this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return CallHistoryActivator.getResources().getI18NString(
            "service.gui.CALL_HISTORY_GROUP_NAME");
    }

    /**
     * Creates query for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, 50);
    }

    /**
     * Creates query for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        if (queryString != null && queryString.length() > 0)
        {
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findByPeer(queryString, contactCount));
        }
        else
        {
            return new CallHistoryContactQuery(
                CallHistoryActivator.getCallHistoryService()
                    .findLast(contactCount));
        }
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
         * Iterator for the queried contacts.
         */
        Iterator<CallRecord> recordsIter = null;

        /**
         * Indicates whether show more label should be displayed or not.
         */
        private boolean showMoreLabelAllowed = true;

        /**
         * Creates an instance of <tt>CallHistoryContactQuery</tt> by specifying
         * the list of call records results.
         * @param callRecords the list of call records, which are the result
         * of this query
         */
        public CallHistoryContactQuery(Collection<CallRecord> callRecords)
        {
            recordsIter = callRecords.iterator();
            Iterator<CallRecord> recordsIter = callRecords.iterator();

            while (recordsIter.hasNext() && status != QUERY_CANCELED)
            {
                sourceContacts.add(
                    new CallHistorySourceContact(
                        CallHistoryContactSource.this,
                        recordsIter.next()));
            }

            showMoreLabelAllowed = false;
        }

        @Override
        public void start()
        {
            if(callHistoryQuery != null)
            {
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
                recordsIter = callHistoryQuery.getCallRecords().iterator();
            }

            while (recordsIter.hasNext())
            {
                SourceContact contact = new CallHistorySourceContact(
                    CallHistoryContactSource.this,
                    recordsIter.next());
                sourceContacts.add(contact);
                fireQueryEvent(contact);
            }
            if (status != QUERY_CANCELED)
            {
                status = QUERY_COMPLETED;
                if(callHistoryQuery == null)
                    fireQueryStatusEvent(status);
            }
        }

        /**
         * Creates an instance of <tt>CallHistoryContactQuery</tt> based on the
         * given <tt>callHistoryQuery</tt>.
         * @param callHistoryQuery the query used to track the call history
         */
        public CallHistoryContactQuery(CallHistoryQuery callHistoryQuery)
        {
            this.callHistoryQuery = callHistoryQuery;
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
            ContactReceivedEvent event
                = new ContactReceivedEvent(this, contact, showMoreLabelAllowed);

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
            Collection<ContactQueryListener> listeners;
            ContactQueryStatusEvent event
                = new ContactQueryStatusEvent(this, newStatus);

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
     * Returns default type to indicate that this contact source can be queried
     * by default filters.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return HISTORY_TYPE;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return -1;
    }
}
