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
 * Implements <code>BundleActivator</code> for the purposes of
 * protocol.jar/protocol.provider.manifest.mf and in order to register and start
 * services independent of the specifics of a particular protocol.
 *
 * @author Lubomir Marinov
 */
public class ProtocolProviderActivator
    implements BundleActivator
{

    /**
     * The <code>ServiceRegistration</code> of the <code>AccountManager</code>
     * implementation registered as a service by this activator and cached so
     * that the service in question can be properly disposed of upon stopping
     * this activator.
     */
    private ServiceRegistration accountManagerServiceRegistration;

    /**
     * The <code>SingleCallInProgressPolicy</code> making sure that the
     * <code>Call</code>s accessible in the <code>BundleContext</code> of this
     * activator will obey to the rule that a new <code>Call</code> should put
     * the other existing <code>Call</code>s on hold.
     */
    private SingleCallInProgressPolicy singleCallInProgressPolicy;

    /**
     * Registers a new <code>AccountManagerImpl</code> instance as an
     * <code>AccountManager</code> service and starts a new
     * <code>SingleCallInProgressPolicy</code> instance to ensure that only one
     * of the <code>Call</code>s accessible in the <code>BundleContext</code>
     * in which this activator is to execute will be in progress and the others
     * will automatically be put on hold.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void start(BundleContext bundleContext)
    {
        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                new AccountManagerImpl(bundleContext), null);

        singleCallInProgressPolicy =
            new SingleCallInProgressPolicy(bundleContext);
    }

    /**
     * Unregisters the <code>AccountManagerImpl</code> instance registered as an
     * <code>AccountManager</code> service in {@link #start(BundleContext)} and
     * stops the <code>SingleCallInProgressPolicy</code> started there as well.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void stop(BundleContext bundleContext)
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
