/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>PhoneNumberContactSource</tt> is a source of phone numbers coming
 * from the server stored contact info of all contacts for all protocol
 * providers.
 *
 * @author Yana Stamcheva
 */
public class PhoneNumberContactSource
    implements ContactSourceService
{
    /**
     * The <tt>List</tt> of <tt>PhoneNumberContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<PhoneNumberContactQuery> queries
        = new LinkedList<PhoneNumberContactQuery>();

    /**
     * Returns DEFAULT_TYPE to indicate that this contact source is a default
     * source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return PNContactSourceActivator.getResources().getI18NString(
            "plugin.phonenumbercontactsource.DISPLAY_NAME");
    }

    /**
     * Queries this contact source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, -1);
    }

    /**
     * Queries this contact source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource( String queryString,
                                            int contactCount)
    {
        if (queryString == null)
            queryString = "";

        PhoneNumberContactQuery contactQuery
            = new PhoneNumberContactQuery(this, queryString, contactCount);

        synchronized (queries)
        {
            queries.add(contactQuery);
        }

        boolean queryHasStarted = false;

        try
        {
            contactQuery.start();
            queryHasStarted = true;
        }
        finally
        {
            if (!queryHasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(contactQuery))
                        queries.notify();
                }
            }
        }
        return contactQuery;
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
