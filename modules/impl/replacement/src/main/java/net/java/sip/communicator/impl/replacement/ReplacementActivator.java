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
package net.java.sip.communicator.impl.replacement;

import java.util.*;
import net.java.sip.communicator.impl.replacement.providers.*;
import net.java.sip.communicator.impl.replacement.smiley.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.directimage.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activator for the replacement bundle.
 */
public class ReplacementActivator
    extends DependentActivator
{
    private static ResourceManagementService resourcesService;

    private final List<ServiceRegistration<? extends ReplacementService>>
        registrations = new ArrayList<>();

    public ReplacementActivator()
    {
        super(
            ConfigurationService.class,
            ResourceManagementService.class
        );
    }

    @Override
    public void startWithServices(BundleContext context)
    {
        resourcesService = getService(ResourceManagementService.class);
        ConfigurationService configService =
            getService(ConfigurationService.class);
        registerService(context, new Bliptv());
        registerService(context, new Dailymotion());
        registerService(context, new DirectImage(configService));
        registerService(context, new Hulu());
        registerService(context, new Metacafe());
        registerService(context, new Twitpic());
        registerService(context, new Vbox7());
        registerService(context, new Vimeo());
        registerService(context, new Youtube());
        registerService(context, new ReplacementServiceSmileyImpl());
    }

    private void registerService(BundleContext bundleContext,
        ReplacementService service)
    {
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put(ReplacementService.SOURCE_NAME, service.getDisplayName());
        registrations.add(
            bundleContext.registerService(
                ReplacementService.class, service, hashtable));

        if (service instanceof DirectImageReplacementService)
        {
            registrations.add(
                bundleContext.registerService(
                    DirectImageReplacementService.class,
                    (DirectImageReplacementService) service, hashtable));
        }

        if (service instanceof SmiliesReplacementService)
        {
            registrations.add(
                bundleContext.registerService(
                    SmiliesReplacementService.class,
                    (SmiliesReplacementService) service, hashtable));
        }
    }

    /**
     * Unregisters the replacement services.
     *
     * @param context the bundleContext provided from the OSGi framework
     */
    public void stop(BundleContext context) throws Exception
    {
        super.stop(context);
        for (ServiceRegistration<?> registration : registrations)
        {
            registration.unregister();
        }
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        return resourcesService;
    }
}
