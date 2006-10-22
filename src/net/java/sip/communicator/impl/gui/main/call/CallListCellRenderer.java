/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class CallListCellRenderer extends JPanel 
    implements ListCellRenderer {
      
    private JPanel dataPanel = new JPanel(new BorderLayout());
    
    private JLabel nameLabel = new JLabel();
    
    private JLabel timeLabel = new JLabel();
    
    private JLabel durationLabel = new JLabel();
    
    private JLabel iconLabel = new JLabel();
    
    private Icon incomingIcon = new ImageIcon(ImageLoader.getImage(
            ImageLoader.INCOMING_CALL_ICON));
    
    private Icon outgoingIcon = new ImageIcon(ImageLoader.getImage(
            ImageLoader.OUTGOING_CALL_ICON));
    
    private boolean isSelected = false;

    private boolean isLeaf = true;
    
    private String direction;    
    
    /**
     * Initialize the panel containing the node.
     */
    public CallListCellRenderer() {

        super(new BorderLayout(5, 5));

        this.setBackground(Color.WHITE);
        
        this.setOpaque(true);
        
        this.dataPanel.setOpaque(false);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        this.nameLabel.setIconTextGap(2);
        
        this.nameLabel.setPreferredSize(new Dimension(10, 25));
        
        this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        
        this.dataPanel.add(nameLabel, BorderLayout.CENTER);
        
        this.add(dataPanel, BorderLayout.CENTER);        
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * 
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        this.dataPanel.remove(timeLabel);
        this.dataPanel.remove(durationLabel);
        this.remove(iconLabel);
        
        if (value instanceof GuiCallParticipantRecord) {

            GuiCallParticipantRecord participant
                = (GuiCallParticipantRecord) value;
            
            this.direction = participant.getDirection();
            
            if(direction.equals(GuiCallParticipantRecord.INCOMING_CALL))
                    iconLabel.setIcon(incomingIcon);
            else
                    iconLabel.setIcon(outgoingIcon);
            
            this.nameLabel.setText(participant.getParticipantName());
            
            this.timeLabel.setText(
                    Messages.getString("at") + " " + 
                    GuiUtils.formatTime(
                    participant.getStartTime()));
           
            this.durationLabel.setText(
                    Messages.getString("duration") + " " +
                    GuiUtils.formatTime(
                    GuiUtils.substractDates(
                            participant.getEndTime(),
                            participant.getStartTime())));
            
            //this.nameLabel.setIcon(listModel
            //        .getMetaContactStatusIcon(contactItem));
            
            this.dataPanel.add(timeLabel, BorderLayout.EAST);
            this.dataPanel.add(durationLabel, BorderLayout.SOUTH);
            
            this.add(iconLabel, BorderLayout.WEST);
                        
            this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 25);

            this.isLeaf = true;
        }
        else if (value instanceof String) {

            String dateString = (String) value;
            
            this.nameLabel.setText(dateString);
            
            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            this.isLeaf = false;
        }
        
        this.isSelected = isSelected;

        return this;
    }
        
    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected. 
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        if (!this.isLeaf) {

            GradientPaint p = new GradientPaint(0, 0,
                    Constants.BLUE_GRAY_BORDER_COLOR,
                    this.getWidth(),
                    this.getHeight(),
                    Constants.MOVER_END_COLOR);

            g2.setPaint(p);            
            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }
        else {
            if(direction.equals(GuiCallParticipantRecord.INCOMING_CALL)) {
                
                GradientPaint p = new GradientPaint(0, 0,
                        Constants.HISTORY_IN_CALL_COLOR,
                        this.getWidth(),
                        this.getHeight(),
                        Constants.MOVER_END_COLOR);

                g2.setPaint(p);            
                g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
            }
            else if(direction.equals(GuiCallParticipantRecord.OUTGOING_CALL)){
                
                GradientPaint p = new GradientPaint(0, 0,
                        Constants.HISTORY_OUT_CALL_COLOR,
                        this.getWidth(),
                        this.getHeight(),
                        Constants.MOVER_END_COLOR);

                g2.setPaint(p);            
                g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
            }
        }
        
        if (this.isSelected) {

            g2.setColor(Constants.SELECTED_END_COLOR);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.BLUE_GRAY_BORDER_DARKER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 2, this.getHeight() - 1,
                    7, 7);
        }
    }
}
