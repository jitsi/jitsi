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
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>FilterQuery</tt> gives information about a current filtering.
 *
 * @author Yana Stamcheva
 */
public abstract class FilterQuery
{
    /**
     * The maximum result count for each contact source.
     */
    private int maxResultCount = 10;

    /**
     * A listener, which is notified when this query finishes.
     */
    private FilterQueryListener filterQueryListener;

    /**
     * Adds the given <tt>contactQuery</tt> to the list of filterQueries.
     * @param contactQuery the <tt>ContactQuery</tt> to add
     */
    public abstract void addContactQuery(Object contactQuery);

    /**
     * Sets the <tt>isSucceeded</tt> property.
     * @param isSucceeded indicates if this query has succeeded
     */
    public abstract void setSucceeded(boolean isSucceeded);

    /**
     * Indicates if this query has succeeded.
     * @return <tt>true</tt> if this query has succeeded, <tt>false</tt> -
     * otherwise
     */
    public abstract boolean isSucceeded();

    /**
     * Indicates if this query is canceled.
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    public abstract boolean isCanceled();

    /**
     * Indicates if this query is canceled.
     *
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    public abstract boolean isRunning();

    /**
     * Cancels this filter query.
     */
    public abstract void cancel();

    /**
     * Closes this query to indicate that no more contact sub-queries would be
     * added to it.
     */
    public abstract void close();

    /**
     * Sets the given <tt>FilterQueryListener</tt>.
     * @param l the <tt>FilterQueryListener</tt> to set
     */
    public void setQueryListener(FilterQueryListener l)
    {
        filterQueryListener = l;
    }

    /**
     * Removes the given query from this filter query, updates the related data
     * and notifies interested parties if this was the last query to process.
     * @param query the <tt>ContactQuery</tt> to remove.
     */
    public abstract void removeQuery(ContactQuery query);

    /**
     * Verifies if the given query is contained in this filter query.
     *
     * @param query the query we're looking for
     * @return <tt>true</tt> if the given <tt>query</tt> is contained in this
     * filter query, <tt>false</tt> - otherwise
     */
    public abstract boolean containsQuery(Object query);


    /**
     * Sets the maximum result count shown.
     *
     * @param resultCount the maximum result count shown
     */
    public void setMaxResultShown(int resultCount)
    {
        this.maxResultCount = resultCount;
    }

    /**
     * Gets the maximum result count shown.
     *
     * @return the maximum result count shown
     */
    public int getMaxResultShown()
    {
        return maxResultCount;
    }
}
