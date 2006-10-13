/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import org.osgi.framework.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.callhistory.*;

/**
 * Activates the CallHistoryService
 *
 * @author Damian Minkov
 */
public class CallHistoryActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(CallHistoryActivator.class);

    private CallHistoryServiceImpl callHistoryService = null;

    /**
     * Initialize and start call history
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try{

            logger.logEntry();

            ServiceReference refHistory = bundleContext.getServiceReference(
                HistoryService.class.getName());

            HistoryService historyService = (HistoryService)
                bundleContext.getService(refHistory);

            //Create and start the call history service.
            callHistoryService =
                new CallHistoryServiceImpl();
            // set the configuration and history service
            callHistoryService.setHistoryService(historyService);

            callHistoryService.start(bundleContext);

            bundleContext.registerService(
                CallHistoryService.class.getName(), callHistoryService, null);

            logger.info("Call History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    public void stop(BundleContext bundleContext) throws Exception
    {

    }
}
