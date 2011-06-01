/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.generalconfig.autoaway.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The general configuration form activator.
 *
 * @author Yana Stamcheva
 */
public class GeneralConfigPluginActivator
    implements  BundleActivator,
                ServiceListener
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(GeneralConfigPluginActivator.class);

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The systray service.
     */
    private static SystrayService systrayService;

    /**
     * The bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The auto away thread.
     */
    private static Thread autoAwayThread = null;

    /**
     * The status update thread.
     */
    private static StatusUpdateThread runner = null;

    /**
     * The indicator which determines whether {@link #startThread()} has been
     * called and thus prevents calling it more than once.
     */
    private static boolean startThreadIsCalled = false;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        ConfigurationManager.loadGuiConfigurations();

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);
        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin." +
                    "generalconfig.GeneralConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.generalconfig.PLUGIN_ICON",
                    "service.gui.GENERAL",
                    0),
                properties);

        // Registers the sip config panel as advanced configuration form.
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                SIPConfigForm.class.getName(),
                getClass().getClassLoader(),
                null,
                "plugin.generalconfig.SIP_CALL_CONFIG",
                52, true),
            properties);

        /*
         * Wait for the first ProtocolProviderService to register in order to
         * start the auto-away functionality i.e. to call #startThread().
         */
        bundleContext.addServiceListener(this);

        if (logger.isInfoEnabled())
            logger.info("PREFERENCES PLUGIN... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc) throws Exception
    {
        stopThread();
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService() {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle
     * context.
     * @return the <tt>SystrayService</tt> obtained from the bundle
     * context
     */
    static SystrayService getSystrayService()
    {
        if(systrayService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(SystrayService.class.getName());

            systrayService = (SystrayService) bundleContext
                .getService(configReference);
        }

        return systrayService;
    }

     /**
     * Returns the <tt>UIService</tt>.
     * 
     * @return the <tt>UIService</tt>
     */
    static UIService getUIService()
    {
        return uiService;
    }

    /**
     * Implements ServiceListener#serviceChanged(ServiceEvent). Waits for the
     * first ProtocolProviderService to register in order to start the auto-away
     * functionality i.e. to call #startThread().
     * @param serviceEvent the <tt>ServiceEvent</tt> that notified us
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
                synchronized (GeneralConfigPluginActivator.class)
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

    /**
     * Starts the auto away thread.
     */
    private static void startThread()
    {
        /*
         * FIXME Even if auto away is disabled at this point, it doesn't mean
         * that it will not get enabled later on so this method likely has to
         * also be called when the configuration property gets changed.
         */
        if (!getConfigurationService().getBoolean(Preferences.ENABLE, false))
            return;

        if (runner == null)
            runner = new StatusUpdateThread();
        if ((autoAwayThread == null) || !runner.isRunning())
        {
            autoAwayThread = new Thread(runner);
            autoAwayThread.setName(GeneralConfigPluginActivator.class.getName());
            autoAwayThread.setPriority(Thread.MIN_PRIORITY);
            autoAwayThread.setDaemon(true);
            autoAwayThread.start();
        } else
        {
            autoAwayThread.interrupt();
        }
    }

    /**
     * Stops the auto away thread.
     */
    private static void stopThread()
    {
        if (runner != null)
        {
            runner.stop();
            runner = null;
        }
        if (autoAwayThread != null)
        {
            autoAwayThread.interrupt();
            autoAwayThread = null;
        }
    }

    /**
     * Returns an array of all available protocol providers.
     * @return an array of all available protocol providers
     */
    public static ProtocolProviderService[] getProtocolProviders()
    {
        // get the protocol provider factory
        BundleContext bundleContext = GeneralConfigPluginActivator.bundleContext;

        ServiceReference[] serRefs = null;
        // String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL + "="
        // + ProtocolNames.SIP + ")";

        try
        {
            // serRefs = bundleContext.getServiceReferences(
            // ProtocolProviderFactory.class.getName(), osgiFilter);
            serRefs = bundleContext.getAllServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
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
     * Gets the service giving access to all application resources.
     * 
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }

    /**
     * Creates a config section label from the given text.
     * @param labelText the text of the label.
     * @return the created label
     */
    public static Component createConfigSectionComponent(String labelText)
    {
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel parentPanel = new TransparentPanel(new BorderLayout());
        parentPanel.add(label, BorderLayout.NORTH);
        parentPanel.setPreferredSize(new Dimension(180, 25));

        return parentPanel;
    }
}
