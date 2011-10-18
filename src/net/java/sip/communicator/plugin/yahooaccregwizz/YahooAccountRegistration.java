/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.yahooaccregwizz;

/**
 * The <tt>YahooAccountRegistration</tt> is used to store all user input data
 * through the <tt>YahooAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class YahooAccountRegistration {

    private String uin;

    private String password;

    private boolean rememberPassword = true;

    /**
     * Returns the password of the yahoo registration account.
     * @return the password of the yahoo registration account.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the yahoo registration account.
     * @param password the password of the yahoo registration account.
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
     * Sets the rememberPassword value of this yahoo account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword) {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the yahoo registration account.
     * @return the UIN of the yahoo registration account.
     */
    public String getUin() {
        return uin;
    }

    /**
     * Sets the UIN of the yahoo registration account.
     * @param uin the UIN of the yahoo registration account.
     */
    public void setUserID(String uin) {
        this.uin = uin;
    }

}
