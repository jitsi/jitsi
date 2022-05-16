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
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The Gibberish protocol provider factory creates instances of the Gibberish
 * protocol provider service. One Service instance corresponds to one account.
 *
 * @author Emil Ivov
 */
public class ProtocolProviderFactoryGibberishImpl
    extends ProtocolProviderFactory
{
    /**
     * Creates an instance of the ProtocolProviderFactoryGibberishImpl.
     */
    public ProtocolProviderFactoryGibberishImpl()
    {
        super(GibberishActivator.getBundleContext(), "Gibberish");
    }

    /**
     * Initialized and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the/a user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    @Override
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        BundleContext context
            = GibberishActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new GibberishAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to acces
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Modifies the account corresponding to the specified accountID. This
     * method is meant to be used to change properties of already existing
     * accounts. Note that if the given accountID doesn't correspond to any
     * registered account this method would do nothing.
     *
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     *
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
    {
        // Make sure the specified arguments are valid.
        if (protocolProvider == null)
            throw new NullPointerException(
                "The specified Protocol Provider was null");
        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        BundleContext context
            = GibberishActivator.getBundleContext();

        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        GibberishAccountID accountID
            = (GibberishAccountID) protocolProvider.getAccountID();

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
            accountProperties.put(PROTOCOL, ProtocolNames.GIBBERISH);

        accountID.setAccountProperties(accountProperties);

        // First store the account and only then load it as the load generates
        // an osgi event, the osgi event triggers (trhgough the UI) a call to
        // the register() method and it needs to acces the configuration service
        // and check for a password.
        this.storeAccount(accountID);

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, ProtocolNames.GIBBERISH);
        properties.put(USER_ID, accountID.getUserID());

        ((ProtocolProviderServiceGibberishImpl) protocolProvider)
            .initialize(accountID.getUserID(), accountID);

        // We store again the account in order to store all properties added
        // during the protocol provider initialization.
        this.storeAccount(accountID);

        registration
            = context.registerService(
                        ProtocolProviderService.class.getName(),
                        protocolProvider,
                        properties);

        // We store the modified account registration.
        registeredAccounts.put(accountID, registration);
    }

    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new GibberishAccountID(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceGibberishImpl service =
            new ProtocolProviderServiceGibberishImpl();

        service.initialize(userID, accountID);
        return service;
    }
}
