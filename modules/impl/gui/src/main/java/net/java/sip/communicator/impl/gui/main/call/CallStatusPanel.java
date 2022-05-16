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

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Represents the background status panel of a peer.
 * @author Yana Stamcheva
 */
public class CallStatusPanel
    extends TransparentPanel
{
    /*
     * Silence the serial warning. Though there isn't a plan to serialize
     * the instances of the class, there're no fields so the default
     * serialization routine will work.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a <tt>CallStatusPanel</tt> by specifying a layout manager.
     * @param layout the <tt>LayoutManager</tt>, which would handle laying out
     * components
     */
    public CallStatusPanel(LayoutManager layout)
    {
        super(layout);
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        this.setBackground(Color.WHITE);
    }

    /**
     * Custom paint for the call status panel.
     * @param g the <tt>Graphics</tt> object
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
        }
        finally
        {
            g.dispose();
        }
    }
}
