/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * Password.
     */
    private String password = null;

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
     * @param password the password to connect to the service
     */
    public GoogleContactsConnectionImpl(String login, String password)
    {
        this.login = login;
        this.password = password;
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
     * get password.
     *
     * @return password to connect to the service
     */
    public String getPassword()
    {
        return password;
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
     * Set password.
     *
     * @param password password to connect to the service
     */
    public void setPassword(String password)
    {
        this.password = password;
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
            googleService.setOAuth2Credentials(this.store.get());
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
        catch (Exception e)
        {
            // FIXME if possible narrow down the exceptions on which to
            // refresh token
            logger.info("Failed to execute query. Going to refresh token"
                + " and try again.", e);
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
