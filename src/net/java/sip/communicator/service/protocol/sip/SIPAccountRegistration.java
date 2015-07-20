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

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;

import org.osgi.framework.*;

/**
 * The <tt>SIPAccountRegistration</tt> is used to store all user input data
 * through the <tt>SIPAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 * @author Grigorii Balutsel
 * @author Boris Grozev
 * @author Pawel Domas
 */
public class SIPAccountRegistration
    extends SipAccountID
    implements Serializable
{
    private String defaultDomain = null;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean rememberPassword = true;

    /**
     * The encodings registration object.
     */
    private EncodingsRegistrationUtil encodingsRegistration
            = new EncodingsRegistrationUtil();

    /**
     * The security registration object.
     */
    private SecurityAccountRegistration securityAccountRegistration
            = new SecurityAccountRegistration()
    {
        /**
         * Sets the method used for RTP/SAVP indication.
         */
        @Override
        public void setSavpOption(int savpOption)
        {
            putAccountProperty( ProtocolProviderFactory.SAVP_OPTION,
                                Integer.toString(savpOption) );
        }

        /**
         * Returns the method used for RTP/SAVP indication.
         * @return the method used for RTP/SAVP indication.
         */
        @Override
        public int getSavpOption()
        {
            String savpOption
                    = getAccountPropertyString(
                    ProtocolProviderFactory.SAVP_OPTION);
            return Integer.parseInt(savpOption);
        }
    };

    /**
     * Initializes a new SIPAccountRegistration.
     */
    public SIPAccountRegistration()
    {
        super();
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
     * Returns encoding registration object holding encodings configuration.
     * @return encoding registration object holding encodings configuration.
     */
    public EncodingsRegistrationUtil getEncodingsRegistration()
    {
        return encodingsRegistration;
    }

    /**
     * Returns security registration object holding security configuration.
     * @return <tt>SecurityAccountRegistration</tt> object holding security
     * configuration.
     */
    public SecurityAccountRegistration getSecurityRegistration()
    {
        return securityAccountRegistration;
    }

    /**
     * Loads configuration properties from given <tt>accountID</tt>.
     * @param accountID the account identifier that will be used.
     * @param bundleContext the OSGI bundle context required for some
     * operations.
     */
    public void loadAccount( AccountID accountID,
                             String password,
                             BundleContext bundleContext )
    {
        mergeProperties(accountID.getAccountProperties(), accountProperties);

        String serverAddress = getServerAddress();

        String userID = (serverAddress == null) ? accountID.getUserID()
                : accountID.getAccountPropertyString(
                        ProtocolProviderFactory.USER_ID);
        setUserID(userID);

        setPassword(password);

        rememberPassword = password != null;

        // Password must be copied from credentials storage
        setClistOptionPassword(
                accountID.getAccountPropertyString(OPT_CLIST_PASSWORD));

        securityAccountRegistration.loadAccount(accountID);

        encodingsRegistration.loadAccount(
                accountID,
                ServiceUtils.getService(bundleContext, MediaService.class));
    }

    /**
     * Stores configuration properties held by this object into given
     * <tt>accountProperties</tt> map.
     *
     * @param userName          the user name that will be used.
     * @param passwd            the password that will be used.
     * @param protocolIconPath  the path to the protocol icon is used
     * @param accountIconPath   the path to the account icon if used
     * @param isModification    flag indication if it's modification process(has
     *                          impact on some properties).
     * @param accountProperties the map that will hold the configuration.
     */
    public void storeProperties(String userName, String passwd,
                                String protocolIconPath,
                                String accountIconPath,
                                Boolean isModification,
                                Map<String, String> accountProperties)
    {
        if(rememberPassword)
            setPassword(passwd);
        else
            setPassword(null);

        String serverAddress = null;
        String serverFromUsername = getServerFromUserName(userName);

        if (getServerAddress() != null)
            serverAddress = getServerAddress();

        if (serverFromUsername == null
                && getDefaultDomain() != null)
        {
            // we have only a username and we want to add
            // a default domain
            userName = userName + "@" + getDefaultDomain();

            if (serverAddress == null)
                serverAddress = getDefaultDomain();
        }
        else if(serverAddress == null &&
                serverFromUsername != null)
        {
            serverAddress = serverFromUsername;
        }

        if (serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                    serverAddress);
            if (userName.indexOf(serverAddress) < 0)
                accountProperties.put(
                        ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                        Boolean.toString(true));
        }

        if(isProxyAutoConfigure())
        {
            removeAccountProperty(ProtocolProviderFactory.PROXY_ADDRESS);
            removeAccountProperty(ProtocolProviderFactory.PROXY_PORT);
            removeAccountProperty(ProtocolProviderFactory.PREFERRED_TRANSPORT);
        }

        // when we are creating registerless account make sure that
        // we don't use PA
        if (serverAddress == null)
        {
            setForceP2PMode(true);
        }

        securityAccountRegistration.storeProperties(this.accountProperties);

        encodingsRegistration.storeProperties(this.accountProperties);

        if (isModification)
        {
            if (isMessageWaitingIndicationsEnabled())
            {
                setVoicemailURI("");
                setVoicemailCheckURI("");
                // remove the property as true is by default,
                // and null removes property
                removeAccountProperty(
                        ProtocolProviderFactory.VOICEMAIL_ENABLED);
            } else
            {
                accountProperties.put(
                        ProtocolProviderFactory.VOICEMAIL_ENABLED,
                        Boolean.FALSE.toString());
            }
        }

        super.storeProperties(
                protocolIconPath, accountIconPath, accountProperties);
    }
}
