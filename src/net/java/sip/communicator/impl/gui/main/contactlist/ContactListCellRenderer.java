/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class ContactListCellRenderer extends JPanel 
    implements ListCellRenderer {
      
    private JLabel nameLabel = new JLabel();

    private JPanel buttonsPanel;
    
    private SIPCommButton extendPanelButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.MORE_INFO_ICON), ImageLoader
            .getImage(ImageLoader.MORE_INFO_ICON));
    
    private boolean isSelected = false;

    private boolean isLeaf = true;
    
    private MainFrame mainFrame;
    
    private int CONTACT_PROTOCOL_BUTTON_WIDTH = 20;

    /**
     * Initialize the panel containing the node.
     */
    public ContactListCellRenderer(MainFrame mainFrame) {

        super(new BorderLayout());

        this.mainFrame = mainFrame;
        this.setBackground(Color.WHITE);

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 0));
        
        this.buttonsPanel.setOpaque(false);
        this.buttonsPanel.setName("buttonsPanel");

        this.setOpaque(true);

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
            int index, boolean isSelected, boolean cellHasFocus) {

        ContactList contactList = (ContactList) list;
        ContactListModel listModel = (ContactListModel) contactList.getModel();

        String toolTipText = "<html>";
        
        if (value instanceof MetaContact) {

            MetaContact contactItem = (MetaContact) value;

            toolTipText += "<b>"+contactItem.getDisplayName()+"</b>";

            this.nameLabel.setText(contactItem.getDisplayName());

            this.nameLabel.setIcon(listModel
                    .getMetaContactStatusIcon(contactItem));

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
            
            this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 17);

            this.buttonsPanel.removeAll();
            
            Iterator i = contactItem.getContacts();
            int buttonsPanelWidth = 0;
            while (i.hasNext()) {
                Contact protocolContact = (Contact) i.next();
                
                Image protocolStatusIcon
                    = ImageLoader.getBytesInImage(
                            protocolContact.getPresenceStatus().getStatusIcon());

                int providerIndex = mainFrame.getProviderIndex(
                        protocolContact.getProtocolProvider());
                
                Image img;
                if(providerIndex > 0) {
                    img = createIndexedImage(protocolStatusIcon, providerIndex);
                }
                else {
                    img = protocolStatusIcon;
                }
                ContactProtocolButton contactProtocolButton 
                    = new ContactProtocolButton(img);

                contactProtocolButton.setProtocolContact(protocolContact);
                
                contactProtocolButton.setBounds(buttonsPanelWidth,
                        16, 
                        CONTACT_PROTOCOL_BUTTON_WIDTH,//the width is fixed in 
                            //order all the icons to be with the same size
                        img.getHeight(null));

                buttonsPanelWidth += CONTACT_PROTOCOL_BUTTON_WIDTH;
                this.buttonsPanel.add(contactProtocolButton);
                
                String contactDisplayName = protocolContact.getDisplayName();
                String contactAddress = protocolContact.getAddress();
                toolTipText
                    += "<br>"
                        + ((!contactDisplayName
                                .equals(contactAddress))
                            ? contactDisplayName + " ("+contactAddress + ")"
                                    : contactDisplayName);
            }
            this.buttonsPanel.setPreferredSize(
                    new Dimension(buttonsPanelWidth, 16));
            this.buttonsPanel.setBounds(
                    list.getWidth() - 2 - buttonsPanelWidth, 0,
                    buttonsPanelWidth, 16);
            this.nameLabel.setBounds(
                    0, 0, list.getWidth() - 2 - buttonsPanelWidth, 17);
            
            this.add(buttonsPanel, BorderLayout.EAST);

            this.isLeaf = true;
        } else if (value instanceof MetaContactGroup) {

            MetaContactGroup groupItem = (MetaContactGroup) value;

            toolTipText += groupItem.getGroupName();
            
            this.nameLabel.setText(groupItem.getGroupName() 
                    + "  ( " + groupItem.countChildContacts() + " )");

            this.nameLabel.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.GROUPS_16x16_ICON)));

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            //this.remove(buttonsPanel);
            this.buttonsPanel.removeAll();
            
            JLabel groupContentIndicator = new JLabel();
            
            if(((ContactListModel)list.getModel()).isGroupClosed(groupItem))
                groupContentIndicator.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CLOSED_GROUP)));
            else 
                groupContentIndicator.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.OPENED_GROUP)));

            //the width is fixed in 
            //order all the icons to be with the same size
            groupContentIndicator.setBounds(0, 0, 12, 12);
            this.buttonsPanel.setPreferredSize(
                    new Dimension(17, 16));
            this.buttonsPanel.setBounds(
                    list.getWidth() - 2 - 17, 0,
                    17, 16);
            
            this.buttonsPanel.add(groupContentIndicator);
            
            this.isLeaf = false;
        }

        toolTipText += "</html>";
        this.setToolTipText(toolTipText);
        
        this.isSelected = isSelected;

        return this;
    }
    
    /**
     * Adds the protocol provider index to the given source image.
     * @param sourceImage
     * @param index
     * @return
     */
    private Image createIndexedImage(Image sourceImage, int index)
    {        
        BufferedImage buffImage = new BufferedImage(
                22, 16, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = (Graphics2D)buffImage.getGraphics();
        AlphaComposite ac =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        
        AntialiasingManager.activateAntialiasing(g);
        g.setColor(Color.DARK_GRAY);
        g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
        g.drawImage(sourceImage, 0, 0, null);
        g.setComposite(ac);
        g.drawString(new Integer(index).toString(), 14, 8);
        
        return buffImage;
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
                    Constants.SELECTED_END_COLOR,
                    this.getWidth(),
                    this.getHeight(),
                    Constants.MOVER_END_COLOR);

            g2.setPaint(p);            
            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }

        if (this.isSelected) {

            g2.setColor(Constants.SELECTED_END_COLOR);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.BLUE_GRAY_BORDER_DARKER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 1, this.getHeight() - 1,
                    7, 7);
        }
    }
}
