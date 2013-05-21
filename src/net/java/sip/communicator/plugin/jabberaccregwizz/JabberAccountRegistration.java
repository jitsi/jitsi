/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.wizard.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;

import org.osgi.framework.*;

/**
 * The <tt>JabberAccountRegistration</tt> is used to store all user input data
 * through the <tt>JabberAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 * @author Boris Grozev
 */
public class JabberAccountRegistration
{
    /**
     * The default value of server port for jabber accounts.
     */
    public static final String DEFAULT_PORT = "5222";

    /**
     * Account suffix for Google service.
     */
    private static final String GOOGLE_USER_SUFFIX = "gmail.com";

    /**
     * XMPP server for Google service.
     */
    private static final String GOOGLE_CONNECT_SRV = "talk.google.com";

    /**
     * The default value of the priority property.
     */
    public static final String DEFAULT_PRIORITY = "30";

    /**
     * The default value of the resource property.
     */
    public static final String DEFAULT_RESOURCE = "jitsi";

    /**
     * The default value of stun server port for jabber accounts.
     */
    public static final String DEFAULT_STUN_PORT = "3478";

    /**
     * Default value for resource auto generating.
     */
    public static final boolean DEFAULT_RESOURCE_AUTOGEN = true;

    /**
     * The default value for DTMF method.
     */
    private String defaultDTMFMethod = "AUTO_DTMF";

    /**
     * The default value of minimale DTMF tone duration.
     */
    public static String DEFAULT_MINIMAL_DTMF_TONE_DURATION = Integer.toString(
            OperationSetDTMF.DEFAULT_DTMF_MINIMAL_TONE_DURATION);

    /**
     * The user identifier.
     */
    private String userID;

    /**
     * The password.
     */
    private String password;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean rememberPassword = true;

    /**
     * The server address.
     */
    private String serverAddress;

    /**
     * The default domain.
     */
    private String defaultUserSufix;

    /**
     * The override domain for phone call.
     *
     * If Jabber account is able to call PSTN number and if domain name of the
     * switch is different than the domain of the account (gw.domain.org vs
     * domain.org), you can use this property to set the switch domain.
     */
    private String overridePhoneSuffix = null;

    /**
     * Always call with gtalk property.
     *
     * It is used to bypass capabilities checks: some softwares do not advertise
     * GTalk support (but they support it).
     */
    private boolean bypassGtalkCaps = false;

    /**
     * Domain name that will bypass GTalk caps.
     */
    private String domainBypassCaps = null;

    /**
     * Is jingle disabled for this account.
     */
    private boolean disableJingle = false;

    /**
     * The port.
     */
    private int port = new Integer(DEFAULT_PORT).intValue();

    /**
     * The resource property, initialized to the default resource.
     */
    private String resource = DEFAULT_RESOURCE;

    /**
     * The priority property.
     */
    private int priority = new Integer(DEFAULT_PRIORITY).intValue();

    /**
     * Indicates if keep alive packets should be send.
     */
    private boolean sendKeepAlive = true;

    /**
     * Indicates if gmail notifications should be enabled.
     */
    private boolean enableGmailNotification = false;

    /**
     * Indicates if Google Contacts should be enabled.
     */
    private boolean enableGoogleContacts = false;

    /**
     * Indicates if ICE should be used.
     */
    private boolean isUseIce = false;

    /**
     * Indicates if Google ICE should be used.
     */
    private boolean isUseGoogleIce = false;

    /**
     * Indicates if STUN server should be automatically discovered.
     */
    private boolean isAutoDiscoverStun = false;

    /**
     * Indicates if default STUN server should be used.
     */
    private boolean isUseDefaultStunServer = false;

    /**
     * The list of additional STUN servers entered by user.
     */
    private List<StunServerDescriptor> additionalStunServers
        = new ArrayList<StunServerDescriptor>();

    /**
     * Indicates if JingleNodes relays should be used.
     */
    private boolean isUseJingleNodes = false;

    /**
     * Indicates if JingleNodes relay server should be automatically discovered.
     */
    private boolean isAutoDiscoverJingleNodes = false;

    /**
     * The list of additional JingleNodes (tracker or relay) entered by user.
     */
    private List<JingleNodeDescriptor> additionalJingleNodes
        = new ArrayList<JingleNodeDescriptor>();

