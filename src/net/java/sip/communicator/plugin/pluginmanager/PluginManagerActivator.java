/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;

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
                    "net.java.sip.communicator.plugin.pluginmanager.PluginManagerPanel",
                    getClass().getClassLoader(),
                    "plugin.pluginmanager.PLUGIN_ICON",
                    "plugin.pluginmanager.PLUGINS",
                    1000, true),
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

        Object sysBundleProp = bundle.getHeaders().get("System-Bundle");

        //ignore if this is a system bundle
        return (sysBundleProp != null && sysBundleProp.equals("yes"));
    }
}
