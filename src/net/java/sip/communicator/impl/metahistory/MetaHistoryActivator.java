/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.metahistory;

import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activates the MetaHistoryService
 *
 * @author Damian Minkov
 */
public class MetaHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>MetaHistoryActivator</tt> class and its instances for logging output.
     */
    private static Logger logger =
        Logger.getLogger(MetaHistoryActivator.class);

    /**
     * The <tt>MetaHistoryService</tt> reference.
     */
    private MetaHistoryServiceImpl metaHistoryService = null;

    /**
     * Initialize and start meta history
     *
     * @param bundleContext BundleContext
     * @throws Exception if initializing and starting meta history service fails
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

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if(metaHistoryService != null)
            metaHistoryService.stop(bundleContext);
    }
}
