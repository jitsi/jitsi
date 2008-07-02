/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class GeneralConfigPluginActivator implements BundleActivator
{
    private Logger logger = Logger.getLogger(GeneralConfigPluginActivator.class);

    private static ConfigurationService configService;

    protected static BundleContext bundleContext;
    
    private static UIService uiService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        
        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        ConfigurationManager.loadGuiConfigurations();

        GeneralConfigurationForm preferencesForm
            = new GeneralConfigurationForm();

        bundleContext.registerService(  ConfigurationForm.class.getName(),
                                        preferencesForm,
                                        null);

        logger.info("PREFERENCES PLUGIN... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {
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
     * Returns the <tt>UIService</tt>.
     * 
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        return uiService;
    }
}
