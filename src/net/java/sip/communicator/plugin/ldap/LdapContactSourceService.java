/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ldap;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactSourceService</tt> for LDAP.
 *
 * @author Sebastien Vincent
 */
public class LdapContactSourceService
    implements ExtendedContactSourceService
{
    /**
     * The <tt>List</tt> of <tt>LdapContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<LdapContactQuery> queries
        = new LinkedList<LdapContactQuery>();

    /**
     * Queries this search source for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(Pattern queryPattern)
    {
        return queryContactSource(queryPattern,
                LdapContactQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Queries this search source for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @param count maximum number of contact returned
     * @return the created query
     */
    public ContactQuery queryContactSource(Pattern queryPattern, int count)
    {
        LdapContactQuery query = new LdapContactQuery(this, queryPattern,
                count);

        synchronized (queries)
        {
            // cancel previous queries
            for(LdapContactQuery q : queries)
            {
                q.cancel();
            }

            queries.add(query);
        }

        boolean hasStarted = false;

        try
        {
            query.start();
            hasStarted = true;
        }
        finally
        {
            if (!hasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(query))
                        queries.notify();
                }
            }
        }

        return query;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return "LDAP directory";
    }

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public String getIdentifier()
    {
        return "LDAP";
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     * @param query the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String query)
    {
        return queryContactSource(
            Pattern.compile(query), LdapContactQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param query the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource(String query, int contactCount)
    {
        return queryContactSource(Pattern.compile(query), contactCount);
    }

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     *
     * @see AsyncContactSourceService#stop()
     */
    public void stop()
    {
        boolean interrupted = false;

        synchronized (queries)
        {
            while (!queries.isEmpty())
            {
                queries.get(0).cancel();
                try
                {
                    queries.wait();
                }
                catch (InterruptedException iex)
                {
                    interrupted = true;
                }
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * Notifies this <tt>LdapContactSourceService</tt> that a specific
     * <tt>LdapContactQuery</tt> has stopped.
     *
     * @param query the <tt>LdapContactQuery</tt> which has stopped
     */
    void stopped(LdapContactQuery query)
    {
        synchronized (queries)
        {
            if (queries.remove(query))
                queries.notify();
        }
    }
}
