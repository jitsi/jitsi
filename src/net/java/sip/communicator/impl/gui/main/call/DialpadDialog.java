/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
 * @author Lubomir Marinov
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
    public DialpadDialog(
        DTMFHandler dtmfHandler)
    {
        this(new DialPanel(dtmfHandler));

        this.setModal(false);

        dtmfHandler.addParent(this);
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
