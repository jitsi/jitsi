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
 * @author Lubomir Marinov
 */
public class AutoAwayActivator
    implements BundleActivator,
               ServiceListener
{
    private static Logger logger = Logger.getLogger(AutoAwayActivator.class);

    static BundleContext bundleContext = null;

    private static Thread thread = null;
    private static StatusUpdateThread runner = null;

    /**
     * The indicator which determines whether {@link #startThread()} has been
     * called and thus prevents calling it more than once.
     */
    private static boolean startThreadIsCalled = false;

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

        /*
         * Wait for the first ProtocolProviderService to register in order to
         * start the auto-away functionality i.e. to call #startThread().
         */
        bundleContext.addServiceListener(this);
    }

    /*
     * Implements ServiceListener#serviceChanged(ServiceEvent). Waits for the
     * first ProtocolProviderService to register in order to start the auto-away
     * functionality i.e. to call #startThread().
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        switch (serviceEvent.getType())
        {
        case ServiceEvent.MODIFIED:
        case ServiceEvent.REGISTERED:
            Object service
                = bundleContext.getService(serviceEvent.getServiceReference());
            if (service instanceof ProtocolProviderService)
            {
                synchronized (AutoAwayActivator.class)
                {
                    if (!startThreadIsCalled)
                    {
                        startThread();
                        startThreadIsCalled = true;
                    }
                }

                bundleContext.removeServiceListener(this);
            }
            break;

        default:
            break;
        }
    }

    private static void startThread()
    {

        /*
         * FIXME Even if auto away is disabled at this point, it doesn't mean
         * that it will not get enabled later on so this method likely has to
         * also be called when the configuration property gets changed.
         */
        if (!getConfigService().getBoolean(Preferences.ENABLE, false))
            return;

        if (runner == null)
            runner = new StatusUpdateThread();
        if ((thread == null) || !runner.isRunning())
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

    private static void stopThread()
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
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService)
                    bundleContext.getService(serviceReference);
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
        ServiceReference confServiceRefs
            = bundleContext
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
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }
}
