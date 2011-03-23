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
     * get password.
     *
     * @return password to connect to the service
     */
    public String getPassword();

    /**
     * Initialize connection.
     *
     * @return true if connection succeed, false if credentials is wrong
     */
    public boolean connect();
}
