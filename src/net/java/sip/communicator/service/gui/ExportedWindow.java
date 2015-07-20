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
package net.java.sip.communicator.service.gui;

/**
 * A window that could be shown, hidden, resized, moved, etc. Meant to be used
 * from other services to show an application window, like for example a
 * "Configuration" or "Add contact" window.
 *
 * @author Yana Stamcheva
 */
public interface ExportedWindow
{
    /**
     * The add contact window identifier.
     */
    public static final WindowID ADD_CONTACT_WINDOW
        = new WindowID("AddContactWindow");

    /**
     * The about window identifier.
     */
    public static final WindowID ABOUT_WINDOW
        = new WindowID("AboutWindow");

    /**
     * The chat window identifier.
     */
    public static final WindowID CHAT_WINDOW
        = new WindowID("ChatWindow");

    /**
     * The main (contact list) window identifier.
     */
    public static final WindowID MAIN_WINDOW
        = new WindowID("MainWindow");

    /**
     * Returns the WindowID corresponding to this window. The window id should
     * be one of the defined in this class XXX_WINDOW constants.
     *
     * @return the WindowID corresponding to this window
     */
    public WindowID getIdentifier();

    /**
     * Returns TRUE if the component is visible and FALSE otherwise.
     *
     * @return <code>true</code> if the component is visible and
     * <code>false</code> otherwise.
     */
    public boolean isVisible();

    /**
     * Returns TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     * @return TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     */
    public boolean isFocused();

    /**
     * Shows or hides this component.
     * @param isVisible indicates whether to set this window visible or hide it
     */
    public void setVisible(boolean isVisible);

    /**
     * Brings the focus to this window.
     */
    public void bringToFront();

    /**
     * Resizes the window with the given width and height.
     *
     * @param width The new width.
     * @param height The new height.
     */
    public void setSize(int width, int height);

    /**
     * Moves the window to the given coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void setLocation(int x, int y);

    /**
     * Minimizes the window.
     */
    public void minimize();

    /**
     * Maximizes the window.
     */
    public void maximize();

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource();

    /**
     * This method can be called to pass any params to the exported window. This
     * method will be automatically called by
     * {@link UIService#getExportedWindow(WindowID, Object[])} in order to set
     * the parameters passed.
     *
     * @param windowParams the parameters to pass.
     */
    public void setParams(Object[] windowParams);
}
