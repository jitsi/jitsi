/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msnaccregwizz;

/**
 * The <tt>MsnAccountRegistration</tt> is used to store all user input data
 * through the <tt>MsnAccountRegistrationWizard</tt>.
 * 
 * @author Yana Stamcheva
 */
public class MsnAccountRegistration
{
    private String id;

    private String password;

    private boolean rememberPassword = true;

    /**
     * Returns the password of the msn registration account.
     * 
     * @return the password of the msn registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the msn registration account.
     * 
     * @param password the password of the msn registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * 
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this msn account registration.
     * 
     * @param rememberPassword TRUE if password has to remembered, FALSE
     *            otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the identifier of the MSN registration account.
     * 
     * @return the identifier of the MSN registration account.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the identifier of the MSN registration account.
     * 
     * @param id the identifier of the MSN registration account.
     */
    public void setUserID(String id)
    {
        this.id = id;
    }

}
