/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.notification.*;

public class NotificationManager
{
    public static final String INCOMING_MESSAGE = "IncomingMessage";
    
    public static final String INCOMING_CALL = "IncomingCall";
    
    public static final String OUTGOING_CALL = "OutgoingCall";
    
    public static final String BUSY_CALL = "BusyCall";
    
    private static Hashtable soundHandlers = new Hashtable();
    
    public static void registerGuiNotifications()
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();
        
        if(notificationService == null)
            return;
        
        // Register incoming message notifications.
        notificationService.registerNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_SOUND,
                Sounds.INCOMING_MESSAGE,
                null);
        
        // Register incoming call notifications.
        notificationService.registerNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
    
        SoundNotificationHandler inCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(Sounds.INCOMING_CALL, 2000);
        
        notificationService.registerNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_SOUND,
                inCallSoundHandler);

        soundHandlers.put(INCOMING_CALL, inCallSoundHandler);
        
        // Register outgoing call notifications.
        SoundNotificationHandler outCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(Sounds.OUTGOING_CALL, 3000);
        
        notificationService.registerNotificationForEvent(
                OUTGOING_CALL,
                NotificationService.ACTION_SOUND,
                outCallSoundHandler);
    
        soundHandlers.put(OUTGOING_CALL, outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationHandler busyCallSoundHandler
            = (SoundNotificationHandler) notificationService
                .createSoundNotificationHandler(Sounds.BUSY, 0);
        
        notificationService.registerNotificationForEvent(
                BUSY_CALL,
                NotificationService.ACTION_SOUND,
                busyCallSoundHandler);
    
        soundHandlers.put(OUTGOING_CALL, outCallSoundHandler);

    }
    
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
    
    public static void fireNotification(String eventType)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();
        
        if(notificationService == null)
            return;
        
        notificationService.fireNotification(eventType);
    }
    
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
