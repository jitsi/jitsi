/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.autoaway;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator of the StatusUpdate Bundle
 * 
 * @author Thomas Hofer
 */
public class AutoAwayActivator
    implements BundleActivator
{
    private static Logger logger = Logger.getLogger(AutoAwayActivator.class);

    static BundleContext bundleContext = null;

    private static Thread thread = null;
    private static StatusUpdateThread runner = null;

    private static ResourceManagementService resourceService;

    /**
     * Starts this bundle
     * 
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        // Set config form
        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.autoaway.AutoAwayConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.autoaway.PLUGIN_ICON",
                    "plugin.autoaway.AUTO_STATUS",
                    20),
                null);

        new Thread(new Runnable()
        {

            public void run()
            {
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException e)
                {
                }
                // wait a few seconds
                startThread();
            }
        }).start();
    }

    static void startThread()
    {
        ConfigurationService configService = getConfigService();
        String e = (String) configService.getProperty(Preferences.ENABLE);
        if (e == null)
        {
            return;
        }
        try
        {
            boolean enabled = Boolean.parseBoolean(e);
            if (!enabled)
            {
                return;
            }
        } catch (NumberFormatException ex)
        {
            return;
        }

        if (runner == null)
        {
            runner = new StatusUpdateThread();
        }
        if (thread == null || !runner.isRunning())
        {
            thread = new Thread(runner);
            thread.setName(AutoAwayActivator.class.getName());
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            thread.start();
        } else
        {
            thread.interrupt();
        }
    }

    /**
     * stop the bundle
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        stopThread();
    }

    static void stopThread()
    {
        if (runner != null)
        {
            runner.stop();
            runner = null;
        }
        if (thread != null)
        {
            thread.interrupt();
            thread = null;
        }
    }

    static ProtocolProviderService[] getProtocolProviders()
    {
        // get the protocol provider factory
        BundleContext bundleContext = AutoAwayActivator.bundleContext;

        ServiceReference[] serRefs = null;
        // String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL + "="
        // + ProtocolNames.SIP + ")";

        try
        {
            // serRefs = bundleContext.getServiceReferences(
            // ProtocolProviderFactory.class.getName(), osgiFilter);
            serRefs = bundleContext.getAllServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        } catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }

        if (serRefs == null || serRefs[0] == null)
        {
            return null;
        }

        Set<ProtocolProviderService> pps = new HashSet<ProtocolProviderService>();

        for (ServiceReference serviceReference : serRefs)
        {
            ProtocolProviderService protocolProvider = (ProtocolProviderService) bundleContext
                    .getService(serviceReference);
            pps.add(protocolProvider);
        }

        return pps.toArray(new ProtocolProviderService[0]);
    }

    /**
     * Gets the ConfigurationService
     * 
     * @return configuration service
     */
    static ConfigurationService getConfigService()
    {
        // retrieve a reference to the config access service.
        ServiceReference confServiceRefs = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

        return (ConfigurationService) bundleContext.getService(confServiceRefs);
    }

    /**
     * Gets the service giving access to all application resources.
     * 
     * @return the service giving access to all application resources.
     */
    static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            // retrieve a reference to the resource access service.
            ServiceReference resourceServiceRefs = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            resourceService = (ResourceManagementService) bundleContext
                .getService(resourceServiceRefs);
        }

        return resourceService;
    }
}
