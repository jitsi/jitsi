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
package net.java.sip.communicator.impl.replacement.viddler;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the Viddler source bundle.
 * @author Purvesh Sahoo
 */
public class ViddlerActivator
    implements BundleActivator
{

    /**
     * The <tt>Logger</tt> used by the <tt>ViddlerActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(ViddlerActivator.class);

    /**
     * The Viddler source service registration.
     */
    private ServiceRegistration viddlerServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService viddlerSource = null;

    /**
     * Starts this bundle.
     *
     * @param context bundle context.
     * @throws Exception
     */
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceViddlerImpl.VIDDLER_CONFIG_LABEL);
        viddlerSource = new ReplacementServiceViddlerImpl();

        viddlerServReg =
            context.registerService(ReplacementService.class.getName(),
                viddlerSource, hashtable);

        logger.info("Viddler source implementation [STARTED].");
    }

    /**
     * Stops bundle.
     *
     * @param bc context.
     * @throws Exception
     */
    public void stop(BundleContext bc) throws Exception
    {
        viddlerServReg.unregister();
        logger.info("Viddler source implementation [STOPPED].");
    }
}
