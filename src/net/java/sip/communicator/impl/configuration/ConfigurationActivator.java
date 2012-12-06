/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.osgi.framework.*;

/**
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> in which the configuration bundle has been
     * started and has not been stopped yet.
     */
    private static BundleContext bundleContext;

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
        ConfigurationActivator.bundleContext = bundleContext;

        ConfigurationService configurationService
            = LibJitsi.getConfigurationService();

        if (configurationService != null)
        {
            bundleContext.registerService(
                    ConfigurationService.class.getName(),
                    configurationService,
                    null);
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
    }

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
}
