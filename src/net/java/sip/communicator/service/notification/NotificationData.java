/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.*;

/**
 * Object to cache fired notifications before all handler implementations are
 * ready registered.
 * 
 * @author Ingo Bauersachs
 */
public class NotificationData
{
    private final String eventType;
    private final String title;
    private final String message;
    private final Map<String,String> extra;
    private final byte[] icon;
    private final Object tag;

    /**
     * Creates a new instance of this class.
     * 
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     *            systray or log notification.)
     * @param extra additional data (such as caller information)
     * @param icon the icon to show in the notification if and where appropriate
     * @param tag additional info to be used by the notification handler
     */
    NotificationData(String eventType, String title, String message,
        Map<String,String> extra, byte[] icon, Object tag)
    {
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.extra = extra;
        this.icon = icon;
        this.tag = tag;
    }

    /**
     * Gets the type of the event that we'd like to fire a notification for
     * 
     * @return the eventType
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Gets the title of the given message.
     * 
     * @return the title
     */
    String getTitle()
    {
        return title;
    }

    /**
     * Gets the message to use if and where appropriate (e.g. with systray or
     * log notification).
     * 
     * @return the message
     */
    String getMessage()
    {
        return message;
    }

    /**
     * Gets additional data (such as caller information).
     * 
     * @return the extra data
     */
    public Map<String,String> getExtra()
    {
        return extra;
    }

    /**
     * Gets the icon to show in the notification if and where appropriate.
     * 
     * @return the icon
     */
    byte[] getIcon()
    {
        return icon;
    }

    /**
     * Gets additional info to be used by the notification handler.
     * 
     * @return the tag
     */
    Object getTag()
    {
        return tag;
    }
}
