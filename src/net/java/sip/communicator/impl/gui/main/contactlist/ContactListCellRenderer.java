/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
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

    private ImageIcon openedGroupIcon = new ImageIcon(ImageLoader
        .getImage(ImageLoader.DOWN_ARROW_ICON));

    private ImageIcon closedGroupIcon = new ImageIcon(ImageLoader
        .getImage(ImageLoader.RIGHT_ARROW_ICON));

    private Color groupForegroundColor;

    protected Color contactForegroundColor;

    protected JLabel nameLabel = new JLabel();

    protected JLabel photoLabel = new JLabel();

    private int rowTransparency
        = GuiActivator.getResources()
            .getSettingsInt("contactListRowTransparency");

    private Image msgReceivedImage
        = ImageLoader.getImage(ImageLoader.MESSAGE_RECEIVED_ICON);

    protected ImageIcon statusIcon = new ImageIcon();

    protected boolean isSelected = false;

    protected int index = 0;

    protected boolean isLeaf = true;

    /**
     * Initialize the panel containing the node.
     */
    public ContactListCellRenderer()
    {
        super(new BorderLayout());

        int groupForegroundProperty = GuiActivator.getResources()
            .getColor("contactListGroupForeground");

        if (groupForegroundProperty > -1)
            groupForegroundColor = new Color (groupForegroundProperty);

        int contactForegroundProperty = GuiActivator.getResources()
                .getColor("contactListContactForeground");

        if (contactForegroundProperty > -1)
            contactForegroundColor = new Color(contactForegroundProperty);

        this.setOpaque(false);
        this.nameLabel.setOpaque(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.nameLabel.setIconTextGap(2);

        this.nameLabel.setPreferredSize(new Dimension(10, 17));

        this.add(nameLabel, BorderLayout.CENTER);
        this.add(photoLabel, BorderLayout.EAST);

        this.setToolTipText("");
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

        if (value instanceof MetaContact)
        {
            this.setPreferredSize(new Dimension(20, 30));

            MetaContact metaContact = (MetaContact) value;

            String displayName = metaContact.getDisplayName();

            if (displayName == null || displayName.length() < 1)
            {
                displayName = Messages.getI18NString("unknown").getText();
            }

            this.nameLabel.setText(displayName);

            if(contactList.isMetaContactActive(metaContact))
            {
                statusIcon.setImage(msgReceivedImage);
            }
            else
            {
                statusIcon.setImage(Constants.getStatusIcon(
                    contactList.getMetaContactStatus(metaContact)));
            }

            this.nameLabel.setIcon(statusIcon);

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            if (contactForegroundColor != null)
                this.nameLabel.setForeground(contactForegroundColor);

            this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 3));

            byte[] avatar = metaContact.getAvatar(true);
            if (avatar != null && avatar.length > 0)
            {
                ImageIcon roundedAvatar
                    = ImageUtils.getScaledRoundedImage( avatar,
                                                        AVATAR_WIDTH,
                                                        AVATAR_HEIGHT);

                if (roundedAvatar != null)
                    this.photoLabel.setIcon(roundedAvatar);
            }

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 30);

            this.nameLabel.setBounds(
                    0, 0, list.getWidth() - 28, 17);
            this.photoLabel.setBounds(
                list.getWidth() - 28, 0, 25, 30);

            this.isLeaf = true;
        }
        else if (value instanceof MetaContactGroup)
        {
            this.setPreferredSize(new Dimension(20, 20));

            MetaContactGroup groupItem = (MetaContactGroup) value;

            this.nameLabel.setText(groupItem.getGroupName() 
                    + "  ( " + groupItem.countChildContacts() + " )");

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            if (groupForegroundColor != null)
                this.nameLabel.setForeground(groupForegroundColor);

            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            if(((ContactListModel)list.getModel()).isGroupClosed(groupItem))
                this.nameLabel.setIcon(closedGroupIcon);
            else
                this.nameLabel.setIcon(openedGroupIcon);

            // We have no photo icon for groups.
            this.photoLabel.setIcon(null);

            this.isLeaf = false;
        }

        this.isSelected = isSelected;

        return this;
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
            GradientPaint p = new GradientPaint(0,
                0,
                Constants.CONTACT_LIST_GROUP_BG_COLOR,
                this.getWidth() - 5,
                0,
                Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR);

            g2.setPaint(p);

            g2.fillRoundRect(   1, 1,
                                this.getWidth() - 2, this.getHeight() - 1,
                                10, 10);
        }
        else if (index%2 > 0)
        {
            Color bgColor = new Color(GuiActivator.getResources()
                    .getColor("contactListRowColor"), true);

            g2.setColor(new Color(  bgColor.getRed(),
                                    bgColor.getGreen(),
                                    bgColor.getBlue(),
                                    rowTransparency));
        }

        if (this.isSelected)
        {
            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRoundRect(   1, 1,
                                this.getWidth() - 2, this.getHeight() - 1,
                                10, 10);
        }
    }
}
