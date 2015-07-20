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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Base64;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Represents an implementation of <tt>AccountManager</tt> which loads the
 * accounts in a separate thread.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public class AccountManager
{
    /**
     * The delay in milliseconds the background <tt>Thread</tt> loading the
     * stored accounts should wait before dying so that it doesn't get recreated
     * for each <tt>ProtocolProviderFactory</tt> registration.
     */
    private static final long LOAD_STORED_ACCOUNTS_TIMEOUT = 30000;

    /**
     * The <tt>BundleContext</tt> this service is registered in.
     */
    private final BundleContext bundleContext;

    /**
     * The <tt>AccountManagerListener</tt>s currently interested in the
     * events fired by this manager.
     */
    private final List<AccountManagerListener> listeners =
        new LinkedList<AccountManagerListener>();

    /**
     * The queue of <tt>ProtocolProviderFactory</tt> services awaiting their
     * stored accounts to be loaded.
     */
    private final Queue<ProtocolProviderFactory> loadStoredAccountsQueue =
        new LinkedList<ProtocolProviderFactory>();

    /**
     * The <tt>Thread</tt> loading the stored accounts of the
     * <tt>ProtocolProviderFactory</tt> services waiting in
     * {@link #loadStoredAccountsQueue}.
     */
    private Thread loadStoredAccountsThread;

    /**
     * The <tt>Logger</tt> used by this <tt>AccountManagerImpl</tt> instance for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(AccountManager.class);

    /**
     * The list of <tt>AccountID</tt>s, corresponding to all stored accounts.
     */
    private final Vector<AccountID> storedAccounts = new Vector<AccountID>();

    /**
     * The prefix of the account unique identifier.
     */
    private static final String ACCOUNT_UID_PREFIX = "acc";

    /**
     * Initializes a new <tt>AccountManagerImpl</tt> instance loaded in a
     * specific <tt>BundleContext</tt> (in which the caller will usually
     * later register it).
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the new
     *            instance is loaded (and in which the caller will usually later
     *            register it as a service)
     */
    public AccountManager(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;

        this.bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                AccountManager.this.serviceChanged(serviceEvent);
            }
        });
    }

    /**
     * Implements AccountManager#addListener(AccountManagerListener).
     * @param listener the <tt>AccountManagerListener</tt> to add
     */
    public void addListener(AccountManagerListener listener)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Loads the accounts stored for a specific
     * <tt>ProtocolProviderFactory</tt>.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to load the
     *            stored accounts of
     */
    private void doLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        ConfigurationService configService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);
        List<String> accounts
            = configService.getPropertyNamesByPrefix(factoryPackage, true);

        if (logger.isDebugEnabled())
            logger.debug("Discovered " + accounts.size() + " stored "
                    + factoryPackage + " accounts");

        for (Iterator<String> storedAccountIter = accounts.iterator();
                storedAccountIter.hasNext();)
        {
            String storedAccount = storedAccountIter.next();

            // If the property is not related to an account we skip it.
            int dotIndex = storedAccount.lastIndexOf(".");
            if (!storedAccount.substring(dotIndex + 1)
                    .startsWith(ACCOUNT_UID_PREFIX))
                continue;

            if (logger.isDebugEnabled())
                logger.debug("Loading account " + storedAccount);

            List<String> storedAccountProperties =
                configService.getPropertyNamesByPrefix(storedAccount, false);
            Map<String, String> accountProperties =
                new Hashtable<String, String>();
            boolean disabled = false;
            CredentialsStorageService credentialsStorage
                = ServiceUtils.getService(
                        bundleContext,
                        CredentialsStorageService.class);

            int prefLen = storedAccount.length() + 1;
            for (Iterator<String> storedAccountPropertyIter
                        = storedAccountProperties.iterator();
                    storedAccountPropertyIter.hasNext();)
            {
                String property = storedAccountPropertyIter.next();
                String value = configService.getString(property);

                //strip the package prefix
                if(prefLen > property.length())
                    continue;

                property = property.substring(prefLen);

                if (ProtocolProviderFactory.IS_ACCOUNT_DISABLED.equals(property))
                    disabled = Boolean.parseBoolean(value);
                // Decode passwords.
                else if (ProtocolProviderFactory.PASSWORD.equals(property)
                        && !credentialsStorage.isStoredEncrypted(storedAccount))
                {
                    if ((value != null) && value.length() != 0)
                    {

                        /*
                         * TODO Converting byte[] to String using the platform's
                         * default charset may result in an invalid password.
                         */
                        value = new String(Base64.decode(value));
                    }
                }

                if (value != null)
                    accountProperties.put(property, value);
            }

            try
            {
                AccountID accountID = factory.createAccount(accountProperties);

                // If for some reason the account id is not created we move to
                // the next account.
                if (accountID == null)
                    continue;

                synchronized (storedAccounts)
                {
                    storedAccounts.add(accountID);
                }
                if (!disabled)
                    factory.loadAccount(accountID);
            }
            catch (Exception ex)
            {

                /*
                 * Swallow the exception in order to prevent a single account
                 * from halting the loading of subsequent accounts.
                 */
                logger.error("Failed to load account " + accountProperties, ex);
            }
        }
    }

    /**
     * Notifies the registered {@link #listeners} that the stored accounts of a
     * specific <tt>ProtocolProviderFactory</tt> have just been loaded.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which had its
     *            stored accounts just loaded
     */
    private void fireStoredAccountsLoaded(ProtocolProviderFactory factory)
    {
        AccountManagerListener[] listeners;
        synchronized (this.listeners)
        {
            listeners =
                this.listeners
                    .toArray(new AccountManagerListener[this.listeners.size()]);
        }

        int listenerCount = listeners.length;
        if (listenerCount > 0)
        {
            AccountManagerEvent event =
                new AccountManagerEvent(this,
                    AccountManagerEvent.STORED_ACCOUNTS_LOADED, factory);

            for (int listenerIndex = 0;
                    listenerIndex < listenerCount; listenerIndex++)
            {
                listeners[listenerIndex].handleAccountManagerEvent(event);
            }
        }
    }

    /**
     * Returns the package name of the <tt>factory</tt>.
     * @param factory the factory which package will be returned.
     * @return the package name of the <tt>factory</tt>.
     */
    public String getFactoryImplPackageName(ProtocolProviderFactory factory)
    {
        String className = factory.getClass().getName();

        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Check for stored accounts for the supplied <tt>protocolName</tt>.
     * @param protocolName the protocol name to check for
     * @param includeHidden whether to include hidden providers
     * @return <tt>true</tt> if there is any account stored in configuration
     * service with <tt>protocolName</tt>, <tt>false</tt> otherwise.
     */
    public boolean hasStoredAccounts(String protocolName, boolean includeHidden)
    {
        return hasStoredAccount(protocolName, includeHidden, null);
    }

    /**
     * Checks whether a stored account with <tt>userID</tt> is stored
     * in configuration.
     *
     * @param protocolName the protocol name
     * @param includeHidden whether to check hidden providers
     * @param userID the user id to check.
     * @return <tt>true</tt> if there is any account stored in configuration
     * service with <tt>protocolName</tt> and <tt>userID</tt>,
     * <tt>false</tt> otherwise.
     */
    public boolean hasStoredAccount(String protocolName,
                                    boolean includeHidden,
                                    String userID)
    {
        Collection<ServiceReference<ProtocolProviderFactory>> factoryRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderFactory.class);
        boolean hasStoredAccounts = false;

        if (!factoryRefs.isEmpty())
        {
            ConfigurationService configService
                = ProtocolProviderActivator.getConfigurationService();

            for (ServiceReference<ProtocolProviderFactory> factoryRef
                    : factoryRefs)
            {
                ProtocolProviderFactory factory
                    = bundleContext.getService(factoryRef);

                if ((protocolName != null)
                        && !protocolName.equals(factory.getProtocolName()))
                {
                    continue;
                }

                String factoryPackage = getFactoryImplPackageName(factory);
                List<String> storedAccounts
                    = configService
                        .getPropertyNamesByPrefix(factoryPackage + ".acc",
                            false);

                /* Ignore the hidden accounts. */
                for (Iterator<String> storedAccountIter =
                    storedAccounts.iterator(); storedAccountIter.hasNext();)
                {
                    String storedAccount = storedAccountIter.next();
                    List<String> storedAccountProperties =
                        configService.getPropertyNamesByPrefix(storedAccount,
                            true);
                    boolean hidden = false;
                    String accountUserID = null;

                    if (!includeHidden || userID != null)
                    {
                        for (Iterator<String> storedAccountPropertyIter =
                            storedAccountProperties.iterator();
                            storedAccountPropertyIter.hasNext();)
                        {
                            String property = storedAccountPropertyIter.next();
                            String value = configService.getString(property);

                            property = stripPackagePrefix(property);

                            if (ProtocolProviderFactory.IS_PROTOCOL_HIDDEN
                                .equals(property))
                            {
                                hidden = (value != null);
                            }
                            else if (ProtocolProviderFactory.USER_ID
                                    .equals(property))
                            {
                                accountUserID = value;
                            }
                        }
                    }

                    if (includeHidden || !hidden)
                    {
                        if(accountUserID != null
                            && userID != null
                            && userID.equals(accountUserID))
                        {
                            hasStoredAccounts = true;
                            break;
                        }
                        else if(userID == null)
                        {
                            hasStoredAccounts = true;
                            break;
                        }
                    }
                }

                if (hasStoredAccounts || (protocolName != null))
                {
                    break;
                }
            }
        }
        return hasStoredAccounts;
    }

    /**
     * Searches for stored account with <tt>uid</tt> in stored
     * configuration. The <tt>uid</tt> is the one generated when creating
     * accounts with prefix <tt>ACCOUNT_UID_PREFIX</tt>.
     *
     * @return <tt>AccountID</tt> if there is any account stored in configuration
     * service with <tt>uid</tt>,
     * <tt>null</tt> otherwise.
     */
    public AccountID findAccountID(String uid)
    {
        Collection<ServiceReference<ProtocolProviderFactory>> factoryRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderFactory.class);

        if (!factoryRefs.isEmpty())
        {
            ConfigurationService configService
                = ProtocolProviderActivator.getConfigurationService();

            for (ServiceReference<ProtocolProviderFactory> factoryRef
                    : factoryRefs)
            {
                ProtocolProviderFactory factory
                    = bundleContext.getService(factoryRef);

                String factoryPackage = getFactoryImplPackageName(factory);
                List<String> storedAccountsProps
                    = configService
                        .getPropertyNamesByPrefix(factoryPackage, true);

                for (Iterator<String> storedAccountIter =
                         storedAccountsProps.iterator();
                     storedAccountIter.hasNext();)
                {
                    String storedAccount = storedAccountIter.next();

                    if(!storedAccount.endsWith(uid))
                        continue;

                    String accountUID = configService.getString(
                        storedAccount //node id
                        + "." + ProtocolProviderFactory.ACCOUNT_UID);// propname

                    for(AccountID acc : storedAccounts)
                    {
                        if(acc.getAccountUniqueID().equals(accountUID))
                            return acc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Loads the accounts stored for a specific
     * <tt>ProtocolProviderFactory</tt> and notifies the registered
     * {@link #listeners} that the stored accounts of the specified
     * <tt>factory</tt> have just been loaded
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to load the
     *            stored accounts of
     */
    private void loadStoredAccounts(ProtocolProviderFactory factory)
    {
        doLoadStoredAccounts(factory);

        fireStoredAccountsLoaded(factory);
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderFactory</tt> has been registered as a service.
     * The current implementation queues the specified <tt>factory</tt> to
     * have its stored accounts as soon as possible.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which has been
     *            registered as a service.
     */
    private void protocolProviderFactoryRegistered(
        ProtocolProviderFactory factory)
    {
        queueLoadStoredAccounts(factory);
    }

    /**
     * Queues a specific <tt>ProtocolProviderFactory</tt> to have its stored
     * accounts loaded as soon as possible.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to be queued for
     *            loading its stored accounts as soon as possible
     */
    private void queueLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        synchronized (loadStoredAccountsQueue)
        {
            loadStoredAccountsQueue.add(factory);
            loadStoredAccountsQueue.notifyAll();

            if (loadStoredAccountsThread == null)
            {
                loadStoredAccountsThread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        runInLoadStoredAccountsThread();
                    }
                };
                loadStoredAccountsThread.setDaemon(true);
                loadStoredAccountsThread.setName(
                        "AccountManager.loadStoredAccounts");
                loadStoredAccountsThread.start();
            }
        }
    }

    /**
     * Implements AccountManager#removeListener(AccountManagerListener).
     * @param listener the <tt>AccountManagerListener</tt> to remove
     */
    public void removeListener(AccountManagerListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Running in {@link #loadStoredAccountsThread}, loads the stored accounts
     * of the <tt>ProtocolProviderFactory</tt> services waiting in
     * {@link #loadStoredAccountsQueue}
     */
    private void runInLoadStoredAccountsThread()
    {
        boolean interrupted = false;
        while (!interrupted)
        {
            try
            {
                ProtocolProviderFactory factory;

                synchronized (loadStoredAccountsQueue)
                {
                    factory = loadStoredAccountsQueue.poll();
                    if (factory == null)
                    {
                        /*
                         * Technically, we should be handing spurious wakeups.
                         * However, we cannot check the condition in a queue.
                         * Anyway, we just want to keep this Thread alive long
                         * enough to allow it to not be re-created multiple
                         * times and not handing a spurious wakeup will just
                         * cause such an inconvenience.
                         */
                        try
                        {
                            loadStoredAccountsQueue
                                .wait(LOAD_STORED_ACCOUNTS_TIMEOUT);
                        }
                        catch (InterruptedException ex)
                        {
                            logger
                                .warn(
                                    "The loading of the stored accounts has"
                                        + " been interrupted",
                                    ex);
                            interrupted = true;
                            break;
                        }
                        factory = loadStoredAccountsQueue.poll();
                    }
                    if (factory != null)
                        loadStoredAccountsQueue.notifyAll();
                }

                if (factory != null)
                {
                    try
                    {
                        loadStoredAccounts(factory);
                    }
                    catch (Exception ex)
                    {

                        /*
                         * Swallow the exception in order to prevent a single
                         * factory from halting the loading of subsequent
                         * factories.
                         */
                        logger.error("Failed to load accounts for " + factory,
                            ex);
                    }
                }
            }
            finally
            {
                synchronized (loadStoredAccountsQueue)
                {
                    if (!interrupted && (loadStoredAccountsQueue.size() <= 0))
                    {
                        if (loadStoredAccountsThread == Thread.currentThread())
                        {
                            loadStoredAccountsThread = null;
                            loadStoredAccountsQueue.notifyAll();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Notifies this manager that an OSGi service has changed. The current
     * implementation tracks the registrations of
     * <tt>ProtocolProviderFactory</tt> services in order to queue them for
     * loading their stored accounts.
     *
     * @param serviceEvent the <tt>ServiceEvent</tt> containing the event
     *            data
     */
    private void serviceChanged(ServiceEvent serviceEvent)
    {
        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            Object service
                = bundleContext.getService(serviceEvent.getServiceReference());

            if (service instanceof ProtocolProviderFactory)
            {
                protocolProviderFactoryRegistered(
                    (ProtocolProviderFactory) service);
            }
            break;
        default:
            break;
        }
    }

    /**
     * Stores an account represented in the form of an <tt>AccountID</tt>
     * created by a specific <tt>ProtocolProviderFactory</tt>.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the account in the form of <tt>AccountID</tt> to be
     * stored
     * @throws OperationFailedException if anything goes wrong while storing the
     * account
     */
    public void storeAccount(
            ProtocolProviderFactory factory,
            AccountID accountID)
        throws OperationFailedException
    {
        synchronized (storedAccounts)
        {
            if (!storedAccounts.contains(accountID))
                storedAccounts.add(accountID);
        }

        ConfigurationService configurationService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);

        String accountNodeName
               = getAccountNodeName( factory,
                                     accountID.getAccountUniqueID() );

        Map<String, Object> configurationProperties
            = new HashMap<String, Object>();

        // Create a unique node name of the properties node that will contain
        // this account's properties.
        if (accountNodeName == null)
        {
            accountNodeName
                = ACCOUNT_UID_PREFIX + Long.toString(System.currentTimeMillis());

            // set a value for the persistent node so that we could later
            // retrieve it as a property
            configurationProperties.put(
                    factoryPackage /* prefix */ + "." + accountNodeName,
                    accountNodeName);

            // register the account in the configuration service.
            // we register all the properties in the following hierarchy
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            configurationProperties.put(factoryPackage// prefix
                + "." + accountNodeName // node name for the account id
                + "." + ProtocolProviderFactory.ACCOUNT_UID, // propname
                accountID.getAccountUniqueID()); // value
        }

        // store the rest of the properties
        Map<String, String> accountProperties = accountID.getAccountProperties();

        for (Map.Entry<String, String> entry : accountProperties.entrySet())
        {
            String property = entry.getKey();
            String value = entry.getValue();
            String secureStorePrefix = null;

            // If the property is a password, store it securely.
            if (property.equals(ProtocolProviderFactory.PASSWORD))
            {
                String accountPrefix = factoryPackage + "." + accountNodeName;
                secureStorePrefix = accountPrefix;
            }
            else if(property.endsWith("." + ProtocolProviderFactory.PASSWORD))
            {
                secureStorePrefix = factoryPackage + "." + accountNodeName +
                    "." + property.substring(0, property.lastIndexOf("."));
            }

            if(secureStorePrefix != null)
            {
                CredentialsStorageService credentialsStorage
                        = ServiceUtils.getService(
                                bundleContext,
                                CredentialsStorageService.class);

                // encrypt and store
                if ((value != null)
                        && (value.length() != 0)
                        && !credentialsStorage.storePassword(
                                secureStorePrefix,
                                value))
                {
                    throw
                        new OperationFailedException(
                                "CredentialsStorageService failed to"
                                    + " storePassword",
                                OperationFailedException.GENERAL_ERROR);
                }
            }
            else
            {
                configurationProperties.put(
                        factoryPackage // prefix
                            + "." + accountNodeName // a unique node name for the account id
                            + "." + property, // propname
                        value); // value
            }
        }

        // clear the password if missing property, modification can request
        // password delete
        if(!accountProperties.containsKey(ProtocolProviderFactory.PASSWORD)
                && // And only if it's not stored already in encrypted form.
                   // Account registration object clears also this property
                   // in order to forget the password
                !configurationProperties.containsKey(
                    factoryPackage+"."+accountNodeName+".ENCRYPTED_PASSWORD"))
        {
            CredentialsStorageService credentialsStorage
                    = ServiceUtils.getService(
                            bundleContext,
                            CredentialsStorageService.class);
            credentialsStorage.removePassword(
                factoryPackage + "." + accountNodeName);
        }

        if (configurationProperties.size() > 0)
            configurationService.setProperties(configurationProperties);

        if (logger.isDebugEnabled())
            logger.debug("Stored account for id " + accountID.getAccountUniqueID()
                    + " for package " + factoryPackage);
    }

    /**
     * Gets account node name under which account configuration properties are
     * stored.
     *
     * @param factory account's protocol provider factory
     * @param accountUID account for which the prefix will be returned
     * @return configuration prefix for given <tt>accountID</tt> if exists or
     *         <tt>null</tt> otherwise
     */
    public String getAccountNodeName( ProtocolProviderFactory factory,
                                      String accountUID )
    {
        ConfigurationService configurationService
                = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);

        // First check if such accountID already exists in the configuration.
        List<String> storedAccounts =
            configurationService.getPropertyNamesByPrefix(factoryPackage, true);
        String accountNodeName = null;

        for (Iterator<String> storedAccountIter = storedAccounts.iterator();
             storedAccountIter.hasNext();)
        {
            String storedAccount = storedAccountIter.next();

            // If the property is not related to an account we skip it.
            int dotIndex = storedAccount.lastIndexOf(".");
            if (!storedAccount.substring(dotIndex + 1)
                    .startsWith(ACCOUNT_UID_PREFIX))
                continue;

            String storedAccountUID
                = configurationService.getString(
                    storedAccount + "." + ProtocolProviderFactory.ACCOUNT_UID);

            if(storedAccountUID == null)
                continue;

            if (storedAccountUID.equals(accountUID))
                accountNodeName = configurationService.getString(storedAccount);
        }
        return accountNodeName;
    }

    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the AccountID of the account to remove.
     * @return true if an account has been removed and false otherwise.
     */
    public boolean removeStoredAccount(ProtocolProviderFactory factory,
        AccountID accountID)
    {
        synchronized (storedAccounts)
        {
            if (storedAccounts.contains(accountID))
                storedAccounts.remove(accountID);
        }

        /*
         * We're already doing it in #unloadAccount(AccountID) - we're figuring
         * out the ProtocolProviderFactory by the AccountID.
         */
        if (factory == null)
        {
            factory
                = ProtocolProviderActivator.getProtocolProviderFactory(
                        accountID.getProtocolName());
        }

        String factoryPackage = getFactoryImplPackageName(factory);

        // remove the stored password explicitly using credentials service
        CredentialsStorageService credentialsStorage
            = ServiceUtils.getService(
                    bundleContext,
                    CredentialsStorageService.class);
        String accountPrefix =
            ProtocolProviderFactory.findAccountPrefix(bundleContext, accountID,
                factoryPackage);

        credentialsStorage.removePassword(accountPrefix);

        ConfigurationService configurationService
            = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        //first retrieve all accounts that we've registered
        List<String> storedAccounts
            = configurationService.getPropertyNamesByPrefix(
                factoryPackage, true);

        //find an account with the corresponding id.
        for (String accountRootPropertyName : storedAccounts)
        {
            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.getString(
                accountRootPropertyName //node id
                + "." + ProtocolProviderFactory.ACCOUNT_UID); // propname

            if (accountID.getAccountUniqueID().equals(accountUID))
            {
                //retrieve the names of all properties registered for the
                //current account.
                List<String> accountPropertyNames
                    = configurationService.getPropertyNamesByPrefix(
                        accountRootPropertyName, false);

                //set all account properties to null in order to remove them.
                for (String propName : accountPropertyNames)
                    configurationService.setProperty(propName, null);

                //and now remove the parent too.
                configurationService.setProperty(accountRootPropertyName, null);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all accounts which have been persistently stored.
     *
     * @see #removeStoredAccount(ProtocolProviderFactory, AccountID)
     */
    public void removeStoredAccounts()
    {
        synchronized (loadStoredAccountsQueue)
        {
            /*
             * Wait for the Thread which loads the stored account to complete so
             * that we can be sure later on that it will not load a stored
             * account while we are deleting it or another one for that matter.
             */
            boolean interrupted = false;

            while (loadStoredAccountsThread != null)
                try
                {
                    loadStoredAccountsQueue.wait(LOAD_STORED_ACCOUNTS_TIMEOUT);
                }
                catch (InterruptedException ie)
                {
                    interrupted = true;
                }
            if (interrupted)
                Thread.currentThread().interrupt();

            synchronized (this.storedAccounts)
            {
                AccountID[] storedAccounts
                    = this.storedAccounts.toArray(
                            new AccountID[this.storedAccounts.size()]);

                for (AccountID storedAccount : storedAccounts)
                {
                    ProtocolProviderFactory ppf
                        = ProtocolProviderActivator.getProtocolProviderFactory(
                                storedAccount.getProtocolName());

                    if (ppf != null)
                        ppf.uninstallAccount(storedAccount);
                }
            }
        }
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all stored
     * <tt>AccountID</tt>s. The list of stored accounts include all registered
     * accounts and all disabled accounts. In other words in this list we could
     * find accounts that aren't loaded.
     * <p>
     * In order to check if an account is already loaded please use the
     * #isAccountLoaded(AccountID accountID) method. To load an account use the
     * #loadAccount(AccountID accountID) method.
     *
     * @return an <tt>Iterator</tt> over a list of all stored
     * <tt>AccountID</tt>s
     */
    public Collection<AccountID> getStoredAccounts()
    {
        synchronized (storedAccounts)
        {
            return new Vector<AccountID>(storedAccounts);
        }
    }

    /**
     * Loads the account corresponding to the given <tt>AccountID</tt>. An
     * account is loaded when its <tt>ProtocolProviderService</tt> is registered
     * in the bundle context. This method is meant to load the account through
     * the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while loading the
     * account corresponding to the specified <tt>accountID</tt>
     */
    public void loadAccount(AccountID accountID)
        throws OperationFailedException
    {
        // If the account with the given id is already loaded we have nothing
        // to do here.
        if (isAccountLoaded(accountID))
            return;

        ProtocolProviderFactory providerFactory
            = ProtocolProviderActivator.getProtocolProviderFactory(
                accountID.getProtocolName());

        if(providerFactory.loadAccount(accountID))
        {
            accountID.putAccountProperty(
                ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
                String.valueOf(false));
            // Finally store the modified properties.
            storeAccount(providerFactory, accountID);
        }
    }

    /**
     * Unloads the account corresponding to the given <tt>AccountID</tt>. An
     * account is unloaded when its <tt>ProtocolProviderService</tt> is
     * unregistered in the bundle context. This method is meant to unload the
     * account through the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while unloading
     * the account corresponding to the specified <tt>accountID</tt>
     */
    public void unloadAccount(AccountID accountID)
        throws OperationFailedException
    {
        // If the account with the given id is already unloaded we have nothing
        // to do here.
        if (!isAccountLoaded(accountID))
            return;

        ProtocolProviderFactory providerFactory
            = ProtocolProviderActivator.getProtocolProviderFactory(
                accountID.getProtocolName());

        // Obtain the protocol provider.
        ServiceReference<ProtocolProviderService> serRef
            = providerFactory.getProviderForAccount(accountID);

        // If there's no such provider we have nothing to do here.
        if (serRef == null)
            return;

        ProtocolProviderService protocolProvider
            = bundleContext.getService(serRef);

        // Set the account icon path for unloaded accounts.
        String iconPathProperty = accountID.getAccountPropertyString(
            ProtocolProviderFactory.ACCOUNT_ICON_PATH);

        if (iconPathProperty == null)
        {
            accountID.putAccountProperty(
                ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                protocolProvider.getProtocolIcon()
                    .getIconPath(ProtocolIcon.ICON_SIZE_32x32));
        }

        accountID.putAccountProperty(
            ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
            String.valueOf(true));

        if (!providerFactory.unloadAccount(accountID))
        {
            accountID.putAccountProperty(
                ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
                String.valueOf(false));
        }
        // Finally store the modified properties.
        storeAccount(providerFactory, accountID);
    }

    /**
     * Checks if the account corresponding to the given <tt>accountID</tt> is
     * loaded. An account is loaded if its <tt>ProtocolProviderService</tt> is
     * registered in the bundle context. By default all accounts are loaded.
     * However the user could manually unload an account, which would be
     * unregistered from the bundle context, but would remain in the
     * configuration file.
     *
     * @param accountID the identifier of the account to load
     * @return <tt>true</tt> to indicate that the account with the given
     * <tt>accountID</tt> is loaded, <tt>false</tt> - otherwise
     */
    public boolean isAccountLoaded(AccountID accountID)
    {
        return storedAccounts.contains(accountID) && accountID.isEnabled();
    }

    private String stripPackagePrefix(String property)
    {
        int packageEndIndex = property.lastIndexOf('.');

        if (packageEndIndex != -1)
            property = property.substring(packageEndIndex + 1);
        return property;
    }
}
