/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.JPopupMenu.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

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
public class ContactListTreeCellRenderer
    extends JPanel
    implements  TreeCellRenderer,
                Icon,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The default height of the avatar.
     */
    private static final int AVATAR_HEIGHT = 30;

    /**
     * The default width of the avatar.
     */
    private static final int AVATAR_WIDTH = 30;

    /**
     * The extended height of the avatar.
     */
    private static final int EXTENDED_AVATAR_HEIGHT = 45;

    /**
     * The extended width of the avatar.
     */
    private static final int EXTENDED_AVATAR_WIDTH = 45;

    /**
     * Left border value.
     */
    private static final int LEFT_BORDER = 5;

    /**
     * Status label right border.
     */
    private static final int STATUS_RIGHT_BORDER = 2;

    /**
     * The icon used for opened groups.
     */
    private ImageIcon openedGroupIcon;

    /**
     * The icon used for closed groups.
     */
    private ImageIcon closedGroupIcon;

    /**
     * The foreground color for groups.
     */
    private Color groupForegroundColor;

    /**
     * The foreground color for contacts.
     */
    private Color contactForegroundColor;

    /**
     * The component showing the name of the contact or group.
     */
    private final JLabel nameLabel = new JLabel();

    /**
     * The status message label.
     */
    private final JLabel displayDetailsLabel = new JLabel();

    /**
     * The call button.
     */
    private final SIPCommButton callButton = new SIPCommButton();

    /**
     * The call video button.
     */
    private final SIPCommButton callVideoButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_SMALL),
        ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_SMALL_PRESSED),
        null);

    /**
     * The desktop sharing button.
     */
    private final SIPCommButton desktopSharingButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.DESKTOP_BUTTON_SMALL),
        ImageLoader.getImage(ImageLoader.DESKTOP_BUTTON_SMALL_PRESSED),
        null);

    /**
     * The chat button.
     */
    private final SIPCommButton chatButton = new SIPCommButton();

    /**
     * The add contact button.
     */
    private final SIPCommButton addContactButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.ADD_CONTACT_BUTTON_SMALL),
        ImageLoader.getImage(ImageLoader.ADD_CONTACT_BUTTON_SMALL_PRESSED),
        null);

    /**
     * The constraints used to align components in the <tt>centerPanel</tt>.
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * The component showing the avatar or the contact count in the case of
     * groups.
     */
    protected final JLabel rightLabel = new JLabel();

    /**
     * The message received image.
     */
    private Image msgReceivedImage;

    /**
     * The label containing the status icon.
     */
    private final JLabel statusLabel = new JLabel();

    /**
     * The icon showing the contact status.
     */
    protected ImageIcon statusIcon = new ImageIcon();

    /**
     * Indicates if the current list cell is selected.
     */
    protected boolean isSelected = false;

    /**
     * The index of the current cell.
     */
    protected int row = 0;

    /**
     * Indicates if the current cell contains a leaf or a group.
     */
    protected TreeNode treeNode;

    /**
     * The parent tree.
     */
    private TreeContactList tree;

    /**
     * Initializes the panel containing the node.
     */
    public ContactListTreeCellRenderer()
    {
        super(new GridBagLayout());

        this.setBorder(BorderFactory.createEmptyBorder(2, LEFT_BORDER, 2, 2));

        loadSkin();

        this.setOpaque(true);
        this.nameLabel.setOpaque(false);

        this.displayDetailsLabel.setFont(getFont().deriveFont(9f));
        this.displayDetailsLabel.setForeground(Color.GRAY);

        this.rightLabel.setHorizontalAlignment(JLabel.RIGHT);

        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 0, 0, STATUS_RIGHT_BORDER));

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 1f;
        this.add(statusLabel, constraints);

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.weighty = 0f;
        constraints.gridheight = 1;
        constraints.gridwidth = 5;
        this.add(nameLabel, constraints);

        rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = 6;
        constraints.gridy = 0;
        constraints.gridheight = 3;
        constraints.weightx = 0f;
        constraints.weighty = 1f;
        this.add(rightLabel, constraints);

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    call(treeNode);
                }
            }
        });

        callVideoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    callVideo(treeNode);
                }
            }
        });

        desktopSharingButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    shareDesktop(treeNode);
                }
            }
        });

        chatButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    UIContact contactDescriptor
                        = ((ContactNode) treeNode).getContactDescriptor();

                    if (contactDescriptor.getDescriptor()
                            instanceof MetaContact)
                    {
                        GuiActivator.getUIService().getChatWindowManager()
                            .startChat(
                                (MetaContact) contactDescriptor.getDescriptor());
                    }
                }
            }
        });

        addContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    UIContact contactDescriptor
                        = ((ContactNode) treeNode).getContactDescriptor();

                    // The add contact function has only sense for external
                    // source contacts.
                    if (contactDescriptor instanceof SourceUIContact)
                    {
                        addContact((SourceUIContact) contactDescriptor);
                    }
                }
            }
        });

        this.setToolTipText("");
    }

    /**
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     *
     * @param tree the source tree
     * @param value the tree node
     * @param selected indicates if the node is selected
     * @param expanded indicates if the node is expanded
     * @param leaf indicates if the node is a leaf
     * @param row indicates the row number of the node
     * @param hasFocus indicates if the node has the focus
     * @return this panel
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus)
    {
        this.tree = (TreeContactList)tree;
        this.row = row;
        this.isSelected = selected;
        this.treeNode = (TreeNode) value;

        this.rightLabel.setIcon(null);

        DefaultTreeContactList contactList = (DefaultTreeContactList) tree;

        // Set background color.
        if (contactList instanceof TreeContactList)
        {
            ContactListFilter filter
                = ((TreeContactList) contactList).getCurrentFilter();

            if (filter != null
                && filter.equals(TreeContactList.historyFilter)
                && value instanceof ContactNode
                && row%2 == 0)
            {
                setBackground(Constants.CALL_HISTORY_EVEN_ROW_COLOR);
            }
            else
            {
                setBackground(Color.WHITE);
            }
        }

        // Make appropriate adjustments for contact nodes and group nodes.
        if (value instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) value).getContactDescriptor();

            String displayName = contact.getDisplayName();

            if ((displayName == null
                || displayName.trim().length() < 1)
                && !(contact instanceof ShowMoreContact))
            {
                displayName = GuiActivator.getResources()
                    .getI18NString("service.gui.UNKNOWN");
            }

            this.nameLabel.setText(displayName);

            if(statusIcon != null && contactList.isContactActive(contact))
                statusIcon.setImage(msgReceivedImage);
            else
                statusIcon = contact.getStatusIcon();
            this.statusLabel.setIcon(statusIcon);

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            if (contactForegroundColor != null)
                nameLabel.setForeground(contactForegroundColor);

            // Initializes status message components if the given meta contact
            // contains a status message.
            this.initDisplayDetails(contact);

            this.initButtonsPanel(contact);

            int avatarWidth, avatarHeight;

            if (isSelected)
            {
                avatarWidth = EXTENDED_AVATAR_WIDTH;
                avatarHeight = EXTENDED_AVATAR_HEIGHT;
            }
            else
            {
                avatarWidth = AVATAR_WIDTH;
                avatarHeight = AVATAR_HEIGHT;
            }

            ImageIcon avatar
                = contact.getAvatar(isSelected, avatarWidth, avatarHeight);

            if (avatar != null)
            {
                this.rightLabel.setIcon(avatar);
            }

            if (contact instanceof ShowMoreContact)
            {
                rightLabel.setFont(rightLabel.getFont().deriveFont(12f));
                rightLabel.setForeground(Color.GRAY);
                rightLabel.setText((String)contact.getDescriptor());
            }
            else
            {
                rightLabel.setFont(rightLabel.getFont().deriveFont(9f));
                rightLabel.setText("");
            }

            this.setToolTipText(contact.getDescriptor().toString());
        }
        else if (value instanceof GroupNode)
        {
            UIGroup groupItem
                = ((GroupNode) value).getGroupDescriptor();

            this.nameLabel.setText(groupItem.getDisplayName());

            this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

            if (groupForegroundColor != null)
                this.nameLabel.setForeground(groupForegroundColor);

            this.remove(displayDetailsLabel);
            this.remove(callButton);
            this.remove(callVideoButton);
            this.remove(desktopSharingButton);
            this.remove(chatButton);
            this.remove(addContactButton);

            this.statusLabel.setIcon(
                    expanded
                    ? openedGroupIcon
                    : closedGroupIcon);

            // We have no photo icon for groups.
            this.rightLabel.setIcon(null);
            this.rightLabel.setText("");

            if (groupItem.countChildContacts() >= 0)
            {
                rightLabel.setFont(rightLabel.getFont().deriveFont(9f));
                this.rightLabel.setForeground(Color.BLACK);
                this.rightLabel.setText( groupItem.countOnlineChildContacts()
                                        + "/" + groupItem.countChildContacts());
            }

            this.setToolTipText(groupItem.getDescriptor().toString());
        }

        return this;
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

        if (!(treeNode instanceof GroupNode) && !isSelected)
            return;

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        try
        {
            internalPaintComponent(g2);
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
     * @param g2 the <tt>Graphics2D</tt> object through which we paint
     */
    private void internalPaintComponent(Graphics2D g2)
    {
        Color borderColor = Color.GRAY;

        if (isSelected)
        {
            g2.setPaint(new GradientPaint(0, 0,
                Constants.SELECTED_COLOR, 0, getHeight(),
                Constants.SELECTED_GRADIENT_COLOR));

            borderColor = Constants.SELECTED_COLOR;
        }
        else if (treeNode instanceof GroupNode)
        {
            g2.setPaint(new GradientPaint(0, 0,
                Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR,
                0, getHeight(),
                Constants.CONTACT_LIST_GROUP_BG_COLOR));

            borderColor = Constants.CONTACT_LIST_GROUP_BG_COLOR;
        }

        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(borderColor);
        g2.drawLine(0, 0, getWidth(), 0);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }

    /**
     * Returns the height of this icon. Used for the drag&drop component.
     * @return the height of this icon
     */
    public int getIconHeight()
    {
        return getPreferredSize().height + 10;
    }

    /**
     * Returns the width of this icon. Used for the drag&drop component.
     * @return the widht of this icon
     */
    public int getIconWidth()
    {
        return tree.getWidth() + 10;
    }

    /**
     * Returns the preferred size of this component.
     * @return the preferred size of this component
     */
    public Dimension getPreferredSize()
    {
        Dimension preferredSize = new Dimension();

        if (treeNode instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) treeNode).getContactDescriptor();

            if (contact instanceof ShowMoreContact)
                preferredSize.height = 18;
            else if (isSelected)
                preferredSize.height = 70;
            else
                preferredSize.height = 30;
        }
        else
            preferredSize.height = 18;

        return preferredSize;
    }

    /**
     * Initializes the display details component for the given
     * <tt>UIContact</tt>.
     * @param contact the <tt>UIContact</tt>, for which we initialize the
     * details component
     */
    private void initDisplayDetails(UIContact contact)
    {
        displayDetailsLabel.setText("");
        this.remove(displayDetailsLabel);

        String displayDetails = contact.getDisplayDetails();

        if (displayDetails != null && displayDetails.length() > 0)
        {
            // Replace all occurrences of new line with slash.
            displayDetails = Html2Text.extractText(displayDetails);
            displayDetails = displayDetails.replaceAll("\n|<br>|<br/>", " / ");

            displayDetailsLabel.setText(displayDetails);

            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            constraints.gridwidth = 5;
            constraints.gridheight = 1;

            this.add(displayDetailsLabel, constraints);
        }
    }

    /**
     * Initializes buttons panel.
     * @param uiContact the <tt>UIContact</tt> for which we initialize the
     * button panel
     */
    private void initButtonsPanel(UIContact uiContact)
    {
        this.remove(chatButton);
        this.remove(callButton);
        this.remove(callVideoButton);
        this.remove(desktopSharingButton);
        this.remove(addContactButton);

        if (!isSelected)
            return;

        int statusMessageLabelHeight = 0;
        if (displayDetailsLabel.getText().length() > 0)
            statusMessageLabelHeight = 20;
        else
            statusMessageLabelHeight = 15;

        UIContactDetail imContact = null;
        // For now we support instance messaging only for contacts in our
        // contact list until it's implemented for external source contacts.
        if (uiContact.getDescriptor() instanceof MetaContact)
            imContact = uiContact.getDefaultContactDetail(
                         OperationSetBasicInstantMessaging.class);

        int x = (statusIcon == null ? 0 : statusIcon.getIconWidth())
                + (statusLabel == null ? 0 : statusLabel.getIconTextGap())
                + LEFT_BORDER
                + STATUS_RIGHT_BORDER;

        if (imContact != null)
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            this.chatButton.setBorder(null);
            this.add(chatButton, constraints);

            chatButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight,
                28, 28);

            x += chatButton.getWidth();
        }

        UIContactDetail telephonyContact
            = uiContact.getDefaultContactDetail(
                OperationSetBasicTelephony.class);

        // Check if contact has additional phone numbers, if yes show the
        // call button
        boolean hasPhone = false;

        // check for phone stored in contact info only
        // if telephony contact is missing
        if(uiContact.getDescriptor() != null
           && uiContact.getDescriptor() instanceof MetaContact
           && telephonyContact == null)
        {
            MetaContact metaContact =
                (MetaContact)uiContact.getDescriptor();
            Iterator<Contact> contacts = metaContact.getContacts();

            while(contacts.hasNext() && !hasPhone)
            {
                Contact contact = contacts.next();

                if(!contact.getProtocolProvider().isRegistered())
                    continue;

                OperationSetServerStoredContactInfo infoOpSet =
                    contact.getProtocolProvider().getOperationSet(
                        OperationSetServerStoredContactInfo.class);
                Iterator<GenericDetail> details;

                if(infoOpSet != null)
                {
                    details = infoOpSet.requestAllDetailsForContact(
                        contact,
                        new DetailsListener(treeNode, callButton, uiContact));

                    if(details != null)
                    {
                        while(details.hasNext())
                        {
                            GenericDetail d = details.next();
                            if(d instanceof PhoneNumberDetail &&
                                !(d instanceof PagerDetail) &&
                                !(d instanceof FaxDetail))
                            {
                                PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                                if(pnd.getNumber() != null &&
                                    pnd.getNumber().length() > 0)
                                {
                                    hasPhone = true;
                                    break;
                                }
                             }
                        }
                    }
                }
            }
        }

        // for SourceContact in history that do not support telephony, we
        // show the button but disabled
        List<ProtocolProviderService> providers
            = GuiActivator.getOpSetRegisteredProviders(
                OperationSetBasicTelephony.class,
                null,
                null);

        if ((telephonyContact != null && telephonyContact.getAddress() != null) ||
            uiContact.getDescriptor() instanceof SourceContact ||
            (hasPhone && providers.size() > 0))
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 2;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            this.callButton.setBorder(null);
            this.add(callButton, constraints);

            callButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight, 28, 28);
            callButton.setEnabled(telephonyContact != null || hasPhone);

            x += callButton.getWidth();
        }

        UIContactDetail videoContact
            = uiContact.getDefaultContactDetail(
                OperationSetVideoTelephony.class);

        if (videoContact != null
            || (ConfigurationManager
                    .isRouteVideoAndDesktopUsingPhoneNumberEnabled()
                    && hasPhone
                    && GuiActivator.getOpSetRegisteredProviders(
                                    OperationSetVideoTelephony.class,
                                    null,
                                    null).size() > 0))
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 3;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            this.callVideoButton.setBorder(null);
            this.add(callVideoButton, constraints);

            callVideoButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight, 28, 28);

            x += callVideoButton.getWidth();
        }

        UIContactDetail desktopContact
            = uiContact.getDefaultContactDetail(
                OperationSetDesktopSharingServer.class);

        if (desktopContact != null
            || (ConfigurationManager
                    .isRouteVideoAndDesktopUsingPhoneNumberEnabled()
                    && hasPhone
                    && GuiActivator.getOpSetRegisteredProviders(
                            OperationSetDesktopSharingServer.class,
                            null,
                            null).size() > 0))
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 4;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            this.desktopSharingButton.setBorder(null);
            this.add(desktopSharingButton, constraints);

            desktopSharingButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight, 28, 28);

            x += desktopSharingButton.getWidth();
        }

        if (uiContact.getDescriptor() instanceof SourceContact
            && !ConfigurationManager.isAddContactDisabled())
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 5;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0f;
            constraints.weighty = 0f;
            this.addContactButton.setBorder(null);
            this.add(addContactButton, constraints);

            addContactButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight, 28, 28);

            x += addContactButton.getWidth();
        }

        this.setBounds(0, 0, tree.getWidth(), getPreferredSize().height);
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
        g = g.create();
        try
        {
            Graphics2D g2 = (Graphics2D) g;
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

            super.paint(g2);

            g2.translate(x, y);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Returns the call button contained in the current cell.
     * @return the call button contained in the current cell
     */
    public JButton getCallButton()
    {
        return callButton;
    }

    /**
     * Returns the call video button contained in the current cell.
     * @return the call video button contained in the current cell
     */
    public JButton getCallVideoButton()
    {
        return callVideoButton;
    }

    /**
     * Returns the desktop sharing button contained in the current cell.
     * @return the desktop sharing button contained in the current cell
     */
    public JButton getDesktopSharingButton()
    {
        return desktopSharingButton;
    }

    /**
     * Returns the add contact button contained in the current cell.
     * @return the add contact button contained in the current cell
     */
    public JButton getAddContactButton()
    {
        return addContactButton;
    }

    /**
     * Calls the given treeNode.
     * @param treeNode the <tt>TreeNode</tt> to call
     */
    private void call(TreeNode treeNode)
    {
        List<UIContactDetail> telephonyContacts
            = ((ContactNode) treeNode).getContactDescriptor()
                .getContactDetailsForOperationSet(
                    OperationSetBasicTelephony.class);

        telephonyContacts.addAll(getAdditionalNumbers());

        ChooseCallAccountPopupMenu chooseAccountDialog = null;

        if (telephonyContacts.size() == 1)
        {
            UIContactDetail detail = telephonyContacts.get(0);

            ProtocolProviderService preferredProvider
                = detail.getPreferredProtocolProvider(
                    OperationSetBasicTelephony.class);

            List<ProtocolProviderService> providers
                = GuiActivator.getOpSetRegisteredProviders(
                    OperationSetBasicTelephony.class,
                    preferredProvider,
                    detail.getPreferredProtocol(
                        OperationSetBasicTelephony.class));

            if (providers != null)
            {
                int providersCount = providers.size();

                if (providersCount <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                    .showDialog();
                }
                else if (providersCount == 1)
                {
                    CallManager.createCall(
                        providers.get(0), detail.getAddress());
                }
                else if (providersCount > 1)
                    chooseAccountDialog = new ChooseCallAccountPopupMenu(
                            tree, detail.getAddress(), providers);
            }
        }
        else if (telephonyContacts.size() > 1)
        {
            chooseAccountDialog
                = new ChooseCallAccountPopupMenu(tree, telephonyContacts);
        }

        // If the choose dialog is created we're going to show it.
        if (chooseAccountDialog != null)
        {
            Point location = new Point(callButton.getX(),
                callButton.getY() + callButton.getHeight());

            SwingUtilities.convertPointToScreen(location, tree);

            location.y = location.y
                + tree.getPathBounds(tree.getSelectionPath()).y;

            chooseAccountDialog.showPopupMenu(location.x + 8, location.y - 8);
        }
    }

    /**
     * Calls the given treeNode with video option enabled.
     * @param treeNode the <tt>TreeNode</tt> to call
     */
    private void callVideo(TreeNode treeNode)
    {
        List<UIContactDetail> videoContacts
            = ((ContactNode) treeNode).getContactDescriptor()
                .getContactDetailsForOperationSet(
                    OperationSetVideoTelephony.class);

        if(ConfigurationManager.isRouteVideoAndDesktopUsingPhoneNumberEnabled())
            videoContacts.addAll(getAdditionalNumbers());

        ChooseCallAccountPopupMenu chooseAccountDialog = null;

        if (videoContacts.size() == 1)
        {
            UIContactDetail detail = videoContacts.get(0);

            ProtocolProviderService preferredProvider
                = detail.getPreferredProtocolProvider(
                    OperationSetVideoTelephony.class);

            List<ProtocolProviderService> providers = null;
            String protocolName = null;

            if (preferredProvider != null)
            {
                if (preferredProvider.isRegistered())
                    CallManager.createVideoCall(
                        preferredProvider, detail.getAddress());
                // If we have a provider, but it's not registered we try to
                // obtain all registered providers for the same protocol as the
                // given preferred provider.
                else
                {
                    protocolName = preferredProvider.getProtocolName();
                    providers = GuiActivator.getRegisteredProviders(protocolName,
                        OperationSetVideoTelephony.class);
                }
            }
            // If we don't have a preferred provider we try to obtain a
            // preferred protocol name and all registered providers for it.
            else
            {
                protocolName = detail
                    .getPreferredProtocol(OperationSetVideoTelephony.class);

                if (protocolName != null)
                    providers
                        = GuiActivator.getRegisteredProviders(protocolName,
                            OperationSetVideoTelephony.class);
                else
                    providers
                        = GuiActivator.getRegisteredProviders(
                            OperationSetVideoTelephony.class);
            }

            // If our call didn't succeed, try to call through one of the other
            // protocol providers obtained above.
            if (providers != null)
            {
                int providersCount = providers.size();

                if (providersCount <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT",
                            new String[]{protocolName}))
                    .showDialog();
                }
                else if (providersCount == 1)
                {
                    CallManager.createVideoCall(
                        providers.get(0), detail.getAddress());
                }
                else if (providersCount > 1)
                    chooseAccountDialog = new ChooseCallAccountPopupMenu(
                            tree, detail.getAddress(), providers,
                            OperationSetVideoTelephony.class);
            }
        }
        else if (videoContacts.size() > 1)
        {
            chooseAccountDialog
                = new ChooseCallAccountPopupMenu(tree, videoContacts,
                    OperationSetVideoTelephony.class);
        }

        // If the choose dialog is created we're going to show it.
        if (chooseAccountDialog != null)
        {
            Point location = new Point(callVideoButton.getX(),
                callVideoButton.getY() + callVideoButton.getHeight());

            SwingUtilities.convertPointToScreen(location, tree);

            location.y = location.y
                + tree.getPathBounds(tree.getSelectionPath()).y;

            chooseAccountDialog.showPopupMenu(location.x + 8, location.y - 8);
        }
    }

    /**
     * Shares the user desktop with the contact contained in the given
     * <tt>treeNode</tt>.
     * @param treeNode the <tt>TreeNode</tt>, containing the contact to share
     * the desktop with
     */
    private void shareDesktop(TreeNode treeNode)
    {
        List<UIContactDetail> desktopContacts
            = ((ContactNode) treeNode).getContactDescriptor()
                .getContactDetailsForOperationSet(
                    OperationSetDesktopSharingServer.class);

        if(ConfigurationManager.isRouteVideoAndDesktopUsingPhoneNumberEnabled())
            desktopContacts.addAll(getAdditionalNumbers());

        ChooseCallAccountPopupMenu chooseAccountDialog = null;

        if (desktopContacts.size() == 1)
        {
            UIContactDetail detail = desktopContacts.get(0);

            ProtocolProviderService preferredProvider
                = detail.getPreferredProtocolProvider(
                    OperationSetDesktopSharingServer.class);

            List<ProtocolProviderService> providers = null;
            String protocolName = null;

            if (preferredProvider != null)
            {
                if (preferredProvider.isRegistered())
                    shareDesktop(preferredProvider, detail.getAddress());

                // If we have a provider, but it's not registered we try to
                // obtain all registered providers for the same protocol as the
                // given preferred provider.
                else
                {
                    protocolName = preferredProvider.getProtocolName();
                    providers
                        = GuiActivator.getRegisteredProviders(protocolName,
                            OperationSetDesktopSharingServer.class);
                }
            }
            // If we don't have a preferred provider we try to obtain a
            // preferred protocol name and all registered providers for it.
            else
            {
                protocolName = detail.getPreferredProtocol(
                        OperationSetDesktopSharingServer.class);

                if (protocolName != null)
                    providers
                        = GuiActivator.getRegisteredProviders(protocolName,
                            OperationSetDesktopSharingServer.class);
                else
                    providers
                        = GuiActivator.getRegisteredProviders(
                            OperationSetDesktopSharingServer.class);
            }

            // If our call didn't succeed, try to call through one of the other
            // protocol providers obtained above.
            if (providers != null)
            {
                int providersCount = providers.size();

                if (providersCount <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT",
                            new String[]{protocolName}))
                    .showDialog();
                }
                else if (providersCount == 1)
                {
                    shareDesktop(providers.get(0), detail.getAddress());
                }
                else if (providersCount > 1)
                    chooseAccountDialog = new ChooseCallAccountPopupMenu(
                            tree, detail.getAddress(), providers,
                            OperationSetDesktopSharingServer.class);
            }
        }
        else if (desktopContacts.size() > 1)
        {
            chooseAccountDialog
                = new ChooseCallAccountPopupMenu(tree, desktopContacts,
                    OperationSetDesktopSharingServer.class);
        }

        // If the choose dialog is created we're going to show it.
        if (chooseAccountDialog != null)
        {
            Point location = new Point(desktopSharingButton.getX(),
                desktopSharingButton.getY() + desktopSharingButton.getHeight());

            SwingUtilities.convertPointToScreen(location, tree);

            location.y = location.y
                + tree.getPathBounds(tree.getSelectionPath()).y;

            chooseAccountDialog.showPopupMenu(location.x + 8, location.y - 8);
        }
    }

    /**
     * Searches for additional phone numbers found in contact information
     * @return additional phone numbers found in contact information;
     */
    private List<UIContactDetail> getAdditionalNumbers()
    {
        List<UIContactDetail> telephonyContacts
            = new ArrayList<UIContactDetail>();

        // Adds additional phone numbers found in contact information
        ContactNode n = (ContactNode)treeNode;
        MetaContact metaContact = null;

        if(n.getContactDescriptor().getDescriptor() instanceof MetaContact)
        {
            metaContact = (MetaContact)n.getContactDescriptor().getDescriptor();
            Iterator<Contact> contacts = metaContact.getContacts();

            while(contacts.hasNext())
            {
                Contact contact = contacts.next();
                OperationSetServerStoredContactInfo infoOpSet =
                    contact.getProtocolProvider().getOperationSet(
                        OperationSetServerStoredContactInfo.class);
                Iterator<GenericDetail> details;
                ArrayList<String> phones = new ArrayList<String>();

                if(infoOpSet != null)
                {
                    details = infoOpSet.getAllDetailsForContact(contact);

                    while(details.hasNext())
                    {
                        GenericDetail d = details.next();
                        if(d instanceof PhoneNumberDetail &&
                            !(d instanceof PagerDetail) &&
                            !(d instanceof FaxDetail))
                        {
                            PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                            if(pnd.getNumber() != null &&
                                pnd.getNumber().length() > 0)
                            {
                                String localizedType = null;

                                if(d instanceof WorkPhoneDetail)
                                {
                                    localizedType =
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.WORK_PHONE");
                                }
                                else if(d instanceof MobilePhoneDetail)
                                {
                                    localizedType =
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.MOBILE_PHONE");
                                }
                                else
                                {
                                    localizedType =
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.PHONE");
                                }

                                phones.add(pnd.getNumber());

                                UIContactDetail cd =
                                    new UIContactDetail(
                                        pnd.getNumber(),
                                        pnd.getNumber() +
                                        " (" + localizedType + ")",
                                        null,
                                        new ArrayList<String>(),
                                        null,
                                        null,
                                        null)
                                {
                                    public PresenceStatus getPresenceStatus()
                                    {
                                        return null;
                                    }
                                };
                                telephonyContacts.add(cd);
                            }
                        }
                    }
                }
            }
        }

        return telephonyContacts;
    }

    /**
     * Shares the user desktop with the contact contained in the given
     * <tt>treeNode</tt>.
     *
     * @param protocolProvider the protocol provider through which we make the
     * sharing
     * @param contactName the address of the contact with which we'd like to
     * share our desktop
     */
    private void shareDesktop(  ProtocolProviderService protocolProvider,
                                String contactName)
    {
        CallManager.createDesktopSharing(protocolProvider, contactName);
    }

    /**
     * Shows the appropriate user interface that would allow the user to add
     * the given <tt>SourceUIContact</tt> to their contact list.
     *
     * @param contact the contact to add
     */
    private void addContact(SourceUIContact contact)
    {
        SourceContact sourceContact = (SourceContact) contact.getDescriptor();

        List<ContactDetail> details = sourceContact.getContactDetails();
        int detailsCount = details.size();

        if (detailsCount > 1)
        {
            JMenuItem addContactMenu = TreeContactList.createAddContactMenu(
                (SourceContact) contact.getDescriptor());

            JPopupMenu popupMenu = ((JMenu) addContactMenu).getPopupMenu();

            // Add a title label.
            JLabel infoLabel = new JLabel();
            infoLabel.setText("<html><b>"
                                + GuiActivator.getResources()
                                    .getI18NString("service.gui.ADD_CONTACT")
                                + "</b></html>");

            popupMenu.insert(infoLabel, 0);
            popupMenu.insert(new Separator(), 1);

            popupMenu.setFocusable(true);
            popupMenu.setInvoker(tree);

            Point location = new Point(addContactButton.getX(),
                addContactButton.getY() + addContactButton.getHeight());

            SwingUtilities.convertPointToScreen(location, tree);

            location.y = location.y
                + tree.getPathBounds(tree.getSelectionPath()).y;

            popupMenu.setLocation(location.x + 8, location.y - 8);
            popupMenu.setVisible(true);
        }
        else if (details.size() == 1)
        {
            TreeContactList.showAddContactDialog(details.get(0));
        }
    }

    /**
     * Returns the drag icon used to represent a cell in all drag operations.
     *
     * @param tree the parent tree object
     * @param dragObject the dragged object
     * @param index the index of the dragged object in the tree
     *
     * @return the drag icon
     */
    public Icon getDragIcon(JTree tree, Object dragObject, int index)
    {
        ContactListTreeCellRenderer dragC
            = (ContactListTreeCellRenderer) getTreeCellRendererComponent(
                                                        tree,
                                                        dragObject,
                                                        false, // is selected
                                                        false, // is expanded
                                                        true, // is leaf
                                                        index,
                                                        true // has focus
                                                     );

        // We should explicitly set the bounds of all components in order that
        // they're correctly painted by paintIcon afterwards. This fixes empty
        // drag component in contact list!
        dragC.setBounds(0, 0, dragC.getIconWidth(), dragC.getIconHeight());

        Icon rightLabelIcon = rightLabel.getIcon();
        int imageHeight = 0;
        int imageWidth = 0;
        if (rightLabelIcon != null)
        {
            imageWidth = rightLabelIcon.getIconWidth();
            imageHeight = rightLabelIcon.getIconHeight();
            dragC.rightLabel.setBounds(
                tree.getWidth() - imageWidth, 0, imageWidth, imageHeight);
        }

        statusLabel.setBounds(  0, 0,
                                statusLabel.getWidth(),
                                statusLabel.getHeight());

        nameLabel.setBounds(statusLabel.getWidth(), 0,
            tree.getWidth() - imageWidth - 5, nameLabel.getHeight());

        displayDetailsLabel.setBounds(
            displayDetailsLabel.getX(),
            nameLabel.getHeight(),
            displayDetailsLabel.getWidth(),
            displayDetailsLabel.getHeight());

        return dragC;
    }

    /**
     * Loads all images and colors.
     */
    public void loadSkin()
    {
        openedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON));

        closedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.RIGHT_ARROW_ICON));

        callButton.setBackgroundImage(ImageLoader.getImage(
                ImageLoader.CALL_BUTTON_SMALL));
        callButton.setPressedImage(ImageLoader.getImage(
                ImageLoader.CALL_BUTTON_SMALL_PRESSED));

        chatButton.setBackgroundImage(ImageLoader.getImage(
                ImageLoader.CHAT_BUTTON_SMALL));
        chatButton.setPressedImage(ImageLoader.getImage(
                ImageLoader.CHAT_BUTTON_SMALL_PRESSED));

        msgReceivedImage
            = ImageLoader.getImage(ImageLoader.MESSAGE_RECEIVED_ICON);

        int groupForegroundProperty = GuiActivator.getResources()
            .getColor("service.gui.CONTACT_LIST_GROUP_FOREGROUND");

        if (groupForegroundProperty > -1)
            groupForegroundColor = new Color (groupForegroundProperty);

        int contactForegroundProperty = GuiActivator.getResources()
                .getColor("service.gui.CONTACT_LIST_CONTACT_FOREGROUND");

        if (contactForegroundProperty > -1)
            contactForegroundColor = new Color(contactForegroundProperty);

        callVideoButton.setBackgroundImage(ImageLoader.getImage(
                ImageLoader.CALL_VIDEO_BUTTON_SMALL));

        callVideoButton.setPressedImage(ImageLoader.getImage(
                ImageLoader.CALL_VIDEO_BUTTON_SMALL_PRESSED));

        desktopSharingButton.setBackgroundImage(ImageLoader.getImage(
                ImageLoader.DESKTOP_BUTTON_SMALL));

        desktopSharingButton.setPressedImage(ImageLoader.getImage(
                ImageLoader.DESKTOP_BUTTON_SMALL_PRESSED));
    }

    /**
     * Listens for contact details if not cached, we will receive when they
     * are retrieved to update current call button state, if meanwhile
     * user hasn't changed the current contact.
     */
    private class DetailsListener
        implements OperationSetServerStoredContactInfo.DetailsResponseListener
    {
        /**
         * The source this listener is created for, if current tree node
         * changes ignore any event.
         */
        private Object source;

        /**
         * The button to change.
         */
        private JButton callButton;

        /**
         * The ui contact to update after changes.
         */
        private UIContact uiContact;

        /**
         * Create listener.
         * @param source the contact this listener is for, if different
         *               than current ignore.
         * @param callButton
         * @param uiContact the contact to refresh
         */
        DetailsListener(Object source, JButton callButton, UIContact uiContact)
        {
            this.source = source;
            this.callButton = callButton;
            this.uiContact = uiContact;
        }

        /**
         * Details have been retrieved.
         * @param details the details retrieved if any.
         */
        public void detailsRetrieved(Iterator<GenericDetail> details)
        {
            // if treenode has changed ignore
            if(!source.equals(treeNode))
                return;

            while(details.hasNext())
            {
                GenericDetail d = details.next();

                if(d instanceof PhoneNumberDetail &&
                    !(d instanceof PagerDetail) &&
                    !(d instanceof FaxDetail))
                {
                    PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                    if(pnd.getNumber() != null &&
                        pnd.getNumber().length() > 0)
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                callButton.setEnabled(true);

                                tree.refreshContact(uiContact);
                            }
                        });

                        return;
                    }
                 }
            }
        }
    }
}