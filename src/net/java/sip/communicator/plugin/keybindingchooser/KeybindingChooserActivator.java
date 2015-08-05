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
package net.java.sip.communicator.plugin.keybindingchooser;

import java.util.*;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for the keybinding chooser.
 *
 * @author Damian Johnson
 */
public class KeybindingChooserActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(KeybindingChooserActivator.class);

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext;

    /**
     * The service used to obtain resources.
     */
    public static ResourceManagementService resourcesService;

    /**
     * Reference to the configuration service
     */
    private static ConfigurationService configService;

    /**
     * Reference to the keybinding service
     */
    private static KeybindingsService keybindingService = null;

    /**
     * Reference to the global shortcut service
     */
    private static GlobalShortcutService globalShortcutService = null;

    /**
     * Indicates if the keybindings configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.keybindingsconfig.DISABLED";

    /**
     * Starts this bundle and adds the
     * <td>KeybindingsConfigPanel</tt> contained in it to the configuration
     * window obtained from the <tt>UIService</tt>.
     * @param bc the <tt>BundleContext</tt>
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;

        if (logger.isDebugEnabled())
            logger.debug(
                    "Service Impl: " + getClass().getName() + " [  STARTED ]");

        // If the dns configuration form is disabled don't continue.
        if(!getConfigService().getBoolean(DISABLED_PROP, false))
        {
            Dictionary<String, String> properties
                = new Hashtable<String, String>();
            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.ADVANCED_TYPE);
            bc.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.keybindingchooser.KeybindingsConfigPanel",
                    getClass().getClassLoader(),
                    "plugin.keybinding.PLUGIN_ICON",
                    "plugin.keybindings.PLUGIN_NAME",
                    900, true),
                properties);
        }
    }

    /**
     * Stops this bundles.
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the stop method
     * fails
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns the bundle context.
     * @return the bundle context
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns the resources service.
     * @return the resources service
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        if(configService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ConfigurationService.class.getName());
            configService
                = (ConfigurationService) bundleContext.getService(
                        confReference);
        }
        return configService;
    }

    /**
     * Returns a reference to a KeybindingsService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the KeybindingsService.
     */
    public static KeybindingsService getKeybindingsService()
    {
        if(keybindingService == null)
        {
            ServiceReference keybindingReference
                = bundleContext.getServiceReference(
                        KeybindingsService.class.getName());
            keybindingService
                = (KeybindingsService) bundleContext.getService(
                        keybindingReference);
        }
        return keybindingService;
    }

    /**
     * Returns a reference to a GlobalShortcutService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the GlobalShortcutService.
     */
    public static GlobalShortcutService getGlobalShortcutService()
    {
        if(globalShortcutService == null)
        {
            ServiceReference globalShortcutReference
                = bundleContext.getServiceReference(
                        GlobalShortcutService.class.getName());
            globalShortcutService
                = (GlobalShortcutService) bundleContext.getService(
                        globalShortcutReference);
        }
        return globalShortcutService;
    }
}
