/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * 
 * @author Yana Stamcheva
 */
public class ExtListCellRenderer extends JPanel
    implements ListCellRenderer {

    private JLabel label = new JLabel();
    private boolean isSelected;
    
    public ExtListCellRenderer() {
        super(new BorderLayout());
        
        this.add(label);
    }
    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        this.label.setText(value.toString());
        this.isSelected = isSelected;
        
        return this;
    }
    
    /**
     * Paint a round background for all selected cells.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);
        
        if (this.isSelected) {

            g2.setColor(Constants.SELECTED_END_COLOR);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.BLUE_GRAY_BORDER_DARKER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1,
                    7, 7);
        }
    }   
}