    /**
     * Indicates if UPnP should be used.
     */
    private boolean isUseUPNP = false;

    /**
     * If non-TLS connection is allowed.
     */
    private boolean isAllowNonSecure = false;

    /**
     * Indicates if the server is overriden.
     */
    private boolean isServerOverridden = false;

    /**
     * Is resource auto generate enabled.
     */
    private boolean resourceAutogenerated = DEFAULT_RESOURCE_AUTOGEN;

    /**
     * The account display name.
     */
    private String accountDisplayName;

    /**
     * The sms default server.
     */
    private String smsServerAddress;

    /**
     * DTMF method.
     */
    private String dtmfMethod = null;

    /**
     * The minimal DTMF tone duration set.
     */
    private String dtmfMinimalToneDuration = DEFAULT_MINIMAL_DTMF_TONE_DURATION;

    /**
     * The client TLS certificate ID.
     */
    private String clientCertificateId = null;

    /**
     * The encodings registration object
     */
    private EncodingsRegistrationUtil encodingsRegistration
            = new EncodingsRegistrationUtil();

    /**
     * The security registration object
     */
    private SecurityAccountRegistration securityRegistration
            = new SecurityAccountRegistration()
    {
        /**
         * Sets the method used for RTP/SAVP indication.
         */
        @Override
        public void setSavpOption(int savpOption)
        {
            // SAVP option is not useful for XMPP account.
            // Thereby, do nothing.
        }

        /**
         * RTP/SAVP is disabled for Jabber protocol.
         *
         * @return Always <tt>ProtocolProviderFactory.SAVP_OFF</tt>.
         */
        @Override
        public int getSavpOption()
        {
            return ProtocolProviderFactory.SAVP_OFF;
        }
    };

    /**
     * Initializes a new JabberAccountRegistration.
     */
    public JabberAccountRegistration()
    {
        super();
    }

    /**
     * Returns the password of the jabber registration account.
     * @return the password of the jabber registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the jabber registration account.
     * @param password the password of the jabber registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this jabber account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the User ID of the jabber registration account.
     * @return the User ID of the jabber registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Returns the user sufix.
     *
     * @return the user sufix
     */
    public String getDefaultUserSufix()
    {
        return defaultUserSufix;
    }

    /**
     * Returns the override phone suffix.
     *
     * @return the phone suffix
     */
    public String getOverridePhoneSuffix()
    {
        return overridePhoneSuffix;
    }

    /**
     * Returns the alwaysCallWithGtalk value.
     *
     * @return the alwaysCallWithGtalk value
     */
    public boolean getBypassGtalkCaps()
    {
        return bypassGtalkCaps;
    }

    /**
     * Returns telephony domain that bypass GTalk caps.
     *
     * @return telephony domain
     */
    public String getTelephonyDomainBypassCaps()
    {
        return domainBypassCaps;
    }

