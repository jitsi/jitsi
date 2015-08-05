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
package net.java.sip.communicator.impl.replacement.smiley;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activator for the Smiley source bundle.
 * @author Purvesh Sahoo
 */
public class SmileyActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>SmileyActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(SmileyActivator.class);

    /**
     * The currently valid bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * The resources service
     */
    private static ResourceManagementService resourcesService;

    /**
     * The smileyy service registration.
     */
    private ServiceRegistration smileyServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService smileySource = null;

    /**
     * Starts the Smiley replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     * framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceSmileyImpl.SMILEY_SOURCE);
        smileySource = new ReplacementServiceSmileyImpl();

        smileyServReg
            = context.registerService(SmiliesReplacementService.class.getName(),
                smileySource, hashtable);

        smileyServReg
            = context.registerService(ReplacementService.class.getName(),
                smileySource, hashtable);

        logger.info("Smiley source implementation [STARTED].");
    }

    /**
     * Unregisters the Smiley replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        smileyServReg.unregister();
        logger.info("Smiley source implementation [STOPPED].");
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     *         access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference =
                bundleContext
                    .getServiceReference(ResourceManagementService.class
                        .getName());

            if (serviceReference == null)
                return null;

            resourcesService =
                (ResourceManagementService) bundleContext
                    .getService(serviceReference);
        }
        return resourcesService;
    }
}
