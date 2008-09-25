/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class ContactListCellRenderer
    extends JPanel 
    implements ListCellRenderer
{
    private Logger logger = Logger.getLogger(ContactListCellRenderer.class);

    private static final int AVATAR_HEIGHT = 30;

    private static final int AVATAR_WIDTH = 30;

    private JLabel nameLabel = new JLabel();

    private JLabel photoLabel = new JLabel();

    private JPanel buttonsPanel;

    private int rowTransparency
        = GuiActivator.getResources()
            .getSettingsInt("contactListRowTransparency");

    private boolean isSelected = false;

    private int index = 0;

    private boolean isLeaf = true;

    private MainFrame mainFrame;

    /**
     * Initialize the panel containing the node.
     */
    public ContactListCellRenderer(MainFrame mainFrame) {

        super(new BorderLayout());

        this.mainFrame = mainFrame;

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 0));

        this.buttonsPanel.setName("buttonsPanel");

        this.setOpaque(false);
        this.nameLabel.setOpaque(false);
        this.buttonsPanel.setOpaque(false);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.nameLabel.setIconTextGap(2);

        this.nameLabel.setPreferredSize(new Dimension(10, 17));

        this.add(nameLabel, BorderLayout.CENTER);
        this.add(photoLabel, BorderLayout.EAST);
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
        this.index = index;

        this.photoLabel.setIcon(null);

        ContactList contactList = (ContactList) list;
        ContactListModel listModel = (ContactListModel) contactList.getModel();

        if (value instanceof MetaContact)
        {
            this.setPreferredSize(new Dimension(20, 30));

            MetaContact contactItem = (MetaContact) value;

            String displayName = contactItem.getDisplayName();

            if (displayName == null || displayName.length() < 1)
            {
                displayName = Messages.getI18NString("unknown").getText();
            }

            this.nameLabel.setText(displayName);

            this.nameLabel.setIcon(listModel
                    .getMetaContactStatusIcon(contactItem));

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));

            byte[] avatar = contactItem.getAvatar(true);
            if (avatar != null && avatar.length > 0)
            {
                Image roundedAvatar = createRoundImage(avatar);
                this.photoLabel.setIcon(new ImageIcon(roundedAvatar));
            }

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 30);

            this.buttonsPanel.removeAll();

            this.nameLabel.setBounds(
                    0, 0, list.getWidth() - 28, 17);
            this.photoLabel.setBounds(
                list.getWidth() - 28, 0, 25, 30);

            this.isLeaf = true;

            this.setToolTipText(getContactToolTip(contactItem));
        }
        else if (value instanceof MetaContactGroup)
        {
            this.setPreferredSize(new Dimension(20, 20));

            MetaContactGroup groupItem = (MetaContactGroup) value;

            this.nameLabel.setText(groupItem.getGroupName() 
                    + "  ( " + groupItem.countChildContacts() + " )");

            this.nameLabel.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.GROUPS_16x16_ICON)));

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            JLabel groupContentIndicator = new JLabel();

            if(((ContactListModel)list.getModel()).isGroupClosed(groupItem))
                photoLabel.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CLOSED_GROUP)));
            else
                photoLabel.setIcon(new ImageIcon(ImageLoader
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

            this.setToolTipText(getGroupToolTip(groupItem));
        }

//        this.add(buttonsPanel, BorderLayout.EAST);

        this.isSelected = isSelected;

        return this;
    }
    
    /**
     * Creates a rounded avatar image.
     * 
     * @param avatarBytes The bytes of the initial avatar image.
     * 
     * @return The rounded corner image.
     */
    private Image createRoundImage(byte[] avatarBytes)
    {
        BufferedImage destImage = null;

        try
        {
            InputStream in = new ByteArrayInputStream(avatarBytes);
            BufferedImage avatarImage = ImageIO.read(in);

            ImageIcon scaledImage = ImageUtils.scaleIconWithinBounds(
                new ImageIcon(avatarImage),
                AVATAR_WIDTH,
                AVATAR_HEIGHT);

            destImage
                = new BufferedImage(scaledImage.getIconWidth(),
                                    scaledImage.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = destImage.createGraphics();
            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, 
                            scaledImage.getIconWidth(),
                            scaledImage.getIconHeight(),
                            10, 10);
            g.setComposite(AlphaComposite.SrcIn);

            g.drawImage(scaledImage.getImage(), 0, 0, null);
        }
        catch (Exception e)
        {
            logger.error("Could not create image.", e);
        }

        return destImage;
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected. 
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        if (!this.isLeaf)
        {
            GradientPaint p = new GradientPaint(this.getWidth()/2, 0,
                Constants.CONTACT_LIST_GROUP_BG_COLOR,
                this.getWidth()/2,
                this.getHeight(),
                Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR);

            g2.setPaint(p);

            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }
        else if (index%2 > 0)
        {
            Color bgColor = new Color(GuiActivator.getResources()
                    .getColor("contactListRowColor"), true);

            g2.setColor(new Color(  bgColor.getRed(),
                                    bgColor.getGreen(),
                                    bgColor.getBlue(),
                                    rowTransparency));
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        }

        if (this.isSelected)
        {
            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 1, this.getHeight() - 1,
                    7, 7);
        }
    }

    /**
     * 
     * @param contactItem
     */
    private String getContactToolTip(MetaContact contactItem)
    {
        String toolTipText = "<html>";

        toolTipText += "<b>"+contactItem.getDisplayName()+"</b>";

        Iterator<Contact> i = contactItem.getContacts();

        while (i.hasNext())
        {
            Contact protocolContact = (Contact) i.next();

            Image protocolStatusIcon
                = ImageLoader.getBytesInImage(
                        protocolContact.getPresenceStatus().getStatusIcon());

            String contactDisplayName = protocolContact.getDisplayName();
            String contactAddress = protocolContact.getAddress();
            String statusMessage = protocolContact.getStatusMessage();

            toolTipText
                += "<br>"
                    + ((!contactDisplayName
                            .equals(contactAddress))
                        ? contactDisplayName + " ("+contactAddress + ")"
                                : contactDisplayName)
                    + ((statusMessage != null && statusMessage.length() > 0) 
                        ? " - " + statusMessage : "");
        }

        toolTipText += "</html>";

        return toolTipText;
    }
    
    private String getGroupToolTip(MetaContactGroup groupItem)
    {
        String toolTipText = "<html>";
        toolTipText += groupItem.getGroupName();
        toolTipText += "</html>";

        return toolTipText;
    }
}