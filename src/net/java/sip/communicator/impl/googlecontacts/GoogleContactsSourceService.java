/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
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
    implements ExtendedContactSourceService
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
     * Password.
     */
    private final String password;

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
     * Constructor.
     *
     * @param login login
     * @param password password
     */
    public GoogleContactsSourceService(String login, String password)
    {
        super();
        this.login = login;
        this.password = password;
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
        this.password = cnx.getPassword();
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
     * Queries this search source for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(Pattern queryPattern)
    {
        return queryContactSource(queryPattern,
                GoogleContactsQuery.GOOGLECONTACTS_MAX_RESULTS);
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
        GoogleContactsQuery query = new GoogleContactsQuery(this, queryPattern,
                count);

        synchronized (queries)
        {
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

            if(srvRecords == null)
            {
                return null;
            }

            for(SRVRecord srv : srvRecords)
            {
                if(srv.getTarget().endsWith("google.com") ||
                        srv.getTarget().endsWith("google.com."))
                {
                    isGoogleAppsOrGmail = true;
                    break;
                }
            }

            if(isGoogleAppsOrGmail)
            {
                if(cnx == null)
                {
                    cnx = new GoogleContactsConnectionImpl(login, password);

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
                            cnx = (GoogleContactsConnectionImpl)
                                settings.getConnection();
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
                        cnx = (GoogleContactsConnectionImpl)
                            settings.getConnection();
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
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public String getIdentifier()
    {
        return "GoogleContacts";
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     * @param query the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String query)
    {
        return queryContactSource(
            Pattern.compile(query),
            GoogleContactsQuery.GOOGLECONTACTS_MAX_RESULTS);
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
}