    /**
     * Gets if Jingle is disabled for this account.
     *
     * @return True if jingle is disabled for this account. False otherwise.
     */
    public boolean isJingleDisabled()
    {
        return this.disableJingle;
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
     * @return the server port
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Determines whether sending of keep alive packets is enabled.
     *
     * @return <tt>true</tt> if keep alive packets are to be sent for this
     * account and <tt>false</tt> otherwise.
     */
    public boolean isSendKeepAlive()
    {
        return sendKeepAlive;
    }

    /**
     * Determines whether SIP Communicator should be querying Gmail servers
     * for unread mail messages.
     *
     * @return <tt>true</tt> if we are to enable Gmail notifications and
     * <tt>false</tt> otherwise.
     */
    public boolean isGmailNotificationEnabled()
    {
        return enableGmailNotification;
    }

    /**
     * Determines whether SIP Communicator should use Google Contacts as
     * ContactSource
     *
     * @return <tt>true</tt> if we are to enable Google Contacts and
     * <tt>false</tt> otherwise.
     */
    public boolean isGoogleContactsEnabled()
    {
        return enableGoogleContacts;
    }

    /**
     * Sets the User ID of the jabber registration account.
     *
     * @param userID the identifier of the jabber registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Sets the default value of the user sufix.
     *
     * @param userSufix the user name sufix (the domain name after the @ sign)
     */
    public void setDefaultUserSufix(String userSufix)
    {
        this.defaultUserSufix = userSufix;
    }

    /**
     * Sets the override value of the phone suffix.
     *
     * @param phoneSuffix the phone name suffix (the domain name after the @
     * sign)
     */
    public void setOverridePhoneSufix(String phoneSuffix)
    {
        this.overridePhoneSuffix = phoneSuffix;
    }

    /**
     * Sets value for alwaysCallWithGtalk.
     *
     * @param bypassGtalkCaps true to enable, false otherwise
     */
    public void setBypassGtalkCaps(boolean bypassGtalkCaps)
    {
        this.bypassGtalkCaps = bypassGtalkCaps;
    }

    /**
     * Sets telephony domain that bypass GTalk caps.
     *
     * @param text telephony domain to set
     */
    public void setTelephonyDomainBypassCaps(String text)
    {
        this.domainBypassCaps = text;
    }

    /**
     * Sets if Jingle is disabled for this account.
     *
     * @param True if jingle is disabled for this account. False otherwise.
     */
    public void setDisableJingle(boolean disabled)
    {
        this.disableJingle = disabled;
    }

    /**
     * Sets the server
     *
     * @param serverAddress the IP address or FQDN of the server.
     */
    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    /**
     * Indicates if the server address has been overridden.
     *
     * @return <tt>true</tt> if the server address has been overridden,
     * <tt>false</tt> - otherwise.
     */
    public boolean isServerOverridden()
    {
        return isServerOverridden;
    }

    /**
     * Sets <tt>isServerOverridden</tt> property.
     * @param isServerOverridden indicates if the server is overridden
     */
    public void setServerOverridden(boolean isServerOverridden)
    {
        this.isServerOverridden = isServerOverridden;
    }

    /**
     * Sets the server port number.
     *
     * @param port the server port number
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Specifies whether SIP Communicator should send send keep alive packets
     * to keep this account registered.
     *
     * @param sendKeepAlive <tt>true</tt> if we are to send keep alive packets
     * and <tt>false</tt> otherwise.
     */
    public void setSendKeepAlive(boolean sendKeepAlive)
    {
        this.sendKeepAlive = sendKeepAlive;
    }

    /**
     * Specifies whether SIP Communicator should be querying Gmail servers
     * for unread mail messages.
     *
     * @param enabled <tt>true</tt> if we are to enable Gmail notification and
     * <tt>false</tt> otherwise.
     */
    public void setGmailNotificationEnabled(boolean enabled)
    {
        this.enableGmailNotification = enabled;
    }

    /**
     * Specifies whether SIP Communicator should use Google Contacts as
     * ContactSource.
     *
     * @param enabled <tt>true</tt> if we are to enable Google Contacts and
     * <tt>false</tt> otherwise.
     */
    public void setGoogleContactsEnabled(boolean enabled)
    {
        this.enableGoogleContacts = enabled;
    }

    /**
     * Returns the resource.
     * @return the resource
     */
    public String getResource()
    {
        return resource;
    }

    /**
     * Sets the resource.
     * @param resource the resource for the jabber account
     */
    public void setResource(String resource)
    {
        this.resource = resource;
    }

    /**
     * Returns the priority property.
     * @return priority
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Sets the priority property.
     * @param priority the priority to set
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * Indicates if ice should be used for this account.
     * @return <tt>true</tt> if ICE should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    public boolean isUseIce()
    {
        return isUseIce;
    }

    /**
     * Sets the <tt>useIce</tt> property.
     * @param isUseIce <tt>true</tt> to indicate that ICE should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    public void setUseIce(boolean isUseIce)
    {
        this.isUseIce = isUseIce;
    }

    /**
     * Indicates if ice should be used for this account.
     * @return <tt>true</tt> if ICE should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    public boolean isUseGoogleIce()
    {
        return isUseGoogleIce;
    }

    /**
     * Sets the <tt>useGoogleIce</tt> property.
     * @param isUseIce <tt>true</tt> to indicate that ICE should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    public void setUseGoogleIce(boolean isUseIce)
    {
        this.isUseGoogleIce = isUseIce;
    }

    /**
     * Indicates if the stun server should be automatically discovered.
     * @return <tt>true</tt> if the stun server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    public boolean isAutoDiscoverStun()
    {
        return isAutoDiscoverStun;
    }

    /**
     * Sets the <tt>autoDiscoverStun</tt> property.
     * @param isAutoDiscover <tt>true</tt> to indicate that stun server should
     * be auto-discovered, <tt>false</tt> - otherwise.
     */
    public void setAutoDiscoverStun(boolean isAutoDiscover)
    {
        this.isAutoDiscoverStun = isAutoDiscover;
    }

    /**
     * Indicates if the stun server should be automatically discovered.
     * @return <tt>true</tt> if the stun server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    public boolean isUseDefaultStunServer()
    {
        return isUseDefaultStunServer;
    }

    /**
     * Sets the <tt>useDefaultStunServer</tt> property.
     * @param isUseDefaultStunServer <tt>true</tt> to indicate that default
     * stun server should be used if no others are available, <tt>false</tt>
     * otherwise.
     */
    public void setUseDefaultStunServer(boolean isUseDefaultStunServer)
    {
        this.isUseDefaultStunServer = isUseDefaultStunServer;
    }

    /**
     * Adds the given <tt>stunServer</tt> to the list of additional stun servers.
     *
     * @param stunServer the <tt>StunServer</tt> to add
     */
    public void addStunServer(StunServerDescriptor stunServer)
    {
        additionalStunServers.add(stunServer);
    }

    /**
     * Returns the <tt>List</tt> of all additional stun servers entered by the
     * user. The list is guaranteed not to be <tt>null</tt>.
     *
     * @return the <tt>List</tt> of all additional stun servers entered by the
     * user.
     */
    public List<StunServerDescriptor> getAdditionalStunServers()
    {
        return additionalStunServers;
    }

    /**
     * Sets the <tt>autoDiscoverJingleNodes</tt> property.
     *
     * @param isAutoDiscover <tt>true</tt> to indicate that relay server should
     * be auto-discovered, <tt>false</tt> - otherwise.
     */
    public void setAutoDiscoverJingleNodes(boolean isAutoDiscover)
    {
        this.isAutoDiscoverJingleNodes = isAutoDiscover;
    }

    /**
     * Indicates if the JingleNodes relay server should be automatically
     * discovered.
     *
     * @return <tt>true</tt> if the relay server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    public boolean isAutoDiscoverJingleNodes()
    {
        return isAutoDiscoverJingleNodes;
    }

    /**
     * Sets the <tt>useJingleNodes</tt> property.
     *
     * @param isUseJingleNodes <tt>true</tt> to indicate that Jingle Nodes
     * should be used for this account, <tt>false</tt> - otherwise.
     */
    public void setUseJingleNodes(boolean isUseJingleNodes)
    {
        this.isUseJingleNodes = isUseJingleNodes;
    }

    /**
     * Sets the <tt>useJingleNodes</tt> property.
     *
     * @param isUseJingleNodes <tt>true</tt> to indicate that JingleNodes relays
     * should be used for this account, <tt>false</tt> - otherwise.
     */
    public void isUseJingleNodes(boolean isUseJingleNodes)
    {
        this.isUseJingleNodes = isUseJingleNodes;
    }

    /**
     * Indicates if JingleNodes relay should be used.
     *
     * @return <tt>true</tt> if JingleNodes should be used, <tt>false</tt>
     * otherwise
     */
    public boolean isUseJingleNodes()
    {
        return isUseJingleNodes;
    }

    /**
     * Adds the given <tt>node</tt> to the list of additional JingleNodes.
     *
     * @param node the <tt>node</tt> to add
     */
    public void addJingleNodes(JingleNodeDescriptor node)
    {
        additionalJingleNodes.add(node);
    }

    /**
     * Returns the <tt>List</tt> of all additional stun servers entered by the
     * user. The list is guaranteed not to be <tt>null</tt>.
     *
     * @return the <tt>List</tt> of all additional stun servers entered by the
     * user.
     */
    public List<JingleNodeDescriptor> getAdditionalJingleNodes()
    {
        return additionalJingleNodes;
    }

    /**
     * Indicates if UPnP should be used for this account.
     * @return <tt>true</tt> if UPnP should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    public boolean isUseUPNP()
    {
        return isUseUPNP;
    }

    /**
     * Sets the <tt>useUPNP</tt> property.
     * @param isUseUPNP <tt>true</tt> to indicate that UPnP should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    public void setUseUPNP(boolean isUseUPNP)
    {
        this.isUseUPNP = isUseUPNP;
    }

    /**
     * Indicates if non-TLS is allowed for this account
     * @return <tt>true</tt> if non-TLS is allowed for this account, otherwise
     * returns <tt>false</tt>
     */
    public boolean isAllowNonSecure()
    {
        return isAllowNonSecure;
    }

    /**
     * Sets the <tt>isAllowNonSecure</tt> property.
     * @param isAllowNonSecure <tt>true</tt> to indicate that non-TLS is allowed
     * for this account, <tt>false</tt> - otherwise.
     */
    public void setAllowNonSecure(boolean isAllowNonSecure)
    {
        this.isAllowNonSecure = isAllowNonSecure;
    }

    /**
     * Is resource auto generate enabled.
     *
     * @return true if resource is auto generated
     */
    public boolean isResourceAutogenerated()
    {
        return resourceAutogenerated;
    }

    /**
     * Set whether resource autogenerate is enabled.
     * @param resourceAutogenerated
     */
    public void setResourceAutogenerated(boolean resourceAutogenerated)
    {
        this.resourceAutogenerated = resourceAutogenerated;
    }

    /**
     * Returns the account display name.
     *
     * @return the account display name
     */
    public String getAccountDisplayName()
    {
        return accountDisplayName;
    }

    /**
     * Sets the account display name.
     *
     * @param accountDisplayName the account display name
     */
    public void setAccountDisplayName(String accountDisplayName)
    {
        this.accountDisplayName = accountDisplayName;
    }

    /**
     * Returns the default sms server.
     *
     * @return the account default sms server
     */
    public String getSmsServerAddress()
    {
        return smsServerAddress;
    }

    /**
     * Sets the default sms server.
     *
     * @param serverAddress the sms server to set as default
     */
    public void setSmsServerAddress(String serverAddress)
    {
        this.smsServerAddress = serverAddress;
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
     * Returns the minimal DTMF tone duration.
     *
     * @return The minimal DTMF tone duration.
     */
    public String getDtmfMinimalToneDuration()
    {
        return dtmfMinimalToneDuration;
    }

    /**
     * Sets the minimal DTMF tone duration.
     *
     * @param dtmfMinimalToneDuration The minimal DTMF tone duration to set.
     */
    public void setDtmfMinimalToneDuration(String dtmfMinimalToneDuration)
    {
        this.dtmfMinimalToneDuration = dtmfMinimalToneDuration;
    }

    /**
     * Returns <tt>EncodingsRegistrationUtil</tt> object which stores encodings
     * configuration.
     * @return <tt>EncodingsRegistrationUtil</tt> object which stores encodings
     * configuration.
     */
    public EncodingsRegistrationUtil getEncodingsRegistration()
    {
        return encodingsRegistration;
    }

    /**
     * Returns <tt>SecurityAccountRegistration</tt> object which stores security
     * settings.
     * @return <tt>SecurityAccountRegistration</tt> object which stores security
     * settings.
     */
    public SecurityAccountRegistration getSecurityRegistration()
    {
        return securityRegistration;
    }

    /**
     * Sets the client certificate configuration entry ID.
     * @param clientCertificateId the client certificate configuration entry ID.
     */
    public void setClientCertificateId(String clientCertificateId)
    {
        this.clientCertificateId = clientCertificateId;
    }

    /**
     * Gets the client certificate configuration entry ID.
     * @returns the client certificate configuration entry ID.
     */
    public String getClientCertificateId()
    {
        return clientCertificateId;
    }

    /**
     * Stores Jabber account configuration held by this registration object into
     * given<tt>accountProperties</tt> map.
     *
     * @param userName the user name that will be used.
     * @param passwd the password for this account.
     * @param protocolIconPath the path to protocol icon if used, or
     * <tt>null</tt> otherwise.
     * @param accountIconPath the path to account icon if used, or
     * <tt>null</tt> otherwise.
     * @param accountProperties the map used for storings account properties.
     *
     * @throws OperationFailedException if properties are invalid.
     */
    public void storeProperties(String userName, String passwd,
                                String protocolIconPath,
                                String accountIconPath,
                                Map<String, String> accountProperties)
            throws OperationFailedException
    {
        if (protocolIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                                    protocolIconPath);

        if (accountIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                    accountIconPath);

        if (isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        //accountProperties.put("SEND_KEEP_ALIVE",
        //                      String.valueOf(isSendKeepAlive()));

        accountProperties.put("GMAIL_NOTIFICATIONS_ENABLED",
                              String.valueOf(isGmailNotificationEnabled()));
        accountProperties.put("GOOGLE_CONTACTS_ENABLED",
                              String.valueOf(isGoogleContactsEnabled()));

        String serverName = null;
        if (getServerAddress() != null
                && getServerAddress().length() > 0)
        {
            serverName = getServerAddress();
        }
        else
        {
            serverName = getServerFromUserName(userName);
        }

        if(isServerOverridden())
        {
            accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(true));
        }
        else
        {
            accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(false));
        }

        if (serverName == null || serverName.length() <= 0)
            throw new OperationFailedException(
                    "Should specify a server for user name " + userName + ".",
                    OperationFailedException.SERVER_NOT_SPECIFIED);

        if(userName.indexOf('@') < 0 && getDefaultUserSufix() != null)
            userName = userName + '@' + getDefaultUserSufix();

        if(getOverridePhoneSuffix() != null)
        {
            accountProperties.put("OVERRIDE_PHONE_SUFFIX",
                                  getOverridePhoneSuffix());
        }

        accountProperties.put(
                ProtocolProviderFactory.IS_CALLING_DISABLED_FOR_ACCOUNT,
                Boolean.toString(isJingleDisabled()));

        accountProperties.put("BYPASS_GTALK_CAPABILITIES",
                              String.valueOf(getBypassGtalkCaps()));

        if(getTelephonyDomainBypassCaps() != null)
        {
            accountProperties.put("TELEPHONY_BYPASS_GTALK_CAPS",
                                  getTelephonyDomainBypassCaps());
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                              serverName);

        String smsServerAddress = getSmsServerAddress();

        String clientCertId = getClientCertificateId();
        if(clientCertId != null)
        {
            accountProperties.put(
                    ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE,
                    clientCertId);
        }
        else
        {
            accountProperties.remove(
                    ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
        }

        if (smsServerAddress != null)
        {
            accountProperties.put(  ProtocolProviderFactory.SMS_SERVER_ADDRESS,
                                    smsServerAddress);
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                              String.valueOf(getPort()));

        accountProperties.put(ProtocolProviderFactory.AUTO_GENERATE_RESOURCE,
                              String.valueOf(isResourceAutogenerated()));

        accountProperties.put(ProtocolProviderFactory.RESOURCE,
                              getResource());

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY,
                              String.valueOf(getPriority()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_ICE,
                              String.valueOf(isUseIce()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_GOOGLE_ICE,
                              String.valueOf(isUseGoogleIce()));

        accountProperties.put(ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                              String.valueOf(isAutoDiscoverStun()));

        accountProperties.put(ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                              String.valueOf(isUseDefaultStunServer()));

        String accountDisplayName = getAccountDisplayName();

        if (accountDisplayName != null && accountDisplayName.length() > 0)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
                                    accountDisplayName);

        List<StunServerDescriptor> stunServers = getAdditionalStunServers();

        int serverIndex = -1;

        for(StunServerDescriptor stunServer : stunServers)
        {
            serverIndex ++;

            stunServer.storeDescriptor(
                    accountProperties,
                    ProtocolProviderFactory.STUN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_JINGLE_NODES,
                              String.valueOf(isUseJingleNodes()));

        accountProperties.put(
                ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES,
                String.valueOf(isAutoDiscoverJingleNodes()));

        List<JingleNodeDescriptor> jnRelays = getAdditionalJingleNodes();

        serverIndex = -1;
        for(JingleNodeDescriptor jnRelay : jnRelays)
        {
            serverIndex ++;

            jnRelay.storeDescriptor(accountProperties,
                                    JingleNodeDescriptor.JN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_UPNP,
                              String.valueOf(isUseUPNP()));

        accountProperties.put(ProtocolProviderFactory.IS_ALLOW_NON_SECURE,
                              String.valueOf(isAllowNonSecure()));

        if(getDTMFMethod() != null)
            accountProperties.put("DTMF_METHOD",
                                  getDTMFMethod());
        else
            accountProperties.put("DTMF_METHOD",
                                  getDefaultDTMFMethod());

        accountProperties.put(
                ProtocolProviderFactory.DTMF_MINIMAL_TONE_DURATION,
                getDtmfMinimalToneDuration());

        securityRegistration.storeProperties(accountProperties);

        encodingsRegistration.storeProperties(accountProperties);
    }

    /**
     * Fills this registration object with configuration properties from given
     * <tt>account</tt>.
     * @param account the account object that will be used.
     * @param bundleContext the OSGi bundle context required for some
     * operations.
     */
    public void loadAccount(AccountID account, BundleContext bundleContext)
    {
        Map<String, String> accountProperties = account.getAccountProperties();

        String password
            = ProtocolProviderFactory.getProtocolProviderFactory(
                    bundleContext,
                    ProtocolNames.JABBER).loadPassword(account);

        setRememberPassword(false);
        setUserID(account.getUserID());

        if (password != null)
        {
            setPassword(password);
            setRememberPassword(true);
        }

        String serverAddress
                = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        setServerAddress(serverAddress);

        setClientCertificateId(
                account.getAccountPropertyString(
                        ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE));

        String serverPort
                = accountProperties.get(ProtocolProviderFactory.SERVER_PORT);

        if(StringUtils.isNullOrEmpty(serverPort))
            serverPort = JabberAccountRegistration.DEFAULT_PORT;

        setPort(new Integer(serverPort));

        boolean keepAlive
            = Boolean.parseBoolean(accountProperties.get("SEND_KEEP_ALIVE"));

        setSendKeepAlive(keepAlive);

        boolean gmailNotificationEnabled
                = Boolean.parseBoolean(
                        accountProperties.get("GMAIL_NOTIFICATIONS_ENABLED"));

        setGmailNotificationEnabled(gmailNotificationEnabled);

        String useGC = accountProperties.get("GOOGLE_CONTACTS_ENABLED");

        boolean googleContactsEnabled = Boolean.parseBoolean(
                (useGC != null && useGC.length() != 0) ? useGC : "true");

        setGoogleContactsEnabled(googleContactsEnabled);

        String resource
                = accountProperties.get(ProtocolProviderFactory.RESOURCE);

        setResource(resource);

        String autoGenerateResourceValue = accountProperties.get(
                ProtocolProviderFactory.AUTO_GENERATE_RESOURCE);

        boolean autoGenerateResource =
                JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN;

        if(autoGenerateResourceValue != null)
            autoGenerateResource = Boolean.parseBoolean(
                autoGenerateResourceValue);

        setResourceAutogenerated(autoGenerateResource);

        String priority
            = accountProperties.get(ProtocolProviderFactory.RESOURCE_PRIORITY);

        if(StringUtils.isNullOrEmpty(priority))
            priority = JabberAccountRegistration.DEFAULT_PRIORITY;

        setPriority(new Integer(priority));

        String dtmfMethod = account.getAccountPropertyString("DTMF_METHOD");

        setDTMFMethod(dtmfMethod);

        String dtmfMinimalToneDuration
            = account.getAccountPropertyString("DTMF_MINIMAL_TONE_DURATION");
        setDtmfMinimalToneDuration(dtmfMinimalToneDuration);

        //Security properties
        securityRegistration.loadAccount(account);

        // ICE
        String useIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_ICE);
        boolean isUseIce = Boolean.parseBoolean(
                (useIce != null && useIce.length() != 0) ? useIce : "true");

        setUseIce(isUseIce);

        String useGoogleIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_GOOGLE_ICE);
        boolean isUseGoogleIce = Boolean.parseBoolean(
                (useGoogleIce != null && useGoogleIce.length() != 0) ?
                    useGoogleIce : "true");

        setUseGoogleIce(isUseGoogleIce);

        String useAutoDiscoverStun
                = accountProperties.get(
                        ProtocolProviderFactory.AUTO_DISCOVER_STUN);
        boolean isUseAutoDiscoverStun = Boolean.parseBoolean(
                (useAutoDiscoverStun != null &&
                        useAutoDiscoverStun.length() != 0) ?
                                useAutoDiscoverStun : "true");

        setAutoDiscoverStun(isUseAutoDiscoverStun);

        String useDefaultStun
                = accountProperties.get(
                ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER);
        boolean isUseDefaultStun = Boolean.parseBoolean(
                (useDefaultStun != null &&
                        useDefaultStun.length() != 0) ?
                                useDefaultStun : "true");

        setUseDefaultStunServer(isUseDefaultStun);

        this.additionalStunServers.clear();
        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                    = StunServerDescriptor.loadDescriptor(
                    accountProperties, ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            String stunPassword = loadStunPassword(
                    bundleContext,
                    account,
                    ProtocolProviderFactory.STUN_PREFIX + i);

            if(stunPassword != null)
            {
                stunServer.setPassword(stunPassword);
            }

            addStunServer(stunServer);
        }

        String useJN =
            accountProperties.get(ProtocolProviderFactory.IS_USE_JINGLE_NODES);
        boolean isUseJN = Boolean.parseBoolean(
            (useJN != null && useJN.length() != 0) ? useJN : "true");

        setUseJingleNodes(isUseJN);

        String useAutoDiscoverJN
                = accountProperties.get(
                        ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES);
        boolean isUseAutoDiscoverJN = Boolean.parseBoolean(
                (useAutoDiscoverJN != null &&
                        useAutoDiscoverJN.length() != 0) ?
                                useAutoDiscoverJN : "true");

        setAutoDiscoverJingleNodes(isUseAutoDiscoverJN);

        this.additionalJingleNodes.clear();
        for (int i = 0; i < JingleNodeDescriptor.MAX_JN_RELAY_COUNT ; i ++)
        {
            JingleNodeDescriptor jn
                = JingleNodeDescriptor.loadDescriptor(
                    accountProperties, JingleNodeDescriptor.JN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (jn == null)
                break;

            addJingleNodes(jn);
        }

        String useUPNP =
                accountProperties.get(ProtocolProviderFactory.IS_USE_UPNP);
        boolean isUseUPNP = Boolean.parseBoolean(
                (useUPNP != null && useUPNP.length() != 0) ? useUPNP : "true");

        setUseUPNP(isUseUPNP);

        String allowNonSecure =
            accountProperties.get(ProtocolProviderFactory.IS_ALLOW_NON_SECURE);
        boolean isAllowNonSecure = Boolean.parseBoolean(
                (allowNonSecure != null && allowNonSecure.length() != 0)
                ? allowNonSecure : "false");

        setAllowNonSecure(isAllowNonSecure);

        boolean isServerOverriden =
                account.getAccountPropertyBoolean(
                        ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                        false);

        setServerOverridden(isServerOverriden);

        boolean disabledJingle = Boolean.parseBoolean(accountProperties.get(
                ProtocolProviderFactory.IS_CALLING_DISABLED_FOR_ACCOUNT));
        setDisableJingle(disabledJingle);

        String overridePhoneSuffix =
                accountProperties.get("OVERRIDE_PHONE_SUFFIX");
        setOverridePhoneSufix(overridePhoneSuffix);

        String bypassCapsDomain = accountProperties.get(
                "TELEPHONY_BYPASS_GTALK_CAPS");
        setTelephonyDomainBypassCaps(bypassCapsDomain);

        // Encodings
        encodingsRegistration.loadAccount(
                account,
                ServiceUtils.getService(bundleContext, MediaService.class));
    }

    /**
     * Load password for this STUN descriptor.
     *
     * @param accountID account ID
     * @param namePrefix name prefix
     * @return password or null if empty
     */
    private static String loadStunPassword(BundleContext bundleContext,
                                           AccountID accountID,
                                           String namePrefix)
    {
        ProtocolProviderFactory providerFactory
                = ProtocolProviderFactory.getProtocolProviderFactory(
                        bundleContext,
                        ProtocolNames.JABBER);

        String password = null;
        String className = providerFactory.getClass().getName();
        String packageSourceName
                = className.substring(0, className.lastIndexOf('.'));

        String accountPrefix = ProtocolProviderFactory.findAccountPrefix(
                bundleContext,
                accountID, packageSourceName);

        CredentialsStorageService credentialsService
                = ServiceUtils.getService(
                bundleContext,
                CredentialsStorageService.class);

        try
        {
            password = credentialsService.
                    loadPassword(accountPrefix + "." + namePrefix);
        }
        catch(Exception e)
        {
            return null;
        }

        return password;
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     *
     * @param userName the full JID that we'd like to parse.
     *
     * @return returns the server part of a full JID
     */
    protected String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            String newServerAddr = userName.substring(delimIndex + 1);
            if (newServerAddr.equals(GOOGLE_USER_SUFFIX))
            {
                return GOOGLE_CONNECT_SRV;
            }
            else
            {
                return newServerAddr;
            }
        }

        return null;
    }
}
