/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activates the MessageHistoryService
 *
 * @author Damian Minkov
 */
public class MessageHistoryActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(MessageHistoryActivator.class);

    private MessageHistoryServiceImpl msgHistoryService = null;

    static BundleContext bundleContext;

    /**
     * Initialize and start message history
     *
     * @param bc the BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        try
        {
            logger.logEntry();

            ServiceReference refHistory = bundleContext.getServiceReference(
                HistoryService.class.getName());

            HistoryService historyService = (HistoryService)
                bundleContext.getService(refHistory);

            //Create and start the message history service.
            msgHistoryService =
                new MessageHistoryServiceImpl();

            msgHistoryService.setHistoryService(historyService);

            msgHistoryService.start(bundleContext);

            bundleContext.registerService(
                MessageHistoryService.class.getName(), msgHistoryService, null);

            if (logger.isInfoEnabled())
                logger.info("Message History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if(msgHistoryService != null)
            msgHistoryService.stop(bundleContext);
    }
}
