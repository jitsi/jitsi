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
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 *
 * @author Yana Stamcheva
 */
public class NotificationActivator
    implements BundleActivator
{
    private final Logger logger =
        Logger.getLogger(NotificationActivator.class);

    protected static BundleContext bundleContext;

    private static AudioNotifierService audioNotifierService;
    private static SystrayService systrayService;
    private static NotificationService notificationService;

    /**
     * A reference to the <tt>UIService</tt> currently in use in Jitsi.
     */
    private static UIService uiService = null;

    private CommandNotificationHandler commandHandler;
    private LogMessageNotificationHandler logMessageHandler;
    private PopupMessageNotificationHandler popupMessageHandler;
    private SoundNotificationHandler soundHandler;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>NotificationActivator</tt> instance to read and write
     * configuration properties.
     */
    private static ConfigurationService configurationService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        try
        {
            logger.logEntry();
            logger.info("Notification handler Service...[  STARTED ]");


            // Get the notification service implementation
            ServiceReference notifReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(notifReference);

            commandHandler = new CommandNotificationHandlerImpl();
            logMessageHandler = new LogMessageNotificationHandlerImpl();
            popupMessageHandler = new PopupMessageNotificationHandlerImpl();
            soundHandler = new SoundNotificationHandlerImpl();

            notificationService.addActionHandler(commandHandler);
            notificationService.addActionHandler(logMessageHandler);
            notificationService.addActionHandler(popupMessageHandler);
            notificationService.addActionHandler(soundHandler);

            logger.info("Notification handler Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
        notificationService.removeActionHandler(
            commandHandler.getActionType());
        notificationService.removeActionHandler(
            logMessageHandler.getActionType());
        notificationService.removeActionHandler(
            popupMessageHandler.getActionType());
        notificationService.removeActionHandler(
            soundHandler.getActionType());

        logger.info("Notification handler Service ...[STOPPED]");
    }

    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            ServiceReference serviceReference
                = bundleContext
                    .getServiceReference(AudioNotifierService.class.getName());

            if (serviceReference != null)
                audioNotifierService
                    = (AudioNotifierService)
                        bundleContext.getService(serviceReference);
        }

        return audioNotifierService;
    }

    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle context.
     *
     * @return the <tt>SystrayService</tt> obtained from the bundle context
     */
    public static SystrayService getSystray()
    {
        if (systrayService == null)
        {
            systrayService = ServiceUtils.getService( bundleContext,
                                                      SystrayService.class );
        }

        return systrayService;
    }

    /**
     * Returns a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
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
