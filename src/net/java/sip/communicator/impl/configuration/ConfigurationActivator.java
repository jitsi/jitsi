/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * @author Emil Ivov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    /**
     * The current bundle context
     */
    public static BundleContext bundleContext;
    
    private final Logger logger = Logger.getLogger(ConfigurationServiceImpl.class);
    private ConfigurationServiceImpl impl = new ConfigurationServiceImpl();

    /**
     * Starts the configuration service
     *
     * @param bundleContext the BundleContext as provided from the osgi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");
        
        ConfigurationActivator.bundleContext = bundleContext;
        impl.start();

        bundleContext.registerService(ConfigurationService.class.getName(),
                                      impl,
                                      new Hashtable());

        logger.debug("Service Impl: " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration servcice.
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
        logger.logEntry();
        impl.stop();
        logger.info("The ConfigurationService stop method has been called.");
    }
}
