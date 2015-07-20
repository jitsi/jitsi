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
package net.java.sip.communicator.plugin.pluginmanager;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the PluginManager plugin.
 *
 * @author Yana Stamcheva
 */
public class PluginManagerActivator
    implements BundleActivator
{
    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

     /**
     * Indicates if the plug-in configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.pluginconfig.DISABLED";

    /**
     * Name of the property that defines which bundles are considered system.
     */
    private static final String SYSTEM_BUNDLES_PROP
        = "net.java.sip.communicator.plugin.pluginmanager.SYSTEM_BUNDLES";

    /**
     * Cache for the system bundles config property.
     */
    private static List<String> systemBundleNames;

    /**
     * Starts this bundle and adds the
     * <td>PluginManagerConfigForm</tt> contained in it to the configuration
     * window obtained from the <tt>UIService</tt>.
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the start method
     * fails
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        // If the plug-in manager configuration form is disabled don't continue.
        if(!getConfigurationService().getBoolean(DISABLED_PROP, false))
        {
            Dictionary<String, String> properties
                = new Hashtable<String, String>();
            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.ADVANCED_TYPE);
            bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    PluginManagerPanel.class.getName(),
                    getClass().getClassLoader(),
                    "plugin.pluginmanager.PLUGIN_ICON",
                    "plugin.pluginmanager.PLUGINS",
                    1000, true),
                properties);
        }

        systemBundleNames = Arrays.asList(getConfigurationService()
            .getString(SYSTEM_BUNDLES_PROP).split("\\s*,\\s*"));
    }

    /**
     * Stops this bundles.
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the stop method
     * fails
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
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
     * Determines whether <tt>bundle</tt> is system or not. We consider system
     * bundles those that we have explicitly marked as such with the
     * <tt>System-Bundle</tt> manifest property or those that belong to the
     * Apache framework itself.
     *
     * @param bundle the bundle that we need to determine as system or not.
     * @return true if <tt>bundle</tt> is a system bundle and <tt>false</tt>
     * otherwise.
     */
    public static boolean isSystemBundle(Bundle bundle)
    {
        if (bundle.getBundleId() <= 1)
        {
            //this is one of the felix bundles
            return true;
        }

        //ignore if this is a system bundle
        return systemBundleNames.contains(bundle.getSymbolicName());
    }
}
