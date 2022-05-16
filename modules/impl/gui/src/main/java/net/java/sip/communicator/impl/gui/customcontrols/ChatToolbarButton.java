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
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>MsgToolbarButton</tt> is a <tt>SIPCommButton</tt> that has
 * specific background and rollover images. It is used in the chat window
 * toolbar.
 *
 * @author Yana Stamcheva
 */
public class ChatToolbarButton extends SIPCommButton
{
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of <tt>MsgToolbarButton</tt>.
     * @param iconImage The icon to display on this button.
     */
    public ChatToolbarButton(Image iconImage)
    {
        super(null, iconImage);

        this.setPreferredSize(new Dimension(25, 25));
    }
}
