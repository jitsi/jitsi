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
package net.java.sip.communicator.impl.replacement.directimage;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.directimage.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Activator for the direct image links source bundle.
 * @author Purvesh Sahoo
 */
public class DirectImageActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>DirectImageActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(DirectImageActivator.class);

    /**
     * The direct image source service registration.
     */
    private ServiceRegistration directImageSourceServReg = null;

    /**
     * The source implementation reference.
     */
    private static ReplacementService directImageSource = null;

    /**
     * The service used for accessing configuration properties.
     */
    private static ConfigurationService confService = null;

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Starts the Direct image links replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     *            framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ReplacementService.SOURCE_NAME,
            ReplacementServiceDirectImageImpl.DIRECT_IMAGE_CONFIG_LABEL);
        directImageSource = new ReplacementServiceDirectImageImpl();

        directImageSourceServReg =
            context.registerService(
                DirectImageReplacementService.class.getName(),
                directImageSource, hashtable);

        directImageSourceServReg =
            context.registerService(
                ReplacementService.class.getName(),
                directImageSource, hashtable);

        logger.info("Direct Image Link source implementation [STARTED].");
    }

    /**
     * Unregisters the Direct image links replacement service.
     *
     * @param context BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext context) throws Exception
    {
        directImageSourceServReg.unregister();
        confService = null;
        bundleContext = null;
        logger.info("Direct Image Link source implementation [STOPPED].");
    }

    /**
    * Returns a reference to a ConfigurationService implementation currently
    * registered in the bundle context or null if no such implementation was
    * found.
    *
    * @return a currently valid implementation of the ConfigurationService.
    */
    public static ConfigurationService getConfigService()
    {
        if(confService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            confService
                = (ConfigurationService) bundleContext.getService(
                    confReference);
        }
        return confService;
    }
}
