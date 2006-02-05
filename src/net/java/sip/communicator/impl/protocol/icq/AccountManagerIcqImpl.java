/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.configuration.*;

/**
 * The ICQ implementation of the ProtocolAccountManager.
 * @author Emil Ivov
 */
public class AccountManagerIcqImpl
    implements AccountManager
{
    private Hashtable registeredAccounts = new Hashtable();


    /**
     * Creates an instance of the IcqAccountManager.
     */
    protected void IcqAccountManager()
    {

    }
    /**
     * Returns a copy of the list containing all accounts currently
     * registered in this protocol provider.
     *
     * @return a copy of the llist containing all accounts currently installed
     * in the protocol provider.
     */
    public ArrayList getRegisteredAcounts()
    {
        return new ArrayList(registeredAccounts.keySet());
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknwon to the
     * account manager.
     */
    public ServiceReference getProviderForAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.get(accountID);

        return (registration == null )
                    ? null
                    : registration.getReference();
    }

    /**
     * Initializaed and creates an accoung corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param context the BundleContext parameter where the newly created
     *   ProtocolProviderService would have to be registered.
     * @param accountIDStr the user identifier for the new account
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    public AccountID installAccount( BundleContext context,
                                     String accountIDStr,
                                     Map accountProperties)
    {
        if(context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if(accountIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if(accountIDStr == null)
            throw new NullPointerException("The specified property map was null");

        AccountID accountID = new IcqAccountID(accountIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if( registeredAccounts.containsKey(accountID) )
            throw new IllegalStateException(
                "An account for id " + accountIDStr + " was already installed!");

        Hashtable properties = new Hashtable();
        properties.put(
            AccountManager.PROTOCOL_PROPERTY_NAME, ProtocolNames.ICQ);
        properties.put(
            AccountManager.ACCOUNT_ID_PROPERTY_NAME, accountIDStr);

        ProtocolProviderServiceIcqImpl icqProtocolProvider
            = new ProtocolProviderServiceIcqImpl();

        icqProtocolProvider.initialize(accountIDStr, accountProperties);

        ServiceRegistration registration
            = context.registerService( ProtocolProviderService.class.getName(),
                                       icqProtocolProvider,
                                       properties);

        registeredAccounts.put(accountID, registration);
        return accountID;
    }

    /**
     * Removes the specified account from the list of accounts that this
     * account manager is handling. If the specified accountID is unknown to the
     * AccountManager, the call has no effect and false is returned. This method
     * is persistent in nature and once called the account corresponding to the
     * specified ID will not be loaded during future runs of the project.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was removed
     * and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.remove(accountID);

        if(registration == null)
            return false;

        //kill the service
        registration.unregister();

        return true;

    }

    /**
     * Loads all previously installed accounts that were stored in the
     * configuration service. The method is only loading accounts the first
     * time it gets called.
     *
     * @param context the context where icq protocol providers shouold be
     *        registered
     * @param configurationService ConfigurationService
     */
    void loadStoredAccounts(BundleContext context,
                            ConfigurationService configurationService)
    {
        /** @todo implement loadStoredAccounts() */
        //make sure we haven't already done so.
        //load all accounts stored in the configuration service
    }
}
