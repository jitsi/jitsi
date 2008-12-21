/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;

public class NotificationManager
{
    public static final String INCOMING_MESSAGE = "IncomingMessage";
    
    public static final String INCOMING_CALL = "IncomingCall";
    
    public static final String OUTGOING_CALL = "OutgoingCall";
    
    public static final String BUSY_CALL = "BusyCall";
    
    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";

    public static void registerGuiNotifications()
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();
        
        if(notificationService == null)
            return;
        
        // Register incoming message notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_SOUND,
                SoundProperties.INCOMING_MESSAGE,
                null);
        
        // Register incoming call notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
    
        SoundNotificationHandler inCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(SoundProperties.INCOMING_CALL, 2000);
        
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_SOUND,
                inCallSoundHandler);

        // Register outgoing call notifications.
        SoundNotificationHandler outCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(SoundProperties.OUTGOING_CALL, 3000);
        
        notificationService.registerDefaultNotificationForEvent(
                OUTGOING_CALL,
                NotificationService.ACTION_SOUND,
                outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationHandler busyCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(SoundProperties.BUSY, 1);
        
        notificationService.registerDefaultNotificationForEvent(
                BUSY_CALL,
                NotificationService.ACTION_SOUND,
                busyCallSoundHandler);

        // Register proactive notifications.
        notificationService.registerDefaultNotificationForEvent(
                PROACTIVE_NOTIFICATION,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
    }
    
    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     * 
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     */
    public static void fireNotification(String eventType,
                                        String messageTitle,
                                        String message)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();
        
        if(notificationService == null)
            return;

        notificationService.fireNotification(eventType, messageTitle, message);
    }

    /**
     * Fires a chat message notification for the given event type through the
     * <tt>NotificationService</tt>.
     * 
     * @param contact the chat contact to which the chat message corresponds;
     * the chat contact could be a Contact or a ChatRoom.
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     */
    public static void fireChatNotification(Object contact,
                                            String eventType,
                                            String messageTitle,
                                            String message)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();

        if(notificationService == null)
            return;

        NotificationActionHandler popupActionHandler = null;

        Chat chatPanel = null;

        if (contact instanceof Contact)
            chatPanel = GuiActivator.getUIService().getChat((Contact) contact);
        else if (contact instanceof ChatRoom)
        {
            // For system rooms we don't want to send notification events.
            if ((contact instanceof ChatRoom)
                && ((ChatRoom) contact).isSystem())
                return;

            chatPanel = GuiActivator.getUIService().getChat((ChatRoom) contact);
        }

        if(eventType.equals(INCOMING_MESSAGE)
            && chatPanel.isChatFocused())
        {
            popupActionHandler = notificationService
                .getEventNotificationActionHandler(
                                    eventType,
                                    NotificationService.ACTION_POPUP_MESSAGE);

            popupActionHandler.setEnabled(false);
        }

        notificationService.fireNotification(eventType, messageTitle, message);

        if(popupActionHandler != null)
            popupActionHandler.setEnabled(true);
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in this class.
     * 
     * @param eventType the event type for which we want to fire a notification
     */
    public static void fireNotification(String eventType)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();
        
        if(notificationService == null)
            return;
        
        notificationService.fireNotification(eventType);
    }

    /**
     * Stops all sounds for the given event type.
     * 
     * @param eventType the event type for which we should stop sounds. One of
     * the static event types defined in this class.
     */
    public static void stopSound(String eventType)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();

        if(notificationService == null)
            return;

        SoundNotificationHandler soundHandler
            = (SoundNotificationHandler) notificationService
                .getEventNotificationActionHandler(
                    eventType, NotificationService.ACTION_SOUND);

        soundHandler.stop();
    }
}
