/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.globalproxyconfig;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Register the configuration form.
 *
 * @author  Atul Aggarwal
 * @author Damian Minkov
 */
public class GlobalProxyPluginActivator implements BundleActivator
{
    /**
     * Our logger.
     */
    private Logger logger = Logger.getLogger(GlobalProxyPluginActivator.class);

    /**
     * The Configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The context of this bundle.
     */
    protected static BundleContext bundleContext;

    /**
     * Starts the bundle.
     * @param bc the context
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        GlobalProxyConfigForm proxyConfigForm = new GlobalProxyConfigForm();

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                GlobalProxyConfigForm.class.getName(),
                getClass().getClassLoader(),
                "plugin.globalproxy.PLUGIN_ICON",
                "plugin.globalproxy.GLOBAL_PROXY_CONFIG",
                51),
            null);

        logger.info("GLOBAL PROXY CONFIGURATION PLUGIN... [REGISTERED]");
    }

    /**
     * Stops it.
     * @param bc the context.
     * @throws Exception
     */
    public void stop(BundleContext bc) throws Exception
    {
    }
    
    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }
}
