package net.java.sip.communicator.impl.protocol.sip;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the SIP package
 * @author Emil Ivov
 */
public class Activator
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(Activator.class.getName());

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
        logger.debug("Started.");
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
        logger.debug("Stopped.");
    }
}
