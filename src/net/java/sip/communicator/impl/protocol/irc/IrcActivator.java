/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

/**
 * Loads the IRC provider factory and registers its services in the OSGI
 * bundle context.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class IrcActivator
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(IrcActivator.class);

    /**
     * A reference to the IRC protocol provider factory.
     */
    private static ProtocolProviderFactoryIrcImpl
                                    ircProviderFactory = null;

    /**
     * The currently valid bundle context.
     */
    public static BundleContext bundleContext = null;

    private static ResourceManagementService resourceService;

    /**
     * Called when this bundle is started. In here we'll export the
     * IRC ProtocolProviderFactory implementation so that it could be
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
        bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);

        ircProviderFactory = new ProtocolProviderFactoryIrcImpl();

        //Register the IRC provider factory.
        context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    ircProviderFactory,
                    hashtable);

        if (logger.isInfoEnabled())
            logger.info("IRC protocol implementation [STARTED].");
    }

    /**
     * Returns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryIrcImpl getProtocolProviderFactory()
    {
        return ircProviderFactory;
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
        if (logger.isInfoEnabled())
            logger.info("IRC protocol implementation [STOPPED].");
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     * 
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }
}
