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

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

public interface ExtendedTooltip
{
    void setImage(ImageIcon image);

    void setTitle(String title);

    void addLine(Icon icon, String text);

    void removeAllLines();

    void setBottomText(String statusMessage);

    void addSubLine(Icon icon, String text, int leftIndent);

    void revalidate();

    void repaint();

    void setComponent(JComponent c);

    void addLine(JLabel[] labels);
}
