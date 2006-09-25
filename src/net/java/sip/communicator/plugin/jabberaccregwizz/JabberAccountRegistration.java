/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

/**
 * The <tt>JabberAccountRegistration</tt> is used to store all user input data
 * through the <tt>JabberAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistration {

    private String uin;

    private String password;

    private boolean rememberPassword;

    private boolean overrideDefOptions;

    private String serverAddress;

    private int port;

    /**
     * Returns the password of the jabber registration account.
     * @return the password of the jabber registration account.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the jabber registration account.
     * @param password the password of the jabber registration account.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword() {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this jabber account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword) {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the jabber registration account.
     * @return the UIN of the jabber registration account.
     */
    public String getUin() {
        return uin;
    }

    /**
     * The address of the server we will use for this account
     * @return String
     */
    public String getServerAddress()
    {
        return serverAddress;
    }

    /**
     * The port on the specified server
     * @return int
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Whether server and port are pointed by the user
     * @return boolean
     */
    public boolean isOverrideDefOptions()
    {
        return overrideDefOptions;
    }

    /**
     * Sets the UIN of the jabber registration account.
     * @param uin the UIN of the jabber registration account.
     */
    public void setUin(String uin) {
        this.uin = uin;
    }

    /**
     * Setting the server
     * @param serverAddress String
     */
    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    /**
     * Setting the port
     * @param port int
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Setting whether user has choosen advance options for server and port
     * @param overrideDefOptions boolean
     */
    public void setOverrideDefOptions(boolean overrideDefOptions)
    {
        this.overrideDefOptions = overrideDefOptions;
    }
}
