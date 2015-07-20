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

import net.java.sip.communicator.impl.googlecontacts.OAuth2TokenStore.FailedAcquireCredentialException;
import net.java.sip.communicator.impl.googlecontacts.OAuth2TokenStore.FailedTokenRefreshException;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.util.*;

import com.google.gdata.client.contacts.*;
import com.google.gdata.data.contacts.*;
import com.google.gdata.util.*;

/**
 * Google Contacts credentials to connect to the service.
 *
 * @author Sebastien Vincent
 * @author Danny van Heumen
 */
public class GoogleContactsConnectionImpl
    implements GoogleContactsConnection
{
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(GoogleContactsConnectionImpl.class);

    /**
     * The credential store to pass around.
     */
    private final OAuth2TokenStore store = new OAuth2TokenStore();

    /**
     * Login.
     */
    private String login = null;

    /**
     * If the connection is enabled.
     */
    private boolean enabled = false;

    /**
     * The google contact prefix.
     */
    private String prefix = null;

    /**
     * Google Contacts service.
     */
    private final ContactsService googleService =
        new ContactsService("GoogleContacts service for Jitsi");

    /**
     * Constructor.
     *
     * @param login the login to connect to the service
     */
    public GoogleContactsConnectionImpl(String login)
    {
        this.login = login;
        googleService.useSsl();
    }

    /**
     * Returns the Google service.
     *
     * @return the Google service
     */
    public ContactsService getGoogleService()
    {
        return googleService;
    }

    /**
     * Get login.
     *
     * @return login to connect to the service
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * Set login.
     *
     * @param login login to connect to the service
     */
    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * Initialize connection.
     *
     * @return connection status
     */
    public synchronized ConnectionStatus connect()
    {
        try
        {
            googleService.setOAuth2Credentials(this.store.get(this.login));
            return ConnectionStatus.SUCCESS;
        }
        catch (FailedAcquireCredentialException e)
        {
            logger.error("Failed to acquire credentials.", e);
            return ConnectionStatus.ERROR_UNKNOWN;
        }
    }

    /**
     * Query for contacts using provided ContactQuery.
     *
     * Executes query. In case of failure, refresh OAuth2 token and retry query.
     * If query fails again, throws FailedContactQueryException.
     *
     * @param query the contact query
     * @return Returns the contact feed with matching contacts.
     * @throws IOException
     * @throws ServiceException
     * @throws FailedContactQueryException Throws in case of failed query.
     * @throws FailedTokenRefreshException Throws in case refreshing OAuth2
     *             token fails.
     */
    public synchronized ContactFeed query(final ContactQuery query)
        throws IOException,
        ServiceException,
        FailedContactQueryException,
        FailedTokenRefreshException
    {
        try
        {
            return this.googleService.query(query, ContactFeed.class);
        }
        catch (NullPointerException e)
        {
            // Don't include a stack trace, since this is will happen at start
            // of Jitsi, as we do not have a valid access token available yet.
            logger.info("Executing query failed with NPE. "
                + "Refreshing access token and trying again.");
            // Maybe we should request an access token immediately after loading
            // the refresh token from the credentials store?
            this.store.refresh();
        }
        catch (Exception e)
        {
            // Catch all and retry with refreshed token. We may need to let this
            // case go through.
            logger.warn("Query failed with unexpected exception. Going to try "
                + "refreshing token anyways ...", e);
            this.store.refresh();
        }
        try
        {
            return this.googleService.query(query, ContactFeed.class);
        }
        catch (Exception e)
        {
            throw new FailedContactQueryException(e);
        }
    }

    /**
     * Returns if the connection is enabled.
     *
     * @return true if connection is enabled, false otherwise
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Set the connection to be enabled or not.
     *
     * @param enabled value to set
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Sets the google contacts prefix.
     *
     * @param prefix the phone number prefix to set
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Returns the google contacts phone number prefix.
     *
     * @return the google contacts phone number prefix
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Exception for signaling failed contact query.
     *
     * @author Danny van Heumen
     */
    public static class FailedContactQueryException
        extends Exception
    {
        private static final long serialVersionUID = -5451421392081973669L;

        private FailedContactQueryException(Throwable cause)
        {
            super("Failed to query Google Contacts API.", cause);
        }
    }
}
