/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
/**
 * The <tt>RssAccountRegistration</tt> is used to store all user input data
 * through the <tt>RssAccountRegistrationWizard</tt>.
 *
 * @author Emil Ivov/Jean-Albert Vescovo
 */
package net.java.sip.communicator.plugin.rssaccregwizz;

public class RssAccountRegistration
{
    private String userID;
    private String password;
    private boolean rememberPassword;

    /**
     * Returns the User ID of the rss registration account.
     * @return the User ID of the rss registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the rss registration account.
     * @param userID the userID of the rss registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the Rss registration account.
     *
     * @return the password of the Rss registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the Rss registration account.
     *
     * @param password the password of the Rss registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
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
        return true;
    }

    /**
     * Sets the rememberPassword value of this Rss account registration.
     *
     * @param rememberPassword <tt>true</tt> if password has to remembered,
     * <tt>false</tt> otherwise.
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = true;
    }

}
