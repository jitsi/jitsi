/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.filehistory;

import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 *
 * @author Damian Minkov
 */
public class FileHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>FileHistoryActivator</tt> class and its instances for logging output.
     */
    private static Logger logger
        = Logger.getLogger(FileHistoryActivator.class);

    /**
     * A <tt>FileHistoryService</tt> service reference.
     */
    private FileHistoryServiceImpl fileHistoryService = null;

    /**
     * Initialize and start file history
     *
     * @param bundleContext BundleContext
     * @throws Exception if initializing and starting file history fails
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try{

            logger.logEntry();

            HistoryService historyService
                = ServiceUtils.getService(bundleContext, HistoryService.class);

            //Create and start the file history service.
            fileHistoryService = new FileHistoryServiceImpl();
            // set the history service
            fileHistoryService.setHistoryService(historyService);

            fileHistoryService.start(bundleContext);

            bundleContext.registerService(
                    FileHistoryService.class.getName(),
                    fileHistoryService,
                    null);

            if (logger.isInfoEnabled())
                logger.info("File History Service ...[REGISTERED]");
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
        if(fileHistoryService != null)
            fileHistoryService.stop(bundleContext);
    }
}
