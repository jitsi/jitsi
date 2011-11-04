/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Lyubomir Marinov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>ConfigurationActivator</tt> instance
     * for logging output.
     */
    private final Logger logger
        = Logger.getLogger(ConfigurationServiceImpl.class);

    /**
     * The <tt>BundleContext</tt> in which the configuration bundle has been
     * started and has not been stopped yet.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ConfigurationService</tt> implementation provided by the bundle
     * represented by this <tt>ConfigurationActivator</tt>.
     */
    private final ConfigurationServiceImpl impl
        = new ConfigurationServiceImpl();

    /**
     * Gets the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet.
     *
     * @return the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Starts the configuration service
     *
     * @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        boolean started = false;

        ConfigurationActivator.bundleContext = bundleContext;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Service Impl: "
                            + getClass().getName()
                            + " [  STARTED ]");
            }

            impl.start(bundleContext);
            bundleContext.registerService(
                    ConfigurationService.class.getName(),
                    impl,
                    null);

            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Service Impl: "
                            + getClass().getName()
                            + " [REGISTERED]");
            }

            started = true;
        }
        finally
        {
            if (!started
                    && (ConfigurationActivator.bundleContext == bundleContext))
                ConfigurationActivator.bundleContext = null;
        }
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration service.
     *
     * @param bundleContext <tt>BundleContext</tt>
     * @throws Exception if anything goes wrong while storing the properties
     * managed by the <tt>ConfigurationService</tt> implementation provided by
     * this bundle and while unregistering the service in question
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            logger.logEntry();
            impl.stop();
            if (logger.isInfoEnabled())
                logger.info("ConfigurationService#stop() has been called.");
        }
        finally
        {
            if (ConfigurationActivator.bundleContext == bundleContext)
                ConfigurationActivator.bundleContext = null;
        }
    }
}
