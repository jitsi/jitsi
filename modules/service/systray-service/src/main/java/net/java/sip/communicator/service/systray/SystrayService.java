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
package net.java.sip.communicator.service.systray;

import java.util.*;

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>SystrayService</tt> manages the system tray icon, menu and messages.
 * It is meant to be used by all bundles that want to show a system tray message.
 *
 * @author Yana Stamcheva
 */
public interface SystrayService
{
    String PNMAE_TRAY_MODE =
        "net.java.sip.communicator.osdependent.systemtray.MODE";

    /**
     * Message type corresponding to an error message.
     */
    int ERROR_MESSAGE_TYPE = 0;

    /**
     * Message type corresponding to an information message.
     */
    int INFORMATION_MESSAGE_TYPE = 1;

    /**
     * Message type corresponding to a warning message.
     */
    int WARNING_MESSAGE_TYPE = 2;

    /**
     * Message type is not accessible.
     */
    int NONE_MESSAGE_TYPE = -1;

    /**
     * Image type corresponding to the jitsi icon
     */
    int SC_IMG_TYPE = 0;

    /**
     * Image type corresponding to the jitsi offline icon
     */
    int SC_IMG_OFFLINE_TYPE = 2;

    /**
     * Image type corresponding to the jitsi away icon
     */
    int SC_IMG_AWAY_TYPE = 3;

    /**
     * Image type corresponding to the jitsi free for chat icon
     */
    int SC_IMG_FFC_TYPE = 4;

    /**
     * Image type corresponding to the jitsi do not disturb icon
     */
    int SC_IMG_DND_TYPE = 5;

    /**
     * Image type corresponding to the jitsi away icon
     */
    int SC_IMG_EXTENDED_AWAY_TYPE = 6;

    /**
     * Shows the given <tt>PopupMessage</tt>
     *
     * @param popupMessage the message to show
     */
    void showPopupMessage(PopupMessage popupMessage);

    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param listener the listener to add
     */
    void addPopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Removes a listener previously added with <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    void removePopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Set the handler which will be used for popup message
     * @param popupHandler the handler to use
     * @return the previously used popup handler
     */
    PopupMessageHandler setActivePopupMessageHandler(
        PopupMessageHandler popupHandler);

    /**
     * Get the handler currently used by the systray service for popup message
     * @return the handler used by the systray service
     */
    PopupMessageHandler getActivePopupMessageHandler();

    /**
     * Sets a new icon to the systray.
     *
     * @param imageType the type of the image to set
     */
    void setSystrayIcon(int imageType);

    /**
     * Selects the best available popup message handler
     */
    void selectBestPopupMessageHandler();

    /**
     * Checks if the systray icon has been initialized.
     * @return True if the systray is initialized, false otherwise.
     */
    boolean checkInitialized();

    /**
     * Set the number that should be shown as an overlay on the try icon.
     * @param count The number of pending notifications.
     */
    void setNotificationCount(int count);

    /**
     * Gets a map of systray modes and resource-keys that describe them.
     * @return key: mode for config property, value: resource key
     */
    Map<String, String> getSystrayModes();

    /**
     * Gets the systray mode that was chosen at startup, either by default or as
     * an override selected by the user.
     *
     * @return The selected mode or {@code disabled} if the user selected mode
     *         is not available.
     */
    String getActiveSystrayMode();
}
