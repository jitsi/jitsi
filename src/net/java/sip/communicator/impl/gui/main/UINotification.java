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
package net.java.sip.communicator.impl.gui.main;

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
     * The display name associated with this notification.
     */
    private final String notificationDisplayName;

    /**
     * The time in milliseconds when the notification was received.
     */
    private final long notificationTime;

    /**
     * Number of unread objects like calls or messages.
     */
    private int unreadObjects = 0;

    /**
     * Creates an instance of <tt>UINotification</tt> by specifying the
     * notification name and time.
     *
     * notification belongs
     * @param displayName the name associated to this notification
     * @param time the time in milliseconds when the notification was received
     * @param parentGroup the group of notifications, to which this notification
     * belongs
     */
    public UINotification(  String displayName,
                            long time,
                            UINotificationGroup parentGroup)
    {
        this(displayName, time, parentGroup, 1);
    }

    /**
     * Creates an instance of <tt>UINotification</tt> by specifying the
     * notification name and time.
     *
     * notification belongs
     * @param displayName the name associated to this notification
     * @param time the time in milliseconds when the notification was received
     * @param parentGroup the group of notifications, to which this notification
     * belongs
     * @param unreadObjects number of unread objects for this notification.
     */
    public UINotification(  String displayName,
                            long time,
                            UINotificationGroup parentGroup,
                            int unreadObjects)
    {
        this(displayName, displayName, time, parentGroup, 1);
    }

    /**
     * Creates an instance of <tt>UINotification</tt> by specifying the
     * notification name and time.
     *
     * notification belongs
     * @param name the notification name
     * @param displayName the name associated to this notification
     * @param time the time in milliseconds when the notification was received
     * @param parentGroup the group of notifications, to which this notification
     * belongs
     * @param unreadObjects number of unread objects for this notification.
     */
    public UINotification(  String name,
                            String displayName,
                            long time,
                            UINotificationGroup parentGroup,
                            int unreadObjects)
    {
        this.notificationName = name;
        this.notificationDisplayName = displayName;
        this.notificationTime = time;
        this.parentGroup = parentGroup;
        this.unreadObjects = unreadObjects;
    }

    /**
     * Returns the name associated with this notification.
     *
     * @return the name associated with this notification
     */
    public String getDisplayName()
    {
        return notificationDisplayName;
    }

    /**
     * Returns the time in milliseconds the notification was received.
     *
     * @return the time in milliseconds the notification was received
     */
    public long getTime()
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

    /**
     * Returns the number of unread objects for this notification.
     * @return
     */
    public int getUnreadObjects()
    {
        return unreadObjects;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;

        UINotification that = (UINotification) o;

        if(notificationName != null ?
                !notificationName.equals(that.notificationName)
                : that.notificationName != null)
            return false;
        if(parentGroup != null ?
                !parentGroup.equals(that.parentGroup)
                : that.parentGroup != null)
            return false;

        return true;
    }
}
