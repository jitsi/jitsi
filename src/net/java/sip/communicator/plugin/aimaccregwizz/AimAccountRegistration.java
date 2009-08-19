/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.aimaccregwizz;

/**
 * The <tt>AimAccountRegistration</tt> is used to store all user input data
 * through the <tt>AimAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class AimAccountRegistration
{
    private String uin;

    private String password;

    /**
     * The indicator which determines whether the password of this account
     * registration is remembered for later automatic use or it is to be
     * acquired from the user whenever necessary.
     * <p>
     * The default value is <tt>true</tt> because it reflects the default
     * behavior of the application for other protocols. Otherwise, creating an
     * AIM account through the simple account registration form (the
     * multi-protocol entry form which appears at startup when there are no
     * accounts) will result in immediately asking for the password again as
     * soon as the simple account registration form is closed while it will not
     * happen for other types of accounts.
     * </p>
     */
    private boolean rememberPassword = true;

    private String proxyPort;

    private String proxy;

    private String proxyType = "http";

    private String proxyUsername;

    private String proxyPassword;

    /**
     * Returns the password of the aim registration account.
     * @return the password of the aim registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the aim registration account.
     * @param password the password of the aim registration account.
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
     *         otherwise
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this aim account registration.
     * 
     * @param rememberPassword
     *            <tt>true</tt> if password has to remembered, <tt>false</tt>
     *            otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the aim registration account.
     * @return the UIN of the aim registration account.
     */
    public String getUin()
    {
        return uin;
    }

    /**
     * Sets the UIN of the aim registration account.
     * @param uin the UIN of the aim registration account.
     */
    public void setUin(String uin)
    {
        this.uin = uin;
    }

    /**
     * Returns the proxy that will be used for this aim account.
     * @return the proxy that will be used for this aim account.
     */
    public String getProxy()
    {
        return proxy;
    }

    /**
     * Sets the proxy for this aim account.
     * @param proxy the proxy for this aim account.
     */
    public void setProxy(String proxy)
    {
        this.proxy = proxy;
    }

    /**
     * Returns the proxy port that will be used for this aim account.
     * @return the proxy port that will be used for this aim account.
     */
    public String getProxyPort()
    {
        return proxyPort;
    }

    /**
     * Sets the proxy port for this aim account.
     * @param proxyPort the proxy port for this aim account.
     */
    public void setProxyPort(String proxyPort)
    {
        this.proxyPort = proxyPort;
    }

    /**
     * Returns the proxy type that will be used for this aim account.
     * @return the proxy type that will be used for this aim account.
     */
    public String getProxyType()
    {
        return proxyType;
    }

    /**
     * Sets the proxy type for this aim account.
     * @param proxyType the proxy type for this aim account
     */
    public void setProxyType(String proxyType)
    {
        this.proxyType = proxyType;
    }

    /**
     * Returns the proxy password of the aim registration account.
     * @return the proxy password of the aim registration account.
     */
    public String getProxyPassword()
    {
        return proxyPassword;
    }

    /**
     * Sets the proxy password of the aim registration account.
     * @param password the proxy password of the aim registration account.
     */
    public void setProxyPassword(String password)
    {
        this.proxyPassword = password;
    }

    /**
    * Returns the proxy username of the aim registration account.
    * @return the proxy username of the aim registration account.
    */
   public String getProxyUsername()
   {
       return proxyUsername;
   }

   /**
    * Sets the proxy username of the aim registration account.
    * @param username the proxy username of the aim registration account
    */
   public void setProxyUsername(String username)
   {
       this.proxyUsername = username;
   }
}
