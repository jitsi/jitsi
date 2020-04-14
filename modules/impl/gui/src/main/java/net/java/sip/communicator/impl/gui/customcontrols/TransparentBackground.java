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
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>TransparentBackground</tt> is a <tt>JComponent</tt>, which is
 * added to a <tt>Window</tt> in order to make it transparent.
 * <p>
 * <b>How to use the <tt>TransparentBackground</tt>?</b>
 * The <tt>TransparentBackground</tt> is created and added to the content pane
 * of the <tt>Window</tt> that should be made transparent. All other components
 * then are added to the <tt>TransparentBackground</tt> component and not
 * directly to the window content pane.
 * <p>
 * <b>How it works?</b>
 * The <tt>TransparentBackground</tt> is a <tt>JComponent</tt> which is not
 * really transparent, but only looks like. It overrides the
 * <code>paintComponent</code> method of <tt>JComponent</tt> to paint its
 * own background image, which is an exact image of the screen at the position
 * where the window will apear and with the same size. The
 * <tt>java.awt.Robot</tt> class is used to make the screen capture.
 * <p>
 * Note that the effect of transparence is gone when behind there is an
 * application which shows dynamic images or something the moves, like
 * a movie for example.
 *
 * @author Yana Stamcheva
 */
public class TransparentBackground extends JComponent {
    private static final long serialVersionUID = 1L;

    private BufferedImage background;

    private final Robot robot;

    private final Window window;

    /**
     * Creates an instance of <tt>TransparentBackground</tt> by specifying
     * the parent <tt>Window</tt> - this is the window that should be made
     * transparent.
     *
     * @param window The parent <tt>Window</tt>
     */
    public TransparentBackground(Window window) {
        this.window = window;

        Robot rbt;
        try {
            rbt = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            rbt = null;
        }
        this.robot = rbt;
    }

    /**
     * Updates the background. Makes a new screen capture at the given
     * coordiantes.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void updateBackground(int x, int y) {
        this.background = robot.createScreenCapture(new Rectangle(x, y, x
                + this.window.getWidth(), y + this.window.getHeight()));

    }

    /**
     * Overrides the <code>paintComponent</code> method in <tt>JComponent</tt>
     * to paint the screen capture image as a background of this component.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            g2.drawImage(this.background, 0, 0, null);

            g2.setColor(new Color(255, 255, 255, 180));

            g2.fillRoundRect(0, 0, width, height, 10, 10);

            g2.setColor(Constants.BORDER_COLOR);

            g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
        }
        finally
        {
            g.dispose();
        }
    }
}
