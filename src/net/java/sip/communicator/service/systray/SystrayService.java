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

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>SystrayService</tt> manages the system tray icon, menu and messages.
 * It is meant to be used by all bundles that want to show a system tray message.
 *
 * @author Yana Stamcheva
 */
public interface SystrayService
{
    /**
     * Message type corresponding to an error message.
     */
    public static final int ERROR_MESSAGE_TYPE = 0;

    /**
     * Message type corresponding to an information message.
     */
    public static final int INFORMATION_MESSAGE_TYPE = 1;

    /**
     * Message type corresponding to a warning message.
     */
    public static final int WARNING_MESSAGE_TYPE = 2;

    /**
     * Message type is not accessible.
     */
    public static final int NONE_MESSAGE_TYPE = -1;

    /**
     * Image type corresponding to the jitsi icon
     */
    public static final int SC_IMG_TYPE = 0;

    /**
     * Image type corresponding to the jitsi offline icon
     */
    public static final int SC_IMG_OFFLINE_TYPE = 2;

    /**
     * Image type corresponding to the jitsi away icon
     */
    public static final int SC_IMG_AWAY_TYPE = 3;

    /**
     * Image type corresponding to the jitsi free for chat icon
     */
    public static final int SC_IMG_FFC_TYPE = 4;

    /**
     * Image type corresponding to the jitsi do not disturb icon
     */
    public static final int SC_IMG_DND_TYPE = 5;

    /**
     * Image type corresponding to the jitsi away icon
     */
    public static final int SC_IMG_EXTENDED_AWAY_TYPE = 6;

    /**
     * Image type corresponding to the envelope icon
     */
    public static final int ENVELOPE_IMG_TYPE = 1;

    /**
     * Shows the given <tt>PopupMessage</tt>
     *
     * @param popupMessage the message to show
     */
    public void showPopupMessage(PopupMessage popupMessage);

    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Removes a listener previously added with <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Set the handler which will be used for popup message
     * @param popupHandler the handler to use
     * @return the previously used popup handler
     */
    public PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler popupHandler);

    /**
     * Get the handler currently used by the systray service for popup message
     * @return the handler used by the systray service
     */
    public PopupMessageHandler getActivePopupMessageHandler();

    /**
     * Sets a new icon to the systray.
     *
     * @param imageType the type of the image to set
     */
    public void setSystrayIcon(int imageType);

    /**
     * Selects the best available popup message handler
     */
    public void selectBestPopupMessageHandler();
}
