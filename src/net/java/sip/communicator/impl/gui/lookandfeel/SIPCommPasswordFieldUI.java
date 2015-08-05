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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The SIPCommPasswordFieldUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommPasswordFieldUI extends BasicPasswordFieldUI {

    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommPasswordFieldUI();
    }

    @Override
    protected void paintSafely(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }

    @Override
    protected void paintBackground(Graphics g)
    {
        JTextComponent c = this.getComponent();
        g.setColor(c.getBackground());
        g.fillRoundRect(1, 1, c.getWidth()-2, c.getHeight()-2, 5, 5);
    }
}
