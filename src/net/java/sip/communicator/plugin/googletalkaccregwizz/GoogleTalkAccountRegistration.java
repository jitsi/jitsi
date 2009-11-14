/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

/**
 * The <tt>GoogleTalkAccountRegistration</tt> is used to store all user input
 * data through the <tt>GoogleTalkAccountRegistrationWizard</tt>.
 *
 * @author Lubomir Marinov
 */
public class GoogleTalkAccountRegistration
{
    public static final int DEFAULT_PORT = 5222;

    public static final int DEFAULT_PRIORITY = 10;

    public static final String DEFAULT_RESOURCE = "sip-comm";

    static final String GOOGLE_CONNECT_SRV = "talk.google.com";

    static final String GOOGLE_USER_SUFFIX = "gmail.com";

    private String userID;

    private String password;

    private boolean rememberPassword = true;

    private String serverAddress;

    private int port = DEFAULT_PORT;

    private String resource = DEFAULT_RESOURCE;

    private int priority = DEFAULT_PRIORITY;

    private boolean sendKeepAlive = true;

    /**
     * Returns the password of the Google Talk registration account.
     * @return the password of the Google Talk registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the Google Talk registration account.
     * @param password the password of the Google Talk registration account.
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
     * Sets the rememberPassword value of this Google Talk account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the User ID of the Google Talk registration account.
     * @return the User ID of the Google Talk registration account.
     */
    public String getUserID()
    {
        String serverAddress = getServerAddress();

        return
            ((userID != null)
                    && (userID.indexOf('@') < 0)
                    && ((serverAddress == null)
                            || serverAddress.equals(GOOGLE_CONNECT_SRV)))
                ? (userID + '@' + GOOGLE_USER_SUFFIX)
                : userID;
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
     * Is sending of keep alive packets is enabled
     * @return boolean
     */
    public boolean isSendKeepAlive()
    {
        return sendKeepAlive;
    }

    /**
     * Sets the User ID of the Google Talk registration account.
     * @param userID the identifier of the Google Talk registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
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
     * Set whether to send keep alive packets
     * @param sendKeepAlive boolean
     */
    public void setSendKeepAlive(boolean sendKeepAlive)
    {
        this.sendKeepAlive = sendKeepAlive;
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
