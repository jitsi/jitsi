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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The ICQ implementation of the ProtocolProviderFactory.
 * @author Emil Ivov
 */
public class ProtocolProviderFactoryIcqImpl
    extends ProtocolProviderFactory
{

    /**
     * Is this factory is created for aim or icq accounts
     */
    private boolean isAimFactory = false;

    /**
     * Creates an instance of the ProtocolProviderFactoryIcqImpl.
     * @param isAimFactory whether its an aim factory
     */
    protected ProtocolProviderFactoryIcqImpl(boolean isAimFactory)
    {
        super(IcqActivator.getBundleContext(), isAimFactory ? ProtocolNames.AIM
            : ProtocolNames.ICQ);

        this.isAimFactory = isAimFactory;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstall account method.
     *
     * @param userIDStr the user identifier for the new account
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    @Override
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        BundleContext context = IcqActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");
        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        // we are installing new aim account from the wizzard, so mark it as aim
        if(isAimFactory)
            accountProperties.put(IcqAccountID.IS_AIM, "true");

        AccountID accountID = new IcqAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if( registeredAccounts.containsKey(accountID) )
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to
        //the register() method and it needs to access the configuration service
        //and check for a password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param accountProperties a set of protocol (or implementation) specific
     *            properties defining the new account.
     * @return the AccountID of the newly created account
     */
    @Override
    public AccountID loadAccount(Map<String, String> accountProperties)
    {
        // there are two factories - one for icq accounts and one for aim ones.
        // if we are trying to load an icq account in aim factory - skip it
        // and the same for aim accounts in icq factory
        boolean accountPropertiesIsAIM = IcqAccountID.isAIM(accountProperties);
        if ((accountPropertiesIsAIM && !isAimFactory)
            || (!accountPropertiesIsAIM && isAimFactory))
        {
            return null;
        }

        return super.loadAccount(accountProperties);
    }

    /**
     * Creates a protocol provider for the given <tt>accountID</tt> and
     * registers it in the bundle context. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstallAccount method.
     *
     * @param accountID the account identifier
     * @return <tt>true</tt> if the account with the given <tt>accountID</tt> is
     * successfully loaded, otherwise returns <tt>false</tt>
     */
    @Override
    public boolean loadAccount(AccountID accountID)
    {
        // there are two factories - one for icq accounts and one for aim ones.
        // if we are trying to load an icq account in aim factory - skip it
        // and the same for aim accounts in icq factory
        boolean accountPropertiesIsAIM
            = IcqAccountID.isAIM(accountID.getAccountProperties());
        if ((accountPropertiesIsAIM && !isAimFactory)
            || (!accountPropertiesIsAIM && isAimFactory))
        {
            return false;
        }
        return super.loadAccount(accountID);
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties.
     *
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     * @return the AccountID of the newly created account
     */
    @Override
    public AccountID createAccount(Map<String, String> accountProperties)
    {
        // there are two factories - one for icq accounts and one for aim ones.
        // if we are trying to load an icq account in aim factory - skip it
        // and the same for aim accounts in icq factory
        boolean accountPropertiesIsAIM = IcqAccountID.isAIM(accountProperties);

        if ((accountPropertiesIsAIM && !isAimFactory)
            || (!accountPropertiesIsAIM && isAimFactory))
        {
            return null;
        }

        return super.createAccount(accountProperties);
    }

    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new IcqAccountID(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceIcqImpl service =
            new ProtocolProviderServiceIcqImpl();

        service.initialize(userID, accountID);
        return service;
    }

    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        // Make sure the specified arguments are valid.
        if (protocolProvider == null)
            throw new NullPointerException(
                "The specified Protocol Provider was null");
        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        BundleContext context
            = IcqActivator.getBundleContext();

        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        IcqAccountID accountID = (IcqAccountID) protocolProvider.getAccountID();

        // If the given accountID doesn't correspond to an existing account
        // we return.
        if(!registeredAccounts.containsKey(accountID))
            return;

        ServiceRegistration registration = registeredAccounts.get(accountID);

        // kill the service
        if (registration != null)
            registration.unregister();

        accountProperties.put(USER_ID, accountID.getUserID());

        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, ProtocolNames.ICQ);

        accountID.setAccountProperties(accountProperties);

        // First store the account and only then load it as the load generates
        // an osgi event, the osgi event triggers (trhgough the UI) a call to
        // the register() method and it needs to acces the configuration service
        // and check for a password.
        this.storeAccount(accountID);

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, ProtocolNames.ICQ);
        properties.put(USER_ID, accountID.getUserID());

        ((ProtocolProviderServiceIcqImpl) protocolProvider)
            .initialize(accountID.getUserID(), accountID);

        // We store again the account in order to store all properties added
        // during the protocol provider initialization.
        this.storeAccount(accountID);

        registration
            = context.registerService(
                        ProtocolProviderService.class.getName(),
                        protocolProvider,
                        properties);

        registeredAccounts.put(accountID, registration);
    }
}
