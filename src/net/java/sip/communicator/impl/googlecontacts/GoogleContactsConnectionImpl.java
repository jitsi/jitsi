/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts;

import com.google.gdata.client.*;
import com.google.gdata.client.contacts.*;
import com.google.gdata.util.*;

import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.util.*;

/**
 * Google Contacts credentials to connect to the service.
 *
 * @author Sebastien Vincent
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
    public ConnectionStatus connect()
    {
        try
        {
            googleService.setUserCredentials(login, password);
        }
        catch(AuthenticationException e)
        {
            logger.info("Google contacts connection failure: " + e);
            if(e instanceof GoogleService.InvalidCredentialsException)
            {
                return ConnectionStatus.ERROR_INVALID_CREDENTIALS;
            }
            else
            {
                return ConnectionStatus.ERROR_UNKNOWN;
            }
        }

        return ConnectionStatus.SUCCESS;
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
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Returns the google contacts prefix.
     *
     * @return the google contacts prefix
     */
    public String getPrefix()
    {
        return prefix;
    }
}
