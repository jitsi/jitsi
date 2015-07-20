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
package net.java.sip.communicator.plugin.desktoputil.border;

import java.awt.*;

import javax.swing.border.*;

public class ExtendedEtchedBorder
    extends EtchedBorder
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Thickness of the top line.
     */
    private final int top;

    /**
     * Thickness of the left line.
     */
    private final int left;

    /**
     * Thickness of the bottom line.
     */
    private final int bottom;

    /**
     * Thickness of the right line.
     */
    private final int right;

    /**
     * Creates an etched border with the specified etch-type and specified
     * thickness of each border: top, left, bottom, right.
     *
     * @param etchType the type of etch to be drawn by the border
     * @param top the thickness of the top border
     * @param left the thickness of the left border
     * @param bottom the thickness of the bottom border
     * @param right the thickness of the right border
     */
    public ExtendedEtchedBorder(int etchType,
                                int top,
                                int left,
                                int bottom,
                                int right)
    {
        super(etchType, null, null);

        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    /**
     * Paints the border for the specified component with the
     * specified position and size.
     * @param c the component for which this border is being painted
     * @param g the paint graphics
     * @param x the x position of the painted border
     * @param y the y position of the painted border
     * @param width the width of the painted border
     * @param height the height of the painted border
     */
    @Override
    public void paintBorder(Component c,
                            Graphics g,
                            int x,
                            int y,
                            int width,
                            int height)
    {
        int w = width;
        int h = height;

        Graphics2D g2 = (Graphics2D) g;

        g2.translate(x, y);

        g2.setColor(etchType == LOWERED  ? getShadowColor(c)
                                        : getHighlightColor(c));

        if (top > 0)
        {
            g2.setStroke(new BasicStroke(top));
            g2.drawLine(0, 0, w-2, 0);
        }

        if (left > 0)
        {
            g2.setStroke(new BasicStroke(left));
            g2.drawLine(0, 0, 0, h-2);
        }

        if (bottom > 0)
        {
            g2.setStroke(new BasicStroke(bottom));
            g2.drawLine(0, h-2, w-2, h-2);
        }

        if (right > 0)
        {
            g2.setStroke(new BasicStroke(right));
            g2.drawLine(w-2, 0, w-2, h-2);
        }

        g2.setColor(etchType == LOWERED  ? getHighlightColor(c)
                                        : getShadowColor(c));

        if (top > 0)
            g2.drawLine(1, 1, w-3, 1);

        if (left > 0)
            g2.drawLine(1, h-3, 1, 1);

        if (right > 0)
            g2.drawLine(0, h-1, w-1, h-1);

        if (bottom > 0)
            g2.drawLine(w-1, h-1, w-1, 0);

        g2.translate(-x, -y);
    }
}
