/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.plugin.generalconfig.autoaway.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The general configuration form activator.
 *
 * @author Yana Stamcheva
 */
public class GeneralConfigPluginActivator
    extends AbstractServiceDependentActivator
    implements  ServiceListener
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
    public static BundleContext bundleContext;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The status update thread.
     */
    private static AutoAwayWatcher runner = null;

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
     * The message history service.
     */
    private static MessageHistoryService messageHistoryService;

    /**
     * Indicates if the general configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.generalconfig.DISABLED";

    /**
     * Indicates if the SIP configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String SIP_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.sipconfig.DISABLED";

    /**
     * Starts this bundle.
     */
    @Override
    public void start(Object dependentService)
    {
        uiService = (UIService)dependentService;

        Dictionary<String, String> properties
            = new Hashtable<String, String>();

        // If the general configuration form is disabled don't continue.
        if (!getConfigurationService().getBoolean(DISABLED_PROP, false))
        {
            ConfigurationUtils.loadGuiConfigurations();

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
        }

        // Checks if the SIP configuration form is disabled.
        if (!getConfigurationService()
                .getBoolean(SIP_CONFIG_DISABLED_PROP, false))
        {
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
        }
        properties.put( ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        SilkConfigForm.class.getName(),
                        getClass().getClassLoader(),
                        null,
                        "plugin.generalconfig.SILK_CONFIG",
                        0, true),
                properties);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        OpusConfigForm.class.getName(),
                        getClass().getClassLoader(),
                        null,
                        "plugin.generalconfig.OPUS_CONFIG",
                        0, true),
                properties);

        try
        {
            /*
             * Wait for the first ProtocolProviderService to register in order to
             * start the auto-away functionality i.e. to call #startThread().
             */
            ServiceReference[] protocolRefs = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
            if(protocolRefs != null && protocolRefs.length > 0)
            {
                synchronized (GeneralConfigPluginActivator.class)
                {
                    if (!startThreadIsCalled)
                    {
                        startThread();
                        startThreadIsCalled = true;
                    }
                }
            }
            else
                bundleContext.addServiceListener(this);
        }
        catch(Throwable t)
        {
            // not supposed to happen
            logger.error("Error starting auto away thread", t);
        }

        if (logger.isInfoEnabled())
            logger.info("PREFERENCES PLUGIN... [REGISTERED]");
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
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
        if (runner == null)
            runner = new AutoAwayWatcher();
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
            return new ProtocolProviderService[0];
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
     * Gets the service giving access to message history.
     *
     * @return the service giving access to message history.
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if (messageHistoryService == null)
            messageHistoryService = ServiceUtils.getService(bundleContext, 
                MessageHistoryService.class);
        return messageHistoryService;
    }

    /**
     * Creates a config section label from the given text.
     * @param labelText the text of the label.
     * @return the created label
     */
    public static JPanel createConfigSectionComponent(String labelText)
    {
        JLabel label = new JLabel(labelText);
        label.setBorder(new EmptyBorder(0, 0, 0, 10));
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        JPanel pnlSectionName = new TransparentPanel();
        pnlSectionName.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridwidth = 2;
        pnlSectionName.add(label, c);
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        pnlSectionName.add(new JSeparator(), c);

        JPanel pnlSection = new TransparentPanel()
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            @Override
            public Component add(Component comp)
            {
                if(comp instanceof JComponent)
                    ((JComponent)comp).setAlignmentX(LEFT_ALIGNMENT);
                return super.add(comp);
            }
        };
        pnlSection.setLayout(new BoxLayout(pnlSection, BoxLayout.Y_AXIS));
        pnlSection.add(pnlSectionName);

        return pnlSection;
    }
}
