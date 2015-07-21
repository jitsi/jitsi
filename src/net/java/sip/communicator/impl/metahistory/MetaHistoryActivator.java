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
