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
package net.java.sip.communicator.impl.replacement.dailymotion;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Dailymotion source bundle.
 * @author Purvesh Sahoo
 */
public class DailymotionActivator
    implements BundleActivator
{
    /**
     * The currently valid bundle context.
     */
    private static final Logger logger =
        Logger.getLogger(DailymotionActivator.class);

    /**
     * The daily motion source service registration.
     */
    private ServiceRegistration dailymotionSourceServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService dailymotionSource = null;

    /**
     * Starts the Dailymotion replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceDailymotionImpl.DAILYMOTION_CONFIG_LABEL);
        dailymotionSource = new ReplacementServiceDailymotionImpl();

        dailymotionSourceServReg =
            context.registerService(ReplacementService.class.getName(),
                dailymotionSource, hashtable);
        logger.info("Dailymotion source implementation [STARTED].");
    }

    /**
     * Unregisters the Dailymotion replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        dailymotionSourceServReg.unregister();
        logger.info("Dailymotion source implementation [STOPPED].");
    }
}
