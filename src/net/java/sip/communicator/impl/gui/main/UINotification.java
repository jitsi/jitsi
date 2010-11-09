/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.util.*;

/**
 * The <tt>UINotification</tt> class represents a notification received in the
 * user interface. This could be a missed call, voicemail, email notification or
 * any other notification.
 *
 * @author Yana Stamcheva
 */
public class UINotification
{
    /**
     * The parent notification group.
     */
    private final UINotificationGroup parentGroup;

    /**
     * The name associated with this notification.
     */
    private final String notificationName;

    /**
     * The time when the notification was received.
     */
    private final Date notificationTime;

    /**
     * Creates an instance of <tt>UINotification</tt> by specifying the
     * notification name and time.
     *
     * notification belongs
     * @param displayName the name associated to this notification
     * @param time the time when the notification was received
     * @param parentGroup the group of notifications, to which this notification
     * belongs
     */
    public UINotification(  String displayName,
                            Date time,
                            UINotificationGroup parentGroup)
    {
        this.notificationName = displayName;
        this.notificationTime = time;
        this.parentGroup = parentGroup;
    }

    /**
     * Returns the name associated with this notification.
     *
     * @return the name associated with this notification
     */
    public String getDisplayName()
    {
        return notificationName;
    }

    /**
     * Returns the time the notification was received.
     *
     * @return the time the notification was received
     */
    public Date getTime()
    {
        return notificationTime;
    }

    /**
     * Returns the parent notification group.
     *
     * @return the parent notification group
     */
    public UINotificationGroup getGroup()
    {
        return parentGroup;
    }
}
