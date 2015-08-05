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
package net.java.sip.communicator.service.protocol.jabber;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.osgi.framework.*;

/**
 * The <tt>JabberAccountRegistration</tt> is used to store all user input data
 * through the <tt>JabberAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 * @author Boris Grozev
 */
public class JabberAccountRegistration
    extends JabberAccountID
    implements Serializable
{
    /**
     * The default domain.
     */
    private String defaultUserSufix;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean rememberPassword = true;

    /**
     * UID of edited account
     */
    private String editedAccUID;

    /**
     * The list of additional STUN servers entered by user.
     */
    private List<StunServerDescriptor> additionalStunServers
        = new ArrayList<StunServerDescriptor>();

    /**
     * The list of additional JingleNodes (tracker or relay) entered by user.
     */
    private List<JingleNodeDescriptor> additionalJingleNodes
        = new ArrayList<JingleNodeDescriptor>();

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
        super(null, new HashMap<String, String>());
    }

    /**
     * Overrides to return UID loaded from edited AccountID.
     * @return UID of edited account.
     */
    public String getAccountUniqueID()
    {
        return editedAccUID;
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
     * Sets the User ID of the jabber registration account.
     *
     * @param userID the identifier of the jabber registration account.
     */
    public void setUserID(String userID)
    {
        setOrRemoveIfEmpty(ProtocolProviderFactory.USER_ID, userID);
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID()
    {
        return getAccountPropertyString(ProtocolProviderFactory.USER_ID);
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
        if(rememberPassword)
            setPassword(passwd);
        else
            setPassword(null);

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

        if (serverName == null || serverName.length() <= 0)
            throw new OperationFailedException(
                    "Should specify a server for user name " + userName + ".",
                    OperationFailedException.SERVER_NOT_SPECIFIED);

        // Remove additional STUN servers and Jingle Nodes properties,
        // before entering new from lists
        BundleContext bContext = ProtocolProviderActivator.getBundleContext();
        ProtocolProviderFactory jbfFactory
                = ProtocolProviderFactory
                        .getProtocolProviderFactory( bContext,
                                                     ProtocolNames.JABBER );
        AccountManager accManager =
                ProtocolProviderActivator.getAccountManager();
        String accountNodeName =
                accManager.getAccountNodeName(jbfFactory, editedAccUID);
        // Only if the account is stored in config
        if(accountNodeName != null)
        {
            ConfigurationService configSrvc =
                    ProtocolProviderActivator.getConfigurationService();
            String factoryPackage =
                    accManager.getFactoryImplPackageName(jbfFactory);
            String accountPrefix = factoryPackage + "." + accountNodeName;

            List<String> allProperties = configSrvc.getAllPropertyNames();
            String stunPrefix
                    = accountPrefix+"."+ProtocolProviderFactory.STUN_PREFIX;
            String jinglePrefix
                    = accountPrefix+"."+JingleNodeDescriptor.JN_PREFIX;
            for(String property : allProperties)
            {
                if( property.startsWith(stunPrefix)
                        || property.startsWith(jinglePrefix) )
                {
                    configSrvc.removeProperty(property);
                }
            }
            // Also from this instance
            String[] accKeys
                    = this.accountProperties.keySet().toArray(
                            new String[accountProperties.size()]);
            for(String property : accKeys)
            {
                if(property.startsWith(ProtocolProviderFactory.STUN_PREFIX)
                        || property.startsWith(JingleNodeDescriptor.JN_PREFIX))
                {
                    this.accountProperties.remove(property);
                }
            }
        }

        List<StunServerDescriptor> stunServers = getAdditionalStunServers();

        int serverIndex = -1;

        for(StunServerDescriptor stunServer : stunServers)
        {
            serverIndex ++;

            stunServer.storeDescriptor(
                    this.accountProperties,
                    ProtocolProviderFactory.STUN_PREFIX + serverIndex);
        }

        List<JingleNodeDescriptor> jnRelays = getAdditionalJingleNodes();
        serverIndex = -1;
        for(JingleNodeDescriptor jnRelay : jnRelays)
        {
            serverIndex ++;

            jnRelay.storeDescriptor(this.accountProperties,
                                    JingleNodeDescriptor.JN_PREFIX+serverIndex);
        }

        securityRegistration.storeProperties(this.accountProperties);

        encodingsRegistration.storeProperties(this.accountProperties);

        super.storeProperties(
                protocolIconPath, accountIconPath, accountProperties);
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
        mergeProperties(account.getAccountProperties(), accountProperties);

        String password
            = ProtocolProviderFactory.getProtocolProviderFactory(
                    bundleContext,
                    ProtocolNames.JABBER).loadPassword(account);

        setUserID(account.getUserID());

        editedAccUID = account.getAccountUniqueID();

        setPassword(password);

        rememberPassword = password != null;

        //Security properties
        securityRegistration.loadAccount(account);

        // ICE
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

        // Encodings
        encodingsRegistration.loadAccount(
                account,
                ServiceUtils.getService(bundleContext, MediaService.class));
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
