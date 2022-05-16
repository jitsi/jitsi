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
package net.java.sip.communicator.plugin.singlecallpolicy;

import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

public class SingleCallPolicyActivator
    extends DependentActivator
{
    /**
     * The <code>BundleContext</code> of the one and only
     * <code>ProtocolProviderActivator</code> instance which is currently
     * started.
     */
    private static BundleContext bundleContext;

    /**
     * The <code>SingleCallInProgressPolicy</code> making sure that the
     * <code>Call</code>s accessible in the <code>BundleContext</code> of this
     * activator will obey to the rule that a new <code>Call</code> should put
     * the other existing
     * <code>Call</code>s on hold.
     */
    private SingleCallInProgressPolicy singleCallInProgressPolicy;

    /**
     * The calendar service instance.
     */
    private static CalendarService calendarService;

    /**
     * The <code>ConfigurationService</code> used by the classes in the bundle
     * represented by <code>ProtocolProviderActivator</code>.
     */
    private static ConfigurationService configurationService;

    public SingleCallPolicyActivator()
    {
        super(ConfigurationService.class);
    }

    @Override
    public void startWithServices(BundleContext context)
    {
        bundleContext = context;
        singleCallInProgressPolicy = new SingleCallInProgressPolicy(context);
    }

    @Override
    public void stop(BundleContext context)
    {
        if (singleCallInProgressPolicy != null)
        {
            singleCallInProgressPolicy.dispose();
            singleCallInProgressPolicy = null;
        }
    }

    /**
     * Gets the <code>ConfigurationService</code> to be used by the classes in
     * the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>ConfigurationService</code> to be used by the classes
     * in the bundle represented by
     * <code>ProtocolProviderActivator</code>
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = (ConfigurationService)
                bundleContext.getService(
                    bundleContext.getServiceReference(
                        ConfigurationService.class.getName()));
        }
        return configurationService;
    }

    /**
     * Gets the <code>CalendarService</code> to be used by the classes in the
     * bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>CalendarService</code> to be used by the classes in the
     * bundle represented by
     * <code>ProtocolProviderActivator</code>
     */
    public static CalendarService getCalendarService()
    {
        if (calendarService == null)
        {
            ServiceReference<CalendarService> serviceReference
                = bundleContext.getServiceReference(
                CalendarService.class);
            if (serviceReference == null)
            {
                return null;
            }
            calendarService
                = bundleContext.getService(serviceReference);
        }
        return calendarService;
    }
}
