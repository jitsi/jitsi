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
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>PopupMessageNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class PopupMessageNotificationAction
    extends NotificationAction
{
    private String defaultMessage;

    /**
     * Suggested timeout in ms for hiding the popup if not clicked by the user.
     */
    private long timeout = -1;

    /**
     * Group name used to group notifications on Android.
     */
    private String groupName;

    /**
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     *
     * @param defaultMessage the default message to use if no message is
     * specified
     */
    public PopupMessageNotificationAction(String defaultMessage)
    {
        super(NotificationAction.ACTION_POPUP_MESSAGE);
        this.defaultMessage = defaultMessage;
    }

    /**
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     *
     * @param defaultMessage the default message to use if no message is
     * specified
     * @param timeout suggested timeout in ms for hiding the popup if not
     *                clicked by the user, -1 for infinity
     */
    public PopupMessageNotificationAction(String defaultMessage, long timeout)
    {
        this(defaultMessage);
        this.timeout = timeout;
    }

    /**
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     *
     * @param defaultMessage the default message to use if no message is
     * specified
     * @param timeout suggested timeout in ms for hiding the popup if not
     *                clicked by the user, -1 for infinity
     * @param groupName name of the group that will be used for merging popups
     */
    public PopupMessageNotificationAction(String defaultMessage, long timeout,
                                          String groupName)
    {
        this(defaultMessage, timeout);
        this.groupName = groupName;
    }

    /**
     * Return the default message to use if no message is specified.
     *
     * @return the default message to use if no message is specified.
     */
    public String getDefaultMessage()
    {
        return defaultMessage;
    }

    /**
     * Returns suggested timeout value in ms for hiding the popup if not clicked
     * by the user.
     * @return timeout value in ms for hiding the popup, -1 for infinity.
     */
    public long getTimeout()
    {
        return timeout;
    }

    /**
     * Sets the name of the group that will be used for merging popups.
     * @param groupName name of popup group to set.
     */
    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    /**
     * Returns name of popup group that will be used for merging notifications.
     * @return name of popup group that will be used for merging notifications.
     */
    public String getGroupName()
    {
        return groupName;
    }
}
