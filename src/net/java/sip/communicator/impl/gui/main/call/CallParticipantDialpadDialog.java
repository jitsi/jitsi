/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactInfoPanel</tt> is a popup dialog containing the contact
 * detailed info.
 * 
 * @author Yana Stamcheva
 */
public class CallParticipantDialpadDialog
    extends JDialog
{
    private DialPanel dialPanel;

    private BackgroundPanel bgPanel;

    /**
     * Creates an instance of the <tt>ContactInfoPanel</tt>.
     * 
     * @param callManager the call manager
     * @param callParticipant the corresponding call participant
     */
    public CallParticipantDialpadDialog(CallManager callManager,
        CallParticipant callParticipant)
    {
        super(callManager.getMainFrame(), false);
        
        dialPanel = new DialPanel(callManager, callParticipant);
        
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
