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
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>DialpadDialog</tt> is a popup dialog containing a dialpad.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class DialpadDialog
    extends JDialog
    implements WindowFocusListener
{
    /**
     * Creates a new instance of this class using the specified
     * <tt>dialPanel</tt>.
     *
     * @param dialPanel the <tt>DialPanel</tt> that we'd like to wrap.
     */
    private DialpadDialog(DialPanel dialPanel)
    {
        dialPanel.setOpaque(false);

        BackgroundPanel bgPanel = new BackgroundPanel();

        bgPanel.setLayout(new BorderLayout());
        bgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bgPanel.add(dialPanel, BorderLayout.CENTER);

        Container contentPane = getContentPane();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(bgPanel, BorderLayout.CENTER);

        this.setUndecorated(true);
        this.pack();
    }

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     *
     * @param dtmfHandler handles DTMFs.
     */
    public DialpadDialog(final DTMFHandler dtmfHandler)
    {
        this(new DialPanel(dtmfHandler));

        this.setModal(false);

        addWindowListener(
                new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        dtmfHandler.removeParent(DialpadDialog.this);
                    }

                    @Override
                    public void windowOpened(WindowEvent e)
                    {
                        dtmfHandler.addParent(DialpadDialog.this);
                    }
                });
    }

    /**
     * New panel used as background for the dialpad which would be painted with
     * round corners and a gradient background.
     */
    private static class BackgroundPanel extends JPanel
    {
        /**
         * Calls <tt>super</tt>'s <tt>paintComponent</tt> method and then adds
         * background with gradient.
         *
         * @param g a reference to the currently valid <tt>Graphics</tt> object
         */
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            GradientPaint p = new GradientPaint(width / 2, 0,
                    Constants.GRADIENT_DARK_COLOR, width / 2,
                    height,
                    Constants.GRADIENT_LIGHT_COLOR);

            g2.setPaint(p);

            g2.fillRoundRect(0, 0, width, height, 10, 10);

            g2.setColor(Constants.GRADIENT_DARK_COLOR);

            g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
        }
    }

    /**
     * Dummy implementation.
     *
     * @param e unused
     */
    public void windowGainedFocus(WindowEvent e)
    {
    }

    /**
     * Dummy implementation.
     *
     * @param e unused
     */
    public void windowLostFocus(WindowEvent e)
    {
        this.removeWindowFocusListener(this);
        this.setVisible(false);
    }
}
