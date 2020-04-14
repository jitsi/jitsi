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

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * SIPCommComboBoxUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommComboBoxUI
    extends BasicComboBoxUI
{
    /**
     * Creates the UI for the given component <tt>c</tt>.
     * @param c the component to create an UI for.
     * @return the created ComponentUI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommComboBoxUI();
    }

    /**
     * Paints the UI for the given component through the given graphics object.
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the component to paint
     */
    @Override
    public void paint(Graphics g, JComponent c)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }

    /**
     * Creates the editor of the combo box related to this UI.
     * @return the created combo box editor
     */
    @Override
    protected ComboBoxEditor createEditor()
    {
        return new SIPCommComboBoxEditor.UIResource();
    }
}
