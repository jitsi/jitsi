/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an implementation of <code>AccountManager</code> which loads the
 * accounts in a separate thread.
 * 
 * @author Lubomir Marinov
 */
public class AccountManagerImpl
    implements AccountManager
{

    /**
     * The delay in milliseconds the background <code>Thread</code> loading the
     * stored accounts should wait before dying so that it doesn't get recreated
     * for each <code>ProtocolProviderFactory</code> registration.
     */
    private static final long LOAD_STORED_ACCOUNTS_TIMEOUT = 30000;

    /**
     * The <code>BundleContext</code> this service is registered in.
     */
    private final BundleContext bundleContext;

    /**
     * The <code>AccountManagerListener</code>s currently interested in the
     * events fired by this manager.
     */
    private final List<AccountManagerListener> listeners =
        new LinkedList<AccountManagerListener>();

    /**
     * The queue of <code>ProtocolProviderFactory</code> services awaiting their
     * stored accounts to be loaded.
     */
    private final Queue<ProtocolProviderFactory> loadStoredAccountsQueue =
        new LinkedList<ProtocolProviderFactory>();

    /**
     * The <code>Thread</code> loading the stored accounts of the
     * <code>ProtocolProviderFactory</code> services waiting in
     * {@link #loadStoredAccountsQueue}.
     */
    private Thread loadStoredAccountsThread;

    private final Logger logger = Logger.getLogger(AccountManagerImpl.class);

    /**
     * Initializes a new <code>AccountManagerImpl</code> instance loaded in a
     * specific <code>BundleContext</code> (in which the caller will usually
     * later register it).
     *
     * @param bundleContext the <code>BundleContext</code> in which the new
     *            instance is loaded (and in which the caller will usually later
     *            register it as a service)
     */
    public AccountManagerImpl(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;

        this.bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                AccountManagerImpl.this.serviceChanged(serviceEvent);
            }
        });
    }

    /*
     * Implements AccountManager#addListener(AccountManagerListener).
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
     * <code>ProtocolProviderFactory</code>.
     *
     * @param factory the <code>ProtocolProviderFactory</code> to load the
     *            stored accounts of
     */
    private void doLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        ConfigurationService configService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);
        List<String> storedAccounts =
            configService.getPropertyNamesByPrefix(factoryPackage, true);

        if (logger.isDebugEnabled())
            logger.debug("Discovered " + storedAccounts.size() + " stored "
                    + factoryPackage + " accounts");

        for (Iterator<String> storedAccountIter = storedAccounts.iterator(); storedAccountIter
            .hasNext();)
        {
            String storedAccount = storedAccountIter.next();

            if (logger.isDebugEnabled())
                logger.debug("Loading account " + storedAccount);

            List<String> storedAccountProperties =
                configService.getPropertyNamesByPrefix(storedAccount, true);
            Map<String, String> accountProperties =
                new Hashtable<String, String>();

            for (Iterator<String> storedAccountPropertyIter =
                storedAccountProperties.iterator(); storedAccountPropertyIter
                .hasNext();)
            {
                String property = storedAccountPropertyIter.next();
                String value = configService.getString(property);

                property = stripPackagePrefix(property);

                // Decode passwords.
                if (ProtocolProviderFactory.PASSWORD.equals(property))
                {
                    if (value == null)
                    {
                        value = "";
                    }
                    else if (value.length() != 0)
                    {

                        /*
                         * TODO Converting byte[] to String using the platform's
                         * default charset may result in an invalid password.
                         */
                        value = new String(Base64.decode(value));
                    }
                }

                if (value != null)
                {
                    accountProperties.put(property, value);
                }
            }

            try
            {
                factory.loadAccount(accountProperties);
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
     * specific <code>ProtocolProviderFactory</code> have just been loaded.
     *
     * @param factory the <code>ProtocolProviderFactory</code> which had its
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

            for (int listenerIndex = 0; listenerIndex < listenerCount; listenerIndex++)
            {
                listeners[listenerIndex].handleAccountManagerEvent(event);
            }
        }
    }

    private String getFactoryImplPackageName(ProtocolProviderFactory factory) {
        String className = factory.getClass().getName();

        return className.substring(0, className.lastIndexOf('.'));
    }

    public boolean hasStoredAccounts(String protocolName, boolean includeHidden)
    {
        ServiceReference[] factoryRefs = null;
        boolean hasStoredAccounts = false;

        try
        {
            factoryRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(
                "Failed to retrieve the registered ProtocolProviderFactories",
                ex);
        }

        if ((factoryRefs != null) && (factoryRefs.length > 0))
        {
            ConfigurationService configService
                = ProtocolProviderActivator.getConfigurationService();

            for (ServiceReference factoryRef : factoryRefs)
            {
                ProtocolProviderFactory factory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(factoryRef);

                if ((protocolName != null)
                    && !protocolName.equals(factory.getProtocolName()))
                {
                    continue;
                }

                String factoryPackage = getFactoryImplPackageName(factory);
                List<String> storedAccounts =
                    configService
                        .getPropertyNamesByPrefix(factoryPackage, true);

                /* Ignore the hidden accounts. */
                for (Iterator<String> storedAccountIter =
                    storedAccounts.iterator(); storedAccountIter.hasNext();)
                {
                    String storedAccount = storedAccountIter.next();
                    List<String> storedAccountProperties =
                        configService.getPropertyNamesByPrefix(storedAccount,
                            true);
                    boolean hidden = false;

                    if (!includeHidden)
                    {
                        for (Iterator<String> storedAccountPropertyIter =
                            storedAccountProperties.iterator(); storedAccountPropertyIter
                            .hasNext();)
                        {
                            String property = storedAccountPropertyIter.next();
                            String value = configService.getString(property);

                            property = stripPackagePrefix(property);

                            if (ProtocolProviderFactory.IS_PROTOCOL_HIDDEN
                                .equals(property))
                            {
                                hidden = (value != null);
                                break;
                            }
                        }
                    }

                    if (!hidden)
                    {
                        hasStoredAccounts = true;
                        break;
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
     * Loads the accounts stored for a specific
     * <code>ProtocolProviderFactory</code> and notifies the registered
     * {@link #listeners} that the stored accounts of the specified
     * <code>factory</code> have just been loaded
     *
     * @param factory the <code>ProtocolProviderFactory</code> to load the
     *            stored accounts of
     */
    private void loadStoredAccounts(ProtocolProviderFactory factory)
    {
        doLoadStoredAccounts(factory);

        fireStoredAccountsLoaded(factory);
    }

    /**
     * Notifies this manager that a specific
     * <code>ProtocolProviderFactory</code> has been registered as a service.
     * The current implementation queues the specified <code>factory</code> to
     * have its stored accounts as soon as possible.
     *
     * @param factory the <code>ProtocolProviderFactory</code> which has been
     *            registered as a service.
     */
    private void protocolProviderFactoryRegistered(
        ProtocolProviderFactory factory)
    {
        queueLoadStoredAccounts(factory);
    }

    /**
     * Queues a specific <code>ProtocolProviderFactory</code> to have its stored
     * accounts loaded as soon as possible.
     *
     * @param factory the <code>ProtocolProviderFactory</code> to be queued for
     *            loading its stored accounts as soon as possible
     */
    private void queueLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        synchronized (loadStoredAccountsQueue)
        {
            loadStoredAccountsQueue.add(factory);
            loadStoredAccountsQueue.notify();

            if (loadStoredAccountsThread == null)
            {
                loadStoredAccountsThread = new Thread()
                {
                    public void run()
                    {
                        runInLoadStoredAccountsThread();
                    }
                };
                loadStoredAccountsThread.setDaemon(true);
                loadStoredAccountsThread
                    .setName("AccountManager.loadStoredAccounts");
                loadStoredAccountsThread.start();
            }
        }
    }

    /*
     * Implements AccountManager#removeListener(AccountManagerListener).
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
     * of the <code>ProtocolProviderFactory</code> services waiting in
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
                                    "The loading of the stored accounts has been interrupted",
                                    ex);
                            interrupted = true;
                            break;
                        }

                        factory = loadStoredAccountsQueue.poll();
                    }
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
     * <code>ProtocolProviderFactory</code> services in order to queue them for
     * loading their stored accounts.
     * 
     * @param serviceEvent the <code>ServiceEvent</code> containing the event
     *            data
     */
    private void serviceChanged(ServiceEvent serviceEvent)
    {
        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            Object service =
                bundleContext.getService(serviceEvent.getServiceReference());

            if (service instanceof ProtocolProviderFactory)
            {
                protocolProviderFactoryRegistered((ProtocolProviderFactory) service);
            }
            break;
        default:
            break;
        }
    }

    /**
     * Stores an account represented in the form of an <code>AccountID</code>
     * created by a specific <code>ProtocolProviderFactory</code>.
     *
     * @param factory the <code>ProtocolProviderFactory</code> which created the
     *            account to be stored
     * @param accountID the account in the form of <code>AccountID</code> to be
     *            stored
     */
    public void storeAccount(ProtocolProviderFactory factory,
        AccountID accountID)
    {
        ConfigurationService configurationService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);

        // First check if such accountID already exists in the configuration.
        List<String> storedAccounts =
            configurationService.getPropertyNamesByPrefix(factoryPackage, true);
        String accountUID = accountID.getAccountUniqueID();
        String accountNodeName = null;

        for (Iterator<String> storedAccountIter = storedAccounts.iterator();
             storedAccountIter.hasNext();)
        {
            String storedAccount = storedAccountIter.next();
            String storedAccountUID =
                configurationService.getString(storedAccount + ".ACCOUNT_UID");

            if (storedAccountUID.equals(accountUID))
            {
                accountNodeName = configurationService.getString(storedAccount);
            }
        }

        Map<String, Object> configurationProperties
            = new HashMap<String, Object>();

        // Create a unique node name of the properties node that will contain
        // this account's properties.
        if (accountNodeName == null)
        {
            accountNodeName = "acc" + Long.toString(System.currentTimeMillis());

            // set a value for the persistent node so that we could later
            // retrieve it as a property
            configurationProperties.put(factoryPackage // prefix
                + "." + accountNodeName, accountNodeName);

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

            // if this is a password - encode it.
            if (property.equals(ProtocolProviderFactory.PASSWORD))
                value = new String(Base64.encode(value.getBytes()));

            configurationProperties.put(factoryPackage // prefix
                + "." + accountNodeName // a unique node name for the account id
                + "." + property, // propname
                value); // value
        }

        if (configurationProperties.size() > 0)
            configurationService.setProperties(configurationProperties);

        if (logger.isDebugEnabled())
            logger.debug("Stored account for id " + accountID.getAccountUniqueID()
                    + " for package " + factoryPackage);
    }

    private String stripPackagePrefix(String property)
    {
        int packageEndIndex = property.lastIndexOf('.');
        if (packageEndIndex != -1)
        {
            property = property.substring(packageEndIndex + 1);
        }
        return property;
    }
}
