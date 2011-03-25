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
     * @return true if connection succeed, false if credentials is wrong
     */
    public boolean connect();
}
