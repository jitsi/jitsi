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
package net.java.sip.communicator.impl.replacement.twitpic;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Twitpic source bundle.
 *
 * @author Purvesh Sahoo
 */
public class TwitpicActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>TwitpicActivator</tt> class.
     */
    private static final Logger logger =
        Logger.getLogger(TwitpicActivator.class);

    /**
     * The twitpic service registration.
     */
    private ServiceRegistration twitpicServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService twitpicSource = null;

    /**
     * Starts the Twitpic replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceTwitpicImpl.TWITPIC_CONFIG_LABEL);
        twitpicSource = new ReplacementServiceTwitpicImpl();

        twitpicServReg =
            context.registerService(ReplacementService.class.getName(),
                twitpicSource, hashtable);

        logger.info("Twitpic source implementation [STARTED].");
    }

    /**
     * Unregisters the Twitpic replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        twitpicServReg.unregister();
        logger.info("Twitpic source implementation [STOPPED].");
    }
}
