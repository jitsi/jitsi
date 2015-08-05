/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * Jitsi's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell. The cell
 * border and background are repainted.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ContactListCellRenderer
    extends JPanel
    implements  ListCellRenderer,
                Icon,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The avatar icon height.
     */
    private static final int AVATAR_HEIGHT = 30;

    /**
     * The avatar icon width.
     */
    private static final int AVATAR_WIDTH = 30;

    /**
     * The key of the user data in <tt>MetaContact</tt> which specifies
     * the avatar cached from previous invocations.
     */
    private static final String AVATAR_DATA_KEY
        = ContactListCellRenderer.class.getName() + ".avatar";

    /**
     * The icon indicating an open group.
     */
    private ImageIcon openedGroupIcon =
        new ImageIcon(ImageLoader.getImage(ImageLoader.OPENED_GROUP_ICON));

    /**
     * The icon indicating a closed group.
     */
    private ImageIcon closedGroupIcon =
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSED_GROUP_ICON));

    /**
     * The foreground color for groups.
     */
    private Color groupForegroundColor;

    /**
     * The foreground color for contacts.
     */
    protected Color contactForegroundColor;

    /**
     * The component showing the name of the contact or group.
     */
    protected final JLabel nameLabel = new JLabel();

    /**
     * The status message label.
     */
    protected final JLabel statusMessageLabel = new JLabel();

    /**
     * The component showing the avatar or the contact count in the case of
     * groups.
     */
    protected final JLabel rightLabel = new JLabel();

    /**
     * An icon indicating that a new message has been received from the
     * corresponding contact.
     */
    private final Image msgReceivedImage =
        ImageLoader.getImage(ImageLoader.MESSAGE_RECEIVED_ICON);

    /**
     * The label containing the status icon.
     */
    private final JLabel statusLabel = new JLabel();

    /**
     * The icon showing the contact status.
     */
    protected final ImageIcon statusIcon = new ImageIcon();

    /**
     * The panel containing the name and status message labels.
     */
    private final TransparentPanel centerPanel
        = new TransparentPanel(new GridLayout(0, 1));

    /**
     * Indicates if the current list cell is selected.
     */
    protected boolean isSelected = false;

    /**
     * The index of the current cell.
     */
    protected int index = 0;

    /**
     * Indicates if the current cell contains a leaf or a group.
     */
    protected boolean isLeaf = true;

    /**
     * Initializes the panel containing the node.
     */
    public ContactListCellRenderer()
    {
        super(new BorderLayout());

        int groupForegroundProperty = GuiActivator.getResources()
            .getColor("service.gui.CONTACT_LIST_GROUP_FOREGROUND");

        if (groupForegroundProperty > -1)
            groupForegroundColor = new Color (groupForegroundProperty);

        int contactForegroundProperty = GuiActivator.getResources()
                .getColor("service.gui.CONTACT_LIST_CONTACT_FOREGROUND");

        if (contactForegroundProperty > -1)
            contactForegroundColor = new Color(contactForegroundProperty);

        this.setOpaque(false);
        this.nameLabel.setOpaque(false);
        this.nameLabel.setPreferredSize(new Dimension(10, 20));

        this.statusMessageLabel.setFont(getFont().deriveFont(9f));
        this.statusMessageLabel.setForeground(Color.GRAY);

        this.rightLabel.setFont(rightLabel.getFont().deriveFont(9f));
        this.rightLabel.setHorizontalAlignment(JLabel.RIGHT);

        centerPanel.add(nameLabel);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        this.add(statusLabel, BorderLayout.WEST);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(rightLabel, BorderLayout.EAST);

        this.setToolTipText("");
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method. Returns this panel that
     * has been configured to display the meta contact and meta contact group
     * cells.
     *
     * @param list the source list
     * @param value the value of the current cell
     * @param index the index of the current cell in the source list
     * @param isSelected indicates if this cell is selected
     * @param cellHasFocus indicates if this cell is focused
     *
     * @return this panel
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        this.index = index;

        this.rightLabel.setIcon(null);

        DefaultContactList contactList = (DefaultContactList) list;

        if (value instanceof MetaContact)
        {
            this.setPreferredSize(new Dimension(20, 30));

            MetaContact metaContact = (MetaContact) value;

            String displayName = metaContact.getDisplayName();

            if (displayName == null || displayName.length() < 1)
            {
                displayName = GuiActivator.getResources()
                    .getI18NString("service.gui.UNKNOWN");
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

            this.statusLabel.setIcon(statusIcon);

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            if (contactForegroundColor != null)
                this.nameLabel.setForeground(contactForegroundColor);

            String statusMessage = getStatusMessage(metaContact);
            if (getStatusMessage(metaContact) != null)
            {
                statusMessageLabel.setText(statusMessage);
                centerPanel.add(statusMessageLabel);
            }
            else
                centerPanel.remove(statusMessageLabel);

            this.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));

            ImageIcon avatar = getAvatar(metaContact);
            if (avatar != null)
                this.rightLabel.setIcon(avatar);
            this.rightLabel.setText("");

            // We should set the bounds of the cell explicitly in order to
            // make getComponentAt work properly.
            final int listWidth = list.getWidth();
            this.setBounds(0, 0, listWidth - 2, 30);

            this.nameLabel.setBounds(0, 0, listWidth - 28, 17);
            this.rightLabel.setBounds(listWidth - 28, 0, 25, 30);

            this.isLeaf = true;
        }
        else if (value instanceof MetaContactGroup)
        {
            this.setPreferredSize(new Dimension(20, 20));

            MetaContactGroup groupItem = (MetaContactGroup) value;

            this.nameLabel.setText(groupItem.getGroupName());

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            if (groupForegroundColor != null)
                this.nameLabel.setForeground(groupForegroundColor);

            centerPanel.remove(statusMessageLabel);

            this.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

            // We should set the bounds of the cell explicitly in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            this.statusLabel.setIcon(
                    contactList.isGroupClosed(groupItem)
                        ? closedGroupIcon
                        : openedGroupIcon);

            // We have no photo icon for groups.
            this.rightLabel.setIcon(null);
            this.rightLabel.setText( groupItem.countOnlineChildContacts()
                    + "/" + groupItem.countChildContacts());

            this.isLeaf = false;
        }
        else if (value instanceof String)
        {
            this.setPreferredSize(new Dimension(20, 30));
            this.nameLabel.setText((String) value);
            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
        }
        else
        {
            this.setPreferredSize(new Dimension(20, 30));
            this.nameLabel.setText(value.toString());
            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
        }

        this.isSelected = isSelected;

        return this;
    }

    /**
     * Gets the avatar of a specific <tt>MetaContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param metaContact the <tt>MetaContact</tt> to retrieve the avatar of
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    private ImageIcon getAvatar(MetaContact metaContact)
    {
        byte[] avatarBytes = metaContact.getAvatar(true);
        ImageIcon avatar = null;

        // Try to get the avatar from the cache.
        Object[] avatarCache = (Object[]) metaContact.getData(AVATAR_DATA_KEY);
        if ((avatarCache != null) && (avatarCache[0] == avatarBytes))
            avatar = (ImageIcon) avatarCache[1];

        // If the avatar isn't available or it's not up-to-date, create it.
        if ((avatar == null)
                && (avatarBytes != null) && (avatarBytes.length > 0))
            avatar
                = ImageUtils.getScaledRoundedIcon(
                        avatarBytes,
                        AVATAR_WIDTH,
                        AVATAR_HEIGHT);

        // Cache the avatar in case it has changed.
        if (avatarCache == null)
        {
            if (avatar != null)
                metaContact.setData(
                    AVATAR_DATA_KEY,
                    new Object[] { avatarBytes, avatar });
        }
        else
        {
            avatarCache[0] = avatarBytes;
            avatarCache[1] = avatar;
        }

        return avatar;
    }

    /**
     * Returns the first found status message for the given
     * <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, for which we'd like to
     * obtain a status message
     * @return the first found status message for the given
     * <tt>metaContact</tt>
     */
    private String getStatusMessage(MetaContact metaContact)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            String statusMessage = protoContact.getStatusMessage();
            if (statusMessage != null && statusMessage.length() > 0)
                return statusMessage;
        }
        return null;
    }

    /**
     * Paints a customized background.
     *
     * @param g the <tt>Graphics</tt> object through which we paint
     */
    @Override
    protected void paintComponent(Graphics g)
    {
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

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     *
     * @param g the <tt>Graphics</tt> object through which we paint
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (!this.isLeaf)
        {
            final int width = getWidth();
            GradientPaint p =
                new GradientPaint(0, 0, Constants.CONTACT_LIST_GROUP_BG_COLOR,
                    width - 5, 0,
                    Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR);

            g2.setPaint(p);
            g2.fillRoundRect(1, 1, width - 2, this.getHeight() - 1, 10, 10);
        }

        if (this.isSelected)
        {
            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRoundRect(   1, 1,
                                this.getWidth() - 2, this.getHeight() - 1,
                                10, 10);
        }
    }

    /**
     * Returns the height of this icon.
     * @return the height of this icon
     */
    public int getIconHeight()
    {
        return this.getHeight() + 10;
    }

    /**
     * Returns the width of this icon.
     * @return the widht of this icon
     */
    public int getIconWidth()
    {
        return this.getWidth() + 10;
    }

    /**
     * Draw the icon at the specified location. Paints this component as an
     * icon.
     * @param c the component which can be used as observer
     * @param g the <tt>Graphics</tt> object used for painting
     * @param x the position on the X coordinate
     * @param y the position on the Y coordinate
     */
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g2);

            g2.setColor(Color.WHITE);
            g2.setComposite(AlphaComposite.
                getInstance(AlphaComposite.SRC_OVER, 0.8f));
            g2.fillRoundRect(x, y,
                            getIconWidth() - 1, getIconHeight() - 1,
                            10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y,
                            getIconWidth() - 1, getIconHeight() - 1,
                            10, 10);

            // Indent component content from the border.
            g2.translate(x + 5, y + 5);

            // Paint component.
            super.paint(g2);

            //
            g2.translate(x, y);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Reloads skin information for this render class.
     */
    public void loadSkin()
    {
        openedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON));

        closedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSED_GROUP_ICON));

        int groupForegroundProperty = GuiActivator.getResources()
            .getColor("service.gui.CONTACT_LIST_GROUP_FOREGROUND");

        if (groupForegroundProperty > -1)
            groupForegroundColor = new Color (groupForegroundProperty);

        int contactForegroundProperty = GuiActivator.getResources()
                .getColor("service.gui.CONTACT_LIST_CONTACT_FOREGROUND");

        if (contactForegroundProperty > -1)
            contactForegroundColor = new Color(contactForegroundProperty);
    }
}
