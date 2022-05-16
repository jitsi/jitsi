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
package net.java.sip.communicator.impl.ldap;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.ldap.*;

/**
 * Implements <tt>ContactSourceService</tt> for LDAP.
 * <p>
 * In contrast to other contact source implementations like AddressBook and
 * Outlook the LDAP contact source implementation is explicitly moved to the
 * "impl.ldap" package in order to allow us to create LDAP contact sources
 * for ldap directories through the LdapService.
 * </p>
 *
 * @author Sebastien Vincent
 */
public class LdapContactSourceService
    implements ContactSourceService, PrefixedContactSourceService
{
    /**
     * The <tt>List</tt> of <tt>LdapContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<LdapContactQuery> queries
        = new LinkedList<LdapContactQuery>();

    /**
     * LDAP name.
     */
    private final LdapDirectory ldapDirectory;

    /**
     * Constructor.
     *
     * @param ldapDirectory LDAP directory
     */
    public LdapContactSourceService(LdapDirectory ldapDirectory)
    {
        this.ldapDirectory = ldapDirectory;
    }

    /**
     * Removes a query from the list.
     * @param query the query
     */
    public synchronized void removeQuery(ContactQuery query)
    {
        if (queries.remove(query))
            queries.notify();
    }
    
    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return ldapDirectory.getSettings().getName();
    }

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    /**
     * Creates query for the given <tt>query</tt>.
     * @param query the string to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(String query)
    {
        return createContactQuery(query, LdapContactQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Creates query for the given <tt>query</tt>.
     *
     * @param query the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery createContactQuery(String query, int contactCount)
    {
        Pattern pattern = null;
        try
        {
            pattern = Pattern.compile(query);
        }
        catch (PatternSyntaxException pse)
        {
            pattern = Pattern.compile(Pattern.quote(query));
        }

        if(pattern != null)
        {
            LdapContactQuery ldapQuery = new LdapContactQuery(this, pattern,
                contactCount);

            synchronized (queries)
            {
                queries.add(ldapQuery);
            }

            return ldapQuery;
        }

        return null;
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
     * Get LDAP directory.
     *
     * @return LDAP directory
     */
    public LdapDirectory getLdapDirectory()
    {
        return ldapDirectory;
    }

    /**
     * Returns the phoneNumber prefix for all phone numbers.
     *
     * @return the phoneNumber prefix for all phone numbers
     */
    @Override
    public String getPhoneNumberPrefix()
    {
        return ldapDirectory.getSettings().getGlobalPhonePrefix();
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
