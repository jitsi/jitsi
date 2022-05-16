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

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>PopupMessageNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * popup message actions.
 *
 * @author Yana Stamcheva
 */
public interface PopupMessageNotificationHandler
    extends NotificationHandler
{
    /**
     * Shows the given <tt>PopupMessage</tt>
     *
     * @param action the action to act upon
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where
     * appropriate
     * @param tag additional info to be used by the notification handler
     */
    public void popupMessage(
            PopupMessageNotificationAction action,
            String title,
            String message,
            byte[] icon,
            Object tag);

    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Removes a listener previously added with
     * <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(
            SystrayPopupMessageListener listener);
}
