/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

/**
 * The <tt>SIPAccountRegistration</tt> is used to store all user input data
 * through the <tt>SIPAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 * @author Grigorii Balutsel
 */
public class SIPAccountRegistration
{
    public static String DEFAULT_PORT = "5060";

    public static String DEFAULT_TLS_PORT = "5061";

    public static String DEFAULT_POLL_PERIOD = "30";

    public static String DEFAULT_SUBSCRIBE_EXPIRES = "3600";

    private String defaultKeepAliveMethod = "OPTIONS";

    public static String DEFAULT_KEEP_ALIVE_INTERVAL = "25";

    private String id;

    private String password;

    private boolean rememberPassword = true;

    private String tlsClientCertificate;

    private String serverAddress;

    private String displayName;

    private String authorizationName;

    private String serverPort = null;

    private boolean proxyAutoConfigure = true;

    private String proxyPort = null;

    private String proxy;

    private String defaultTransport = "UDP";

    private String preferredTransport = defaultTransport;

    private boolean enablePresence = true;

    private boolean forceP2PMode = false;

    private boolean defaultEncryption = true;

    private boolean sipZrtpAttribute = true;

    private String pollingPeriod = DEFAULT_POLL_PERIOD;

    private String subscriptionExpiration = DEFAULT_SUBSCRIBE_EXPIRES;

    private String keepAliveMethod = null;

    private String keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;

    private String defaultDomain = null;

    private boolean xCapEnable = false;

    private boolean xCapUseSipCredetials = true;

    private String xCapServerUri;

    private String xCapUser;

    private String xCapPassword;

