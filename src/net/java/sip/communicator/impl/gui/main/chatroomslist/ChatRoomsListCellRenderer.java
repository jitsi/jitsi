/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatRoomsListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ChatRoomsList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomsListCellRenderer extends JPanel 
    implements ListCellRenderer {
      
    private JLabel nameLabel = new JLabel();
    
    private boolean isSelected = false;

    private boolean isLeaf = true;
    
    /**
     * Initialize the panel containing the node.
     */
    public ChatRoomsListCellRenderer()
    {
        super(new BorderLayout());

        this.setOpaque(false);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        this.nameLabel.setIconTextGap(2);
        
        this.nameLabel.setPreferredSize(new Dimension(10, 17));
        
        this.add(nameLabel, BorderLayout.CENTER);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * 
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        String toolTipText = "<html>";

        if (value instanceof ChatRoomWrapper)
        {
            ChatRoomWrapper chatRoomWrapper = (ChatRoomWrapper) value;

            toolTipText += "<b>"+chatRoomWrapper.getChatRoomName()+"</b>";

            this.nameLabel.setText(chatRoomWrapper.getChatRoomName());

            Image chatRoomImage = ImageLoader
                .getImage(ImageLoader.CHAT_ROOM_16x16_ICON);
            
            if(chatRoomWrapper.getChatRoom() == null ||
                !chatRoomWrapper.getChatRoom().isJoined())
            {
                chatRoomImage
                    = LightGrayFilter.createDisabledImage(chatRoomImage);
            }
            
            this.nameLabel.setIcon(new ImageIcon(chatRoomImage));
                
            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
            
            this.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 1));

            // We should set the bounds of the cell explicitly in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 17);

            this.isLeaf = true;
        }
        else if (value instanceof ChatRoomProviderWrapper)
        {
            ChatRoomProviderWrapper serverWrapper
                = (ChatRoomProviderWrapper) value;

            ProtocolProviderService pps = serverWrapper.getProtocolProvider();

            toolTipText += pps.getAccountID().getService();

            this.nameLabel.setText(pps.getAccountID().getService()
                + " (" + pps.getAccountID().getAccountAddress() + ")");

            this.nameLabel.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CHAT_SERVER_16x16_ICON)));

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // We should set the bounds of the cell explicitly in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);
            
            JLabel groupContentIndicator = new JLabel();
            /*
            if(chatRoomsList.isChatServerClosed(pps))
                groupContentIndicator.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CLOSED_GROUP)));
            else 
                groupContentIndicator.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.OPENED_GROUP)));
                    */
            //the width is fixed in 
            //order all the icons to be with the same size
            groupContentIndicator.setBounds(0, 0, 12, 12);
            
            this.isLeaf = false;
        }

        toolTipText += "</html>";
        this.setToolTipText(toolTipText);
        
        this.isSelected = isSelected;

        return this;
    }
    
    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected. 
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    private void internalPaintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        if (!this.isLeaf) {

            GradientPaint p = new GradientPaint(0, 0,
                    Constants.SELECTED_COLOR,
                    this.getWidth(),
                    this.getHeight(),
                    Constants.GRADIENT_LIGHT_COLOR);

            g2.setPaint(p);            
            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }

        if (this.isSelected) {

            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 1, this.getHeight() - 1,
                    7, 7);
        }
    }
}
