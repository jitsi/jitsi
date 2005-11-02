package net.java.sip.communicator.util;

import org.osgi.framework.*;

/**
 *
 * @author Emil Ivov
 */
public class Activator
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());
    /**
     * start
     *
     * @param bundleContext BundleContext
     * @throws Exception
     * @todo Implement this org.osgi.framework.BundleActivator method
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        logger.debug("Successfully activated!");
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     * @todo Implement this org.osgi.framework.BundleActivator method
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
    }
}
