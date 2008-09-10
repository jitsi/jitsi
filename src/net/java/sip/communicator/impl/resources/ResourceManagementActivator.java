/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import net.java.sip.communicator.service.resources.*;

import net.java.sip.communicator.util.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author damencho
 */
public class ResourceManagementActivator
    implements BundleActivator
{

    private Logger logger =
        Logger.getLogger(ResourceManagementActivator.class);
    static BundleContext bundleContext;

    private ResourceManagementServiceImpl resPackImpl = null;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        resPackImpl =
            new ResourceManagementServiceImpl();

        bundleContext.registerService(  ResourceManagementService.class.getName(),
                                        resPackImpl,
                                        null);

        logger.info("Resource manager ... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {
        bc.removeServiceListener(resPackImpl);
    }
}
