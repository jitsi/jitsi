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
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>MessageHistoryActivator</tt> class and its instances for logging
     * output.
     */
    private static Logger logger =
        Logger.getLogger(MessageHistoryActivator.class);

    /**
     * The <tt>MessageHistoryService</tt> reference.
     */
    private MessageHistoryServiceImpl msgHistoryService = null;

    /**
     * The <tt>BundleContext</tt> of the service.
     */
    static BundleContext bundleContext;

    /**
     * Initialize and start message history
     *
     * @param bc the BundleContext
     * @throws Exception if initializing and starting message history service
     * fails
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

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if(msgHistoryService != null)
            msgHistoryService.stop(bundleContext);
    }
}
