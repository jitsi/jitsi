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
package net.java.sip.communicator.impl.googlecontacts;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.googlecontacts.configform.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ContactSourceService</tt> for Google Contacts.
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsSourceService
    implements ExtendedContactSourceService, PrefixedContactSourceService
{
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(GoogleContactsSourceService.class);

    /**
     * The <tt>List</tt> of <tt>GoogleContactsQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<GoogleContactsQuery> queries
        = new LinkedList<GoogleContactsQuery>();

    /**
     * Login.
     */
    private final String login;

    /**
     * The prefix for all google contact phone numbers.
     */
    private String phoneNumberprefix;

    /**
     * Google Contacts connection.
     */
    private GoogleContactsConnection cnx = null;

    /**
     * The account settings form.
     */
    private AccountSettingsForm settings = null;

    /**
     * If the account has been created using GoogleTalk wizard or via
     * external Google Contacts.
     */
    private boolean googleTalk = false;

    /**
     * Constructor.
     *
     * @param login login
     * @param password password
     */
    public GoogleContactsSourceService(String login)
    {
        super();
        this.login = login;
    }

    /**
     * Constructor.
     *
     * @param cnx connection
     */
    public GoogleContactsSourceService(GoogleContactsConnection cnx)
    {
        super();
        this.cnx = cnx;
        this.login = cnx.getLogin();
        this.phoneNumberprefix = cnx.getPrefix();
    }

    /**
     * Returns login.
     *
     * @return login
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * Set whether or not the account has been created via GoogleTalk wizard or
     * external Google contacts.
     *
     * @param googleTalk value to set
     */
    public void setGoogleTalk(boolean googleTalk)
    {
        this.googleTalk = googleTalk;
    }

    /**
     * Returns whether or not the account has been created via GoogleTalk
     * wizard or via external Google Contacts.
     *
     * @return true if account has been created via GoogleTalk wizard or via
     * external Google Contacts.
     */
    public boolean isGoogleTalk()
    {
        return googleTalk;
    }

    /**
     * Creates query for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(Pattern queryPattern)
    {
        return createContactQuery(queryPattern,
                GoogleContactsQuery.GOOGLECONTACTS_MAX_RESULTS);
    }

    /**
     * Creates query for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @param count maximum number of contact returned
     * @return the created query
     */
    public ContactQuery createContactQuery(Pattern queryPattern, int count)
    {
        GoogleContactsQuery query = new GoogleContactsQuery(this, queryPattern,
                count);

        synchronized (queries)
        {
            queries.add(query);
        }

        return query;
    }

    /**
     * Removes query from the list of queries.
     * 
     * @param query the query that will be removed.
     */
    public synchronized void removeQuery(ContactQuery query)
    {
        if (queries.remove(query))
            queries.notify();
    }
    
    /**
     * Returns the Google Contacts connection.
     *
     * @return Google Contacts connection
     */
    public GoogleContactsConnectionImpl getConnection()
    {
        int s = login.indexOf('@');
        boolean isGoogleAppsOrGmail = false;

        if(s == -1)
        {
            return null;
        }

        String domain = login.substring((s + 1));

        try
        {
            SRVRecord srvRecords[] =
                NetworkUtils.getSRVRecords("xmpp-client", "tcp", domain);

            if(srvRecords != null)
            {
                // To detect that account is a google ones, we try following:
                // - lookup in SRV and see if it is google.com;
                // - if the account has been created with GoogleTalk form;
                // - if it is an "external" google contact.

                // SRV checks
                for(SRVRecord srv : srvRecords)
                {
                    if(srv.getTarget().endsWith("google.com") ||
                            srv.getTarget().endsWith("google.com."))
                    {
                        isGoogleAppsOrGmail = true;
                        break;
                    }
                }
            }

            // GoogleTalk based account or external Google Contacts ?
            if(!isGoogleAppsOrGmail)
            {
                isGoogleAppsOrGmail = googleTalk;
            }

            if(isGoogleAppsOrGmail)
            {
                if(cnx == null)
                {
                    cnx = new GoogleContactsConnectionImpl(login);

                    if(cnx.connect() ==
                        GoogleContactsConnection.ConnectionStatus.
                            ERROR_INVALID_CREDENTIALS)
                    {
                        synchronized(this)
                        {
                            if(settings != null)
                            {
                                cnx = null;
                                return null;
                            }
                            else
                            {
                                settings = new AccountSettingsForm();
                            }
                        }
                        settings.setModal(true);
                        settings.loadData(cnx);
                        int ret = settings.showDialog();

                        if(ret == 1)
                        {
                            cnx = settings.getConnection();
                            GoogleContactsActivator.getGoogleContactsService().
                                saveConfig(cnx);
                        }
                        else
                        {
                            cnx = null;
                        }
                    }
                }
                else if(cnx.connect() ==
                    GoogleContactsConnection.ConnectionStatus.
                        ERROR_INVALID_CREDENTIALS)
                {
                    synchronized(this)
                    {
                        if(settings != null)
                        {
                            cnx = null;
                            return null;
                        }
                        else
                        {
                            settings = new AccountSettingsForm();
                        }
                    }
                    settings.setModal(true);
                    settings.loadData(cnx);
                    int ret = settings.showDialog();

                    if(ret == 1)
                    {
                        cnx = settings.getConnection();
                        GoogleContactsActivator.getGoogleContactsService().
                            saveConfig(cnx);
                    }
                    else
                    {
                        cnx = null;
                    }
                }
            }
            else
            {
                cnx = null;
            }
        }
        catch(Exception e)
        {
            logger.info("GoogleContacts connection error", e);
            return null;
        }

        return (GoogleContactsConnectionImpl)cnx;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return login;
    }

    /**
     * Returns SEARCH_TYPE to indicate that this contact source
     *
     * @return the identifier of this contact source
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     * @param query the string to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(String query)
    {
        return createContactQuery(
                query,
                GoogleContactsQuery.GOOGLECONTACTS_MAX_RESULTS);
    }

    /**
     *Creates query for the given <tt>queryString</tt>.
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
            pattern = Pattern.compile(
                    Pattern.quote(query));
        }

        if(pattern != null)
        {
            return createContactQuery(pattern, contactCount);
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
     * Notifies this <tt>GoogleContactsSourceService</tt> that a specific
     * <tt>GoogleContactsQuery</tt> has stopped.
     *
     * @param query the <tt>GoogleContactsQuery</tt> which has stopped
     */
    void stopped(GoogleContactsQuery query)
    {
        synchronized (queries)
        {
            if (queries.remove(query))
                queries.notify();
        }
    }

    /**
     * Returns the phoneNumber prefix for all phone numbers.
     *
     * @return the phoneNumber prefix for all phone numbers
     */
    @Override
    public String getPhoneNumberPrefix()
    {
        return phoneNumberprefix;
    }

    /**
     * Sets the phone number prefix.
     *
     * @param phoneNumberprefix the phone number prefix to set
     */
    public void setPhoneNumberPrefix(String phoneNumberprefix)
    {
        this.phoneNumberprefix = phoneNumberprefix;
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
