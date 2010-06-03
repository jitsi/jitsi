/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.metahistory;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.metahistory.*;

/**
 * Activates the MetaHistoryService
 *
 * @author Damian Minkov
 */
public class MetaHistoryActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(MetaHistoryActivator.class);

    private MetaHistoryServiceImpl metaHistoryService = null;

    /**
     * Initialize and start meta history
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try{

            logger.logEntry();

            //Create and start the meta history service.
            metaHistoryService =
                new MetaHistoryServiceImpl();

            metaHistoryService.start(bundleContext);

            bundleContext.registerService(
                MetaHistoryService.class.getName(), metaHistoryService, null);

            if (logger.isInfoEnabled())
                logger.info("Meta History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if(metaHistoryService != null)
            metaHistoryService.stop(bundleContext);
    }
}
