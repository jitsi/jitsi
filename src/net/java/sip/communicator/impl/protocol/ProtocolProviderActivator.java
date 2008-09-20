/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;

public class ProtocolProviderActivator
    implements BundleActivator
{
    private ServiceRegistration accountManagerServiceRegistration;

    public void start(BundleContext bundleContext) throws Exception
    {
        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                new AccountManagerImpl(bundleContext), null);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if (accountManagerServiceRegistration != null)
        {
            accountManagerServiceRegistration.unregister();
            accountManagerServiceRegistration = null;
        }
    }
}
