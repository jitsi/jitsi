/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.googlecontacts;

/**
 * Interface that define a Google Contacts connection.
 *
 * @author Sebastien Vincent
 */
public interface GoogleContactsConnection
{
    /**
     * Enumeration for connection status.
     *
     */
    public enum ConnectionStatus
    {
        /**
         * Connection has failed due to invalid credentials.
         */
        ERROR_INVALID_CREDENTIALS,

        /**
         * Connection has failed due to unknown reason.
         */
        ERROR_UNKNOWN,

        /**
         * Connection has succeed.
         */
        SUCCESS;
    }

    /**
     * Get login.
     *
     * @return login to connect to the service
     */
    public String getLogin();

    /**
     * Get password.
     *
     * @return password to connect to the service
     */
    public String getPassword();

    /**
     * Set login.
     *
     * @param login login to connect to the service
     */
    public void setLogin(String login);

    /**
     * Set password.
     *
     * @param password password to connect to the service
     */
    public void setPassword(String password);

    /**
     * Initialize connection.
     *
     * @return connection status
     */
    public ConnectionStatus connect();

    /**
     * Returns the google contacts prefix.
     *
     * @return the google contacts prefix
     */
    public String getPrefix();
}
