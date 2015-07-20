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
package net.java.sip.communicator.service.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 * @author Pawel Domas
 */
public class SipAccountID
    extends AccountID
{
    /**
     * The name of the property under which the user may specify whether to use
     * or not XCAP.
     */
    public static final String XCAP_ENABLE = "XCAP_ENABLE";

    /**
     * The name of the property under which the user may specify whether to use
     * or not xivo.
     */
    public static final String XIVO_ENABLE = "XIVO_ENABLE";

    /**
     * The name of the property under which the user may specify whether to use
     * original sip credentials for the contact list.
     */
    public static final String OPT_CLIST_USE_SIP_CREDETIALS =
            "OPT_CLIST_USE_SIP_CREDETIALS";

    /**
     * The name of the property under which the user may specify the contact
     * list server uri.
     */
    public static final String OPT_CLIST_SERVER_URI = "OPT_CLIST_SERVER_URI";

    /**
     * The name of the property under which the user may specify the XCAP user.
     */
    public static final String OPT_CLIST_USER = "OPT_CLIST_USER";

    /**
     * The name of the property under which the user may specify the XCAP user
     * password.
     */
    public static final String OPT_CLIST_PASSWORD = "OPT_CLIST_PASSWORD";

    /**
     * Default properties prefix used in jitsi-defaults.properties file
     * for SIP protocol.
     */
    private static final String SIP_DEFAULTS_PREFIX
            = AccountID.DEFAULTS_PREFIX +"sip.";

    /**
     * Creates a SIP account id from the specified ide and account properties.
     *
     * @param userID the user id part of the SIP uri identifying this contact.
     * @param accountProperties any other properties necessary for the account.
     * @param serverName the name of the server that the user belongs to.
     */
    protected SipAccountID(String userID, Map<String, String> accountProperties,
                           String serverName)
    {
        super(userID, accountProperties,
              ProtocolNames.SIP,
              serverName);
    }

    /**
     * Default constructor for wizard purposes.
     */
    public SipAccountID()
    {
        super(null, new HashMap<String, String>(), ProtocolNames.SIP, null);
    }

    /**
     * The proxy address
     * @return the proxy address
     */
    public String getProxy()
    {
        return getAccountPropertyString(ProtocolProviderFactory.PROXY_ADDRESS);
    }

    /**
     * Set new proxy address
     * @param proxy the proxy address to set
     */
    public void setProxy(String proxy)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.PROXY_ADDRESS, proxy);
    }

    /**
     * Returns the UIN of the sip registration account.
     *
     * @return the UIN of the sip registration account.
     */
    public String getId()
    {
        return getAccountPropertyString(ProtocolProviderFactory.USER_ID);
    }

    /**
     * Get the preferred transport.
     * @return the preferred transport for this account identifier.
     */
    public String getPreferredTransport()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.PREFERRED_TRANSPORT);
    }

    /**
     * Sets the preferred transport for this account identifier.
     * @param preferredTransport the preferred transport for this account
     *                           identifier.
     */
    public void setPreferredTransport(String preferredTransport)
    {
        putAccountProperty(
                ProtocolProviderFactory.PREFERRED_TRANSPORT,
                preferredTransport);
    }

    /**
     * The port on the specified proxy
     *
     * @return int
     */
    public String getProxyPort()
    {
        return getAccountPropertyString(ProtocolProviderFactory.PROXY_PORT);
    }

    /**
     * Sets the identifier of the sip registration account.
     *
     * @param id the identifier of the sip registration account.
     */
    public void setUserID(String id)
    {
        putAccountProperty(ProtocolProviderFactory.USER_ID, id);
    }

    /**
     * Is proxy auto configured.
     * @return <tt>true</tt> if proxy is auto configured.
     */
    public boolean isProxyAutoConfigure()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.PROXY_AUTO_CONFIG, true);
    }

    /**
     * Sets auto configuration of proxy enabled or disabled.
     * @param proxyAutoConfigure <tt>true</tt> if the proxy will be
     *                           auto configured.
     */
    public void setProxyAutoConfigure(boolean proxyAutoConfigure)
    {
        putAccountProperty(ProtocolProviderFactory.PROXY_AUTO_CONFIG,
                proxyAutoConfigure);
    }

    /**
     * Sets the proxy port.
     *
     * @param port int
     */
    public void setProxyPort(String port)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.PROXY_PORT, port);
    }

    /**
     * If the presence is enabled
     *
     * @return If the presence is enabled
     */
    public boolean isEnablePresence()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_PRESENCE_ENABLED, true);
    }

    /**
     * If the p2p mode is forced
     *
     * @return If the p2p mode is forced
     */
    public boolean isForceP2PMode()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.FORCE_P2P_MODE, false);
    }

    /**
     * The offline contact polling period
     *
     * @return the polling period
     */
    public String getPollingPeriod()
    {
        return getAccountPropertyString(ProtocolProviderFactory.POLLING_PERIOD);
    }

    /**
     * The default expiration of subscriptions
     *
     * @return the subscription expiration
     */
    public String getSubscriptionExpiration()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION);
    }

    /**
     * Sets if the presence is enabled
     *
     * @param enablePresence if the presence is enabled
     */
    public void setEnablePresence(boolean enablePresence)
    {
        putAccountProperty(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                enablePresence);
    }

    /**
     * Sets if we have to force the p2p mode
     *
     * @param forceP2PMode if we have to force the p2p mode
     */
    public void setForceP2PMode(boolean forceP2PMode)
    {
        putAccountProperty(ProtocolProviderFactory.FORCE_P2P_MODE,
                forceP2PMode);
    }

    /**
     * Sets the offline contacts polling period
     *
     * @param pollingPeriod the offline contacts polling period
     */
    public void setPollingPeriod(String pollingPeriod)
    {
        putAccountProperty( ProtocolProviderFactory.POLLING_PERIOD,
                            pollingPeriod );
    }

    /**
     * Sets the subscription expiration value
     *
     * @param subscriptionExpiration the subscription expiration value
     */
    public void setSubscriptionExpiration(String subscriptionExpiration)
    {
        putAccountProperty( ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                            subscriptionExpiration );
    }

    /**
     * Returns the keep alive method.
     *
     * @return the keep alive method.
     */
    public String getKeepAliveMethod()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.KEEP_ALIVE_METHOD);
    }

    /**
     * Sets the keep alive method.
     *
     * @param keepAliveMethod the keep alive method to set
     */
    public void setKeepAliveMethod(String keepAliveMethod)
    {
        putAccountProperty( ProtocolProviderFactory.KEEP_ALIVE_METHOD,
                            keepAliveMethod );
    }

    /**
     * Returns the keep alive interval.
     *
     * @return the keep alive interval
     */
    public String getKeepAliveInterval()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.KEEP_ALIVE_INTERVAL);
    }

    /**
     * Sets the keep alive interval.
     *
     * @param keepAliveInterval the keep alive interval to set
     */
    public void setKeepAliveInterval(String keepAliveInterval)
    {
        putAccountProperty( ProtocolProviderFactory.KEEP_ALIVE_INTERVAL,
                            keepAliveInterval );
    }

    /**
     * Checks if XCAP is enabled.
     *
     * @return true if XCAP is enabled otherwise false.
     */
    public boolean isXCapEnable()
    {
        return getAccountPropertyBoolean(XCAP_ENABLE, false);
    }
    /**
     * Sets if XCAP is enable.
     *
     * @param xCapEnable XCAP enable.
     */
    public void setXCapEnable(boolean xCapEnable)
    {
        putAccountProperty(XCAP_ENABLE, xCapEnable);
    }

    /**
     * Checks if XiVO option is enabled.
     *
     * @return true if XiVO is enabled otherwise false.
     */
    public boolean isXiVOEnable()
    {
        return getAccountPropertyBoolean(XIVO_ENABLE, false);
    }
    /**
     * Sets if XiVO option is enable.
     *
     * @param xivoEnable XiVO enable.
     */
    public void setXiVOEnable(boolean xivoEnable)
    {
        putAccountProperty(XIVO_ENABLE, xivoEnable);
    }

    /**
     * Gets the property related to XCAP/XIVO in old properties compatibility
     * mode. If there is no value under new key then old keys are selected
     * based on whether XCAP or XIVO is currently enabled.
     *
     * @param newKey currently used property key
     * @param oldKeyXcap old XCAP property key
     * @param oldKeyXivo old XIVO property key
     * @return XIVO/XCAP property value
     */
    private String getXcapCompatible( String newKey,
                                      String oldKeyXcap,
                                      String oldKeyXivo )
    {
        String value = getAccountPropertyString(newKey);
        if(value == null)
        {
            String oldKey = isXCapEnable() ? oldKeyXcap : oldKeyXivo;
            value = getAccountPropertyString(oldKey);
            if(value != null)
            {
                // remove old
                accountProperties.remove(oldKey);
                // store under new property key
                accountProperties.put(newKey, value);
            }
        }
        return value;
    }

    /**
     * Checks if contact list has to use SIP account credentials.
     *
     * @return <tt>true</tt> if contact list has to use SIP account credentials
     *         otherwise <tt>false</tt>.
     */
    public boolean isClistOptionUseSipCredentials()
    {
        String val = getXcapCompatible( OPT_CLIST_USE_SIP_CREDETIALS,
                                        "XCAP_USE_SIP_CREDETIALS",
                                        "XIVO_USE_SIP_CREDETIALS" );

        if(val == null)
            getDefaultString(OPT_CLIST_USE_SIP_CREDETIALS);

        return Boolean.parseBoolean(val);
    }

    /**
     * Sets if contact list has to use SIP account credentials.
     *
     * @param useSipCredentials if the clist has to use SIP account credentials.
     */
    public void setClistOptionUseSipCredentials(boolean useSipCredentials)
    {
        putAccountProperty( OPT_CLIST_USE_SIP_CREDETIALS,
                                useSipCredentials );
    }

    /**
     * Gets the contact list server uri.
     *
     * @return the contact list  server uri.
     */
    public String getClistOptionServerUri()
    {
        return getXcapCompatible( OPT_CLIST_SERVER_URI,
                                  "XCAP_SERVER_URI",
                                  "XIVO_SERVER_URI"  );
    }

    /**
     * Sets the contact list server uri.
     *
     * @param clistOptionServerUri the contact list server uri.
     */
    public void setClistOptionServerUri(String clistOptionServerUri)
    {
        setOrRemoveIfNull(OPT_CLIST_SERVER_URI, clistOptionServerUri);
    }

    /**
     * Gets the contact list user.
     *
     * @return the contact list user.
     */
    public String getClistOptionUser()
    {
        return getXcapCompatible(OPT_CLIST_USER, "XCAP_USER", "XIVO_USER");
    }

    /**
     * Sets the contact list user.
     *
     * @param clistOptionUser the contact list user.
     */
    public void setClistOptionUser(String clistOptionUser)
    {
        setOrRemoveIfNull(OPT_CLIST_USER, clistOptionUser);
    }

    /**
     * Gets the contact list password.
     *
     * @return the contact list password.
     */
    public String getClistOptionPassword()
    {
        return getXcapCompatible( OPT_CLIST_PASSWORD,
                                  "XCAP_PASSWORD",
                                  "XIVO_PASSWORD"  );
    }

    /**
     * Sets the contact list password.
     *
     * @param clistOptionPassword the contact list password.
     */
    public void setClistOptionPassword(String clistOptionPassword)
    {
        setOrRemoveIfEmpty(OPT_CLIST_PASSWORD, clistOptionPassword);
    }

    /**
     * The voicemail URI.
     * @return the voicemail URI.
     */
    public String getVoicemailURI()
    {
        return getAccountPropertyString(ProtocolProviderFactory.VOICEMAIL_URI);
    }

    /**
     * Sets voicemail URI.
     * @param voicemailURI new URI.
     */
    public void setVoicemailURI(String voicemailURI)
    {
        putAccountProperty( ProtocolProviderFactory.VOICEMAIL_URI,
                            voicemailURI );
    }

    /**
     * The voicemail check URI.
     * @return the voicemail URI.
     */
    public String getVoicemailCheckURI()
    {
        return getAccountPropertyString(
                ProtocolProviderFactory.VOICEMAIL_CHECK_URI);
    }

    /**
     * Sets voicemail check URI.
     * @param voicemailCheckURI new URI.
     */
    public void setVoicemailCheckURI(String voicemailCheckURI)
    {
        putAccountProperty( ProtocolProviderFactory.VOICEMAIL_CHECK_URI,
                            voicemailCheckURI );
    }

    /**
     * Check if messageWaitingIndications is enabled
     *
     * @return if messageWaitingIndications is enabled
     */
    public boolean isMessageWaitingIndicationsEnabled()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.VOICEMAIL_ENABLED, true);
    }

    /**
     * Sets message waiting indications.
     *
     * @param messageWaitingIndications <tt>true</tt> to enable message waiting
     *                                  indications.
     */
    public void setMessageWaitingIndications(boolean messageWaitingIndications)
    {

        putAccountProperty( ProtocolProviderFactory.VOICEMAIL_ENABLED,
                            messageWaitingIndications );
    }

    /**
     * Returns the protocol name
     *
     * @return the name of the protocol for this registration object
     */
    public String getProtocolName()
    {
        return ProtocolNames.SIP;
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the procotol can use to communicate with us.
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
        StringBuffer accountAddress = new StringBuffer();
        accountAddress.append("sip:");
        accountAddress.append(getUserID());

        String service = getService();
        if (service != null)
        {
            accountAddress.append('@');
            accountAddress.append(service);
        }

        return accountAddress.toString();
    }

    /**
     * {@inheritDoc}
     */
    protected String getDefaultString(String key)
    {
        return SipAccountID.getDefaultStr(key);
    }

    public static String getDefaultStr(String key)
    {
        String value = ProtocolProviderActivator
                .getConfigurationService()
                .getString(SIP_DEFAULTS_PREFIX +key);

        if(value == null)
            value = AccountID.getDefaultStr(key);

        return value;
    }

    /**
     * Return the server part of the sip user name.
     *
     * @param userName the username.
     * @return the server part of the sip user name.
     */
    public static String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
    }
}
