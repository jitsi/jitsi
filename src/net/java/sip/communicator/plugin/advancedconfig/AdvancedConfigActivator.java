package net.java.sip.communicator.plugin.advancedconfig;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class AdvancedConfigActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AdvancedConfigActivator.class);

    /**
     * The bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>SecurityConfigActivator</tt> instance to read and
     * write configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * Indicates if the advanced configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.advancedconfig.DISABLED";

    /**
     * The advanced configuration panel registered by this bundle.
     */
    private static AdvancedConfigurationPanel panel;

    /**
     * The OSGi service registration of the panel.
     */
    private static ServiceRegistration panelRegistration;

    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        // If the notification configuration form is disabled don't continue.
        if (getConfigurationService().getBoolean(DISABLED_PROP, false))
            return;

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);
        panel = new AdvancedConfigurationPanel();
        panelRegistration = bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                panel,
                properties);


        bundleContext.addServiceListener(panel);

        if (logger.isInfoEnabled())
            logger.info("ADVANCED CONFIG PLUGIN... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc) throws Exception
    {
        if(panel != null)
            bc.removeServiceListener(panel);

        if(panelRegistration != null)
            panelRegistration.unregister();
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> implementation.
     * @return the <tt>ResourceManagementService</tt> implementation
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }

    /**
     * Returns a reference to the ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }
}
