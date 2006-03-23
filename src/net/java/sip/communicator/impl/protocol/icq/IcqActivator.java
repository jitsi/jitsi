package net.java.sip.communicator.impl.protocol.icq;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;
import java.util.Hashtable;
import net.java.sip.communicator.service.configuration.*;

/**
 * Loads the  ICQ account manager and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Emil Ivov
 */
public class IcqActivator
    implements BundleActivator
{
    ServiceRegistration icqAccManRegistration = null;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable hashtable = new Hashtable();
        hashtable.put(AccountManager.PROTOCOL_PROPERTY_NAME, "ICQ");

        AccountManagerIcqImpl icqAccountManager =
                                        new AccountManagerIcqImpl();

        ServiceReference confReference
            = context.getServiceReference(ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService)context.getService(confReference);

        //load all icq providers
        icqAccountManager.loadStoredAccounts(context, configurationService);

        //reg the icq account man.
        icqAccManRegistration =  context.registerService(
                    AccountManager.class.getName(),
                    icqAccountManager,
                    hashtable);
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
        icqAccManRegistration.unregister();
    }
}
