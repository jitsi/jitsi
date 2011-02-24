package net.java.sip.communicator.plugin.advancedconfig;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

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
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);
        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new AdvancedConfigurationPanel(),
                properties);

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
}
