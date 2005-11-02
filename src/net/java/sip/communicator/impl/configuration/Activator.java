/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.Logger;
import java.io.*;
import net.java.sip.communicator.util.xml.*;
import net.java.sip.communicator.impl.configuration.xml.*;

/**
 *
 * @author Emil Ivov
 */
public class Activator
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(ConfigurationServiceImpl.class);
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
        try
        {
            logger.logEntry();

            bundleContext.registerService( ConfigurationService.class.getName(),
                                           impl,
                                           new java.util.Hashtable() );

            logger.debug("Successfully registered " + getClass().getName());
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration servcice.
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     * @todo Implement this org.osgi.framework.BundleActivator method
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
        try
        {
            logger.logEntry();



            logger.info("The ConfigurationService stop method has been called.");
        }
        finally
        {
            logger.logEntry();
        }
    }
}
