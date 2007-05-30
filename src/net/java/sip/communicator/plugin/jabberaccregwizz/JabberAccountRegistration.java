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

    private String userID;

    private String password;

    private boolean rememberPassword;

    private String serverAddress;

    private int port;

	private String resource;

	private int priority;

    private boolean sendKeepAlive;

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
     * Is sending of keep alive packets is enabled
     * @return boolean
     */
    public boolean isSendKeepAlive()
    {
        return sendKeepAlive;
    }

    /**
     * Sets the User ID of the jabber registration account.
     * @param userID the UIN of the jabber registration account.
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
