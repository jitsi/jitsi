/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.statusupdate;

import java.util.*;

import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.*;

/**
 * Activator of the StatusUpdate Bundle
 * 
 * @author Thomas Hofer
 */
public class StatusUpdateActivator implements BundleActivator
{

    private static Logger logger = Logger
            .getLogger(StatusUpdateActivator.class);

    static BundleContext bundleContext = null;

    private static Thread thread = null;
    private static StatusUpdateThread runner = null;

    private ServiceRegistration menuRegistration;

    /**
     * Starts this bundle
     * 
     * @param bundleContext
     *                BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
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
        registerMenuEntry();
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
            thread.setName(StatusUpdateActivator.class.getName());
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
        unRegisterMenuEntry();
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
        BundleContext bundleContext = StatusUpdateActivator.bundleContext;

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
     * @return
     */
    static ConfigurationService getConfigService()
    {
        // retrieve a reference to the config access service.
        ServiceReference confServiceRefs = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

        return (ConfigurationService) bundleContext.getService(confServiceRefs);
    }

    private void registerMenuEntry()
    {
        SettingsWindowMenuEntry menuEntry = new SettingsWindowMenuEntry(
                Container.CONTAINER_TOOLS_MENU);

        Hashtable<String, String> toolsMenuFilter = new Hashtable<String, String>();
        toolsMenuFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

        menuRegistration = bundleContext.registerService(PluginComponent.class
                .getName(), menuEntry, toolsMenuFilter);
    }

    private void unRegisterMenuEntry()
    {
        if (menuRegistration != null)
        {
            menuRegistration.unregister();
        }
    }

}