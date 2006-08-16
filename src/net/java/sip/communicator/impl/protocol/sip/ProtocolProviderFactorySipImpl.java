package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A SIP implementation of the protocol provider factory interface.
 *
 * @author Emil Ivov
 */
public class ProtocolProviderFactorySipImpl
    extends ProtocolProviderFactory
{
    /**
     * The table that we store our accounts in.
     */
    private Hashtable registeredAccounts = new Hashtable();

    /**
     * The package name that we use to store properties in the configuration
     * service.
     */
    private String implementationPackageName = null;

    /**
     * Constructs a new instance of the ProtocolProviderFactorySipImpl.
     */
    public ProtocolProviderFactorySipImpl()
    {
        implementationPackageName
            = ProtocolProviderFactorySipImpl.class.getName().substring(0
                , ProtocolProviderFactorySipImpl.class.getName()
                .lastIndexOf("."));
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
     * Returns a copy of the list containing all accounts currently
     * registered in this protocol provider.
     *
     * @return a copy of the llist containing all accounts currently installed
     * in the protocol provider.
     */
    public ArrayList getRegisteredAccounts()
    {
        return new ArrayList(registeredAccounts.keySet());
    }


    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws IllegalArgumentException if userID does not correspond to an
     *   identifier in the context of the underlying protocol or if
     *   accountProperties does not contain a complete set of account
     *   installation properties.
     * @throws IllegalStateException if the account has already been
     *   installed.
     * @throws NullPointerException if any of the arguments is null.
     */
    public AccountID installAccount( String userIDStr,
                                 Map accountProperties)
    {
        BundleContext context
            = SipActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        accountProperties.put(USER_ID, userIDStr);

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        String serverAddress = (String)accountProperties.get(SERVER_ADDRESS);

        if(serverAddress == null)
            throw new NullPointerException(
                        serverAddress
                        + " is not a valid ServerAddress");

        AccountID accountID =
            new SipAccountID(userIDStr, accountProperties, serverAddress);

        //make sure we haven't seen this account id before.
        if( registeredAccounts.containsKey(accountID) )
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");


        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (trhgough the UI) a call to
        //the register() method and it needs to acces the configuration service
        //and check for a password.
        this.storeAccount(
            SipActivator.getBundleContext()
            , accountID
            , implementationPackageName);

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
     * @return the AccountID of the newly created account
     */
    protected AccountID loadAccount(Map accountProperties)
    {
        BundleContext context
            = SipActivator.getBundleContext();
        if(context == null)
            throw new NullPointerException("The specified BundleContext was null");

        String userIDStr = (String)accountProperties.get(USER_ID);
        if(userIDStr == null)
            throw new NullPointerException(
                "The account properties contained no user id.");

        if(accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        String serverAddress = (String)accountProperties.get(SERVER_ADDRESS);

        if(serverAddress == null)
            throw new NullPointerException(
                        serverAddress
                        + " is not a valid ServerAddress");

        AccountID accountID =
            new SipAccountID(userIDStr, accountProperties, serverAddress);

        //get a reference to the configuration service and register whatever
        //properties we have in it.

        Hashtable properties = new Hashtable();
        properties.put(PROTOCOL, ProtocolNames.SIP);
        properties.put(USER_ID, userIDStr);

        ProtocolProviderServiceSipImpl sipProtocolProvider
            = new ProtocolProviderServiceSipImpl();

        try
        {
            sipProtocolProvider.initialize(userIDStr, accountID);
        }
        catch (OperationFailedException ex)
        {
            throw new IllegalArgumentException("Failed to initialize account"
                , ex);
        }

        ServiceRegistration registration
            = context.registerService( ProtocolProviderService.class.getName(),
                                       sipProtocolProvider,
                                       properties);

        registeredAccounts.put(accountID, registration);
        return accountID;
    }

    /**
     * Loads (and hence installs) all accounts previously stored in the
     * configuration service.
     */
    public void loadStoredAccounts()
    {
        super.loadStoredAccounts(
            SipActivator.getBundleContext()
            , implementationPackageName);
    }


    /**
     * Removes the specified account from the list of accounts that this
     * provider factory is handling. If the specified accountID is unknown to
     * the ProtocolProviderFactory, the call has no effect and false is returned.
     * This method is persistent in nature and once called the account
     * corresponding to the specified ID will not be loaded during future runs
     * of the project.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was removed
     * and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration) registeredAccounts.remove(accountID);

        if (registration == null)
            return false;

        //kill the service
        registration.unregister();

        return removeStoredAccount(
            SipActivator.getBundleContext()
            , accountID
            , implementationPackageName);
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

            reg.unregister();


        }

        Enumeration idEnum = registeredAccounts.keys();

        while(idEnum.hasMoreElements())
        {
            registeredAccounts.remove(idEnum.nextElement());
        }
    }


}
