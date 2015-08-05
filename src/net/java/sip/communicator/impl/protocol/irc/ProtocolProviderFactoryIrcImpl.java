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
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The IRC protocol provider factory creates instances of the IRC protocol
 * provider service. One Service instance corresponds to one account.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Danny van Heumen
 */
public class ProtocolProviderFactoryIrcImpl
    extends ProtocolProviderFactory
{
    /**
     * Property indicating whether or not to enable a periodic task for querying
     * channel member presence (available, away).
     */
    public static final String CHAT_ROOM_PRESENCE_TASK =
        "CHAT_ROOM_PRESENCE_TASK";

    /**
     * Property indicating whether or not to enable a periodic task for querying
     * contact presence (offline, online).
     */
    public static final String CONTACT_PRESENCE_TASK = "CONTACT_PRESENCE_TASK";

    /**
     * Property indicating SASL is enabled.
     */
    public static final String SASL_ENABLED = "SASL_ENABLED";

    /**
     * Property for SASL user name.
     */
    public static final String SASL_USERNAME = "SASL_USERNAME";

    /**
     * Property for SASL authorization role.
     */
    public static final String SASL_ROLE = "SASL_ROLE";

    /**
     * Property for indicating that DNS names should be resolved through the
     * SOCKS proxy if a proxy server is configured.
     */
    public static final String RESOLVE_DNS_THROUGH_PROXY =
        "RESOLVE_DNS_THROUGH_PROXY";

 /**
     * Constructor.
     */
    public ProtocolProviderFactoryIrcImpl()
    {
        super(IrcActivator.getBundleContext(), ProtocolNames.IRC);
    }

    /**
     * Initialized and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    @Override
    public AccountID installAccount(final String userIDStr,
                                    final Map<String, String> accountProperties)
    {
        BundleContext context = IrcActivator.getBundleContext();

        if (context == null)
        {
            throw new NullPointerException(
                "The specified BundleContext was null");
        }

        if (userIDStr == null)
        {
            throw new NullPointerException(
                "The specified AccountID was null");
        }

        if (accountProperties == null)
        {
            throw new NullPointerException(
                "The specified property map was null");
        }

        accountProperties.put(USER_ID, userIDStr);
        final String host =
            accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);
        final String port =
            accountProperties.get(ProtocolProviderFactory.SERVER_PORT);
        AccountID accountID =
            new IrcAccountID(userIDStr, host, port, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
        {
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");
        }

        //first store the account and only then load it as the load generates
        //an OSGI event, the OSGI event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to access
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Create an IRC Account ID.
     *
     * {@inheritDoc}
     */
    @Override
    protected AccountID createAccountID(final String userID,
        final Map<String, String> accountProperties)
    {
        final String host =
            accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);
        final String port =
            accountProperties.get(ProtocolProviderFactory.SERVER_PORT);
        return new IrcAccountID(userID, host, port, accountProperties);
    }

    /**
     * Create an IRC provider service.
     *
     * {@inheritDoc}
     */
    @Override
    protected ProtocolProviderService createService(final String userID,
        final AccountID accountID)
    {
        ProtocolProviderServiceIrcImpl service =
            new ProtocolProviderServiceIrcImpl();

        service.initialize(userID, accountID);
        return service;
    }

    /**
     * Modify an existing IRC account.
     *
     * {@inheritDoc}
     */
    @Override
    public void modifyAccount(final ProtocolProviderService protocolProvider,
        final Map<String, String> accountProperties)
    {
        // Make sure the specified arguments are valid.
        if (protocolProvider == null)
        {
            throw new NullPointerException(
                "The specified Protocol Provider was null");
        }
        if (!(protocolProvider instanceof ProtocolProviderServiceIrcImpl))
        {
            throw new IllegalArgumentException(
                "expected IRC instance of ProtocolProviderService");
        }
        if (accountProperties == null)
        {
            throw new NullPointerException(
                "The specified property map was null");
        }

        BundleContext context = IrcActivator.getBundleContext();
        ProtocolProviderServiceIrcImpl ircProvider =
            (ProtocolProviderServiceIrcImpl) protocolProvider;

        if (context == null)
        {
            throw new NullPointerException(
                "The specified BundleContext was null");
        }

        IrcAccountID accountID = (IrcAccountID) ircProvider.getAccountID();

        // If the given accountID doesn't correspond to an existing account
        // we return.
        if (!registeredAccounts.containsKey(accountID))
        {
            return;
        }

        ServiceRegistration<ProtocolProviderService> registration =
            registeredAccounts.get(accountID);

        // kill the service
        if (registration != null)
        {
            registration.unregister();
        }

        accountProperties.put(USER_ID, accountID.getUserID());

        if (!accountProperties.containsKey(PROTOCOL))
        {
            accountProperties.put(PROTOCOL, ProtocolNames.IRC);
        }

        accountID.setAccountProperties(accountProperties);

        // First store the account and only then load it as the load generates
        // an OSGi event, the OSGi event triggers (through the UI) a call to
        // the register() method and it needs to access the configuration
        // service and check for a password.
        this.storeAccount(accountID);

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, ProtocolNames.IRC);
        properties.put(USER_ID, accountID.getUserID());

        ircProvider.initialize(accountID.getUserID(), accountID);

        // We store again the account in order to store all properties added
        // during the protocol provider initialization.
        this.storeAccount(accountID);

        registration =
            context.registerService(ProtocolProviderService.class,
                protocolProvider, properties);

        registeredAccounts.put(accountID, registration);
    }
}
