/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>FilterQuery</tt> gives information about a current filtering.
 *
 * @author Yana Stamcheva
 */
public class FilterQuery
    implements ContactQueryListener
{
    /**
     * A listener, which is notified when this query finishes.
     */
    private FilterQueryListener filterQueryListener;

    /**
     * Indicates if the query succeeded, i.e. if the filter has returned any
     * results.
     */
    private boolean isSucceeded = false;

    /**
     * Indicates if this query has been canceled.
     */
    private boolean isCanceled = false;

    /**
     * The list of filter queries.
     */
    private Collection<ContactQuery> filterQueries
        = new LinkedList<ContactQuery>();

    /**
     * Adds the given <tt>contactQuery</tt> to the list of filterQueries.
     * @param contactQuery the <tt>ContactQuery</tt> to add
     */
    public void addContactQuery(ContactQuery contactQuery)
    {
        filterQueries.add(contactQuery);
        contactQuery.addContactQueryListener(this);
    }

    /**
     * Sets the <tt>isSucceeded</tt> property.
     * @param isSucceeded indicates if this query has succeeded
     */
    public void setSucceeded(boolean isSucceeded)
    {
        this.isSucceeded = isSucceeded;
    }

    /**
     * Indicates if this query has succeeded.
     * @return <tt>true</tt> if this query has succeeded, <tt>false</tt> -
     * otherwise
     */
    public boolean isSucceeded()
    {
        return isSucceeded;
    }

    /**
     * Indicates if this query is canceled.
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    public boolean isCanceled()
    {
        return isCanceled;
    }

    /**
     * Cancels this filter query.
     */
    public void cancel()
    {
        isCanceled = true;
        filterQueries.clear();
        fireFilterQueryEvent();
    }

    /**
     * Sets the given <tt>FilterQueryListener</tt>.
     * @param l the <tt>FilterQueryListener</tt> to set
     */
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
        if (!filterQueries.contains(query))
            return;

        // First set the isSucceeded property.
        if (!isSucceeded() && !query.getQueryResults().isEmpty())
            setSucceeded(true);

        // Then remove the wait result from the filterQuery.
        filterQueries.remove(query);
        query.removeContactQueryListener(this);

        // If no queries have rest we notify interested listeners that query
        // has finished.
        if (filterQueries.isEmpty())
            fireFilterQueryEvent();
    }

    public void contactReceived(ContactReceivedEvent event)
    {}
}
