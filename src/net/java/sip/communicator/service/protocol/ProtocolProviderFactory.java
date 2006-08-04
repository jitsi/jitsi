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
 * things are in the SIP Communicator, a user account is representedy (in a 1:1
 * relationship) by  an AccountID and a ProtocolProvider. In other words - one
 * would have as many protocol providers installed in a given moment as they
 * would user account registered through the various services.
 *
 * @author Emil Ivov
 */
public abstract class ProtocolProviderFactory
{
    /**
     * Then name of a property which represenstots a password.
     */
    public static final String PASSWORD = "PASSWORD";

    /**
     * The name of a property representing the name of the protocol for an
     * ProtocolProviderFactory.
     */
    public static final String PROTOCOL = "PROTOCOL_NAME";

    /**
     * The name of a property which represents the AccountID of a
     * ProtocolProvider and that, together with a password is used to login
     * on the protocol network..
     */
    public static final String USER_ID = "USER_ID";

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
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. Note that account
     * registration is persistent and accounts that are registered during
     * a particular sip-communicator session would be automatically reloaded
     * during all following sessions until they are removed through the
     * removeAccount method.
     *
     * @param userID tha/a user identifier uniquely representing the newly
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
                                             Map    accountProperties)
        throws IllegalArgumentException,
               IllegalStateException,
               NullPointerException;

    /**
     * Returns a copy of the list containing all accounts currently registered
     * in this protocol provider.
     * @return ArrayList
     */
    public abstract ArrayList getRegisteredAccounts();

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknwon to the
     * provider factory.
     */
     public abstract ServiceReference getProviderForAccount(
        AccountID accountID);

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
    public abstract boolean uninstallAccount(AccountID accountID);

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
     * <p>
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID corresponding to the account that we would
     * like to store.
     * @param sourcePackageName the name of the package to register the account
     * into (typically the java package containing the implementation calling
     * this method).
     */
    protected void storeAccount(BundleContext bundleContext,
                                AccountID accountID,
                                String sourcePackageName)
    {
        Map accountProperties = accountID.getAccountProperties();
        //create a unique node name fo the properties node that will contain
        //this account's properties.
        String accNodeName
            = "acc" + Long.toString(System.currentTimeMillis());

        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //set a value for the persistent node so that we could later retrieve it
        //as a property
        configurationService.setProperty(
            sourcePackageName //prefix
            + "." + accNodeName
            , accNodeName);

        //register the account in the configuration service.
        //we register all the properties in the following hierarchy
        //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
        configurationService.setProperty(
            sourcePackageName //prefix
            + "." + accNodeName // node name for the account id
            + "." + ACCOUNT_UID // propname
            , accountID.getAccountUniqueID()); // value

        configurationService.setProperty(
            sourcePackageName //prefix
            + "." + accNodeName // a uniew node name for the account id
            + "." + USER_ID // propname
            , accountID.getUserID()); // value

        String password = (String) accountProperties.get(PASSWORD);

        if (password != null)
            configurationService.setProperty(
                sourcePackageName //prefix
                + "." + accNodeName // a uniew node name for the account id
                + "." + PASSWORD // propname
                , new String(Base64.encode(password.getBytes()))); // value

    }


    protected void storePassword(AccountID accountID,
                                 String    password)
    {
        /** @todo implement storePassword() */
    }

    protected String loadPassword(AccountID accountID)
    {
        /** @todo implement loadPassword() */
        return null;
    }


    /**
     * Restores all accounts stored for the package corresponding to
     * sourceFactory and and installs everyone of them through the install
     * account method.
     * <p>
     * @param bundleContext a currently valid bundle context.
     * @param sourcePackageName the name of the package to register the account
     * into (typically the java package containing the implementation calling
     * this method).
     */
    protected void loadStoredAccounts(BundleContext bundleContext,
                                      String        sourcePackageName)
    {
        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List storedAccounts = configurationService.getPropertyNamesByPrefix(
            sourcePackageName, true);

        //load all accounts stored in the configuration service
        Iterator storedAccountsIter = storedAccounts.iterator();

        while (storedAccountsIter.hasNext())
        {
            String accountRootPropName = (String) storedAccountsIter.next();
            Map accountProperties = new Hashtable();

            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.getString(
                accountRootPropName //node id
                + "." + ACCOUNT_UID); // propname

            accountProperties.put(ACCOUNT_UID, accountUID);

            String userID = configurationService.getString(
                accountRootPropName //node id
                + "." + USER_ID); // propname

            String password = configurationService.getString(
                accountRootPropName //node id
                + "." + PASSWORD);

            //decode
            if (password != null)
            {
                password = new String(Base64.decode(password));
                accountProperties.put(PASSWORD, password);
            }

            loadAccount(userID, accountProperties);
        }
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
     * @return the AccountID of the newly loaded account
     */
    protected abstract AccountID loadAccount(String userIDStr,
                                            Map accountProperties);


    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     * <p>
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID of the account to remove.
     * @param sourcePackageName the name of the package to register the account
     * into (typically the java package containing the implementation calling
     * this method).
     * <p>
     * @return true if an account has been removed and false otherwise.
     */
    protected boolean removeStoredAccount(BundleContext bundleContext,
                                          AccountID     accountID,
                                          String        sourcePackageName)
    {
        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List storedAccounts = configurationService.getPropertyNamesByPrefix(
            sourcePackageName, true);

        Iterator storedAccountsIter = storedAccounts.iterator();

        //find an account with the corresponding id.
        while (storedAccountsIter.hasNext())
        {
            String accountRootPropertyName = (String) storedAccountsIter.next();

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
                List accountPropertyNames
                    = configurationService.getPropertyNamesByPrefix(
                        accountRootPropertyName, false);

                Iterator propsIter = accountPropertyNames.iterator();

                //set all account properties to null in order to remove them.
                while (propsIter.hasNext())
                {
                    String propName = (String) propsIter.next();

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
     * @param sourcePackageName the name of the account source package
     * @return a String indicating the ConfigurationService property name
     * prefix under which all account properties are stored or null if no
     * account corresponding to the specified id was found.
     */
    protected String findAccountPrefix(BundleContext bundleContext,
                                       AccountID     accountID,
                                       String        sourcePackageName)
    {
        ServiceReference confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List storedAccounts = configurationService.getPropertyNamesByPrefix(
            sourcePackageName, true);

        Iterator storedAccountsIter = storedAccounts.iterator();

        //find an account with the corresponding id.
        while (storedAccountsIter.hasNext())
        {
            String accountRootPropertyName = (String) storedAccountsIter.next();

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

}
