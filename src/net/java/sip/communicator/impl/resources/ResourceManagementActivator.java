/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.resources.*;

import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * Starts Resource Management Service.
 * @author Damian Minkov
 */
public class ResourceManagementActivator
    implements BundleActivator
{
    private Logger logger =
        Logger.getLogger(ResourceManagementActivator.class);

    static BundleContext bundleContext;

    private ResourceManagementServiceImpl resPackImpl = null;

    private static ConfigurationService configService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        resPackImpl =
            new ResourceManagementServiceImpl();

        bundleContext.registerService(  ResourceManagementService.class.getName(),
                                        resPackImpl,
                                        null);

        if (logger.isInfoEnabled())
            logger.info("Resource manager ... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {
        bc.removeServiceListener(resPackImpl);
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }
}
