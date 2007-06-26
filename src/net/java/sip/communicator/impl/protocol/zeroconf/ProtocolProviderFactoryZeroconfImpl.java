/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderFactoryZeroconfImpl.class);

    /**
     * The table that we store our accounts in.
     */
    private Hashtable registeredAccounts = new Hashtable();


    /**
     * Creates an instance of the ProtocolProviderFactoryZeroconfImpl.
     */
    public ProtocolProviderFactoryZeroconfImpl()
    {
        super();
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding
     * to the specified accountID or null if the accountID is unknown.
     *
     * @param accountID the accountID of the protocol provider we'd like to
     *   get
     * @return a ServiceReference object to the protocol provider with the
     *   specified account id and null if the account id is unknwon to the
     *   provider factory.
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
     * Returns a copy of the list containing the <tt>AccoudID</tt>s of all
     * accounts currently registered in this protocol provider.
     *
     * @return a copy of the list containing the <tt>AccoudID</tt>s of all
     *   accounts currently registered in this protocol provider.
     */
    public ArrayList getRegisteredAccounts()
    {
        return new ArrayList(registeredAccounts.keySet());
    }

    /**
     * Loads (and hence installs) all accounts previously stored in the
     * configuration service.
     */
    public void loadStoredAccounts()
    {
        super.loadStoredAccounts( ZeroconfActivator.getBundleContext());
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
    public AccountID installAccount( String userIDStr,
                                     Map accountProperties)
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
        //ProtocolProviderService.register() method and it needs to acces
        //the configuration service and check for a stored password.
        this.storeAccount(
            ZeroconfActivator.getBundleContext()
            , accountID);

        accountID = loadAccount(accountProperties);

        return accountID;
    }
    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly loaded account
     */
    public AccountID loadAccount( Map accountProperties)
    {
        BundleContext context
            = ZeroconfActivator.getBundleContext();
        if(context == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        String userIDStr = (String)accountProperties.get(USER_ID);

        AccountID accountID =
            new ZeroconfAccountID(userIDStr, accountProperties);

        //get a reference to the configuration service and register whatever
        //properties we have in it.

        Hashtable properties = new Hashtable();
        properties.put(PROTOCOL, ProtocolNames.ZEROCONF);
        properties.put(USER_ID, userIDStr);

        ProtocolProviderServiceZeroconfImpl zeroconfProtocolProvider
            = new ProtocolProviderServiceZeroconfImpl();

        zeroconfProtocolProvider.initialize(userIDStr, accountID);

        ServiceRegistration registration
            = context.registerService( ProtocolProviderService.class.getName(),
                                       zeroconfProtocolProvider,
                                       properties);

        registeredAccounts.put(accountID, registration);

        return accountID;
    }


    /**
     * Removes the specified account from the list of accounts that this
     * provider factory is handling.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was
     *   removed and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID)
    {
        //unregister the protocol provider
        ServiceReference serRef = getProviderForAccount(accountID);

        if(serRef == null)
            return false;

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) ZeroconfActivator.getBundleContext()
                .getService(serRef);
        if (protocolProvider == null)
        {
            logger.error("ProtocolProviderService = null !!");
            return false;
        }

        try
        {
            protocolProvider.unregister();
        }
        catch (OperationFailedException exc)
        {
            logger.error("Failed to unregister protocol provider for account : "
                    + accountID + " caused by : " + exc);
        }

        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.remove(accountID);

        if(registration == null)
            return false;

        //kill the service
        registration.unregister();

        registeredAccounts.remove(accountID     );

        return removeStoredAccount(
            ZeroconfActivator.getBundleContext()
            , accountID);
    }

    /**
     * Saves the password for the specified account after scrambling it a bit
     * so that it is not visible from first sight (Method remains highly
     * insecure).
     *
     * @param accountID the AccountID for the account whose password we're
     * storing.
     * @param passwd the password itself.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public void storePassword(AccountID accountID, String passwd)
        throws IllegalArgumentException
    {
        super.storePassword(ZeroconfActivator.getBundleContext()
                            , accountID
                            , passwd);
    }

    /**
     * Returns the password last saved for the specified account.
     *
     * @param accountID the AccountID for the account whose password we're
     * looking for..
     *
     * @return a String containing the password for the specified accountID.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public String loadPassword(AccountID accountID)
        throws IllegalArgumentException
    {
        return super.loadPassword(ZeroconfActivator.getBundleContext()
                                  , accountID );
    }

    /**
     * Prepares the factory for bundle shutdown.
     */
    public void stop()
    {
        Enumeration registrations = this.registeredAccounts.elements();

        while(registrations.hasMoreElements())
        {
            ServiceRegistration reg
                = ((ServiceRegistration)registrations.nextElement());

            ProtocolProviderServiceZeroconfImpl provider
               = (ProtocolProviderServiceZeroconfImpl) ZeroconfActivator
                   .getBundleContext().getService(reg.getReference());

           //do an attempt to kill the provider
           provider.shutdown();


            reg.unregister();
        }

        registeredAccounts.clear();
    }

}
