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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The panel, containing the title name of the call peer or member. It defines
 * its background color depending on the specified in initialization background
 * color.
 *
 * @author Yana Stamcheva
 */
public class CallTitlePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = 0L;

    private Color backgroundColor;

    /**
     * Creates a <tt>CallTitlePanel</tt> by specifying the <tt>layout</tt>
     * manager to use when layout out components.
     *
     * @param layout the layout manager to use for layout
     */
    public CallTitlePanel(LayoutManager layout)
    {
        super(layout);
    }

    /**
     * Sets the background color of this panel.
     * @param bgColor the background color of this panel
     */
    @Override
    public void setBackground(Color bgColor)
    {
        this.backgroundColor = bgColor;
    }

    /**
     * Customizes the background of this panel, by painting a round rectangle in
     * the background color previously set.
     * @param g the <tt>Graphics</tt> object to use for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(backgroundColor);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
        }
        finally
        {
            g.dispose();
        }
    }
}
