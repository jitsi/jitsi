package net.java.sip.communicator.plugin.chatconfig;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ChatConfigActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(ChatConfigActivator.class);

    /**
     * The bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.generalconfig.GeneralConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.generalconfig.PLUGIN_ICON",
                    "service.gui.GENERAL",
                    30),
                null);

        if (logger.isInfoEnabled())
            logger.info("PREFERENCES PLUGIN... [REGISTERED]");
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
