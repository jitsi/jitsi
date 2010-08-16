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
    
    public static final String DIALING = "Dialing";
    
    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";
    
    public static final String SECURITY_MESSAGE = "SecurityMessage";
    
    public static final String CALL_SECURITY_ON = "CallSecurityOn";
    
    public static final String CALL_SECURITY_ERROR = "CallSecurityError";
    
    public static final String INCOMING_FILE = "IncomingFile";

    public static final String CALL_SAVED = "CallSaved";

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
            = notificationService
                .createSoundNotificationHandler(SoundProperties.INCOMING_CALL,
                                                2000);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_SOUND,
                inCallSoundHandler);

        // Register outgoing call notifications.
        SoundNotificationHandler outCallSoundHandler
            = notificationService
                .createSoundNotificationHandler(SoundProperties.OUTGOING_CALL,
                                                3000);

        notificationService.registerDefaultNotificationForEvent(
                OUTGOING_CALL,
                NotificationService.ACTION_SOUND,
                outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationHandler busyCallSoundHandler
            = notificationService
                .createSoundNotificationHandler(SoundProperties.BUSY, 1);

        notificationService.registerDefaultNotificationForEvent(
                BUSY_CALL,
                NotificationService.ACTION_SOUND,
                busyCallSoundHandler);

        // Register dial notifications.
        SoundNotificationHandler dialSoundHandler
            = notificationService
                .createSoundNotificationHandler(SoundProperties.DIALING, 0);

        notificationService.registerDefaultNotificationForEvent(
                DIALING,
                NotificationService.ACTION_SOUND,
                dialSoundHandler);

        // Register proactive notifications.
        notificationService.registerDefaultNotificationForEvent(
                PROACTIVE_NOTIFICATION,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register warning message notifications.
        notificationService.registerDefaultNotificationForEvent(
                SECURITY_MESSAGE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register sound notification for security state on during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ON,
                NotificationService.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ON,
                null);

        // Register sound notification for security state off during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ERROR,
                NotificationService.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ERROR,
                null);

        // Register sound notification for incoming files.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationService.ACTION_SOUND,
                SoundProperties.INCOMING_FILE,
                null);

        // Register notification for saved calls.
        notificationService.registerDefaultNotificationForEvent(
            CALL_SAVED,
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

        notificationService.fireNotification(   eventType,
                                                messageTitle,
                                                message,
                                                null,
                                                null);
    }

    /**
     * Fires a chat message notification for the given event type through the
     * <tt>NotificationService</tt>.
     * 
     * @param chatContact the chat contact to which the chat message corresponds;
     * the chat contact could be a Contact or a ChatRoom.
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     */
    public static void fireChatNotification(Object chatContact,
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
        byte[] contactIcon = null;
        if (chatContact instanceof Contact)
        {
            Contact contact = (Contact) chatContact;

            chatPanel = GuiActivator.getUIService().getChat(contact);

            contactIcon = contact.getImage();
        }
        else if (chatContact instanceof ChatRoom)
        {
            ChatRoom chatRoom = (ChatRoom) chatContact;

            // For system rooms we don't want to send notification events.
            if (chatRoom.isSystem())
                return;

            chatPanel = GuiActivator.getUIService().getChat(chatRoom);
        }

        if (chatPanel != null) 
        {
            if (eventType.equals(INCOMING_MESSAGE) && chatPanel.isChatFocused()) 
            {
                popupActionHandler = notificationService
                        .getEventNotificationActionHandler(eventType,
                                NotificationService.ACTION_POPUP_MESSAGE);

                popupActionHandler.setEnabled(false);
            }
        }

        notificationService.fireNotification(   eventType,
                                                messageTitle,
                                                message,
                                                contactIcon,
                                                chatContact);

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
