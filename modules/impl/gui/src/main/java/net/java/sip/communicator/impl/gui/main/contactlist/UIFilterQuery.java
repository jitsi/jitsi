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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>FilterQuery</tt> gives information about a current filtering.
 *
 * @author Yana Stamcheva
 */
public class UIFilterQuery
    extends FilterQuery
    implements  ContactQueryListener,
                MetaContactQueryListener
{
    /**
     * A listener, which is notified when this query finishes.
     */
    private FilterQueryListener filterQueryListener;

    /**
     * Indicates if the query succeeded, i.e. if any of the filters associated
     * with this query has returned any results.
     */
    private boolean isSucceeded = false;

    /**
     * Indicates if this query has been canceled.
     */
    private boolean isCanceled = false;

    /**
     * Indicates if this query is currently running.
     */
    private boolean isRunning = false;

    /**
     * Indicates if this query is closed, means no more queries could be added
     * to it. A <tt>FilterQuery</tt>, which is closed knows that it has to wait
     * for a final number of queries to finish before notifying interested
     * parties of the result.
     */
    private boolean isClosed = false;

    /**
     * The list of filter queries.
     */
    private final Map<Object, List<SourceContact>> filterQueries
        = Collections.synchronizedMap(
                new Hashtable<Object, List<SourceContact>>());

    /**
     * Indicates the number of running queries.
     */
    private int runningQueries = 0;

    /**
     * The parent contact list of this query.
     */
    private final ContactList contactList;

    /**
     * Map of the created show more contacts for a query.
     * We stored them, so we can remove them (sometimes those
     * contacts are not added to UI, so they are not removed and
     * not cleared)
     */
    private final Map<ContactQuery,ShowMoreContact> showMoreContactMap
        = new HashMap<ContactQuery, ShowMoreContact>();

    /**
     * Creates an instance of <tt>UIFilterQuery</tt> by specifying the parent
     * <tt>ContactList</tt>.
     *
     * @param contactList the <tt>ContactList</tt> on which the query is
     * performed
     */
    public UIFilterQuery(ContactList contactList)
    {
        this.contactList = contactList;
    }

    /**
     * Adds the given <tt>contactQuery</tt> to the list of filterQueries.
     * @param contactQuery the <tt>ContactQuery</tt> to add
     */
    @Override
    public void addContactQuery(Object contactQuery)
    {
        synchronized (filterQueries)
        {
            // If this filter query has been already canceled and someone wants
            // to add something to it, we just cancel the incoming query and
            // return.
            if (isCanceled)
            {
                cancelQuery(contactQuery);
                return;
            }

            List<SourceContact> queryResults = new ArrayList<SourceContact>();

            if (contactQuery instanceof ContactQuery)
            {
                ContactQuery externalQuery = (ContactQuery) contactQuery;

                externalQuery.addContactQueryListener(this);
            }
            else if (contactQuery instanceof MetaContactQuery)
                ((MetaContactQuery) contactQuery).addContactQueryListener(this);

            isRunning = true;
            filterQueries.put(contactQuery, queryResults);
            runningQueries++;
        }
    }

    /**
     * Sets the <tt>isSucceeded</tt> property.
     * @param isSucceeded indicates if this query has succeeded
     */
    @Override
    public void setSucceeded(boolean isSucceeded)
    {
        this.isSucceeded = isSucceeded;
    }

    /**
     * Indicates if this query has succeeded.
     * @return <tt>true</tt> if this query has succeeded, <tt>false</tt> -
     * otherwise
     */
    @Override
    public boolean isSucceeded()
    {
        return isSucceeded;
    }

    /**
     * Indicates if this query is canceled.
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    @Override
    public boolean isCanceled()
    {
        synchronized (filterQueries)
        {
            return isCanceled;
        }
    }

    /**
     * Indicates if this query is canceled.
     *
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    @Override
    public boolean isRunning()
    {
        synchronized (filterQueries)
        {
            return isRunning;
        }
    }

    /**
     * Cancels this filter query.
     */
    @Override
    public void cancel()
    {
        synchronized(filterQueries)
        {
            isCanceled = true;

            Iterator<Object> queriesIter = filterQueries.keySet().iterator();

            while (queriesIter.hasNext())
                cancelQuery(queriesIter.next());
        }
    }

    /**
     * Closes this query to indicate that no more contact sub-queries would be
     * added to it.
     */
    @Override
    public void close()
    {
        isClosed = true;

        if (runningQueries == 0)
            fireFilterQueryEvent();
    }

    /**
     * Sets the given <tt>FilterQueryListener</tt>.
     * @param l the <tt>FilterQueryListener</tt> to set
     */
    @Override
    public void setQueryListener(FilterQueryListener l)
    {
        filterQueryListener = l;
    }

    /**
     * Notifies the <tt>FilterQueryListener</tt> of the result status of
     * this query.
     */
    private void fireFilterQueryEvent()
    {
        isRunning = false;

        if (filterQueryListener == null)
            return;

        if (isSucceeded)
            filterQueryListener.filterQuerySucceeded(this);
        else
            filterQueryListener.filterQueryFailed(this);
    }

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        ContactQuery query = event.getQuerySource();

        // Check if this query is in our filter queries list.
        if (!filterQueries.containsKey(query)
                || event.getEventType() == ContactQuery.QUERY_IN_PROGRESS)
            return;

        removeQuery(query);
    }

    /**
     * Removes the given query from this filter query, updates the related data
     * and notifies interested parties if this was the last query to process.
     * @param query the <tt>ContactQuery</tt> to remove.
     */
    @Override
    public void removeQuery(ContactQuery query)
    {
        // First set the isSucceeded property.
        if (!isSucceeded() && !query.getQueryResults().isEmpty())
            setSucceeded(true);

        // Then remove the wait result from the filterQuery.
        runningQueries--;
        query.removeContactQueryListener(this);

        // If no queries have rest we notify interested listeners that query
        // has finished.
        if (runningQueries == 0 && isClosed)
            fireFilterQueryEvent();
    }

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event)
    {
        MetaContactQuery query = event.getQuerySource();

        // Check if this query is in our filter queries list.
        if (!filterQueries.containsKey(query))
            return;

        // First set the isSucceeded property.
        if (!isSucceeded() && query.getResultCount() > 0)
            setSucceeded(true);

        // We don't remove the query from our list, because even if the query
        // has finished its GUI part is scheduled in the Swing thread and we
        // don't know anything about these events, so if someone calls cancel()
        // we need to explicitly cancel all contained queries even they are
        // finished.
        runningQueries--;
        query.removeContactQueryListener(this);

        // If no queries have rest we notify interested listeners that query
        // has finished.
        if (runningQueries == 0 && isClosed)
            fireFilterQueryEvent();
    }

    /**
     * Cancels the given query.
     * @param query the query to cancel
     */
    private void cancelQuery(Object query)
    {
        if (query instanceof ContactQuery)
        {
            ContactQuery contactQuery = (ContactQuery) query;
            contactQuery.cancel();
            contactQuery.removeContactQueryListener(contactList);
            if (!isSucceeded && contactQuery.getQueryResults().size() > 0)
                isSucceeded = true;

            // removes ShowMoreContact and clears it
            ShowMoreContact showMoreContact
                = showMoreContactMap.remove(contactQuery);
            if(showMoreContact != null)
            {
                showMoreContact.setContactNode(null);
            }
        }
        else if (query instanceof MetaContactQuery)
        {
            MetaContactQuery metaContactQuery = (MetaContactQuery) query;
            metaContactQuery.cancel();
            metaContactQuery.removeContactQueryListener(contactList);
            if (!isSucceeded && metaContactQuery.getResultCount() > 0)
                isSucceeded = true;
        }
    }

    /**
     * Verifies if the given query is contained in this filter query.
     *
     * @param query the query we're looking for
     * @return <tt>true</tt> if the given <tt>query</tt> is contained in this
     * filter query, <tt>false</tt> - otherwise
     */
    @Override
    public boolean containsQuery(Object query)
    {
        return filterQueries.containsKey(query);
    }

    /**
     * Indicates that a contact has been received as a result of a query.
     *
     * @param event the <tt>ContactReceivedEvent</tt> that notified us
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        ContactQuery query = event.getQuerySource();
        SourceContact contact = event.getContact();

        // First set the isSucceeded property.
        if (!isSucceeded() && !query.getQueryResults().isEmpty())
            setSucceeded(true);

        // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();

        List<SourceContact> queryResults = filterQueries.get(query);

        queryResults.add(contact);
        if (getMaxResultShown() > -1 && event.isShowMoreEnabled()
            && queryResults.size() == getMaxResultShown())
        {
            query.removeContactQueryListener(contactList);

            ShowMoreContact moreInfoContact
                = new ShowMoreContact(query, queryResults, getMaxResultShown());
            showMoreContactMap.put(query, moreInfoContact);

            ContactSourceService contactSource = query.getContactSource();

            contactList.addContact(
                query,
                moreInfoContact,
                contactList.getContactSource(contactSource).getUIGroup(),
                false);
        }
    }

    /**
     * Indicates that a contact has been removed after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the received <tt>SourceContact</tt>
     */
    public void contactRemoved(ContactRemovedEvent event)
    {}

    /**
     * Indicates that a contact has been updated after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the updated <tt>SourceContact</tt>
     */
    public void contactChanged(ContactChangedEvent event)
    {}

    public void metaContactReceived(MetaContactQueryEvent event)
    {
        if (!isSucceeded() && event.getQuerySource().getResultCount() > 0)
            setSucceeded(true);

     // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();
    }

    public void metaGroupReceived(MetaGroupQueryEvent event)
    {
        if (!isSucceeded() && event.getQuerySource().getResultCount() > 0)
            setSucceeded(true);

     // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();
    }
}
