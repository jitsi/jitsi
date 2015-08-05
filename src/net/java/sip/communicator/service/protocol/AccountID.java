/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.credentialsstorage.*;

import org.jitsi.service.neomedia.*;
import org.osgi.framework.*;

/**
 * The AccountID is an account identifier that, uniquely represents a specific
 * user account over a specific protocol. The class needs to be extended by
 * every protocol implementation because of its protected
 * constructor. The reason why this constructor is protected is mostly avoiding
 * confusion and letting people (using the protocol provider service) believe
 * that they are the ones who are supposed to instantiate the accountid class.
 * <p>
 * Every instance of the <tt>ProtocolProviderService</tt>, created through the
 * ProtocolProviderFactory is assigned an AccountID instance, that uniquely
 * represents it and whose string representation (obtained through the
 * getAccountUID() method) can be used for identification of persistently stored
 * account details.
 * <p>
 * Account id's are guaranteed to be different for different accounts and in the
 * same time are bound to be equal for multiple installations of the same
 * account.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Pawel Domas
 */
public abstract class AccountID
{
    /**
     * The <tt>Logger</tt> used by the <tt>AccountID</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(AccountID.class);

    /**
     * The default properties key prefix used in lib/jitsi-defaults.properties
     */
    protected static final String DEFAULTS_PREFIX
            = "net.java.sip.communicator.service.protocol.";

    /**
     * The protocol display name. In the case of overridden protocol name this
     * would be the new name.
     */
    private final String protocolDisplayName;

    /**
     * The real protocol name.
     */
    private final String protocolName;

    /**
     * Allows a specific set of account properties to override a given default
     * protocol name (e.g. account registration wizards which want to present a
     * well-known protocol name associated with the account that is different
     * from the name of the effective protocol).
     * <p>
     * Note: The logic of the SIP protocol implementation at the time of this
     * writing modifies <tt>accountProperties</tt> to contain the default
     * protocol name if an override hasn't been defined. Since the desire is to
     * enable all account registration wizards to override the protocol name,
     * the current implementation places the specified
     * <tt>defaultProtocolName</tt> in a similar fashion.
     * </p>
     *
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param defaultProtocolName the protocol name to be used in case
     * <tt>accountProperties</tt> doesn't provide an overriding value
     * @return the protocol name
     */
    private static final String getOverriddenProtocolName(
            Map<String, String> accountProperties, String defaultProtocolName)
    {
        String key = ProtocolProviderFactory.PROTOCOL;
        String protocolName = accountProperties.get(key);
        if ((protocolName == null) && (defaultProtocolName != null))
        {
            protocolName = defaultProtocolName;
            accountProperties.put(key, protocolName);
        }
        return protocolName;
    }

    /**
     * Contains all implementation specific properties that define the account.
     * The exact names of the keys are protocol (and sometimes implementation)
     * specific.
     * Currently, only String property keys and values will get properly stored.
     * If you need something else, please consider converting it through custom
     * accessors (get/set) in your implementation.
     */
    protected Map<String, String> accountProperties = null;

    /**
     * A String uniquely identifying the user for this particular account.
     */
    private final String userID;

    /**
     * A String uniquely identifying this account, that can also be used for
     * storing and unambiguously retrieving details concerning it.
     */
    private final String accountUID;

    /**
     * The name of the service that defines the context for this account.
     */
    private final String serviceName;

    /**
     * Creates an account id for the specified provider userid and
     * accountProperties.
     * If account uid exists in account properties, we are loading the account
     * and so load its value from there, prevent changing account uid
     * when server changed (serviceName has changed).
     * @param userID a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param protocolName the name of the protocol implemented by the provider
     * that this id is meant for.
     * @param serviceName the name of the service (e.g. iptel.org, jabber.org,
     * icq.com) that this account is registered with.
     */
    protected AccountID( String userID,
                         Map<String, String> accountProperties,
                         String protocolName,
                         String serviceName)
    {
        /*
         * Allow account registration wizards to override the default protocol
         * name through accountProperties for the purposes of presenting a
         * well-known protocol name associated with the account that is
         * different from the name of the effective protocol.
         */
        this.protocolDisplayName
            = getOverriddenProtocolName(accountProperties, protocolName);

        this.protocolName = protocolName;
        this.userID = userID;
        this.accountProperties
            = new HashMap<String, String>(accountProperties);
        this.serviceName = serviceName;

        String existingAccountUID =
                accountProperties.get(ProtocolProviderFactory.ACCOUNT_UID);

        if(existingAccountUID == null)
        {
            //create a unique identifier string
            this.accountUID
                = protocolDisplayName
                    + ":"
                    + userID
                    + "@"
                    + ((serviceName == null) ? "" : serviceName);
        }
        else
        {
            this.accountUID = existingAccountUID;
        }
    }

