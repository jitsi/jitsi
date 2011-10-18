/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

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
     * Starts this bundle and adds the
     * <td>KeybindingsConfigPanel</tt> contained in it to the configuration
     * window obtained from the <tt>UIService</tt>.
     * @param bc the <tt>BundleContext</tt>
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        Dictionary<String, String> properties = new Hashtable<String, String>();
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
}
