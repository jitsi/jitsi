/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

/**
 * The <tt>IcqAccountRegistration</tt> is used to store all user input data
 * through the <tt>IcqAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class IcqAccountRegistration
{
    private String uin;

    private String password;

    private boolean rememberPassword = true;

    private String proxyPort;

    private String proxy;

    private String proxyType = "http";

    private String proxyUsername;

    private String proxyPassword;

    /**
     * Returns the password of the icq registration account.
     * @return the password of the icq registration account.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the icq registration account.
     * @param password the password of the icq registration account.
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
     * Sets the rememberPassword value of this icq account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword) {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the icq registration account.
     * @return the UIN of the icq registration account.
     */
    public String getUin() {
        return uin;
    }

    /**
     * Sets the UIN of the icq registration account.
     * @param uin the UIN of the icq registration account.
     */
    public void setUin(String uin) {
        this.uin = uin;
    }

    /**
     * Returns the proxy that will be used for this icq account.
     * @return the proxy that will be used for this icq account.
     */
    public String getProxy() {
        return proxy;
    }

    /**
     * Sets the proxy for this icq account.
     * @param proxy the proxy for this icq account.
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns the proxy port that will be used for this icq account.
     * @return the proxy port that will be used for this icq account.
     */
    public String getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets the proxy port for this icq account.
     * @param proxyPort the proxy port for this icq account.
     */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Returns the proxy type that will be used for this icq account.
     * @return the proxy type that will be used for this icq account.
     */
    public String getProxyType() {
        return proxyType;
    }

    /**
     * Sets the proxy type for this icq account.
     * @param proxyType the proxy type for this icq account
     */
    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    /**
     * Returns the proxy password of the icq registration account.
     * @return the proxy password of the icq registration account.
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Sets the proxy password of the icq registration account.
     * @param password the proxy password of the icq registration account.
     */
    public void setProxyPassword(String password) {
        this.proxyPassword = password;
    }

    /**
    * Returns the proxy username of the icq registration account.
    * @return the proxy username of the icq registration account.
    */
   public String getProxyUsername() {
       return proxyUsername;
   }

   /**
    * Sets the proxy username of the icq registration account.
    * @param username the proxy username of the icq registration account
    */
   public void setProxyUsername(String username) {
       this.proxyUsername = username;
   }
}
