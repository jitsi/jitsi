/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts;

import com.google.gdata.client.contacts.*;

import net.java.sip.communicator.service.googlecontacts.*;

/**
 * Google Contacts credentials to connect to the service.
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsConnectionImpl
    implements GoogleContactsConnection
{
    /**
     * Login.
     */
    private final String login;

    /**
     * Password.
     */
    private final String password;

    /**
     * If the connection is enabled.
     */
    private boolean enabled = false;

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
     * @throws Exception if something goes wrong during connection
     */
    public GoogleContactsConnectionImpl(String login, String password)
        throws Exception
    {
        this.login = login;
        this.password = password;

        googleService.setUserCredentials(login, password);
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
}