    /**
     * The voicemail uri if any.
     */
    private String voicemailURI;

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
        if(proxy != null && proxy.length() == 0)
            this.proxy = null;
        else
            this.proxy = proxy;
    }

    /**
     * Returns the password of the sip registration account.
     *
     * @return the password of the sip registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the sip registration account.
     *
     * @param password the password of the sip registration account.
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
     * Sets the rememberPassword value of this sip account registration.
     *
     * @param rememberPassword TRUE if password has to remembered, FALSE
     *            otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Gets the ID of the client certificate configuration.
     * @return the ID of the client certificate configuration.
     */
    public String getTlsClientCertificate()
    {
        return tlsClientCertificate;
    }

    /**
     * Sets the ID of the client certificate configuration.
     * @param id the client certificate configuration template ID.
     */
    public void setTlsClientCertificate(String id)
    {
        tlsClientCertificate = id;
    }

    /**
     * Returns the UIN of the sip registration account.
     *
     * @return the UIN of the sip registration account.
     */
    public String getId()
    {
        return id;
    }

    /**
     * The address of the server we will use for this account
     *
     * @return String
     */
    public String getServerAddress()
    {
        return serverAddress;
    }

    /**
     * The port on the specified server
     *
     * @return int
     */
    public String getServerPort()
    {
        return serverPort;
    }

    /**
     * The display name
     *
     * @return String display name
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * The authorization name
     *
     * @return String auth name
     */
    public String getAuthorizationName()
    {
        return authorizationName;
    }

    /**
     * The port on the specified proxy
     *
     * @return int
     */
    public String getProxyPort()
    {
        return proxyPort;
    }

    /**
     * Sets the identifier of the sip registration account.
     *
     * @param id the identifier of the sip registration account.
     */
    public void setUserID(String id)
    {
        this.id = id;
    }

    /**
     * Sets the server
     *
     * @param serverAddress String
     */
    public void setServerAddress(String serverAddress)
    {
        if(serverAddress != null && serverAddress.length() == 0)
            this.serverAddress = null;
        else
            this.serverAddress = serverAddress;
    }

    /**
     * Sets the server port.
     *
     * @param port int
     */
    public void setServerPort(String port)
    {
        if(port != null && port.length() == 0)
            this.serverPort = null;
        else
            this.serverPort = port;
    }

    /**
     * Sets the display name.
     *
     * @param displayName String
     */
    public void setDisplayName(String displayName)
    {
        if(displayName != null && displayName.length() == 0)
            this.displayName = null;
        else
            this.displayName = displayName;
    }

    /**
     * Sets authorization name.
     *
     * @param authName String
     */
    public void setAuthorizationName(String authName)
    {
        if(authName != null && authName.length() == 0)
            this.authorizationName = null;
        else
            this.authorizationName = authName;
    }

    /**
     * Sets the proxy port.
     *
     * @param port int
     */
    public void setProxyPort(String port)
    {
        if(port != null && port.length() == 0)
            this.proxyPort = null;
        else
            this.proxyPort = port;
    }

    /**
     * If the presence is enabled
     *
     * @return If the presence is enabled
     */
    public boolean isEnablePresence()
    {
        return enablePresence;
    }

    /**
     * If the p2p mode is forced
     *
     * @return If the p2p mode is forced
     */
    public boolean isForceP2PMode()
    {
        return forceP2PMode;
    }

    /**
     * The offline contact polling period
     *
     * @return the polling period
     */
    public String getPollingPeriod()
    {
        return pollingPeriod;
    }

    /**
     * The default expiration of subscriptions
     *
     * @return the subscription expiration
     */
    public String getSubscriptionExpiration()
    {
        return subscriptionExpiration;
    }

    /**
     * Sets if the presence is enabled
     *
     * @param enablePresence if the presence is enabled
     */
    public void setEnablePresence(boolean enablePresence)
    {
        this.enablePresence = enablePresence;
    }

    /**
     * Sets if we have to force the p2p mode
     *
     * @param forceP2PMode if we have to force the p2p mode
     */
    public void setForceP2PMode(boolean forceP2PMode)
    {
        this.forceP2PMode = forceP2PMode;
    }

    /**
     * Sets the offline contacts polling period
     *
     * @param pollingPeriod the offline contacts polling period
     */
    public void setPollingPeriod(String pollingPeriod)
    {
        this.pollingPeriod = pollingPeriod;
    }

    /**
     * Sets the subscription expiration value
     *
     * @param subscriptionExpiration the subscription expiration value
     */
    public void setSubscriptionExpiration(String subscriptionExpiration)
    {
        this.subscriptionExpiration = subscriptionExpiration;
    }

    /**
     * Returns the keep alive method.
     *
     * @return the keep alive method.
     */
    public String getKeepAliveMethod()
    {
        return keepAliveMethod;
    }

    /**
     * Sets the keep alive method.
     *
     * @param keepAliveMethod the keep alive method to set
     */
    public void setKeepAliveMethod(String keepAliveMethod)
    {
        this.keepAliveMethod = keepAliveMethod;
    }

    /**
     * Returns the keep alive interval.
     *
     * @return the keep alive interval
     */
    public String getKeepAliveInterval()
    {
        return keepAliveInterval;
    }

    /**
     * Sets the keep alive interval.
     *
     * @param keepAliveInterval the keep alive interval to set
     */
    public void setKeepAliveInterval(String keepAliveInterval)
    {
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * If default call encryption is enabled
     *
     * @return If default call encryption is enabled
     */
    public boolean isDefaultEncryption()
    {
        return defaultEncryption;
    }

    /**
     * Sets default call encryption
     *
     * @param defaultEncryption if we want to set call encryption on as default
     */
    public void setDefaultEncryption(boolean defaultEncryption)
    {
        this.defaultEncryption = defaultEncryption;
    }

    /**
     * Sets SIP ZRTP attribute support
     *
     * @param sipZrtpAttribute include the ZRTP attribute to SIP/SDP
     */
    public void setSipZrtpAttribute(boolean sipZrtpAttribute) {
        this.sipZrtpAttribute = sipZrtpAttribute;
    }


    /**
     * Check if to include the ZRTP attribute to SIP/SDP
     *
     * @return include the ZRTP attribute to SIP/SDP
     */
    public boolean isSipZrtpAttribute() {
        return sipZrtpAttribute;
    }

    /**
     * This is the default domain.
     * @return the defaultDomain
     */
    public String getDefaultDomain()
    {
        return defaultDomain;
    }

    /**
     * If default domain is set this means we cannot create registerless
     * accounts through this wizard. And every time we write only the username,
     * will will end up with username@defaultDomain.
     * 
     * @param defaultDomain the defaultDomain to set
     */
    public void setDefaultDomain(String defaultDomain)
    {
        this.defaultDomain = defaultDomain;
    }

    /**
     * @return the defaultKeepAliveMethod
     */
    public String getDefaultKeepAliveMethod()
    {
        return defaultKeepAliveMethod;
    }

    /**
     * @param defaultKeepAliveMethod the defaultKeepAliveMethod to set
     */
    public void setDefaultKeepAliveMethod(String defaultKeepAliveMethod)
    {
        this.defaultKeepAliveMethod = defaultKeepAliveMethod;
    }

    /**
     * @return the defaultTransport
     */
    public String getDefaultTransport()
    {
        return defaultTransport;
    }

    /**
     * @param defaultTransport the defaultTransport to set
     */
    public void setDefaultTransport(String defaultTransport)
    {
        this.defaultTransport = defaultTransport;
    }

    /**
     * Checks if XCAP is enabled.
     *
     * @return true if XCAP is enabled otherwise false.
     */
    public boolean isXCapEnable()
    {
        return xCapEnable;
    }
    /**
     * Sets if XCAP is enable.
     *
     * @param xCapEnable XCAP enable.
     */
    public void setXCapEnable(boolean xCapEnable)
    {
        this.xCapEnable = xCapEnable;
    }

    /**
     * Checks if XCAP has to use SIP account credetials.
     *
     * @return true if XCAP has to use SIP account credetials otherwise false.
     */
    public boolean isXCapUseSipCredetials()
    {
        return xCapUseSipCredetials;
    }

    /**
     * Sets if XCAP has to use SIP account credetials.
     *
     * @param xCapUseSipCredetials if XCAP has to use SIP account credetials.
     */
    public void setXCapUseSipCredetials(boolean xCapUseSipCredetials)
    {
        this.xCapUseSipCredetials = xCapUseSipCredetials;
    }

    /**
     * Gets the XCAP server uri.
     *
     * @return the XCAP server uri.
     */
    public String getXCapServerUri()
    {
        return xCapServerUri;
    }

    /**
     * Sets the XCAP server uri.
     *
     * @param xCapServerUri the XCAP server uri.
     */
    public void setXCapServerUri(String xCapServerUri)
    {
        this.xCapServerUri = xCapServerUri;
    }

    /**
     * Gets the XCAP user.
     *
     * @return the XCAP user.
     */
    public String getXCapUser()
    {
        return xCapUser;
    }

    /**
     * Sets the XCAP user.
     *
     * @param xCapUser the XCAP user.
     */
    public void setXCapUser(String xCapUser)
    {
        this.xCapUser = xCapUser;
    }

    /**
     * Gets the XCAP password.
     *
     * @return the XCAP password.
     */
    public String getXCapPassword()
    {
        return xCapPassword;
    }

    /**
     * Sets the XCAP password.
     *
     * @param xCapPassword the XCAP password.
     */
    public void setXCapPassword(String xCapPassword)
    {
        this.xCapPassword = xCapPassword;
    }

    /**
     * Is proxy auto configured.
     * @return
     */
    public boolean isProxyAutoConfigure()
    {
        return proxyAutoConfigure;
    }

    /**
     * Sets auto configuration of proxy enabled or disabled.
     * @param proxyAutoConfigure
     */
    public void setProxyAutoConfigure(boolean proxyAutoConfigure)
    {
        this.proxyAutoConfigure = proxyAutoConfigure;
    }

    /**
     * The voicemail URI.
     * @return the voicemail URI.
     */
    public String getVoicemailURI()
    {
        return voicemailURI;
    }

    /**
     * Sets voicemail URI.
     * @param voicemailURI new URI.
     */
    public void setVoicemailURI(String voicemailURI)
    {
        this.voicemailURI = voicemailURI;
    }
}
