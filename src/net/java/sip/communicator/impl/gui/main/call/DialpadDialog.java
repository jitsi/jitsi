/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>DialpadDialog</tt> is a popup dialog containing a dialpad.
 *
 * @author Yana Stamcheva
 */
public class DialpadDialog
    extends JDialog
    implements WindowFocusListener
{
    private DialPanel dialPanel;

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     *
     * @param callPeers The corresponding call peers.
     */
    public DialpadDialog(Iterator<CallPeer> callPeers)
    {
        this.setModal(false);

        dialPanel = new DialPanel(callPeers);

        this.init();

        this.addWindowFocusListener(this);
    }

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     *
     * @param mainCallPanel The call panel.
     */
    public DialpadDialog(MainCallPanel mainCallPanel)
    {
        dialPanel = new DialPanel(mainCallPanel);

        this.init();

        this.addWindowFocusListener(this);
    }

    private void init()
    {
        this.setUndecorated(true);

        this.dialPanel.setOpaque(false);

        BackgroundPanel bgPanel = new BackgroundPanel();

        bgPanel.setLayout(new BorderLayout());

        bgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(bgPanel, BorderLayout.CENTER);

        bgPanel.add(dialPanel, BorderLayout.CENTER);

        this.pack();
    }

    /**
     * New panel used as background for the dialpad which would be painted with
     * round corners and a gradient background.
     */
    private static class BackgroundPanel extends JPanel
    {
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

    public void windowGainedFocus(WindowEvent e)
    {
    }

    public void windowLostFocus(WindowEvent e)
    {
        this.setVisible(false);
    }
}
