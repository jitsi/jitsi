/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

/**
 * The <tt>SIPAccountRegistration</tt> is used to store all user input data
 * through the <tt>SIPAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class SIPAccountRegistration {

    private String uin;

    private String password;

    private boolean rememberPassword;

    private String serverAddress;

    private String port;
    
    private String proxy;
    
    private String preferredTransport;
    
    public String getPreferredTransport()
    {
        return preferredTransport;
    }

    public void setPreferredTransport(String preferredTransport)
    {
        this.preferredTransport = preferredTransport;
    }

    public String getProxy()
    {
        return proxy;
    }

    public void setProxy(String proxy)
    {
        this.proxy = proxy;
    }

    /**
     * Returns the password of the sip registration account.
     * @return the password of the sip registration account.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the sip registration account.
     * @param password the password of the sip registration account.
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
     * Sets the rememberPassword value of this sip account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword) {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the sip registration account.
     * @return the UIN of the sip registration account.
     */
    public String getUin() {
        return uin;
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
    public String getPort()
    {
        return port;
    }

    /**
     * Sets the UIN of the sip registration account.
     * @param uin the UIN of the sip registration account.
     */
    public void setUin(String uin) {
        this.uin = uin;
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
    public void setPort(String port)
    {
        this.port = port;
    }
}
