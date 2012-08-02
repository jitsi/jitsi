/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
    implements SecurityAccountRegistration
{
    public static String DEFAULT_PORT = "5060";

    public static String DEFAULT_TLS_PORT = "5061";

    public static String DEFAULT_POLL_PERIOD = "30";

    public static String DEFAULT_SUBSCRIBE_EXPIRES = "3600";

    private String defaultKeepAliveMethod = "OPTIONS";

    public static String DEFAULT_KEEP_ALIVE_INTERVAL = "25";

    private String defaultDTMFMethod = "AUTO_DTMF";

    private String id;

    private String password;

    private boolean rememberPassword = true;

    private String tlsClientCertificate;

    private String defaultServerAddress = null;

    private String serverAddress;

    private String displayName;

    private String authorizationName;

    private String defaultServerPort = null;

    private String serverPort = null;

    private boolean defaultProxyAutoConfigure = true;

    private boolean proxyAutoConfigure = defaultProxyAutoConfigure;

    private String defaultProxyPort = null;

    private String proxyPort = null;

    private String defaultProxy = null;

    private String proxy;

    private String defaultTransport = "UDP";

    private String preferredTransport = defaultTransport;

    private boolean enablePresence = true;

    private boolean forceP2PMode = false;

    /**
     * Enables support to encrypt calls.
     */
    private boolean defaultEncryption = true;

    /**
     * Enqbles ZRTP encryption.
     */
    private boolean sipZrtpAttribute = true;

    /**
     * Tells if SDES is enabled for this SIP account.
     */
    private boolean sdesEnabled = false;

    private int savpOption = 0;

    /**
     * The list of cipher suites enabled for SDES.
     */
    private String sdesCipherSuites = null;

    private String pollingPeriod = DEFAULT_POLL_PERIOD;

    private String subscriptionExpiration = DEFAULT_SUBSCRIBE_EXPIRES;

    private String keepAliveMethod = null;

    private String keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;

    private String dtmfMethod = null;

    private String defaultDomain = null;

    private boolean xCapEnable = false;

    private boolean xivoEnable = false;

    private boolean clistOptionUseSipCredentials = true;

    private String clistOptionServerUri;

    private String clistOptionUser;

    private String clistOptionPassword;

    /**
     * The voicemail uri if any.
     */
    private String voicemailURI;

    /**
     * The voicemail check uri if any.
     */
    private String voicemailCheckURI;

    /**
     * Whether message waiting indications is enabled.
     */
    private boolean messageWaitingIndications = true;

    public String getPreferredTransport()
    {
        return preferredTransport;
    }

    public void setPreferredTransport(String preferredTransport)
    {
        this.preferredTransport = preferredTransport;
    }

    public String getDefaultProxy()
    {
        return defaultProxy;
    }

    public void setDefaultProxy(String proxy)
    {
        if(proxy != null && proxy.length() == 0)
            this.defaultProxy = null;
        else
            this.defaultProxy = proxy;
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
     * The default value of address of the server we will use for this account
     *
     * @return String
     */
    public String getDefaultServerAddress()
    {
        return defaultServerAddress;
    }

    /**
     * The port on the specified server
     *
     * @return int
     */
    public String getDefaultServerPort()
    {
        return defaultServerPort;
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
     * The default port on the specified proxy
     *
     * @return int
     */
    public String getDefaultProxyPort()
    {
        return defaultProxyPort;
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
     * Sets default the server
     *
     * @param serverAddress String
     */
    public void setDefaultServerAddress(String serverAddress)
    {
        if(serverAddress != null && serverAddress.length() == 0)
            this.defaultServerAddress = null;
        else
            this.defaultServerAddress = serverAddress;
    }

    /**
     * Sets the server port.
     *
     * @param port int
     */
    public void setDefaultServerPort(String port)
    {
        if(port != null && port.length() == 0)
            this.defaultServerPort = null;
        else
            this.defaultServerPort = port;
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
     * Sets the default proxy port.
     *
     * @param port int
     */
    public void setDefaultProxyPort(String port)
    {
        if(port != null && port.length() == 0)
            this.defaultProxyPort = null;
        else
            this.defaultProxyPort = port;
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
     * Returns the DTMF method.
     *
     * @return the DTMF method.
     */
    public String getDTMFMethod()
    {
        return dtmfMethod;
    }

    /**
     * Sets the DTMF method.
     *
     * @param dtmfMethod the DTMF method to set
     */
    public void setDTMFMethod(String dtmfMethod)
    {
        this.dtmfMethod = dtmfMethod;
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
     * Check if to include the ZRTP attribute to SIP/SDP
     *
     * @return include the ZRTP attribute to SIP/SDP
     */
    public boolean isSipZrtpAttribute()
    {
        return sipZrtpAttribute;
    }

    /**
     * Sets SIP ZRTP attribute support
     *
     * @param sipZrtpAttribute include the ZRTP attribute to SIP/SDP
     */
    public void setSipZrtpAttribute(boolean sipZrtpAttribute)
    {
        this.sipZrtpAttribute = sipZrtpAttribute;
    }

    /**
     * Tells if SDES is enabled for this SIP account.
     *
     * @return True if SDES is enabled. False, otherwise.
     */
    public boolean isSDesEnabled()
    {
        return sdesEnabled;
    }

    /**
     * Enables or disables SDES for this SIP account.
     *
     * @param sdesEnabled True to enable SDES. False, otherwise.
     */
    public void setSDesEnabled(boolean sdesEnabled)
    {
        this.sdesEnabled = sdesEnabled;
    }

    /**
     * Returns the list of cipher suites enabled for SDES.
     *
     * @return The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
     */
    public String getSDesCipherSuites()
    {
        return sdesCipherSuites;
    }

    /**
     * Sets the list of cipher suites enabled for SDES.
     *
     * @param The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
     */
    public void setSDesCipherSuites(String cipherSuites)
    {
        this.sdesCipherSuites = cipherSuites;
    }

    /**
     * Gets the method used for RTP/SAVP indication.
     *
     * @return The method used for RTP/SAVP indication.
     */
    public int getSavpOption()
    {
        return savpOption;
    }

    /**
     * Sets the method used for RTP/SAVP indication.
     */
    public void setSavpOption(int savpOption)
    {
        this.savpOption = savpOption;
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
     * @return the defaultDTMFMethod
     */
    public String getDefaultDTMFMethod()
    {
        return defaultDTMFMethod;
    }

    /**
     * @param defaultDTMFMethod the defaultDTMFMethod to set
     */
    public void setDefaultDTMFMethod(String defaultDTMFMethod)
    {
        this.defaultDTMFMethod = defaultDTMFMethod;
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
     * Checks if XiVO option is enabled.
     *
     * @return true if XiVO is enabled otherwise false.
     */
    public boolean isXiVOEnable()
    {
        return xivoEnable;
    }
    /**
     * Sets if XiVO option is enable.
     *
     * @param xivoEnable XiVO enable.
     */
    public void setXiVOEnable(boolean xivoEnable)
    {
        this.xivoEnable = xivoEnable;
    }

    /**
     * Checks if XCAP has to use SIP account credentials.
     *
     * @return true if XCAP has to use SIP account credentials otherwise false.
     */
    public boolean isClistOptionUseSipCredentials()
    {
        return clistOptionUseSipCredentials;
    }

    /**
     * Sets if contact list has to use SIP account credentials.
     *
     * @param clistOptionUseSipCredentials if the clist has
     * to use SIP account credentials.
     */
    public void setClistOptionUseSipCredentials(
        boolean clistOptionUseSipCredentials)
    {
        this.clistOptionUseSipCredentials = clistOptionUseSipCredentials;
    }

    /**
     * Gets the contact list server uri.
     *
     * @return the contact list  server uri.
     */
    public String getClistOptionServerUri()
    {
        return clistOptionServerUri;
    }

    /**
     * Sets the contact list server uri.
     *
     * @param clistOptionServerUri the contact list server uri.
     */
    public void setClistOptionServerUri(String clistOptionServerUri)
    {
        this.clistOptionServerUri = clistOptionServerUri;
    }

    /**
     * Gets the contact list user.
     *
     * @return the contact list user.
     */
    public String getClistOptionUser()
    {
        return clistOptionUser;
    }

    /**
     * Sets the contact list user.
     *
     * @param clistOptionUser the contact list user.
     */
    public void setClistOptionUser(String clistOptionUser)
    {
        this.clistOptionUser = clistOptionUser;
    }

    /**
     * Gets the contact list password.
     *
     * @return the contact list password.
     */
    public String getClistOptionPassword()
    {
        return clistOptionPassword;
    }

    /**
     * Sets the contact list password.
     *
     * @param clistOptionPassword the contact list password.
     */
    public void setClistOptionPassword(String clistOptionPassword)
    {
        this.clistOptionPassword = clistOptionPassword;
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
     * Is proxy auto configured by default.
     * @return
     */
    public boolean isDefaultProxyAutoConfigure()
    {
        return defaultProxyAutoConfigure;
    }

    /**
     * Sets default auto configuration of proxy enabled or disabled.
     * @param proxyAutoConfigure
     */
    public void setDefaultProxyAutoConfigure(boolean proxyAutoConfigure)
    {
        this.defaultProxyAutoConfigure = proxyAutoConfigure;
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

    /**
     * The voicemail check URI.
     * @return the voicemail URI.
     */
    public String getVoicemailCheckURI()
    {
        return voicemailCheckURI;
    }

    /**
     * Sets voicemail check URI.
     * @param voicemailCheckURI new URI.
     */
    public void setVoicemailCheckURI(String voicemailCheckURI)
    {
        this.voicemailCheckURI = voicemailCheckURI;
    }

    /**
     * Check if messageWaitingIndications is enabled
     *
     * @return if messageWaitingIndications is enabled
     */
    public boolean isMessageWaitingIndicationsEnabled()
    {
        return messageWaitingIndications;
    }

    /**
     * Sets message waiting indications.
     *
     * @param messageWaitingIndications
     */
    public void setMessageWaitingIndications(boolean messageWaitingIndications)
    {
        this.messageWaitingIndications = messageWaitingIndications;
    }
}
