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

import java.util.*;

/**
 * The <tt>UINotificationGroup</tt> class represents a group of notifications.
 *
 * @author Yana Stamcheva
 */
public class UINotificationGroup
{
    /**
     * A list of all unread notifications.
     */
    private Collection<UINotification> unreadNotifications
        = new ArrayList<UINotification>();

    /**
     * The name of the group to which this notification belongs.
     */
    private final String groupName;

    /**
     * The display name of the group to which this notification belongs.
     */
    private final String groupDisplayName;

    /**
     *
     * @param groupName the name of the group to which this notification belongs
     * @param groupDisplayName the display name of the group to which this
     */
    public UINotificationGroup(String groupName, String groupDisplayName)
    {
        this.groupName = groupName;
        this.groupDisplayName = groupDisplayName;
    }

    /**
     * Returns the name of the group, to which this notification belongs.
     *
     * @return the name of the group, to which this notification belongs
     */
    public String getGroupName()
    {
        return groupName;
    }

    /**
     * Returns the display name of the group, to which this notification
     * belongs.
     *
     * @return the display name of the group, to which this notification
     * belongs
     */
    public String getGroupDisplayName()
    {
        return groupDisplayName;
    }

    /**
     * Adds the given notification to the list of unread notifications and
     * notifies interested listeners.
     *
     * @param notification the <tt>UINotification</tt> to add
     */
    public void addNotification(UINotification notification)
    {
        synchronized (unreadNotifications)
        {

            // make sure we override it if we have the same
            unreadNotifications.remove(notification);
            unreadNotifications.add(notification);
        }
    }

    /**
     * Removes all unread notifications.
     */
    public void removeAllNotifications()
    {
        synchronized (unreadNotifications)
        {
            unreadNotifications.clear();
        }
    }

    /**
     * Returns a list of all unread notifications.
     *
     * @return a list of all unread notifications
     */
    public Iterator<UINotification> getUnreadNotifications()
    {
        return new ArrayList<UINotification>(unreadNotifications).iterator();
    }

    /**
     * Returns the count of unread notifications for this group.
     *
     * @return the count of unread notifications for this group
     */
    public int getUnreadNotificationsCount()
    {
        synchronized (unreadNotifications)
        {
            int count = 0;

            for(UINotification n : unreadNotifications)
                count += n.getUnreadObjects();

            return count;
        }
    }
}
