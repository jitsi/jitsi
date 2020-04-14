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

import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

/**
 * SIPCommBorders is where all component borders used in the SIPComm L&F are
 * drawn.
 *
 * @author Yana Stamcheva
 */
public class SIPCommBorders
{
    /**
     * The RoundBorder is common border which is used throughout the SIPComm
     * L&F.
     */
    public static class RoundBorder
        extends AbstractBorder
        implements UIResource
    {
        private static final long serialVersionUID = 0L;

        private static final Insets insets = new Insets(2, 2, 2, 2);

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w,
            int h)
        {
            if (c.isEnabled())
            {
                SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 5, 5);
            }
            else
            {
                SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 5, 5);
            }
        }

        @Override
        public Insets getBorderInsets(Component c)
        {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets newInsets)
        {
            newInsets.top = insets.top;
            newInsets.left = insets.left;
            newInsets.bottom = insets.bottom;
            newInsets.right = insets.right;

            return newInsets;
        }
    }

    private static Border roundBorder;

    public static Border getRoundBorder()
    {
        if (roundBorder == null
            || !(roundBorder instanceof SIPCommBorders.RoundBorder))
        {
            roundBorder =
                new BorderUIResource.CompoundBorderUIResource(
                    new SIPCommBorders.RoundBorder(),
                    new BasicBorders.MarginBorder());
        }
        return roundBorder;
    }

    /**
     * The BoldRoundBorder is common border which is used throughout the SIPComm
     * L&F.
     */
    public static class BoldRoundBorder
        extends AbstractBorder
        implements UIResource
    {
        private static final long serialVersionUID = 0L;

        private static final Insets insets = new Insets(2, 2, 2, 2);

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w,
            int h)
        {
            SIPCommLFUtils.drawBoldRoundBorder(g, x, y, w, h, 8, 8);
        }

        @Override
        public Insets getBorderInsets(Component c)
        {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets newInsets)
        {
            newInsets.top = insets.top;
            newInsets.left = insets.left;
            newInsets.bottom = insets.bottom;
            newInsets.right = insets.right;

            return newInsets;
        }
    }

    private static Border boldRoundBorder;

    public static Border getBoldRoundBorder()
    {
        if (boldRoundBorder == null
            || !(boldRoundBorder instanceof SIPCommBorders.BoldRoundBorder))
        {
            boldRoundBorder =
                new BorderUIResource.CompoundBorderUIResource(
                    new SIPCommBorders.BoldRoundBorder(),
                    new BasicBorders.MarginBorder());
        }
        return boldRoundBorder;
    }

    private static Border textFieldBorder;

    /**
     * Returns a border instance for a JTextField.
     */
    public static Border getTextFieldBorder()
    {
        if (textFieldBorder == null
            || !(textFieldBorder instanceof SIPCommBorders.TextFieldBorder))
        {
            textFieldBorder =
                new BorderUIResource.CompoundBorderUIResource(
                    new SIPCommBorders.TextFieldBorder(),
                    new BasicBorders.MarginBorder());
        }
        return textFieldBorder;
    }

    /**
     * The TextField border which is used in SIPComm L&F for all text fields.
     */
    public static class TextFieldBorder
        extends RoundBorder
    {
        private static final long serialVersionUID = 0L;

        @Override
        public void paintBorder(Component c,
                                Graphics g,
                                int x,
                                int y,
                                int w,
                                int h)
        {
            if (!(c instanceof JTextComponent))
            {
                if (c.isEnabled())
                {
                    SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 7, 7);
                }
                else
                {
                    SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 7, 7);
                }
                return;
            }

            if (c.isEnabled() && ((JTextComponent) c).isEditable())
            {
                SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 7, 7);
            }
            else
            {
                SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 7, 7);
            }
        }
    }
}
