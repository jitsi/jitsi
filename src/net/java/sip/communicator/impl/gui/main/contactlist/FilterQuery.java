/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

/**
 * The <tt>FilterQuery</tt> gives information about a current filtering.
 *
 * @author Yana Stamcheva
 */
public class FilterQuery
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
     * The number of results we're waiting for, before notifying interested
     * <tt>filterQueryListener</tt> that the query has finished.
     */
    private int waitResults = 0;

    /**
     * Adds a wait result.
     */
    public void addWaitResult()
    {
        waitResults ++;
    }

    /**
     * Removes a wait result. If no more results are waited then we notify
     * interested listener that this query has finished.
     */
    public void removeWaitResult()
    {
        waitResults --;

        if (waitResults == 0)
            fireFilterQueryEvent();
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

    public boolean isCanceled()
    {
        return isCanceled;
    }

    public void cancel()
    {
        isCanceled = true;
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
}
