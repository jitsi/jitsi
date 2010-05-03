/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>ContactQuery</tt> corresponds to a particular query made through the
 * <tt>ContactSourceService</tt>. Each query once started could be
 * canceled. One could also register a listener in order to be notified for
 * changes in query status and query contact results.
 *
 * @author Yana Stamcheva
 */
public interface ContactQuery
{
    /**
     * Returns the <tt>ContactSourceService</tt>, where this query was first
     * initiated.
     * @return the <tt>ContactSourceService</tt>, where this query was first
     * initiated
     */
    public ContactSourceService getContactSource();

    /**
     * Returns the list of <tt>SourceContact</tt>s returned by this query.
     * @return the list of <tt>SourceContact</tt>s returned by this query
     */
    public List<SourceContact> getQueryResults();

    /**
     * Cancels this query.
     */
    public void cancel();

    /**
     * Adds the given <tt>ContactQueryListener</tt> to the list of registered
     * listeners. The <tt>ContactQueryListener</tt> would be notified each
     * time a new <tt>ContactQuery</tt> result has been received or if the
     * query has been completed or has been canceled by user or for any other
     * reason.
     * @param l the <tt>ContactQueryListener</tt> to add
     */
    public void addContactQueryListener(ContactQueryListener l);

    /**
     * Removes the given <tt>ContactQueryListener</tt> to the list of
     * registered listeners. The <tt>ContactQueryListener</tt> would be
     * notified each time a new <tt>ContactQuery</tt> result has been received
     * or if the query has been completed or has been canceled by user or for
     * any other reason.
     * @param l the <tt>ContactQueryListener</tt> to remove
     */
    public void removeContactQueryListener(ContactQueryListener l);
}
