/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Listens for system activity changes like sleep, network change, inactivity
 * and inform all its listeners.
 *
 * @author Damian Minkov
 */
public class SysActivityActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(SysActivityActivator.class);

    /**
     * The OSGi service registration.
     */
    private ServiceRegistration sysActivitiesServReg = null;

    /**
     * The OSGi <tt>BundleContext</tt>.
     */
    private static BundleContext bundleContext = null;

    /**
     * The system activity service impl.
     */
    private static SystemActivityNotificationsServiceImpl sysActivitiesServiceImpl;

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
    public void start(BundleContext context)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");
        SysActivityActivator.bundleContext = context;

        sysActivitiesServiceImpl = new SystemActivityNotificationsServiceImpl();
        sysActivitiesServiceImpl.start();

        sysActivitiesServReg =  context.registerService(
                    SystemActivityNotificationsService.class.getName(),
                    sysActivitiesServiceImpl,
                    null);
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
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static SystemActivityNotificationsServiceImpl
        getSystemActivityService()
    {
        return sysActivitiesServiceImpl;
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
        sysActivitiesServReg.unregister();

        if(sysActivitiesServiceImpl != null)
            sysActivitiesServiceImpl.stop();
    }
}
