/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.pluginmanager;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the PluginManager plugin.
 * 
 * @author Yana Stamcheva
 */
public class PluginManagerActivator implements BundleActivator
{
    public static BundleContext bundleContext;
    
    private static UIService uiService;
    
    private static ConfigurationService configService;
   
    /**
     * Starts this bundle and adds the <td>PluginManagerConfigForm</tt> contained
     * in it to the configuration window obtained from the <tt>UIService</tt>.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference uiServiceRef
            = bc.getServiceReference(UIService.class.getName());

        uiService = (UIService) bc.getService(uiServiceRef);

        ConfigurationWindow configWindow = uiService.getConfigurationWindow();

        if(configWindow != null)
        {
            PluginManagerConfigForm pluginManager
                        = new PluginManagerConfigForm();

            configWindow.addConfigurationForm(pluginManager);
        }
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {   
    }
    
    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        return uiService;
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
}
