/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.ResourceManagementService;

import java.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the PropertiesEditor plugin.
 *
 * @author Marin Dzhigarov
 * @author Pawel Domas
 */
public class PropertiesEditorActivator 
    implements BundleActivator 
{

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * Starts this bundle and adds the
     * <td>PropertiesEditorPanel</tt> contained in it to the configuration
     * window obtained from the <tt>UIService</tt>.
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the start method
     * fails
     */
    public void start(BundleContext bc) throws Exception 
    {
        bundleContext = bc;
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    StartingPanel.class.getName(),
                    getClass().getClassLoader(),
                    "",
                    "plugin.propertieseditor.TITLE",
                    1002, true),
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
     * The ui service
     */
    private static UIService uiService;

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context.
     */
    public static UIService getUIService() 
    {
        if (uiService == null) 
        {
            ServiceReference uiReference =
                    bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                    (UIService) bundleContext.getService(uiReference);
        }

        return uiService;
    }

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle context.
     * 
     * @return the <tt>ConfigurationService</tt> obtained from the bundle context.
     */
    public static ConfigurationService getConfigurationService() 
    {
        if (configService == null) 
        {
            configService = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        }
        return configService;
    }

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceManagementService;

    
    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the bundle context.
     * 
     * @return the <tt>ResourceManagementService</tt> obtained from the bundle context.
     */
    public static ResourceManagementService getResourceManagementService() {
        if (resourceManagementService == null) {
            resourceManagementService = ServiceUtils.getService(
                    bundleContext,
                    ResourceManagementService.class);
        }
        return resourceManagementService;
    }
}