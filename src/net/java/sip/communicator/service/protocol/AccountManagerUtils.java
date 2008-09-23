/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

import org.osgi.framework.*;

/**
 * Provides utilities to aid the manipulation of {@link AccountManager}.
 * 
 * @author Lubomir Marinov
 */
public final class AccountManagerUtils
{
    private static AccountManager getAccountManager(BundleContext bundleContext)
    {
        return (AccountManager) bundleContext.getService(bundleContext
            .getServiceReference(AccountManager.class.getName()));
    }

    /**
     * Starts a specific <code>Bundle</code> and wait for the
     * <code>AccountManager</code> available in a specific
     * <code>BundleContext</code> to load the stored accounts of a
     * <code>ProtocolProviderFactory</code> with a specific protocol name.
     * 
     * @param bundleContextWithAccountManager the <code>BundleContext</code> in
     *            which an <code>AccountManager</code> service is registered
     * @param bundleToStart the <code>Bundle</code> to be started
     * @param protocolNameToWait the protocol name of a
     *            <code>ProtocolProviderFactory</code> to wait the end of the
     *            loading of the stored accounts for
     * @throws BundleException
     * @throws InterruptedException if any thread interrupted the current thread
     *             before or while the current thread was waiting for the
     *             loading of the stored accounts
     */
    public static void startBundleAndWaitStoredAccountsLoaded(
        BundleContext bundleContextWithAccountManager, Bundle bundleToStart,
        final String protocolNameToWait)
        throws BundleException,
        InterruptedException
    {
        AccountManager accountManager =
            getAccountManager(bundleContextWithAccountManager);
        final boolean[] storedAccountsAreLoaded = new boolean[1];
        AccountManagerListener listener = new AccountManagerListener()
        {
            public void handleAccountManagerEvent(AccountManagerEvent event)
            {
                if (AccountManagerEvent.STORED_ACCOUNTS_LOADED == event
                    .getType())
                {
                    ProtocolProviderFactory factory = event.getFactory();

                    if ((factory == null)
                        || protocolNameToWait.equals(factory.getProtocolName()))
                    {
                        synchronized (storedAccountsAreLoaded)
                        {
                            storedAccountsAreLoaded[0] = true;
                            storedAccountsAreLoaded.notify();
                        }
                    }
                }
            }
        };

        accountManager.addListener(listener);
        try
        {
            bundleToStart.start();

            while (true)
            {
                synchronized (storedAccountsAreLoaded)
                {
                    if (storedAccountsAreLoaded[0])
                    {
                        break;
                    }
                    storedAccountsAreLoaded.wait();
                }
            }
        }
        finally
        {
            accountManager.removeListener(listener);
        }
    }

    /**
     * Prevents the creation of <code>AccountManagerUtils</code> instances.
     */
    private AccountManagerUtils()
    {
    }
}
