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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.googlecontacts.configform.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;

import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.contacts.*;
import com.google.gdata.data.*;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.*;

/**
 * Implementation of Google Contacts service.
 * We first get {@link #MAX_RESULT} contacts from Google Contacts then
 * we filter it to get {@link #MAX_RESULT} that matched our query.
 * If {@link #MAX_RESULT} is not reach, we try with additional
 * contacts (if there are more than {@link #MAX_RESULT} contacts).
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsServiceImpl
    implements GoogleContactsService
{
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(GoogleContactsServiceImpl.class);

    /**
     * Google Contacts feed URL string.
     */
    private static final String feedURL =
        "https://www.google.com/m8/feeds/contacts/default/full";

    /**
     * Maximum number of results for a query.
     */
    public static final int MAX_RESULT = 20;

    /**
     * Maximum number of contacts retrieved for a query.
     */
    public static final int MAX_NUMBER = 1000;

    /**
     * List of Google Contacts account.
     */
    private final List<GoogleContactsConnectionImpl> accounts =
        new ArrayList<GoogleContactsConnectionImpl>();

    /**
     * Path where to store the account settings
     */
    final static String CONFIGURATION_PATH =
        "net.java.sip.communicator.impl.googlecontacts";

    /**
     * Constructor.
     */
    public GoogleContactsServiceImpl()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                loadConfig();
            }
        }.start();
    }

    /**
     * Get list of stored connections.
     *
     * @return list of connections
     */
    public List<GoogleContactsConnectionImpl> getAccounts()
    {
        return accounts;
    }

    /**
     * Loads configuration.
     */
    private void loadConfig()
    {
        ConfigurationService configService =
            GoogleContactsActivator.getConfigService();

        List<String> list = configService.getPropertyNamesByPrefix(
                    CONFIGURATION_PATH, true);

        for(Object configEntry : list)
        {
            String path = configEntry.toString();
            Object oen = configService.getProperty(path + ".enabled");
            boolean enabled = Boolean.parseBoolean((String)oen);
            String login =
                (String)configService.getProperty(path + ".account");

            String prefix =
                (String)configService.getProperty(path + ".prefix");
            // If this property doesn't exist, like for old stored accounts, we
            // just need to initialize it to the empty string.
            if(prefix == null)
                prefix = "";

            GoogleContactsConnectionImpl cnx = (GoogleContactsConnectionImpl)
                getConnection(login);
            cnx.setEnabled(enabled);
            cnx.setPrefix(prefix);

            if(cnx != null)
            {
                if(cnx.connect() ==
                    GoogleContactsConnection.ConnectionStatus.
                        ERROR_INVALID_CREDENTIALS)
                {
                    cnx.setEnabled(false);
                    AccountSettingsForm settings = new AccountSettingsForm();
                    settings.setModal(true);
                    settings.loadData(cnx);
                    int ret = settings.showDialog();

                    if(ret == 1)
                    {
                        cnx = (GoogleContactsConnectionImpl)
                            settings.getConnection();
                        // set the enabled state as before
                        cnx.setEnabled(enabled);
                        cnx.setPrefix(prefix);
                        saveConfig(cnx);
                    }
                }

                accounts.add(cnx);

                /* register contact source */
                if(cnx.isEnabled())
                {
                    addContactSource(cnx, true);
                }
            }
        }
    }

    /**
     * Remove a connection.
     *
     * @param cnx connection to save
     */
    public void removeConfig(GoogleContactsConnection cnx)
    {
        ConfigurationService configService =
            GoogleContactsActivator.getConfigService();
        configService.removeProperty(CONFIGURATION_PATH + ".acc" +
                Math.abs(cnx.getLogin().hashCode()));
    }

    /**
     * Save configuration.
     *
     * @param cnx connection to save
     */
    public void saveConfig(GoogleContactsConnection cnx)
    {
        ConfigurationService configService =
            GoogleContactsActivator.getConfigService();

        String login = cnx.getLogin();
        String path = CONFIGURATION_PATH + ".acc" + Math.abs(login.hashCode());

        configService.setProperty(
                path,
                login);
        configService.setProperty(
                path + ".account",
                login);
        configService.setProperty(
                path + ".enabled",
                ((GoogleContactsConnectionImpl)cnx).isEnabled());

        configService.setProperty(
            path + ".prefix",
            ((GoogleContactsConnectionImpl)cnx).getPrefix());
    }

    /**
     * Perform a search for a contact using regular expression.
     *
     * @param cnx <tt>GoogleContactsConnection</tt> to perform the query
     * @param gQuery Google query
     * @param count maximum number of matched contacts
     * @param callback object that will be notified for each new
     * <tt>GoogleContactsEntry</tt> found
     * @return list of <tt>GoogleContactsEntry</tt>
     */
    public List<GoogleContactsEntry> searchContact(
            GoogleContactsConnection cnx, GoogleQuery gQuery, int count,
            GoogleEntryCallback callback)
    {
        URL url = null;
        ContactFeed contactFeed = null;
        ContactQuery query = null;
        List<GoogleContactsEntry> ret = new ArrayList<GoogleContactsEntry>();
        boolean endOfContacts = false;
        int matchedContacts = 0;
        int index = 1;
        GoogleContactsConnectionImpl cnxImpl =
            (GoogleContactsConnectionImpl)cnx;

        if(count <= 0)
        {
            count = MAX_RESULT;
        }

        try
        {
            url = new URL(feedURL);
        }
        catch(MalformedURLException e)
        {
            logger.info("Malformed URL", e);
            return ret;
        }

        if(gQuery.isCancelled())
        {
            return ret;
        }

        while(matchedContacts < count || endOfContacts)
        {
            query = new ContactQuery(url);
            query.setStartIndex(index);
            query.setMaxResults(MAX_NUMBER);
            query.setSortOrder(ContactQuery.SortOrder.DESCENDING);

            if(gQuery.isCancelled())
            {
                return ret;
            }

            try
            {
                contactFeed = cnxImpl.query(query);
            }
            catch(Exception e)
            {
                logger.warn("Problem occurred during Google Contacts query", e);
                return ret;
            }

            if(contactFeed.getEntries().size() == 0)
            {
                endOfContacts = true;
                break;
            }

            for (int i = 0; i < contactFeed.getEntries().size(); i++)
            {
                if(gQuery.isCancelled())
                {
                    return ret;
                }

                ContactEntry entry = contactFeed.getEntries().get(i);

                if(filter(entry, gQuery.getQueryPattern()))
                {
                    GoogleContactsEntry gcEntry = null;

                    gcEntry = getGoogleContactsEntry(entry);
                    matchedContacts++;
                    ret.add(gcEntry);

                    if(callback != null)
                    {
                        callback.callback(gcEntry);
                    }

                    if(matchedContacts >= count)
                    {
                        break;
                    }
                }
            }

            index += contactFeed.getEntries().size();
        }
        return ret;
    }

    /**
     * Filter according to <tt>filter</tt>.
     *
     * @param entry <tt>ContactEntry</tt>
     * @param filter regular expression
     * @return true if entry match the filter, false otherwise
     */
    private boolean filter(ContactEntry entry, Pattern filter)
    {
        Name name = entry.getName();

        /* try to see if name, mail or phone match */

        if(name != null)
        {
            if(name.hasFamilyName())
            {
                Matcher m = filter.matcher(name.getFamilyName().getValue());
                if(m.matches())
                {
                    return true;
                }
            }

            if(name.hasGivenName())
            {
                Matcher m = filter.matcher(name.getGivenName().getValue());
                if(m.find())
                {
                    return true;
                }
            }

            if(name.hasFullName())
            {
                Matcher m = filter.matcher(name.getFullName().getValue());
                if(m.find())
                {
                    return true;
                }
            }
        }

        for(Email mail : entry.getEmailAddresses())
        {
            Matcher m = filter.matcher(mail.getAddress());

            if(m.find())
            {
                return true;
            }
        }

        for(PhoneNumber phone : entry.getPhoneNumbers())
        {
            Matcher m = filter.matcher(phone.getPhoneNumber());

            if(m.find())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a <tt>GoogleContactsEntry</tt> from a <tt>ContactEntry</tt>
     *
     * @param entry <tt>ContactEntry</tt>
     * @return <tt>GoogleContactsEntry</tt>
     */
    private GoogleContactsEntry getGoogleContactsEntry(ContactEntry entry)
    {
        GoogleContactsEntryImpl ret = new GoogleContactsEntryImpl();

        ret.setField(entry);
        return ret;
    }

    /**
     * Get the full contacts list.
     *
     * @return list of <tt>GoogleContactsEntry</tt>
     */
    public List<GoogleContactsEntry> getContacts()
    {
        return null;
    }

    /**
     * Get a <tt>GoogleContactsConnection</tt>.
     *
     * @param login login to connect to the service
     * @return <tt>GoogleContactsConnection</tt>.
     */
    public GoogleContactsConnection getConnection(String login)
    {
        try
        {
            return new GoogleContactsConnectionImpl(login);
        }
        catch(Exception e)
        {
            logger.info("Failed to obtain Google Contacts connection", e);
            return null;
        }
    }

    /**
     * Add a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param cnx <tt>GoogleContactsConnection</tt>.
     * @param googleTalk if the contact source has been created as GoogleTalk
     * account or via external Google Contacts
     */
    public void addContactSource(GoogleContactsConnection cnx,
        boolean googleTalk)
    {
        GoogleContactsActivator.enableContactSource(cnx, googleTalk);
    }

    /**
     * Add a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     */
    public void addContactSource(String login)
    {
        GoogleContactsActivator.enableContactSource(login, false);
    }

    /**
     * Add a contact source service with the specified.
     *
     * <tt>GoogleContactsConnection</tt>.
     * @param cnx <tt>GoogleContactsConnection</tt>.
     */
    public void removeContactSource(GoogleContactsConnection cnx)
    {
        GoogleContactsActivator.disableContactSource(cnx);
    }

    /**
     * Remove a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     */
    public void removeContactSource(String login)
    {
        GoogleContactsActivator.disableContactSource(login);
    }

    /**
     * Retrieve photo of a contact. Adapted from Google sample.
     *
     * @param photoLink photo link
     * @param service
     * @return byte array containing image photo or null if problem happened
     */
    public static byte[] downloadPhoto(ILink photoLink,
            ContactsService service)
    {
        try
        {
            if (photoLink != null)
            {
                GDataRequest request =
                    service.createLinkQueryRequest(photoLink);
                request.execute();
                InputStream in = request.getResponseStream();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];

                for (int read = 0 ; (read = in.read(buffer)) != -1;
                    out.write(buffer, 0, read));

                return out.toByteArray();
            }
        }
        catch(Exception e)
        {
            logger.debug("Failed to retrieve photo of the contact", e);
        }
        return null;
    }
}
