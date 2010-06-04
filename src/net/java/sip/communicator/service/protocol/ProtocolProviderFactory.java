/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * The ProtocolProviderFactory is what actually creates instances of a
 * ProtocolProviderService implementation. A provider factory  would register,
 * persistently store, and remove when necessary, ProtocolProviders. The way
 * things are in the SIP Communicator, a user account is represented (in a 1:1
 * relationship) by  an AccountID and a ProtocolProvider. In other words - one
 * would have as many protocol providers installed in a given moment as they
 * would user account registered through the various services.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class ProtocolProviderFactory
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderFactory.class);
    /**
     * Then name of a property which represents a password.
     */
    public static final String PASSWORD = "PASSWORD";

    /**
     * The name of a property representing the name of the protocol for an
     * ProtocolProviderFactory.
     */
    public static final String PROTOCOL = "PROTOCOL_NAME";

    /**
     * The name of a property representing the path to protocol icons.
     */
    public static final String PROTOCOL_ICON_PATH = "PROTOCOL_ICON_PATH";

    /**
     * The name of a property which represents the AccountID of a
     * ProtocolProvider and that, together with a password is used to login
     * on the protocol network..
     */
    public static final String USER_ID = "USER_ID";

    /**
     * The name that should be displayed to others when we are calling or
     * writing them.
     */
    public static final String DISPLAY_NAME = "DISPLAY_NAME";

    /**
     * The name of the property under which we store protocol AccountID-s.
     */
    public static final String ACCOUNT_UID = "ACCOUNT_UID";

    /**
     * The name of the property under which we store protocol the address of
     * a protocol centric entity (any protocol server).
     */
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";

    /**
     * The name of the property under which we store the number of the port
     * where the server stored against the SERVER_ADDRESS property is expecting
     * connections to be made via this protocol.
     */
    public static final String SERVER_PORT = "SERVER_PORT";

    /**
     * The name of the property under which we store the name of the transport
     * protocol that needs to be used to access the server.
     */
    public static final String SERVER_TRANSPORT = "SERVER_TRANSPORT";

    /**
     * The name of the property under which we store protocol the address of
     * a protocol proxy.
     */
    public static final String PROXY_ADDRESS = "PROXY_ADDRESS";

    /**
     * The name of the property under which we store the number of the port
     * where the proxy stored against the PROXY_ADDRESS property is expecting
     * connections to be made via this protocol.
     */
    public static final String PROXY_PORT = "PROXY_PORT";

    /**
     * The property indicating the preferred UDP and TCP
     * port to bind to for clear communications.
     */
    public static final String PREFERRED_CLEAR_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_CLEAR_PORT";

    /**
     * The property indicating the preferred TLS (TCP)
     * port to bind to for secure communications.
     */
    public static final String PREFERRED_SECURE_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_SECURE_PORT";

    /**
     * The name of the property under which we store the the type of the proxy
     * stored against the PROXY_ADDRESS property. Exact type values depend on
     * protocols and among them are socks4, socks5, http and possibly others.
     */
    public static final String PROXY_TYPE = "PROXY_TYPE";

    /**
    * The name of the property under which we store the the username for the proxy
    * stored against the PROXY_ADDRESS property.
    */
   public static final String PROXY_USERNAME = "PROXY_USERNAME";

   /**
    * The name of the property under which we store the the authorization name
    * for the proxy stored against the PROXY_ADDRESS property.
    */
   public static final String AUTHORIZATION_NAME = "AUTHORIZATION_NAME";

   /**
    * The name of the property under which we store the password for the proxy
    * stored against the PROXY_ADDRESS property.
    */
   public static final String PROXY_PASSWORD = "PROXY_PASSWORD";

    /**
     * The name of the property under which we store the name of the transport
     * protocol that needs to be used to access the proxy.
     */
    public static final String PROXY_TRANSPORT = "PROXY_TRANSPORT";


    /**
     * The name of the property under which we store the user preference for a
     * transport protocol to use (i.e. tcp or udp).
     */
    public static final String PREFERRED_TRANSPORT = "PREFERRED_TRANSPORT";

    /**
     * The name of the property under which we store resources such as the jabber
     * resource property.
     */
    public static final String RESOURCE = "RESOURCE";

    /**
     * The name of the property under which we store resource priority.
     */
    public static final String RESOURCE_PRIORITY = "RESOURCE_PRIORITY";
    
    /**
     * The name of the property which defines that the call is encrypted by default
     */
    public static final String DEFAULT_ENCRYPTION = "DEFAULT_ENCRYPTION"; 

    /**
     * The name of the property which defines if to include the ZRTP attribute to SIP/SDP
     */
    public static final String DEFAULT_SIPZRTP_ATTRIBUTE = "DEFAULT_SIPZRTP_ATTRIBUTE";
    
    /**
     * The name of the property under which we store the boolean value
     * indicating if the user name should be automatically changed if the
     * specified name already exists. This property is meant to be used by IRC
     * implementations.
     */
    public static final String AUTO_CHANGE_USER_NAME = "AUTO_CHANGE_USER_NAME";

    /**
     * The name of the property under which we store the boolean value
     * indicating if a password is required. Initially this property is meant to
     * be used by IRC implementations.
     */
    public static final String NO_PASSWORD_REQUIRED = "NO_PASSWORD_REQUIRED";

    /**
     * The name of the property under which we store if the presence is enabled.
     */
    public static final String IS_PRESENCE_ENABLED = "IS_PRESENCE_ENABLED";

    /**
     * The name of the property under which we store if the p2p mode for SIMPLE
     * should be forced.
     */
    public static final String FORCE_P2P_MODE = "FORCE_P2P_MODE";

    /**
     * The name of the property under which we store the offline contact polling
     * period for SIMPLE.
     */
    public static final String POLLING_PERIOD = "POLLING_PERIOD";

    /**
     * The name of the property under which we store the chosen default
     * subscription expiration value for SIMPLE.
     */
    public static final String SUBSCRIPTION_EXPIRATION
                                                = "SUBSCRIPTION_EXPIRATION";

    /**
     * Indicates if the server address has been validated.
     */
    public static final String SERVER_ADDRESS_VALIDATED
                                                = "SERVER_ADDRESS_VALIDATED";

    /**
     * Indicates if the server settings are over
     */
    public static final String IS_SERVER_OVERRIDDEN
                                                = "IS_SERVER_OVERRIDDEN";
    /**
     * Indicates if the proxy address has been validated.
     */
    public static final String PROXY_ADDRESS_VALIDATED
                                                = "PROXY_ADDRESS_VALIDATED";

    /**
     * Indicates the search strategy chosen for the DICT protocole.
     */
    public static final String STRATEGY = "STRATEGY";

    /**
     * Indicates a protocol that would not be shown in the user interface as an
     * account.
     */
    public static final String IS_PROTOCOL_HIDDEN = "IS_PROTOCOL_HIDDEN";

    /**
     * The <code>BundleContext</code> containing (or to contain) the service
     * registration of this factory.
     */
    private final BundleContext bundleContext;

    /**
     * The name of the protocol this factory registers its
     * <code>ProtocolProviderService</code>s with and to be placed in the
     * properties of the accounts created by this factory.
     */
    private final String protocolName;

    /**
     * The table that we store our accounts in.
     * <p>
     * TODO Synchronize the access to the field which may in turn be better
     * achieved by also hiding it from protected into private access.
     * </p>
     */
    protected final Hashtable<AccountID, ServiceRegistration> registeredAccounts =
        new Hashtable<AccountID, ServiceRegistration>();

    protected ProtocolProviderFactory(BundleContext bundleContext,
        String protocolName)
    {
        this.bundleContext = bundleContext;
        this.protocolName = protocolName;
    }

    /**
     * Gets the <code>BundleContext</code> containing (or to contain) the
     * service registration of this factory.
     * 
     * @return the <code>BundleContext</code> containing (or to contain) the
     *         service registration of this factory
     */
    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. Note that account
     * registration is persistent and accounts that are registered during
     * a particular sip-communicator session would be automatically reloaded
     * during all following sessions until they are removed through the
     * removeAccount method.
     *
     * @param userID the user identifier uniquely representing the newly
     * created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws java.lang.IllegalArgumentException if userID does not correspond
     * to an identifier in the context of the underlying protocol or if
     * accountProperties does not contain a complete set of account installation
     * properties.
     * @throws java.lang.IllegalStateException if the account has already been
     * installed.
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    public abstract AccountID installAccount(String userID,
                                             Map<String, String>    accountProperties)
        throws IllegalArgumentException,
               IllegalStateException,
               NullPointerException;


    /**
     * Modifies the account corresponding to the specified accountID. This
     * method is meant to be used to change properties of already existing
     * accounts. Note that if the given accountID doesn't correspond to any
     * registered account this method would do nothing.
     *
     * @param protocolProvider the protocol provider service corresponding to
     * the modified account.
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     *
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    public abstract void modifyAccount(
                                ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
            throws NullPointerException;

    /**
     * Returns a copy of the list containing the <tt>AccountID</tt>s of all
     * accounts currently registered in this protocol provider.
     * @return a copy of the list containing the <tt>AccountID</tt>s of all
     * accounts currently registered in this protocol provider.
     */
    public ArrayList<AccountID> getRegisteredAccounts()
    {
        synchronized (registeredAccounts)
        {
            return new ArrayList<AccountID>(registeredAccounts.keySet());
        }
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknown to the
     * provider factory.
     */
    public ServiceReference getProviderForAccount(AccountID accountID)
    {
        ServiceRegistration registration;

        synchronized (registeredAccounts)
        {
            registration =
                registeredAccounts.get(accountID);
        }
        return (registration == null) ? null : registration.getReference();
    }

    /**
     * Removes the specified account from the list of accounts that this
     * provider factory is handling. If the specified accountID is unknown to the
     * ProtocolProviderFactory, the call has no effect and false is returned.
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
        // Unregister the protocol provider.
        ServiceReference serRef = getProviderForAccount(accountID);

        if (serRef == null)
        {
            return false;
        }

        BundleContext bundleContext = getBundleContext();
        ProtocolProviderService protocolProvider =
            (ProtocolProviderService) bundleContext.getService(serRef);

        try
        {
            protocolProvider.unregister();
        }
        catch (OperationFailedException ex)
        {
            logger
                .error("Failed to unregister protocol provider for account : "
                    + accountID + " caused by: " + ex);
        }

        ServiceRegistration registration;

        synchronized (registeredAccounts)
        {
            registration = registeredAccounts.remove(accountID);
        }
        if (registration == null)
        {
            return false;
        }

        // Kill the service.
        registration.unregister();

        return removeStoredAccount(bundleContext, accountID);
    }

    /**
     * The method stores the specified account in the configuration service
     * under the package name of the source factory. The restore and remove
     * account methods are to be used to obtain access to and control the stored
     * accounts.
     * <p>
     * In order to store all account properties, the method would create an
     * entry in the configuration service corresponding (beginning with) the
     * <tt>sourceFactory</tt>'s package name and add to it a unique identifier
     * (e.g. the current miliseconds.)
     * </p>
     * 
     * @param accountID the AccountID corresponding to the account that we would
     *            like to store.
     */
    protected void storeAccount(AccountID accountID)
    {
        getAccountManager().storeAccount(this, accountID);
    }

    /**
     * Saves the password for the specified account after scrambling it a bit so
     * that it is not visible from first sight. (The method remains highly
     * insecure).
     * 
     * @param accountID the AccountID for the account whose password we're
     *            storing
     * @param password the password itself
     * 
     * @throws IllegalArgumentException if no account corresponding to
     *             <code>accountID</code> has been previously stored
     */
    public void storePassword(AccountID accountID, String password)
        throws IllegalArgumentException
    {
        storePassword(getBundleContext(), accountID, password);
    }

    /**
     * Saves the password for the specified account after scrambling it a bit
     * so that it is not visible from first sight (Method remains highly
     * insecure).
     * <p>
     * TODO Delegate the implementation to {@link AccountManager} because it
     * knows the format in which the password (among the other account
     * properties) is to be saved.
     * </p>
     * 
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID for the account whose password we're
     *            storing.
     * @param password the password itself.
     * 
     * @throws IllegalArgumentException if no account corresponding to
     *             <tt>accountID</tt> has been previously stored.
     */
    protected void storePassword(BundleContext bundleContext,
                                 AccountID    accountID,
                                 String       password)
        throws IllegalArgumentException
    {
        String accountPrefix = findAccountPrefix(
            bundleContext, accountID);

        if (accountPrefix == null)
            throw new IllegalArgumentException(
                "No previous records found for account ID: "
                + accountID.getAccountUniqueID()
                + " in package" + getFactoryImplPackageName());

        //obscure the password
        String mangledPassword = null;

        //if password is null then the caller simply wants the current password
        //removed from the cache. make sure they don't get a null pointer
        //instead.
        if(password != null)
            mangledPassword = new String(Base64.encode(password.getBytes()));

        //get a reference to the config service and store it.
        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

       configurationService.setProperty(
                accountPrefix + "." + PASSWORD, mangledPassword);
    }

    /**
     * Returns the password last saved for the specified account.
     * 
     * @param accountID the AccountID for the account whose password we're
     *            looking for
     * 
     * @return a String containing the password for the specified accountID
     */
    public String loadPassword(AccountID accountID)
    {
        return loadPassword(getBundleContext(), accountID);
    }

    /**
     * Returns the password last saved for the specified account.
     * <p>
     * TODO Delegate the implementation to {@link AccountManager} because it
     * knows the format in which the password (among the other account
     * properties) was saved.
     * </p>
     * 
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID for the account whose password we're
     *            looking for..
     * 
     * @return a String containing the password for the specified accountID.
     */
    protected String loadPassword(BundleContext bundleContext,
                                  AccountID     accountID)
    {
        String accountPrefix = findAccountPrefix(
            bundleContext, accountID);

        if (accountPrefix == null)
            return null;

        //get a reference to the config service and store it.
        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //obscure the password
         String mangledPassword
             =  configurationService.getString(
                    accountPrefix + "." + PASSWORD);

         if(mangledPassword == null)
             return null;

         return new String(Base64.decode(mangledPassword));
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstallAccount method.
     * 
     * @param accountProperties a set of protocol (or implementation) specific
     *            properties defining the new account.
     * @return the AccountID of the newly loaded account
     */
    public AccountID loadAccount(Map<String, String> accountProperties)
    {
        BundleContext bundleContext = getBundleContext();
        if (bundleContext == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        String userID = accountProperties.get(USER_ID);
        if (userID == null)
            throw new NullPointerException(
                "The account properties contained no user id.");

        String protocolName = getProtocolName();
        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, protocolName);

        AccountID accountID = createAccountID(userID, accountProperties);

        ProtocolProviderService service = createService(userID, accountID);

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, protocolName);
        properties.put(USER_ID, userID);

        ServiceRegistration serviceRegistration =
            bundleContext.registerService(ProtocolProviderService.class
                .getName(), service, properties);

        synchronized (registeredAccounts)
        {
            registeredAccounts.put(accountID, serviceRegistration);
        }

        return accountID;
    }

    /**
     * Creates a new <code>AccountID</code> instance with a specific user ID to
     * represent a given set of account properties.
     * <p>
     * The method is a pure factory allowing implementers to specify the runtime
     * type of the created <code>AccountID</code> and customize the instance.
     * The returned <code>AccountID</code> will later be associated with a
     * <code>ProtocolProviderService</code> by the caller (e.g. using
     * {@link #createService(String, AccountID)}).
     * </p>
     * 
     * @param userID the user ID of the new instance
     * @param accountProperties the set of properties to be represented by the
     *            new instance
     * @return a new <code>AccountID</code> instance with the specified user ID
     *         representing the given set of account properties
     */
    protected abstract AccountID createAccountID(
        String userID, Map<String, String> accountProperties);

    /**
     * Gets the name of the protocol this factory registers its
     * <code>ProtocolProviderService</code>s with and to be placed in the
     * properties of the accounts created by this factory.
     * 
     * @return the name of the protocol this factory registers its
     *         <code>ProtocolProviderService</code>s with and to be placed in
     *         the properties of the accounts created by this factory
     */
    public String getProtocolName()
    {
        return protocolName;
    }

    /**
     * Initializes a new <code>ProtocolProviderService</code> instance with a
     * specific user ID to represent a specific <code>AccountID</code>.
     * <p>
     * The method is a pure factory allowing implementers to specify the runtime
     * type of the created <code>ProtocolProviderService</code> and customize
     * the instance. The caller will later register the returned service with
     * the <code>BundleContext</code> of this factory.
     * </p>
     * 
     * @param userID the user ID to initialize the new instance with
     * @param accountID the <code>AccountID</code> to be represented by the new
     *            instance
     * @return a new <code>ProtocolProviderService</code> instance with the
     *         specific user ID representing the specified
     *         <code>AccountID</code>
     */
    protected abstract ProtocolProviderService createService(String userID,
        AccountID accountID);

    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     * <p>
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID of the account to remove.
     * <p>
     * @return true if an account has been removed and false otherwise.
     */
    protected boolean removeStoredAccount(BundleContext bundleContext,
                                          AccountID     accountID)
    {
        String sourcePackageName = getFactoryImplPackageName();

        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List<String> storedAccounts = configurationService.getPropertyNamesByPrefix(
            sourcePackageName, true);

        //find an account with the corresponding id.
        for (String accountRootPropertyName : storedAccounts)
        {
            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.getString(
                accountRootPropertyName //node id
                + "." + ACCOUNT_UID); // propname

            if (accountUID.equals(accountID.getAccountUniqueID()))
            {
                //retrieve the names of all properties registered for the
                //current account.
                List<String> accountPropertyNames
                    = configurationService.getPropertyNamesByPrefix(
                        accountRootPropertyName, false);

                //set all account properties to null in order to remove them.
                for (String propName : accountPropertyNames)
                {
                    configurationService.setProperty(propName, null);
                }

                //and now remove the parent too.
                configurationService.setProperty(
                    accountRootPropertyName, null);

                return true;
            }
        }
        return false;
    }

    /**
     * Returns the prefix for all persistently stored properties of the account
     * with the specified id.
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID of the account whose properties we're
     * looking for.
     * @return a String indicating the ConfigurationService property name
     * prefix under which all account properties are stored or null if no
     * account corresponding to the specified id was found.
     */
    protected String findAccountPrefix(BundleContext bundleContext,
                                       AccountID     accountID)
    {
        String sourcePackageName = getFactoryImplPackageName();

        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List<String> storedAccounts = configurationService.getPropertyNamesByPrefix(
            sourcePackageName, true);

        //find an account with the corresponding id.
        for (String accountRootPropertyName : storedAccounts)
        {
            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.getString(
                accountRootPropertyName //node id
                + "." + ACCOUNT_UID); // propname

            if (accountUID.equals(accountID.getAccountUniqueID()))
            {
                return accountRootPropertyName;
            }
        }
        return null;
    }

    /**
     * Returns the name of the package that we're currently running in (i.e.
     * the name of the package containing the proto factory that extends us).
     *
     * @return a String containing the package name of the concrete factory
     * class that extends us.
     */
    private String getFactoryImplPackageName()
    {
        String className = getClass().getName();

        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Prepares the factory for bundle shutdown.
     */
    public void stop()
    {
        if (logger.isTraceEnabled())
            logger.trace("Preparing to stop all protocol providers of" + this);

        synchronized (registeredAccounts)
        {
            for (Enumeration<ServiceRegistration> registrations =
                registeredAccounts.elements(); registrations.hasMoreElements();)
            {
                ServiceRegistration reg = registrations.nextElement();

                stop(reg);

                reg.unregister();
            }

            registeredAccounts.clear();
        }
    }

    /**
     * Shuts down the <code>ProtocolProviderService</code> representing an
     * account registered with this factory.
     * 
     * @param registeredAccount the <code>ServiceRegistration</code> of the
     *            <code>ProtocolProviderService</code> representing an account
     *            registered with this factory
     */
    protected void stop(ServiceRegistration registeredAccount)
    {
        ProtocolProviderService protocolProviderService =
            (ProtocolProviderService) getBundleContext().getService(
                registeredAccount.getReference());

        protocolProviderService.shutdown();
    }

    private AccountManager getAccountManager()
    {
        BundleContext bundleContext = getBundleContext();
        ServiceReference serviceReference =
            bundleContext.getServiceReference(AccountManager.class.getName());

        return (AccountManager) bundleContext.getService(serviceReference);
    }
}
