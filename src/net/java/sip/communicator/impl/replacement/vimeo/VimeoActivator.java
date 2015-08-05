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
package net.java.sip.communicator.impl.replacement.vimeo;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Vimeo source bundle.
 *
 * @author Purvesh Sahoo
 */
public class VimeoActivator
    implements BundleActivator
{
    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(VimeoActivator.class);

    /**
     * The vimeo service registration.
     */
    private ServiceRegistration vimeoServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService vimeoSource = null;

    /**
     * Starts the Vimeo replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceVimeoImpl.VIMEO_CONFIG_LABEL);
        vimeoSource = new ReplacementServiceVimeoImpl();

        vimeoServReg =
            context.registerService(ReplacementService.class.getName(),
                vimeoSource, hashtable);

        logger.info("Vimeo source implementation [STARTED].");
    }

    /**
     * Unregisters the Vimeo replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        vimeoServReg.unregister();
        logger.info("Vimeo source implementation [STOPPED].");
    }
}
