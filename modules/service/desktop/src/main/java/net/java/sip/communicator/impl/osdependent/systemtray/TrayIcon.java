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
package net.java.sip.communicator.impl.osdependent.systemtray;

import java.awt.event.*;

import javax.swing.*;

/**
 * Interface for all platform specific TrayIcon implementations. See
 * {@link java.awt.TrayIcon} for a description of the methods.
 *
 * @author Lubomir Marinov
 */
public interface TrayIcon
{
    void setDefaultAction(Object menuItem);

    void addBalloonActionListener(ActionListener listener);

    void displayMessage(String caption, String text,
                               java.awt.TrayIcon.MessageType messageType);

    void setIcon(ImageIcon icon) throws NullPointerException;

    void setIconAutoSize(boolean autoSize);
}
