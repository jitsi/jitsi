/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    private final Logger logger
        = Logger.getLogger(ConfigurationServiceImpl.class);

    private final ConfigurationServiceImpl impl
        = new ConfigurationServiceImpl();

    /**
     * Starts the configuration service
     * 
     * @param bundleContext
     *            the BundleContext as provided from the OSGi framework.
     * @throws Exception
     *             if anything goes wrong
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        impl.start(bundleContext);

        bundleContext.registerService(ConfigurationService.class.getName(),
                                      impl,
                                      null);

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration service.
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        logger.logEntry();
        impl.stop();
        if (logger.isInfoEnabled())
            logger.info("The ConfigurationService stop method has been called.");
    }
}
