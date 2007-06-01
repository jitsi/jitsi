/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.impl.gui.*;
/**
 * Loads the Rss provider factory and registers its services in the OSGI
 * bundle context.
 *
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
    private static BundleContext bundleContext = null;


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
        this.bundleContext = context;

        Hashtable hashtable = new Hashtable();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, "Rss");

        rssProviderFactory = new ProtocolProviderFactoryRssImpl();

        //load all stored Rss accounts.
        rssProviderFactory.loadStoredAccounts();

        //reg the rss provider factory.
        rssPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    rssProviderFactory,
                    hashtable);
        
        logger.info("Rss protocol implementation [STARTED].");
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
        this.rssProviderFactory.stop();
        rssPpFactoryServReg.unregister();
        logger.info("Rss protocol implementation [STOPPED].");
    }
}
