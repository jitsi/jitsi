/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Lubomir Marinov
 */
public class ProtocolProviderActivator
    implements BundleActivator
{
    private ServiceRegistration accountManagerServiceRegistration;

    private SingleCallInProgressPolicy singleCallInProgressPolicy;

    public void start(BundleContext bundleContext) throws Exception
    {
        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                new AccountManagerImpl(bundleContext), null);

        singleCallInProgressPolicy =
            new SingleCallInProgressPolicy(bundleContext);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if (accountManagerServiceRegistration != null)
        {
            accountManagerServiceRegistration.unregister();
            accountManagerServiceRegistration = null;
        }

        if (singleCallInProgressPolicy != null)
        {
            singleCallInProgressPolicy.dispose();
            singleCallInProgressPolicy = null;
        }
    }
}
