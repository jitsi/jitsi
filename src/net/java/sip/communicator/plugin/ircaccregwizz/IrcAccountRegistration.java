/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ircaccregwizz;

/**
 * The <tt>IrcAccountRegistration</tt> is used to store all user input data
 * through the <tt>IrcAccountRegistrationWizard</tt>.
 *
 * @author Lionel Ferreira & Michael Tarantino
 */
public class IrcAccountRegistration
{
    private String userID;
    private String password;
    private String server;
    private String port;
    private boolean rememberPassword;
    private boolean autoChangeNick;
    private boolean isRequiredPassword;

    /**
     * Returns the User ID of the IRC registration account.
     * 
     * @return the User ID of the IRC registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the IRC registration account.
     * 
     * @param userID the userID of the IRC registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the IRC registration account.
     *
     * @return the password of the IRC registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the IRC registration account.
     *
     * @param password the password of the IRC registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns the server address.
     * 
     * @return the server address.
     */
    public String getServer()
    {
        return server;
    }

    /**
     * Sets the server address.
     * 
     * @param server the address of the server
     */
    public void setServer(String server)
    {
        this.server = server;
    }

    /**
     * Returns the port to use.
     * 
     * @return the port to use
     */
    public String getPort()
    {
        return this.port;
    }

    /**
     * Sets the port to use.
     * 
     * @param port the port to use
     */
    public void setPort(String port)
    {
        this.port = port;
    }

    /**
     * Returns <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Indicates if the nick should be changed automatically in case of nick
     * collision.
     * 
     * @return <code>true</code> if the nick should be changed,
     * <code>false</code> - otherwise.
     */
    public boolean isAutoChangeNick()
    {
        return autoChangeNick;
    }

    /**
     * Sets the property indicating if the nick should be changed automatically
     * in case of nick collision. 
     * @param autoChangeNick <code>true</code> to indicate that the nick could
     * be changed, <code>false</code> - otherwise.
     */
    public void setAutoChangeNick(boolean autoChangeNick)
    {
        this.autoChangeNick = autoChangeNick;
    }

    /**
     * Indicates if the password is required or not. 
     * @return <code>true</code> to indicate that the password is required,*
     * <code>false</code> - otherwise.
     */
    public boolean isRequiredPassword()
    {
        return isRequiredPassword;
    }

    /**
     * Sets the <tt>isRequiredPassword</tt> property.
     * 
     * @param isRequiredPassword <code>true</code> to indicate that the password
     * is required, <code>false</code> - otherwise.
     */
    public void setRequiredPassword(boolean isRequiredPassword)
    {
        this.isRequiredPassword = isRequiredPassword;
    }

    /**
     * Sets the rememberPassword value of this IRC account registration.
     *
     * @param rememberPassword <tt>true</tt> if password has to remembered,
     * <tt>false</tt> otherwise.
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }
}
