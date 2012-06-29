/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Loads the Rss provider factory and registers its services in the OSGI
 * bundle context.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 * @author Emil Ivov
 */
public class RssActivator
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(RssActivator.class);

    /**
     * A reference to the registration of our Rss protocol provider
     * factory.
     */
    private ServiceRegistration  rssPpFactoryServReg   = null;

    /**
     * A reference to the Rss protocol provider factory.
     */
    private static ProtocolProviderFactoryRssImpl
                                    rssProviderFactory = null;

    /**
     * The currently valid bundle context.
     */
    static BundleContext bundleContext = null;

    /**
     * The <tt>ResourceManagementService</tt> that we use in this provider.
     */
    private static ResourceManagementService resourcesService = null;

    /**
     * The <tt>UIService</tt> that we use in this provider.
     */
    private static UIService uiService = null;

    /**
     * The uri handler that would be handling all feed:// links.
     */
    private UriHandlerRssImpl uriHandler = null;

    /**
     * Called when this bundle is started. In here we'll export the
     * rss ProtocolProviderFactory implementation so that it could be
     * possible to register accounts with it in SIP Communicator.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        RssActivator.bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, "RSS");

        rssProviderFactory = new ProtocolProviderFactoryRssImpl();

        //reg the rss provider factory.
        rssPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    rssProviderFactory,
                    hashtable);

        if (logger.isInfoEnabled())
            logger.info("RSS protocol implementation [STARTED].");

        uriHandler = new UriHandlerRssImpl();
        bundleContext.addServiceListener(uriHandler);
        uriHandler.registerHandlerService();
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryRssImpl getProtocolProviderFactory()
    {
        return rssProviderFactory;
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
    public void stop(BundleContext context)
        throws Exception
    {
        RssActivator.rssProviderFactory.stop();
        rssPpFactoryServReg.unregister();

        context.removeServiceListener(uriHandler);

        if (logger.isInfoEnabled())
            logger.info("RSS protocol implementation [STOPPED].");
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns a reference to the <tt>UIService</tt> instance that is currently
     * in use.
     * @return a reference to the currently valid <tt>UIService</tt>.
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference serviceReference
                = bundleContext.getServiceReference(UIService.class.getName());

            if (serviceReference != null)
                uiService
                    = (UIService) bundleContext.getService(serviceReference);
        }
        return uiService;
    }
}
