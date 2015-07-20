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
package net.java.sip.communicator.impl.replacement.bliptv;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Blip.tv source bundle.
 *
 * @author Purvesh Sahoo
 */
public class BliptvActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>BliptvActivator</tt> class.
     */
    private static final Logger logger =
        Logger.getLogger(BliptvActivator.class);

    /**
     * The blip tv service registration.
     */
    private ServiceRegistration bliptvServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService bliptvSource = null;

    /**
     * Starts the Blip.tv replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceBliptvImpl.BLIPTV_CONFIG_LABEL);
        bliptvSource = new ReplacementServiceBliptvImpl();

        bliptvServReg =
            context.registerService(ReplacementService.class.getName(),
                bliptvSource, hashtable);

        logger.info("Blip.TV source implementation [STARTED].");
    }

    /**
     * Unregisters the Blip.tv replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        bliptvServReg.unregister();
        logger.info("Blip.TV source implementation [STOPPED].");
    }
}
