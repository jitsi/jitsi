/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
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
{
    private DialPanel dialPanel;

    private BackgroundPanel bgPanel;

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     * 
     * @param callParticipants The corresponding call participants.
     */
    public DialpadDialog(Iterator<CallParticipant> callParticipants)
    {
        this.setModal(false);

        dialPanel = new DialPanel(callParticipants);

        this.init();
    }

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     * 
     * @param mainCallPanel The call panel.
     */
    public DialpadDialog(MainCallPanel mainCallPanel)
    {
        super();

        dialPanel = new DialPanel(mainCallPanel);

        this.init();
    }

    private void init()
    {
        this.setUndecorated(true);

        this.dialPanel.setOpaque(false);

        this.bgPanel = new BackgroundPanel();

        this.bgPanel.setLayout(new BorderLayout());

        this.bgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.getContentPane().setLayout(new BorderLayout());

        this.getContentPane().add(bgPanel, BorderLayout.CENTER);

        this.bgPanel.add(dialPanel, BorderLayout.CENTER);

        this.pack();
    }

    /**
     * New panel used as background for the dialpad which would be painted with
     * round corners and a gradient background.
     */
    private class BackgroundPanel extends JPanel
    {
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                    Constants.GRADIENT_DARK_COLOR, this.getWidth() / 2,
                    getHeight(),
                    Constants.GRADIENT_LIGHT_COLOR);

            g2.setPaint(p);

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            g2.setColor(Constants.GRADIENT_DARK_COLOR);

            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
    }
}
