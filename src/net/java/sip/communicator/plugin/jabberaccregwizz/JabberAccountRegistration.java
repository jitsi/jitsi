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
public class JabberAccountRegistration
{
    public static final String DEFAULT_PORT = "5222";

    public static final String DEFAULT_PRIORITY = "10";

    public static final String DEFAULT_RESOURCE = "sip-comm";

    private String userID;

    private String password;

    private boolean rememberPassword = true;

    private String serverAddress;

    private int port = new Integer(DEFAULT_PORT).intValue();

    private String resource = DEFAULT_RESOURCE;

    private int priority = new Integer(DEFAULT_PRIORITY).intValue();

    private boolean sendKeepAlive = true;

    private boolean enableGmailNotification = false;

    /**
     * Returns the password of the jabber registration account.
     * @return the password of the jabber registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the jabber registration account.
     * @param password the password of the jabber registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this jabber account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the User ID of the jabber registration account.
     * @return the User ID of the jabber registration account.
     */
    public String getUserID()
    {
        return userID;
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
     * Determines whether sending of keep alive packets is enabled.
     *
     * @return <tt>true</tt> if keep alive packets are to be sent for this
     * account and <tt>false</tt> otherwise.
     */
    public boolean isSendKeepAlive()
    {
        return sendKeepAlive;
    }

    /**
     * Determines whether SIP Communicator should be querying GMail servers
     * for unread mail messages.
     *
     * @return <tt>true</tt> if we are to enable GMail notifications and
     * <tt>false</tt> otherwise.
     */
    public boolean isGmailNotificationEnabled()
    {
        return enableGmailNotification;
    }

    /**
     * Sets the User ID of the jabber registration account.
     *
     * @param userID the identifier of the jabber registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Sets the server
     *
     * @param serverAddress the IP address or FQDN of the server.
     */
    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    /**
     * Sets the server port number.
     *
     * @param port the server port number
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Specifies whether SIP Communicator should send send keep alive packets
     * to keep this account registered.
     *
     * @param sendKeepAlive <tt>true</tt> if we are to send keep alive packets
     * and <tt>false</tt> otherwise.
     */
    public void setSendKeepAlive(boolean sendKeepAlive)
    {
        this.sendKeepAlive = sendKeepAlive;
    }

    /**
     * Specifies whether SIP Communicator should be querying GMail servers
     * for unread mail messages.
     *
     * @param enabled <tt>true</tt> if we are to enable GMail notification and
     * <tt>false</tt> otherwise.
     */
    public void setGmailNotificationEnabled(boolean enabled)
    {
        this.enableGmailNotification = enabled;
    }

    public String getResource()
    {
        return resource;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }
}
