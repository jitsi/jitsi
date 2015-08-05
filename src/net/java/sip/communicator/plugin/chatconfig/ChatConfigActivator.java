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
package net.java.sip.communicator.plugin.chatconfig;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The chat configuration form activator.
 *
 * @author Purvesh Sahoo
 */
public class ChatConfigActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatConfigActivator</tt> class.
     */
    private final static Logger logger =
        Logger.getLogger(ChatConfigActivator.class);

    /**
     * The currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * The Replacement sources map.
     */
    private static final Map<String, ReplacementService>
    replacementSourcesMap = new Hashtable<String, ReplacementService>();

    /**
     * Indicates if the chat configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.chatconfig.DISABLED";

    /**
     * Starts this bundle.
     *
     * @param bc the BundleContext
     * @throws Exception if some of the operations executed in the start method
     *             fails
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        // If the chat configuration form is disabled don't continue.
        if (getConfigurationService().getBoolean(DISABLED_PROP, false))
            return;

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
            ConfigurationForm.GENERAL_TYPE);
        bundleContext.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.chatconfig.ChatConfigPanel",
                getClass().getClassLoader(), "plugin.chatconfig.PLUGIN_ICON",
                "plugin.chatconfig.TITLE", 40), properties);

        if (logger.isTraceEnabled())
            logger.trace("Chat Configuration: [ STARTED ]");
    }

    /**
     * Stops this bundle.
     *
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Gets the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }

    /**
     * Creates a config section label from the given text.
     *
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

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     *         context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference configReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            configService =
                (ConfigurationService) bundleContext
                    .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns all <tt>ReplacementService</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ReplacementService</tt> implementation obtained from the
     *         bundle context
     */
    public static Map<String, ReplacementService> getReplacementSources()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered sources
            serRefs =
                bundleContext.getServiceReferences(ReplacementService.class
                    .getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Error : " + e);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                ReplacementService replacementSources =
                    (ReplacementService) bundleContext.getService(serRefs[i]);

                replacementSourcesMap.put((String)serRefs[i]
                    .getProperty(ReplacementService.SOURCE_NAME),
                    replacementSources);
            }
        }
        return replacementSourcesMap;
    }

}
