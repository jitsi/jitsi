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
package net.java.sip.communicator.plugin.chatconfig;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.chatconfig.replacement.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The chat configuration panel.
 *
 * @author Purvesh Sahoo
 */
public class ChatConfigPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates the <tt>ChatConfigPanel</tt>.
     */
    public ChatConfigPanel()
    {
        super(new BorderLayout());

        TransparentPanel mainPanel = new TransparentPanel();

        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        this.add(mainPanel, BorderLayout.NORTH);

        mainPanel.add(new ReplacementConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
    }
}