    /**
     * Returns the user id associated with this account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Returns a name that can be displayed to the user when referring to this
     * account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getDisplayName()
    {
        // If the ACCOUNT_DISPLAY_NAME property has been set for this account
        // we'll be using it as a display name.
        String key = ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME;
        String accountDisplayName = accountProperties.get(key);
        if (accountDisplayName != null && accountDisplayName.length() > 0)
        {
            return accountDisplayName;
        }

        // Otherwise construct a display name.
        String returnValue = getUserID();
        String protocolName = getProtocolDisplayName();

        if (protocolName != null && protocolName.trim().length() > 0)
            returnValue += " (" + protocolName + ")";

        return returnValue;
    }

    /**
     * Sets {@link ProtocolProviderFactory#DISPLAY_NAME} property value.
     *
     * @param displayName the display name value to set.
     */
    public void setDisplayName(String displayName)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.DISPLAY_NAME,
            displayName);
    }

    /**
     * Returns the display name of the protocol.
     *
     * @return the display name of the protocol
     */
    public String getProtocolDisplayName()
    {
        return protocolDisplayName;
    }

    /**
     * Returns the name of the protocol.
     *
     * @return the name of the protocol
     */
    public String getProtocolName()
    {
        return protocolName;
    }

    /**
     * Returns a String uniquely identifying this account, guaranteed to remain
     * the same across multiple installations of the same account and to always
     * be unique for differing accounts.
     * @return String
     */
    public String getAccountUniqueID()
    {
        return accountUID;
    }

    /**
     * Returns a Map containing protocol and implementation account
     * initialization properties.
     * @return a Map containing protocol and implementation account
     * initialization properties.
     */
    public Map<String, String> getAccountProperties()
    {
        return new HashMap<String, String>(accountProperties);
    }

    /**
     * Returns the specific account property.
     *
     * @param key property key
     * @param defaultValue default value if the property does not exist
     * @return property value corresponding to property key
     */
    public boolean getAccountPropertyBoolean(Object key, boolean defaultValue)
    {
        String value = getAccountPropertyString(key);
        if(value == null)
            value = getDefaultString(key.toString());
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * Gets the value of a specific property as a signed decimal integer. If the
     * specified property key is associated with a value in this
     * <tt>AccountID</tt>, the string representation of the value is parsed into
     * a signed decimal integer according to the rules of
     * {@link Integer#parseInt(String)} . If parsing the value as a signed
     * decimal integer fails or there is no value associated with the specified
     * property key, <tt>defaultValue</tt> is returned.
     *
     * @param key the key of the property to get the value of as a
     * signed decimal integer
     * @param defaultValue the value to be returned if parsing the value of the
     * specified property key as a signed decimal integer fails or there is no
     * value associated with the specified property key in this
     * <tt>AccountID</tt>
     * @return the value of the property with the specified key in this
     * <tt>AccountID</tt> as a signed decimal integer; <tt>defaultValue</tt> if
     * parsing the value of the specified property key fails or no value is
     * associated in this <tt>AccountID</tt> with the specified property name
     */
    public int getAccountPropertyInt(Object key, int defaultValue)
    {
        String stringValue = getAccountPropertyString(key);
        int intValue = defaultValue;

        if ((stringValue == null) || (stringValue.isEmpty()))
        {
            stringValue = getDefaultString(key.toString());
        }

        if ((stringValue != null) && (stringValue.length() > 0))
        {
            try
            {
                intValue = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException ex)
            {
                logger.error("Failed to parse account property " + key
                    + " value " + stringValue + " as an integer", ex);
            }
        }
        return intValue;
    }

    /**
     * Returns the account property string corresponding to the given key.
     *
     * @param key the key, corresponding to the property string we're looking
     * for
     * @return the account property string corresponding to the given key
     */
    public String getAccountPropertyString(Object key)
    {
        return getAccountPropertyString(key, null);
    }

    /**
     * Returns the account property string corresponding to the given key.
     *
     * @param key the key, corresponding to the property string we're looking
     *        for
     * @param defValue the default value returned when given <tt>key</tt>
     *        is not present
     * @return the account property string corresponding to the given key
     */
    public String getAccountPropertyString(Object key, String defValue)
    {
        String value = accountProperties.get(key);
        if(value == null)
            value = getDefaultString(key.toString());
        return (value == null) ? defValue : value;
    }

    /**
     * Adds a property to the map of properties for this account identifier.
     *
     * @param key the key of the property
     * @param value the property value
     */
    public void putAccountProperty(String key, String value)
    {
        accountProperties.put(key, value);
    }

    /**
     * Adds property to the map of properties for this account
     * identifier.
     * @param key the key of the property
     * @param value the property value
     */
    public void putAccountProperty(String key, Object value)
    {
        accountProperties.put(key, String.valueOf(value));
    }

    /**
     * Removes specified account property.
     * @param key the key to remove.
     */
    public void removeAccountProperty(String key)
    {
        accountProperties.remove(key);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    @Override
    public int hashCode()
    {
        return (accountUID == null)? 0 : accountUID.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     * @see     #hashCode()
     * @see     java.util.Hashtable
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        return (obj != null)
            && getClass().isInstance(obj)
            && userID.equals(((AccountID)obj).userID);
    }

    /**
     * Returns a string representation of this account id (same as calling
     * getAccountUniqueID()).
     *
     * @return  a string representation of this account id.
     */
    @Override
    public String toString()
    {
        return getAccountUniqueID();
    }

    /**
     * Returns the name of the service that defines the context for this
     * account. Often this name would be an sqdn or even an ipaddress but this
     * would not always be the case (e.g. p2p providers may return a name that
     * does not directly correspond to an IP address or host name).
     * <p>
     * @return the name of the service that defines the context for this
     * account.
     */
    public String getService()
    {
        return this.serviceName;
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the protocol can use to communicate with us.
     * By default this string is set to userid@servicename. Protocol
     * implementors should override it if they'd need it to respect a different
     * syntax.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        String userID = getUserID();
        return (userID.indexOf('@') > 0) ? userID
            : (userID + "@" + getService());
    }

    /**
     * Indicates if this account is currently enabled.
     * @return <tt>true</tt> if this account is enabled, <tt>false</tt> -
     * otherwise.
     */
    public boolean isEnabled()
    {
        return !getAccountPropertyBoolean(
            ProtocolProviderFactory.IS_ACCOUNT_DISABLED, false);
    }

    /**
     * The address of the server we will use for this account
     *
     * @return String
     */
    public String getServerAddress()
    {
        return getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);
    }

    /**
     * Get the {@link ProtocolProviderFactory#ACCOUNT_DISPLAY_NAME} property.
     *
     * @return the {@link ProtocolProviderFactory#ACCOUNT_DISPLAY_NAME}
     *         property value.
     */
    public String getAccountDisplayName()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME);
    }

    /**
     * Sets {@link ProtocolProviderFactory#ACCOUNT_DISPLAY_NAME} property value.
     *
     * @param displayName the account display name value to set.
     */
    public void setAccountDisplayName(String displayName)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
                displayName);
    }

    /**
     * Returns the password of the account.
     *
     * @return the password of the account.
     */
    public String getPassword()
    {
        return getAccountPropertyString(ProtocolProviderFactory.PASSWORD);
    }

    /**
     * Sets the password of the account.
     *
     * @param password the password of the account.
     */
    public void setPassword(String password)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.PASSWORD, password);
    }

    /**
     * The authorization name
     *
     * @return String auth name
     */
    public String getAuthorizationName()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.AUTHORIZATION_NAME);
    }

    /**
     * Sets authorization name.
     *
     * @param authName String
     */
    public void setAuthorizationName(String authName)
    {
        setOrRemoveIfEmpty(
                ProtocolProviderFactory.AUTHORIZATION_NAME,
                authName);
    }

    /**
     * The port on the specified server
     *
     * @return int
     */
    public String getServerPort()
    {
        return getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT);
    }

    /**
     * Sets the server port.
     *
     * @param port int
     */
    public void setServerPort(String port)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.SERVER_PORT, port);
    }

    /**
     * Sets the server
     *
     * @param serverAddress String
     */
    public void setServerAddress(String serverAddress)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.SERVER_ADDRESS,
                serverAddress);
    }

    /**
     * Returns <tt>true</tt> if server was overriden.
     * @return <tt>true</tt> if server was overriden.
     */
    public boolean isServerOverridden()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, false);
    }

    /**
     * Sets <tt>isServerOverridden</tt> property.
     * @param isServerOverridden indicates if the server is overridden
     */
    public void setServerOverridden(boolean isServerOverridden)
    {
        putAccountProperty(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                isServerOverridden);
    }

    /**
     * Returns the protocol icon path stored under
     * {@link ProtocolProviderFactory#PROTOCOL_ICON_PATH} key.
     *
     * @return the protocol icon path.
     */
    public String getProtocolIconPath()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.PROTOCOL_ICON_PATH);
    }

    /**
     * Sets the protocl icon path that will be held under
     * {@link ProtocolProviderFactory#PROTOCOL_ICON_PATH} key.
     *
     * @param iconPath a path to the protocol icon to set.
     */
    public void setProtocolIconPath(String iconPath)
    {
        putAccountProperty(
                ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                iconPath);
    }

    /**
     * Returns the protocol icon path stored under
     * {@link ProtocolProviderFactory#ACCOUNT_ICON_PATH} key.
     *
     * @return the protocol icon path.
     */
    public String getAccountIconPath()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.ACCOUNT_ICON_PATH);
    }

    /**
     * Sets the account icon path that will be held under
     * {@link ProtocolProviderFactory#ACCOUNT_ICON_PATH} key.
     *
     * @param iconPath a path to the account icon to set.
     */
    public void setAccountIconPath(String iconPath)
    {
        putAccountProperty(
                ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                iconPath);
    }

    /**
     * Returns the DTMF method.
     *
     * @return the DTMF method.
     */
    public String getDTMFMethod()
    {
        return getAccountPropertyString(ProtocolProviderFactory.DTMF_METHOD);
    }

    /**
     * Sets the DTMF method.
     *
     * @param dtmfMethod the DTMF method to set
     */
    public void setDTMFMethod(String dtmfMethod)
    {
        putAccountProperty(ProtocolProviderFactory.DTMF_METHOD, dtmfMethod);
    }

    /**
     * Returns the minimal DTMF tone duration.
     *
     * @return The minimal DTMF tone duration.
     */
    public String getDtmfMinimalToneDuration()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.DTMF_MINIMAL_TONE_DURATION);
    }

    /**
     * Sets the minimal DTMF tone duration.
     *
     * @param dtmfMinimalToneDuration The minimal DTMF tone duration to set.
     */
    public void setDtmfMinimalToneDuration(String dtmfMinimalToneDuration)
    {
        putAccountProperty( ProtocolProviderFactory.DTMF_MINIMAL_TONE_DURATION,
                            dtmfMinimalToneDuration );
    }

    /**
     * Gets the ID of the client certificate configuration.
     * @return the ID of the client certificate configuration.
     */
    public String getTlsClientCertificate()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
    }

    /**
     * Sets the ID of the client certificate configuration.
     * @param id the client certificate configuration template ID.
     */
    public void setTlsClientCertificate(String id)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE, id);
    }

    /**
     * Checks if the account is hidden.
     * @return <tt>true</tt> if this account is hidden or <tt>false</tt>
     * otherwise.
     */
    public boolean isHidden()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;
    }

    /**
     * Checks if the account config is hidden.
     * @return <tt>true</tt> if the account config is hidden or <tt>false</tt>
     * otherwise.
     */
    public boolean isConfigHidden()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.IS_ACCOUNT_CONFIG_HIDDEN) != null;
    }

    /**
     * Checks if the account status menu is hidden.
     * @return <tt>true</tt> if the account status menu is hidden or
     * <tt>false</tt> otherwise.
     */
    public boolean isStatusMenuHidden()
    {
        return getAccountPropertyString(
            ProtocolProviderFactory.IS_ACCOUNT_STATUS_MENU_HIDDEN) != null;
    }

    /**
     * Checks if the account is marked as readonly.
     * @return <tt>true</tt> if the account is marked as readonly or
     * <tt>false</tt> otherwise.
     */
    public boolean isReadOnly()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.IS_ACCOUNT_READ_ONLY) != null;
    }

    /**
     * Returns the first <tt>ProtocolProviderService</tt> implementation
     * corresponding to the preferred protocol
     *
     * @return the <tt>ProtocolProviderService</tt> corresponding to the
     * preferred protocol
     */
    public boolean isPreferredProvider()
    {
        String preferredProtocolProp
                = getAccountPropertyString(
                        ProtocolProviderFactory.IS_PREFERRED_PROTOCOL);

        if (preferredProtocolProp != null
                && preferredProtocolProp.length() > 0
                && Boolean.parseBoolean(preferredProtocolProp))
        {
            return true;
        }

        return false;
    }

    /**
     * Set the account properties.
     *
     * @param accountProperties the properties of the account
     */
    public void setAccountProperties(Map<String, String> accountProperties)
    {
        this.accountProperties = accountProperties;
    }

    /**
     * Returns if the encryption protocol given in parameter is enabled.
     *
     * @param encryptionProtocolName The name of the encryption protocol
     * ("ZRTP", "SDES" or "MIKEY").
     */
    public boolean isEncryptionProtocolEnabled(SrtpControlType type)
    {
        // The default value is false, except for ZRTP.
        boolean defaultValue = type == SrtpControlType.ZRTP;

        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + "."
                        + type.toString(),
                    defaultValue);
    }

    /**
     * Returns the list of STUN servers that this account is currently
     * configured to use.
     *
     * @return the list of STUN servers that this account is currently
     * configured to use.
     */
    public List<StunServerDescriptor> getStunServers(
                                                    BundleContext bundleContext)
    {
        Map<String, String> accountProperties = getAccountProperties();
        List<StunServerDescriptor> stunServerList
            = new ArrayList<StunServerDescriptor>();

        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                        accountProperties,
                        ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // there are no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            String password
                = loadStunPassword(
                        bundleContext,
                        this,
                        ProtocolProviderFactory.STUN_PREFIX + i);

            if(password != null)
                stunServer.setPassword(password);

            stunServerList.add(stunServer);
        }

        return stunServerList;
    }

    /**
     * Returns the password for the STUN server with the specified prefix.
     *
     * @param bundleContext the OSGi bundle context that we are currently
     * running in.
     * @param accountID account ID
     * @param namePrefix name prefix
     *
     * @return password or null if empty
     */
    protected static String loadStunPassword(BundleContext bundleContext,
                                             AccountID     accountID,
                                             String        namePrefix)
    {
        ProtocolProviderFactory providerFactory
                = ProtocolProviderFactory.getProtocolProviderFactory(
                        bundleContext,
                        accountID.getSystemProtocolName());

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
     * Determines whether this account's provider is supposed to auto discover
     * STUN and TURN servers.
     *
     * @return <tt>true</tt> if this provider would need to discover STUN/TURN
     * servers and false otherwise.
     */
    public boolean isStunServerDiscoveryEnabled()
    {
        return getAccountPropertyBoolean(
                    ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                    true);
    }

    /**
     * Determines whether this account's provider uses UPnP (if available).
     *
     * @return <tt>true</tt> if this provider would use UPnP (if available),
     * <tt>false</tt> otherwise
     */
    public boolean isUPNPEnabled()
    {
        return getAccountPropertyBoolean(
                                        ProtocolProviderFactory.IS_USE_UPNP,
                                        true);
    }

    /**
     * Determines whether this account's provider uses the default STUN server
     * provided by Jitsi (stun.jitsi.net) if there is no other STUN/TURN server
     * discovered/configured.
     *
     * @return <tt>true</tt> if this provider would use the default STUN server,
     * <tt>false</tt> otherwise
     */
    public boolean isUseDefaultStunServer()
    {
        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                    true);
    }

    /**
     * Returns the actual name of the protocol used rather than a branded
     * variant. The method is primarily meant for open protocols such as SIP
     * or XMPP so that it would always return SIP or XMPP even in branded
     * protocols who otherwise return things like GTalk and ippi for
     * PROTOCOL_NAME.
     *
     * @return the real non-branded name of the protocol.
     */
    public String getSystemProtocolName()
    {
        return getProtocolName();
    }

    /**
     * Sorts the enabled encryption protocol list given in parameter to match
     * the preferences set for this account.
     *
     * @return Sorts the enabled encryption protocol list given in parameter to
     * match the preferences set for this account.
     */
    public List<SrtpControlType> getSortedEnabledEncryptionProtocolList()
    {
        Map<String, Integer> encryptionProtocols
            = getIntegerPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL,
                    true);
        Map<String, Boolean> encryptionProtocolStatus
            = getBooleanPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS,
                    true,
                    false);

        // If the account is not yet configured, then ZRTP is activated by
        // default.
        if(encryptionProtocols.size() == 0)
        {
            encryptionProtocols.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL + ".ZRTP",
                    0);
            encryptionProtocolStatus.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + ".ZRTP",
                    true);
        }

        List<SrtpControlType> sortedEncryptionProtocols
            = new ArrayList<SrtpControlType>(encryptionProtocols.size());

        // First: add all protocol in the right order.
        for (Map.Entry<String, Integer> e : encryptionProtocols.entrySet())
        {
            int index = e.getValue();

            // If the key is set.
            if (index != -1)
            {
                if (index > sortedEncryptionProtocols.size())
                    index = sortedEncryptionProtocols.size();

                String name =
                    e.getKey()
                        .substring(
                            ProtocolProviderFactory.ENCRYPTION_PROTOCOL
                                .length() + 1);

                try
                {
                    sortedEncryptionProtocols.add(index,
                        SrtpControlType.fromString(name));
                }
                catch(IllegalArgumentException exc)
                {
                    logger.error(
                        "Failed to get SRTP control type for name: '"
                            + name + "', key: '" + e.getKey() + "'", exc);
                }
            }
        }

        // Second: remove all disabled protocols.
        for (Iterator<SrtpControlType> i = sortedEncryptionProtocols.iterator();
                i.hasNext();)
        {
            String name = i.next().toString();

            if (!encryptionProtocolStatus.get(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + "."
                        + name))
            {
                i.remove();
            }
        }

        return sortedEncryptionProtocols;
    }

    /**
     * Returns a <tt>java.util.Map</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix and <tt>Boolean</tt>
     * containing the value for each property selected. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @param defaultValue the default value if the key is not set.
     * @return a <tt>java.util.Map</tt> containing all property name String-s
     * matching the specified conditions and the corresponding values as
     * Boolean.
     */
    public Map<String, Boolean> getBooleanPropertiesByPrefix(
            String prefix,
            boolean exactPrefixMatch,
            boolean defaultValue)
    {
        String propertyName;
        List<String> propertyNames
            = getPropertyNamesByPrefix(prefix, exactPrefixMatch);
        Map<String, Boolean> properties
            = new HashMap<String, Boolean>(propertyNames.size());

        for(int i = 0; i < propertyNames.size(); ++i)
        {
            propertyName = propertyNames.get(i);
            properties.put(
                    propertyName,
                    getAccountPropertyBoolean(propertyName, defaultValue));
        }

        return properties;
    }

    /**
     * Returns a <tt>java.util.Map</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix and <tt>Integer</tt>
     * containing the value for each property selected. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @return a <tt>java.util.Map</tt> containing all property name String-s
     * matching the specified conditions and the corresponding values as
     * Integer.
     */
    public Map<String, Integer> getIntegerPropertiesByPrefix(
            String prefix,
            boolean exactPrefixMatch)
    {
        String propertyName;
        List<String> propertyNames
            = getPropertyNamesByPrefix(prefix, exactPrefixMatch);
        Map<String, Integer> properties
            = new HashMap<String, Integer>(propertyNames.size());

        for(int i = 0; i < propertyNames.size(); ++i)
        {
            propertyName = propertyNames.get(i);
            properties.put(
                    propertyName,
                    getAccountPropertyInt(propertyName, -1));
        }

        return properties;
    }

    /**
     * Returns a <tt>java.util.List</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @return a <tt>java.util.List</tt>containing all property name String-s
     * matching the specified conditions.
     */
    public List<String> getPropertyNamesByPrefix(
            String prefix,
            boolean exactPrefixMatch)
    {
        List<String> resultKeySet = new LinkedList<String>();

        for (String key : accountProperties.keySet())
        {
            int ix = key.lastIndexOf('.');

            if(ix == -1)
                continue;

            String keyPrefix = key.substring(0, ix);

            if(exactPrefixMatch)
            {
                if(prefix.equals(keyPrefix))
                    resultKeySet.add(key);
            }
            else
            {
                if(keyPrefix.startsWith(prefix))
                    resultKeySet.add(key);
            }
        }

        return resultKeySet;
    }

    /**
     * Sets the property a new value, but only if it's not <tt>null</tt> or
     * the property is removed from the map.
     *
     * @param key the property key
     * @param value the property value
     */
    public void setOrRemoveIfNull(String key, String value)
    {
        if(value != null)
        {
            putAccountProperty(key, value);
        }
        else
        {
            removeAccountProperty(key);
        }
    }

    /**
     * Puts the new property value if it's not <tt>null</tt> nor empty.
     * @param key the property key
     * @param value the property value
     */
    public void setOrRemoveIfEmpty(String key, String value)
    {
        setOrRemoveIfEmpty(key, value, false);
    }

    /**
     * Puts the new property value if it's not <tt>null</tt> nor empty. If
     * <tt>trim</tt> parameter is set to <tt>true</tt> the string will be
     * trimmed, before checked for emptiness.
     *
     * @param key the property key
     * @param value the property value
     * @param trim <tt>true</tt> if the value  will be trimmed, before
     *             <tt>isEmpty()</tt> is called.
     */
    public void setOrRemoveIfEmpty(String key, String value, boolean trim)
    {
        if( value != null
            && (trim ? !value.trim().isEmpty() : !value.isEmpty()) )
        {
            putAccountProperty(key, value);
        }
        else
        {
            removeAccountProperty(key);
        }
    }

    /**
     * Stores configuration properties held by this object into given
     * <tt>accountProperties</tt> map.
     *
     * @param protocolIconPath  the path to the protocol icon is used
     * @param accountIconPath   the path to the account icon if used
     * @param accountProperties output properties map
     */
    public void storeProperties( String protocolIconPath,
                                 String accountIconPath,
                                 Map<String, String> accountProperties )
    {
        if(protocolIconPath != null)
            setProtocolIconPath(protocolIconPath);

        if(accountIconPath != null)
            setAccountIconPath(accountIconPath);

        mergeProperties(this.accountProperties, accountProperties);

        // Removes encrypted password property, as it will be restored during
        // account storage, but only if the password property is present.
        accountProperties.remove("ENCRYPTED_PASSWORD");
    }

    /**
     * Gets default property value for given <tt>key</tt>.
     *
     * @param key the property key
     * @return default property value for given<tt>key</tt>
     */
    protected String getDefaultString(String key)
    {
        return getDefaultStr(key);
    }

    /**
     * Gets default property value for given <tt>key</tt>.
     *
     * @param key the property key
     * @return default property value for given<tt>key</tt>
     */
    public static String getDefaultStr(String key)
    {
        return ProtocolProviderActivator
                .getConfigurationService()
                .getString(DEFAULTS_PREFIX +key);
    }

    /**
     * Copies all properties from <tt>input</tt> map to <tt>output</tt> map.
     * @param input source properties map
     * @param output destination properties map
     */
    public static void mergeProperties( Map<String, String> input,
                                        Map<String, String> output )
    {
        for(String key : input.keySet())
        {
            output.put(key, input.get(key));
        }
    }
}
