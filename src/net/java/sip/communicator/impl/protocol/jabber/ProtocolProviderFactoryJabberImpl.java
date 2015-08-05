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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.osgi.framework.*;

/**
 * The Jabber implementation of the ProtocolProviderFactory.
 * @author Damian Minkov
 */
public class ProtocolProviderFactoryJabberImpl
    extends ProtocolProviderFactory
{
    /**
     * Indicates if ICE should be used.
     */
    public static final String IS_USE_JINGLE_NODES = "JINGLE_NODES_ENABLED";

    /**
     * Our provider manager instances.
     */
    static ProviderManager providerManager = null;

    static
    {
        try
        {
            ProviderManager.setInstance(new ProviderManagerExt());
        }
        catch(Throwable t)
        {
            // once loaded if we try to set instance second time
            // IllegalStateException is thrown
        }
        finally
        {
            providerManager = ProviderManager.getInstance();
        }

        // checks class names, not using instanceof
        // tests do unloading and loading the protocol bundle and
        // ProviderManagerExt class get loaded two times from different
        // classloaders
        if (!(providerManager.getClass().getName()
                .equals(ProviderManagerExt.class.getName())))
        {
            throw new RuntimeException(
                "ProviderManager set to the default one");
        }
    }

    /**
     * Creates an instance of the ProtocolProviderFactoryJabberImpl.
     */
    protected ProtocolProviderFactoryJabberImpl()
    {
        super(JabberActivator.getBundleContext(), ProtocolNames.JABBER);
    }

    /**
     * Ovverides the original in order give access to protocol implementation.
     *
     * @param accountID the account identifier.
     */
    @Override
    protected void storeAccount(AccountID accountID)
    {
        super.storeAccount(accountID);
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
        BundleContext context
            = JabberActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException(
                    "The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                    "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        // if server address is null, we must extract it from userID
        if(accountProperties.get(SERVER_ADDRESS) == null)
        {
            String serverAddress = StringUtils.parseServer(userIDStr);

            if (serverAddress != null)
                accountProperties.put(SERVER_ADDRESS,
                                      StringUtils.parseServer(userIDStr));
            else throw new IllegalArgumentException(
                "Should specify a server for user name " + userIDStr + ".");
        }

        // if server port is null, we will set default value
        if(accountProperties.get(SERVER_PORT) == null)
        {
            accountProperties.put(SERVER_PORT,
                                  "5222");
        }

        AccountID accountID
                = new JabberAccountIDImpl(userIDStr, accountProperties);

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
     * Create an account.
     *
     * @param userID the user ID
     * @param accountProperties the properties associated with the user ID
     * @return new <tt>AccountID</tt>
     */
    @Override
    protected AccountID createAccountID(String userID,
            Map<String, String> accountProperties)
    {
        return new JabberAccountIDImpl(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceJabberImpl service =
            new ProtocolProviderServiceJabberImpl();

        service.initialize(userID, accountID);
        return service;
    }

    /**
     * Modify an existing account.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> responsible
     * of the account
     * @param accountProperties modified properties to be set
     */
    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        BundleContext context
            = JabberActivator.getBundleContext();

        if (context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (protocolProvider == null)
            throw new NullPointerException(
                "The specified Protocol Provider was null");

        JabberAccountIDImpl accountID
            = (JabberAccountIDImpl) protocolProvider.getAccountID();

        // If the given accountID doesn't correspond to an existing account
        // we return.
        if(!registeredAccounts.containsKey(accountID))
            return;

        ServiceRegistration registration = registeredAccounts.get(accountID);

        // kill the service
        if (registration != null)
        {
            // unregister provider before removing it.
            try
            {
                if(protocolProvider.isRegistered())
                {
                    protocolProvider.unregister();
                    protocolProvider.shutdown();
                }
            } catch (Throwable e)
            {
                // we don't care for this, cause we are modifying and
                // will unregister the service and will register again
            }

            registration.unregister();
        }

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        accountProperties.put(USER_ID, accountID.getUserID());

        String serverAddress = accountProperties.get(SERVER_ADDRESS);

        if(serverAddress == null)
            throw new NullPointerException("null is not a valid ServerAddress");

        // if server port is null, we will set default value
        if(accountProperties.get(SERVER_PORT) == null)
        {
            accountProperties.put(SERVER_PORT,
                                  "5222");
        }

        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, ProtocolNames.JABBER);

        accountID.setAccountProperties(accountProperties);

        // First store the account and only then load it as the load generates
        // an osgi event, the osgi event triggers (trhgough the UI) a call to
        // the register() method and it needs to acces the configuration service
        // and check for a password.
        this.storeAccount(accountID);

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, ProtocolNames.JABBER);
        properties.put(USER_ID, accountID.getUserID());

        ((ProtocolProviderServiceJabberImpl) protocolProvider)
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
