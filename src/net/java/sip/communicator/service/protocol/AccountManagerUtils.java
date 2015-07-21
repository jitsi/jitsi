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

import net.java.sip.communicator.service.protocol.event.*;

import org.osgi.framework.*;

/**
 * Provides utilities to aid the manipulation of {@link AccountManager}.
 *
 * @author Lyubomir Marinov
 */
public final class AccountManagerUtils
{
    private static AccountManager getAccountManager(BundleContext bundleContext)
    {
        return
            bundleContext.getService(
                    bundleContext.getServiceReference(AccountManager.class));
    }

    /**
     * Starts a specific <code>Bundle</code> and waits for the
     * <code>AccountManager</code> available in a specific
     * <code>BundleContext</code> to load the stored accounts of a
     * <code>ProtocolProviderFactory</code> with a specific protocol name.
     *
     * @param bundleContextWithAccountManager
     *            the <code>BundleContext</code> in which an
     *            <code>AccountManager</code> service is registered
     * @param bundleToStart
     *            the <code>Bundle</code> to be started
     * @param protocolNameToWait
     *            the protocol name of a <code>ProtocolProviderFactory</code> to
     *            wait the end of the loading of the stored accounts for
     * @throws BundleException
     * @throws InterruptedException
     *             if any thread interrupted the current thread before or while
     *             the current thread was waiting for the loading of the stored
     *             accounts
     */
    public static void startBundleAndWaitStoredAccountsLoaded(
            BundleContext bundleContextWithAccountManager,
            final Bundle bundleToStart,
            final String protocolNameToWait)
        throws BundleException,
               InterruptedException
    {
        AccountManager accountManager
            = getAccountManager(bundleContextWithAccountManager);
        final boolean[] storedAccountsAreLoaded = new boolean[1];
        AccountManagerListener listener = new AccountManagerListener()
        {
            public void handleAccountManagerEvent(AccountManagerEvent event)
            {
                if (AccountManagerEvent.STORED_ACCOUNTS_LOADED
                        != event.getType())
                    return;

                ProtocolProviderFactory factory = event.getFactory();

                /*
                 * If the event is for a factory with a protocol name other than
                 * protocolNameToWait, it's not the one we're waiting for.
                 */
                if ((factory != null)
                        && !protocolNameToWait
                                .equals(factory.getProtocolName()))
                    return;

                /*
                 * If the event if for a factory which is no longer registered,
                 * then it's not the one we're waiting for because we're waiting
                 * for the specified bundle to start and register a factory.
                 */
                if (factory != null)
                {
                    BundleContext bundleContext
                        = bundleToStart.getBundleContext();

                    /*
                     * If the specified bundle still hasn't started, the event
                     * cannot be the one we're waiting for.
                     */
                    if (bundleContext == null)
                        return;

                    Collection<ServiceReference<ProtocolProviderFactory>> factoryRefs;

                    try
                    {
                        factoryRefs
                            = bundleContext.getServiceReferences(
                                    ProtocolProviderFactory.class,
                                    "("
                                        + ProtocolProviderFactory.PROTOCOL
                                        + "="
                                        + protocolNameToWait
                                        + ")");
                    }
                    catch (InvalidSyntaxException isex)
                    {
                        /*
                         * Not likely so ignore it and assume the event is for
                         * a valid factory.
                         */
                        factoryRefs = null;
                    }
                    if ((factoryRefs != null) && !factoryRefs.isEmpty())
                    {
                        boolean factoryIsRegistered = false;

                        for (ServiceReference<ProtocolProviderFactory> factoryRef
                                : factoryRefs)
                        {
                            if (factory == bundleContext.getService(factoryRef))
                            {
                                factoryIsRegistered = true;
                                break;
                            }
                        }
                        if (!factoryIsRegistered)
                            return;
                    }
                }

                synchronized (storedAccountsAreLoaded)
                {
                    storedAccountsAreLoaded[0] = true;
                    storedAccountsAreLoaded.notify();
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
