/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.filehistory;

import org.osgi.framework.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Damian Minkov
 */
public class FileHistoryActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(FileHistoryActivator.class);

    private FileHistoryServiceImpl fileHistoryService = null;

    /**
     * Initialize and start file history
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

            //Create and start the file history service.
            fileHistoryService =
                new FileHistoryServiceImpl();
            // set the history service
            fileHistoryService.setHistoryService(historyService);

            fileHistoryService.start(bundleContext);

            bundleContext.registerService(
                FileHistoryService.class.getName(), fileHistoryService, null);

            if (logger.isInfoEnabled())
                logger.info("File History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if(fileHistoryService != null)
            fileHistoryService.stop(bundleContext);
    }
}
