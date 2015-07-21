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
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>SearchTextFieldUI</tt> is the one responsible for the search field
 * look & feel. It draws a search icon inside the field and adjusts the bounds
 * of the editor rectangle according to it.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class DialPadFieldUI
    extends SIPCommTextFieldUI
    implements Skinnable
{
    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public DialPadFieldUI()
    {
        loadSkin();
    }

    /**
     * Adds the custom mouse listeners defined in this class to the installed
     * listeners.
     */
    @Override
    protected void installListeners()
    {
        super.installListeners();
    }

    /**
     * Implements parent paintSafely method and enables antialiasing.
     * @param g the <tt>Graphics</tt> object that notified us
     */
    @Override
    protected void paintSafely(Graphics g)
    {
        customPaintBackground(g);
        super.paintSafely(g);
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    protected void customPaintBackground(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g2);
            super.customPaintBackground(g2);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    @Override
    protected Rectangle getVisibleEditorRect()
    {
        Rectangle rect = super.getVisibleEditorRect();

        if ((rect.width > 0) && (rect.height > 0))
        {
            rect.x += 8;
            rect.width -= 18;

            return rect;
        }
        return null;
    }

    /**
     * Creates a UI.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new DialPadFieldUI();
    }
}
