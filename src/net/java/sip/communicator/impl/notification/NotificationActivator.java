/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
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

    private CommandNotificationHandler commandHandler;
    private LogMessageNotificationHandler logMessageHandler;
    private PopupMessageNotificationHandler popupMessageHandler;
    private SoundNotificationHandler soundHandler;

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
            ServiceReference serviceReference = bundleContext
                .getServiceReference(SystrayService.class.getName());

            systrayService = (SystrayService) bundleContext
                .getService(serviceReference);
        }

        return systrayService;
    }
}
