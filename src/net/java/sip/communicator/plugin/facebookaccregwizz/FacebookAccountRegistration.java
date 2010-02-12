/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

/**
 * The <tt>FacebookAccountRegistration</tt> is used to store all user input data
 * through the <tt>FacebookAccountRegistrationWizard</tt>.
 *
 * @author Dai Zhiwei
 */
public class FacebookAccountRegistration
{
    private String username;
    private String password;
    private boolean rememberPassword;

    /**
     * Returns the username of the facebook registration account.
     * @return the username of the facebook registration account.
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Sets the username of the facebook registration account.
     * @param username the userID of the facebook registration account.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the password of the Facebook registration account.
     *
     * @return the password of the Facebook registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the Facebook registration account.
     *
     * @param password the password of the Facebook registration account.
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
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this Facebook account registration.
     *
     * @param rememberPassword <tt>true</tt> if password has to remembered,
     * <tt>false</tt> otherwise.
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }
}
