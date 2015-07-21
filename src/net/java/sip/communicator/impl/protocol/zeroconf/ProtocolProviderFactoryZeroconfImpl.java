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
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The Zeroconf protocol provider factory creates instances of the Zeroconf
 * protocol provider service. One Service instance corresponds to one account.
 *
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ProtocolProviderFactoryZeroconfImpl
    extends ProtocolProviderFactory
{

    /**
     * Creates an instance of the ProtocolProviderFactoryZeroconfImpl.
     */
    public ProtocolProviderFactoryZeroconfImpl()
    {
        super(ZeroconfActivator.getBundleContext(), ProtocolNames.ZEROCONF);
    }

    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr tha/a user identifier uniquely representing the newly
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
            = ZeroconfActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException(
                "The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID =
            new ZeroconfAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to access
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new ZeroconfAccountID(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceZeroconfImpl service =
            new ProtocolProviderServiceZeroconfImpl();

        service.initialize(userID, accountID);
        return service;
    }

    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        // TODO Auto-generated method stub
    }
}
