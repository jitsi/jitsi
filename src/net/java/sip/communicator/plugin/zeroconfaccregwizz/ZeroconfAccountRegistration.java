/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

/**
 * The <tt>ZeroconfAccountRegistration</tt> is used to store 
 * all user input data 
 * through the <tt>ZeroconfAccountRegistrationWizard</tt>.
 * 
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ZeroconfAccountRegistration
{
    private String userID;
    private String first;
    private String last;
    private String mail;
    private boolean rememberContacts;

    /**
     * Returns the User ID of the zeroconf registration account.
     * @return the User ID of the zeroconf registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the zeroconf registration account.
     * @param userID the userID of the zeroconf registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the Zeroconf registration account.
     * @return the password of the Zeroconf registration account.
     */
    public String getFirst()
    {
        return first;
    }

    /**
     * Sets the password of the Zeroconf registration account.
     * @param first first name
     */
    public void setFirst(String first)
    {
        this.first = first;
    }

    /**
     * Returns <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     * @return <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     */
    public boolean isRememberContacts()
    {
        return rememberContacts;
    }

    /**
     * Sets the rememberPassword value of this Zeroconf account registration.
     * @param rememberContacts true if we want to remember the 
     *        contacts we meet, false otherwise
     */
    public void setRememberContacts(boolean rememberContacts)
    {
        this.rememberContacts = rememberContacts;
    }

    /**
     * Returns the last name
     * @return last name
     */
    public String getLast()
    {
        return last;
    }

    /**
     * Sets the last name
     * @param last last name
     */
    public void setLast(String last)
    {
        this.last = last;
    }

    /**
     * Returns the mail address
     * @return mail address
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Sets the mail address
     * @param mail mail address
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }
    

}
