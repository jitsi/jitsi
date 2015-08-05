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
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the AudioConfiguration plugin.
 *
 * @author Alexandre Maillard
 */
public class NotificationConfigurationActivator
    implements BundleActivator
{
    private final Logger logger
        = Logger.getLogger(NotificationConfigurationActivator.class);

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static AudioNotifierService audioService;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>SecurityConfigActivator</tt> instance to read and
     * write configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * Indicates if the notification configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.notificationconfiguration.DISABLED";

    /**
     * Starts this bundle and adds the <tt>AudioConfigurationConfigForm</tt>
     * contained in it to the configuration window obtained from the
     * <tt>UIService</tt>.
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        // If the notification configuration form is disabled don't continue.
        if (getConfigurationService().getBoolean(DISABLED_PROP, false))
            return;

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);
        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.notificationconfiguration.NotificationConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.notificationconfig.PLUGIN_ICON",
                    "service.gui.EVENTS",
                    30),
                properties);

        if (logger.isTraceEnabled())
            logger.trace("Notification Configuration: [ STARTED ]");
    }

    /**
     * Stops this bundle.
     */
    public void stop(BundleContext bc)
        throws Exception
    {
    }

    /**
     * Returns the <tt>AudioService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifierService()
    {
        if(audioService == null)
        {
            audioService
                = ServiceUtils.getService(
                        bundleContext,
                        AudioNotifierService.class);
        }
        return audioService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle
     * context.
     * <p>
     * <b>Note</b>: No caching of the returned value is made available. Clients
     * interested in bringing down the penalties imposed by acquiring the value
     * in question should provide it by themselves.
     * </p>
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        return
            ServiceUtils.getService(bundleContext, NotificationService.class);
    }

    /**
     * Returns a reference to the ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }
}
