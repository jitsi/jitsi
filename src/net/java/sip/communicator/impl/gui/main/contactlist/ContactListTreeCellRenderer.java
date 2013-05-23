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
import javax.swing.JPopupMenu.Separator;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.call.*;
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
     * The default width of the button.
     */
    private static final int BUTTON_WIDTH = 26;

    /**
     * The default height of the button.
     */
    private static final int BUTTON_HEIGHT = 27;

    /**
     * Left border value.
     */
    private static final int LEFT_BORDER = 5;

    /**
     * Left border value.
     */
    private static final int TOP_BORDER = 2;

    /**
     * Bottom border value.
     */
    private static final int BOTTOM_BORDER = 2;

    /**
     * Right border value.
     */
    private static final int RIGHT_BORDER = 2;

    /**
     * The horizontal gap between columns in pixels;
     */
    private static final int H_GAP = 2;

    /**
     * The vertical gap between rows in pixels;
     */
    private static final int V_GAP = 3;

    /**
     * The separator image for the button toolbar.
     */
    private static final Image BUTTON_SEPARATOR_IMG
        = ImageLoader.getImage(ImageLoader.CONTACT_LIST_BUTTON_SEPARATOR);

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
    private final SIPCommButton callVideoButton = new SIPCommButton();

    /**
     * The desktop sharing button.
     */
    private final SIPCommButton desktopSharingButton = new SIPCommButton();

    /**
     * The chat button.
     */
    private final SIPCommButton chatButton = new SIPCommButton();

    /**
     * The add contact button.
     */
    private final SIPCommButton addContactButton = new SIPCommButton();

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
    protected Icon statusIcon = new ImageIcon();

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
    protected TreeNode treeNode = null;

    /**
     * The parent tree.
     */
    private TreeContactList treeContactList;

    /**
     * A list of the custom action buttons for contacts UIContacts.
     */
    private List<JButton> customActionButtons;

    /**
     * A list of the custom action buttons for groups.
     */
    private List<JButton> customActionButtonsUIGroup;

    /**
     * The last added button.
     */
    private SIPCommButton lastAddedButton;

    /**
     * Initializes the panel containing the node.
     */
    public ContactListTreeCellRenderer()
    {
        super(new GridBagLayout());

        loadSkin();

        this.setOpaque(true);
        this.nameLabel.setOpaque(false);

        this.displayDetailsLabel.setFont(getFont().deriveFont(9f));
        this.displayDetailsLabel.setForeground(Color.GRAY);

        this.rightLabel.setHorizontalAlignment(JLabel.RIGHT);

        // !! IMPORTANT: General insets used for all components if not
        // overwritten!
        constraints.insets = new Insets(0, 0, 0, H_GAP);

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 1f;
        this.add(statusLabel, constraints);

        addLabels(1);

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    call(treeNode, callButton, false, false);
                }
            }
        });

        callVideoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    call(treeNode, callVideoButton, true, false);
                }
            }
        });

        desktopSharingButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    call(treeNode, desktopSharingButton, true, true);
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

        initButtonToolTips();
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
        this.treeContactList = (TreeContactList) tree;
        this.row = row;
        this.isSelected = selected;
        this.treeNode = (TreeNode) value;

        this.rightLabel.setIcon(null);

        DefaultTreeContactList contactList = (DefaultTreeContactList) tree;

        setBorder();
        addLabels(1);

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
            UIContactImpl contact
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

            if(statusIcon != null
                && contactList.isContactActive(contact)
                && statusIcon instanceof ImageIcon)
                ((ImageIcon) statusIcon).setImage(msgReceivedImage);
            else
                statusIcon = contact.getStatusIcon();

            this.statusLabel.setIcon(statusIcon);

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            if (contactForegroundColor != null)
                nameLabel.setForeground(contactForegroundColor);

            // Initializes status message components if the given meta contact
            // contains a status message.
            this.initDisplayDetails(contact.getDisplayDetails());

            if (this.treeContactList.isContactButtonsVisible())
                this.initButtonsPanel(contact);

            int avatarWidth, avatarHeight;

            if (isSelected && treeContactList.isContactButtonsVisible())
            {
                avatarWidth = EXTENDED_AVATAR_WIDTH;
                avatarHeight = EXTENDED_AVATAR_HEIGHT;
            }
            else
            {
                avatarWidth = AVATAR_WIDTH;
                avatarHeight = AVATAR_HEIGHT;
            }

            Icon avatar
                = contact.getScaledAvatar(isSelected, avatarWidth, avatarHeight);

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
            UIGroupImpl groupItem
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

            clearCustomActionButtons();

            statusIcon = expanded
                                ? openedGroupIcon
                                : closedGroupIcon;
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

            this.initDisplayDetails(groupItem.getDisplayDetails());
            this.initButtonsPanel(groupItem);
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
        return treeContactList.getWidth() + 10;
    }

    /**
     * Returns the preferred size of this component.
     * @return the preferred size of this component
     */
    @Override
    public Dimension getPreferredSize()
    {
        Dimension preferredSize = new Dimension();
        int preferredHeight;

        if (treeNode instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) treeNode).getContactDescriptor();

            preferredHeight = contact.getPreferredHeight();

            if (preferredHeight > 0)
                preferredSize.height = preferredHeight;
            else if (contact instanceof ShowMoreContact)
                preferredSize.height = 20;
            else if (isSelected && treeContactList.isContactButtonsVisible())
                preferredSize.height = 70;
            else
                preferredSize.height = 35;
        }
        else if (treeNode instanceof GroupNode)
        {
            UIGroup group
                = ((GroupNode) treeNode).getGroupDescriptor();

            preferredHeight = group.getPreferredHeight();

            if (isSelected
                    && customActionButtonsUIGroup != null
                    && !customActionButtonsUIGroup.isEmpty())
                preferredSize.height = 70;
            else if (preferredHeight > 0)
                preferredSize.height = preferredHeight;
            else
                preferredSize.height = 20;
        }

        return preferredSize;
    }

    /**
     * Adds contact entry labels.
     *
     * @param nameLabelGridWidth the grid width of the contact entry name
     * label
     */
    private void addLabels(int nameLabelGridWidth)
    {
        remove(nameLabel);
        remove(rightLabel);
        remove(displayDetailsLabel);

        if (treeNode != null && !(treeNode instanceof GroupNode))
            constraints.insets = new Insets(0, 0, V_GAP, H_GAP);
        else
            constraints.insets = new Insets(0, 0, 0, H_GAP);

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.weighty = 0f;
        constraints.gridheight = 1;
        constraints.gridwidth = nameLabelGridWidth;
        this.add(nameLabel, constraints);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = nameLabelGridWidth + 1;
        constraints.gridy = 0;
        constraints.gridheight = 3;
        constraints.weightx = 0f;
        constraints.weighty = 1f;
        this.add(rightLabel, constraints);

        if (treeNode != null && treeNode instanceof ContactNode)
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1f;
            constraints.weighty = 0f;
            constraints.gridwidth = nameLabelGridWidth;
            constraints.gridheight = 1;

            this.add(displayDetailsLabel, constraints);
        }
        else if (treeNode != null && treeNode instanceof GroupNode)
        {
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1f;
            constraints.weighty = 0f;
            constraints.gridwidth = nameLabelGridWidth;
            constraints.gridheight = 1;

            this.add(displayDetailsLabel, constraints);
        }
    }

    /**
     * Initializes the display details component for the given
     * <tt>UIContact</tt>.
     * @param displayDetails the display details to show
     */
    private void initDisplayDetails(String displayDetails)
    {
        remove(displayDetailsLabel);
        displayDetailsLabel.setText("");

        if (displayDetails != null && displayDetails.length() > 0)
        {
            // Replace all occurrences of new line with slash.
            displayDetails = Html2Text.extractText(displayDetails);
            displayDetails = displayDetails.replaceAll("\n|<br>|<br/>", " / ");

            displayDetailsLabel.setText(displayDetails);
        }

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1f;
        constraints.weighty = 0f;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        this.add(displayDetailsLabel, constraints);
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

        clearCustomActionButtons();

        if (!isSelected)
            return;

        UIContactDetail imContact = null;
        // For now we support instance messaging only for contacts in our
        // contact list until it's implemented for external source contacts.
        if (uiContact.getDescriptor() instanceof MetaContact)
            imContact = uiContact.getDefaultContactDetail(
                         OperationSetBasicInstantMessaging.class);

        int x = (statusIcon == null ? 0 : statusIcon.getIconWidth())
                + LEFT_BORDER
                + H_GAP;

        // Re-initialize the x grid.
        constraints.gridx = 0;
        int gridX = 0;

        if (imContact != null)
        {
            x += addButton(chatButton, ++gridX, x, false);
        }

        UIContactDetail telephonyContact
            = uiContact.getDefaultContactDetail(
                OperationSetBasicTelephony.class);

        // Check if contact has additional phone numbers, if yes show the
        // call button
        MetaContactPhoneUtil contactPhoneUtil = null;
        DetailsResponseListener detailsListener = null;

        // check for phone stored in contact info only
        // if telephony contact is missing
        if(uiContact.getDescriptor() != null
           && uiContact.getDescriptor() instanceof MetaContact
           && telephonyContact == null)
        {
            contactPhoneUtil = MetaContactPhoneUtil.getPhoneUtil(
                (MetaContact)uiContact.getDescriptor());

            detailsListener
                = new DetailsListener(treeNode, callButton, uiContact);
        }

        // for SourceContact in history that do not support telephony, we
        // show the button but disabled
        List<ProtocolProviderService> providers
            = AccountUtils.getOpSetRegisteredProviders(
                OperationSetBasicTelephony.class,
                null,
                null);

        if ((telephonyContact != null && telephonyContact.getAddress() != null)
            || (contactPhoneUtil != null
                && contactPhoneUtil.isCallEnabled(detailsListener)
                && providers.size() > 0))
        {
            x += addButton(callButton, ++gridX, x, false);
        }

        UIContactDetail videoContact
            = uiContact.getDefaultContactDetail(
                OperationSetVideoTelephony.class);

        if (videoContact != null
            || (contactPhoneUtil != null
                && contactPhoneUtil.isVideoCallEnabled(detailsListener)))
        {
            x += addButton(callVideoButton, ++gridX, x, false);
        }

        UIContactDetail desktopContact
            = uiContact.getDefaultContactDetail(
                OperationSetDesktopSharingServer.class);

        if (desktopContact != null
            || (contactPhoneUtil != null
                && contactPhoneUtil.isDesktopSharingEnabled(detailsListener)))
        {
            x += addButton(desktopSharingButton, ++gridX, x, false);
        }

        // enable add contact button if contact source has indicated
        // that this is possible
        if (uiContact.getDescriptor() instanceof SourceContact
            && uiContact.getDefaultContactDetail(
                    OperationSetPersistentPresence.class) != null
            && AccountUtils.getOpSetRegisteredProviders(
                    OperationSetPersistentPresence.class,
                    null,
                    null).size() > 0
            && !ConfigurationUtils.isAddContactDisabled())
        {
            x += addButton(addContactButton, ++gridX, x, false);
        }

        // The list of the contact actions
        // we will create a button for every action
        Collection<SIPCommButton> contactActions
            = uiContact.getContactCustomActionButtons();

        int lastGridX = gridX;
        if (contactActions != null && contactActions.size() > 0)
        {
            lastGridX = initContactActionButtons(contactActions, gridX, x);
        }
        else
        {
            addLabels(gridX);
        }

        if (lastAddedButton != null)
            setButtonBg(lastAddedButton, lastGridX, true);

        this.setBounds(0, 0, treeContactList.getWidth(),
                        getPreferredSize().height);
    }

    /**
     * Initializes buttons panel.
     * @param uiGroup the <tt>UIGroup</tt> for which we initialize the
     * button panel
     */
    private void initButtonsPanel(UIGroup uiGroup)
    {
        if (!isSelected)
            return;

        int x = (statusIcon == null ? 0 : statusIcon.getIconWidth())
                + LEFT_BORDER
                + H_GAP;
        int gridX = 0;

        // The list of the actions
        // we will create a button for every action
        Collection<SIPCommButton> contactActions
            = uiGroup.getCustomActionButtons();

        int lastGridX = gridX;
        if (contactActions != null && contactActions.size() > 0)
        {
            lastGridX = initGroupActionButtons(contactActions, gridX, x);
        }
        else
        {
            addLabels(gridX);
        }

        if (lastAddedButton != null)
            setButtonBg(lastAddedButton, lastGridX, true);

        this.setBounds(0, 0, treeContactList.getWidth(),
                        getPreferredSize().height);
    }

    /**
     * Clears the custom action buttons.
     */
    private void clearCustomActionButtons()
    {
        if (customActionButtons != null && customActionButtons.size() > 0)
        {
            Iterator<JButton> buttonsIter = customActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                remove(buttonsIter.next());
            }
            customActionButtons.clear();
        }

        if (customActionButtonsUIGroup != null
            && customActionButtonsUIGroup.size() > 0)
        {
            Iterator<JButton> buttonsIter =
                customActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                remove(buttonsIter.next());
            }
            customActionButtonsUIGroup.clear();
        }
    }

    /**
     * Initializes custom contact action buttons.
     *
     * @param contactActionButtons the list of buttons to initialize
     * @param gridX the X grid of the first button
     * @param xBounds the x bounds of the first button
     *
     * @return the new grid X coordinate after adding all the buttons
     */
    private int initGroupActionButtons(
        Collection<SIPCommButton> contactActionButtons,
        int gridX,
        int xBounds)
    {
        // Reinit the labels to take the whole horizontal space.
        addLabels(gridX + contactActionButtons.size());

        Iterator<SIPCommButton> actionsIter = contactActionButtons.iterator();
        while (actionsIter.hasNext())
        {
            final SIPCommButton actionButton = actionsIter.next();

            // We need to explicitly remove the buttons from the tooltip manager,
            // because we're going to manager the tooltip ourselves in the
            // DefaultTreeContactList class. We need to do this in order to have
            // a different tooltip for every button and for non button area.
            ToolTipManager.sharedInstance().unregisterComponent(actionButton);

            if (customActionButtonsUIGroup == null)
                customActionButtonsUIGroup = new LinkedList<JButton>();

            customActionButtonsUIGroup.add(actionButton);

            xBounds
                += addButton(actionButton, ++gridX, xBounds, false);
        }

        return gridX;
    }

    /**
     * Initializes custom contact action buttons.
     *
     * @param contactActionButtons the list of buttons to initialize
     * @param gridX the X grid of the first button
     * @param xBounds the x bounds of the first button
     *
     * @return the new grid X coordiante after adding all the buttons
     */
    private int initContactActionButtons(
        Collection<SIPCommButton> contactActionButtons,
        int gridX,
        int xBounds)
    {
        // Reinit the labels to take the whole horizontal space.
        addLabels(gridX + contactActionButtons.size());

        Iterator<SIPCommButton> actionsIter = contactActionButtons.iterator();
        while (actionsIter.hasNext())
        {
            final SIPCommButton actionButton = actionsIter.next();

            // We need to explicitly remove the buttons from the tooltip manager,
            // because we're going to manager the tooltip ourselves in the
            // DefaultTreeContactList class. We need to do this in order to have
            // a different tooltip for every button and for non button area.
            ToolTipManager.sharedInstance().unregisterComponent(actionButton);

            if (customActionButtons == null)
                customActionButtons = new LinkedList<JButton>();

            customActionButtons.add(actionButton);

            xBounds
                += addButton(actionButton, ++gridX, xBounds, false);
        }

        return gridX;
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
    public JButton getChatButton()
    {
        return chatButton;
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
    private void call(TreeNode treeNode, JButton button,
                      boolean isVideo, boolean isDesktopSharing)
    {
        if (!(treeNode instanceof ContactNode))
            return;

        UIContact contactDescriptor
            = ((ContactNode) treeNode).getContactDescriptor();

        Point location = new Point(button.getX(),
            button.getY() + button.getHeight());

        SwingUtilities.convertPointToScreen(location, treeContactList);

        location.y = location.y
            + treeContactList.getPathBounds(treeContactList.getSelectionPath()).y;
        location.x += 8;
        location.y -= 8;

        CallManager.call(contactDescriptor,
            isVideo, isDesktopSharing,
            treeContactList, location);
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

        List<ContactDetail> details = sourceContact.getContactDetails(
                    OperationSetPersistentPresence.class);
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
            popupMenu.setInvoker(treeContactList);

            Point location = new Point(addContactButton.getX(),
                addContactButton.getY() + addContactButton.getHeight());

            SwingUtilities.convertPointToScreen(location, treeContactList);

            location.y = location.y
                + treeContactList.getPathBounds(treeContactList.getSelectionPath()).y;

            popupMenu.setLocation(location.x + 8, location.y - 8);
            popupMenu.setVisible(true);
        }
        else if (details.size() == 1)
        {
            TreeContactList.showAddContactDialog(
                details.get(0),
                sourceContact.getDisplayName());
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

        dragC.statusLabel.setBounds(  0, 0,
                                statusLabel.getWidth(),
                                statusLabel.getHeight());

        dragC.nameLabel.setBounds(statusLabel.getWidth(), 0,
            tree.getWidth() - imageWidth - 5, nameLabel.getHeight());

        dragC.displayDetailsLabel.setBounds(
            displayDetailsLabel.getX(),
            nameLabel.getHeight(),
            displayDetailsLabel.getWidth(),
            displayDetailsLabel.getHeight());

        return dragC;
    }

    /**
     * Resets the rollover state of all rollover components in the current cell.
     */
    public void resetRolloverState()
    {
        chatButton.getModel().setRollover(false);
        callButton.getModel().setRollover(false);
        callVideoButton.getModel().setRollover(false);
        desktopSharingButton.getModel().setRollover(false);
        addContactButton.getModel().setRollover(false);

        if (customActionButtons != null)
        {
            Iterator<JButton> buttonsIter = customActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                button.getModel().setRollover(false);
            }
        }

        if (customActionButtonsUIGroup != null)
        {
            Iterator<JButton> buttonsIter = customActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                button.getModel().setRollover(false);
            }
        }
    }

    /**
     * Resets the rollover state of all rollover components in the current cell
     * except the component given as a parameter.
     *
     * @param excludeComponent the component to exclude from the reset
     */
    public void resetRolloverState(Component excludeComponent)
    {
        if (!chatButton.equals(excludeComponent))
            chatButton.getModel().setRollover(false);

        if (!callButton.equals(excludeComponent))
            callButton.getModel().setRollover(false);

        if (!callVideoButton.equals(excludeComponent))
            callVideoButton.getModel().setRollover(false);

        if (!desktopSharingButton.equals(excludeComponent))
            desktopSharingButton.getModel().setRollover(false);

        if (!addContactButton.equals(excludeComponent))
            addContactButton.getModel().setRollover(false);

        if (customActionButtons != null)
        {
            Iterator<JButton> buttonsIter = customActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();

                if (!button.equals(excludeComponent))
                    button.getModel().setRollover(false);
            }
        }

        if (customActionButtonsUIGroup != null)
        {
            Iterator<JButton> buttonsIter =
                customActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();

                if (!button.equals(excludeComponent))
                    button.getModel().setRollover(false);
            }
        }
    }

    /**
     * Loads all images and colors.
     */
    public void loadSkin()
    {
        openedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.OPENED_GROUP_ICON));

        closedGroupIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSED_GROUP_ICON));

        callButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CALL_BUTTON_SMALL));
        callButton.setRolloverIcon(ImageLoader.getImage(
                ImageLoader.CALL_BUTTON_SMALL_ROLLOVER));
        callButton.setPressedIcon(ImageLoader.getImage(
                ImageLoader.CALL_BUTTON_SMALL_PRESSED));

        chatButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_BUTTON_SMALL));
        chatButton.setRolloverIcon(ImageLoader.getImage(
            ImageLoader.CHAT_BUTTON_SMALL_ROLLOVER));
        chatButton.setPressedIcon(ImageLoader.getImage(
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

        callVideoButton.setIconImage(
            ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_SMALL));
        callVideoButton.setRolloverIcon(
            ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_SMALL_ROLLOVER));
        callVideoButton.setPressedIcon(
            ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_SMALL_PRESSED));

        desktopSharingButton.setIconImage(
            ImageLoader.getImage(ImageLoader.DESKTOP_BUTTON_SMALL));
        desktopSharingButton.setRolloverIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_BUTTON_SMALL_ROLLOVER));
        desktopSharingButton.setPressedIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_BUTTON_SMALL_PRESSED));

        addContactButton.setIconImage(
            ImageLoader.getImage(ImageLoader.ADD_CONTACT_BUTTON_SMALL));
        addContactButton.setRolloverIcon(
            ImageLoader.getImage(ImageLoader.ADD_CONTACT_BUTTON_SMALL_ROLLOVER));
        addContactButton.setPressedIcon(
            ImageLoader.getImage(ImageLoader.ADD_CONTACT_BUTTON_SMALL_PRESSED));
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
                    final PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                    if(pnd.getNumber() != null &&
                        pnd.getNumber().length() > 0)
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                callButton.setEnabled(true);

                                if(pnd instanceof VideoDetail)
                                {
                                    callVideoButton.setEnabled(true);
                                    desktopSharingButton.setEnabled(true);
                                }

                                treeContactList.refreshContact(uiContact);
                            }
                        });

                        return;
                    }
                 }
            }
        }
    }

    private int addButton(  SIPCommButton button,
                            int gridX,
                            int xBounds,
                            boolean isLast)
    {
        lastAddedButton = button;

        constraints.insets = new Insets(0, 0, V_GAP, 0);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = gridX;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        this.add(button, constraints);

        int yBounds = TOP_BORDER + BOTTOM_BORDER + 2*V_GAP
                + ComponentUtils.getStringSize(
                    nameLabel, nameLabel.getText()).height
                + ComponentUtils.getStringSize(
                    displayDetailsLabel, displayDetailsLabel.getText()).height;

        button.setBounds(xBounds, yBounds, BUTTON_WIDTH, BUTTON_HEIGHT);

        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        setButtonBg(button, gridX, isLast);

        return button.getWidth();
    }

    /**
     * Sets the background of the button depending on its position in the button
     * bar.
     *
     * @param button the button which background to set
     * @param gridX the position of the button in the grid
     * @param isLast indicates if this is the last button in the button bar
     */
    private void setButtonBg(SIPCommButton button,
                            int gridX,
                            boolean isLast)
    {
        if (!isLast)
        {
            if (gridX == 1)
                button.setBackgroundImage(ImageLoader.getImage(
                    ImageLoader.CONTACT_LIST_BUTTON_BG_LEFT));
            else if (gridX > 1)
                button.setBackgroundImage(ImageLoader.getImage(
                    ImageLoader.CONTACT_LIST_BUTTON_BG_MIDDLE));
        }
        else
        {
            if (gridX == 1) // We have only one button shown.
                button.setBackgroundImage(ImageLoader.getImage(
                    ImageLoader.CONTACT_LIST_ONE_BUTTON_BG));
            else // We set the background of the last button in the toolbar
                button.setBackgroundImage(ImageLoader.getImage(
                    ImageLoader.CONTACT_LIST_BUTTON_BG_RIGHT));
        }
    }

    /**
     * Sets the correct border depending on the contained object.
     */
    private void setBorder()
    {
        /*
         * !!! When changing border values we should make sure that we
         * recalculate the X and Y coordinates of the buttons added in
         * initButtonsPanel and initContactActionButtons functions. If not
         * correctly calculated problems may occur when clicking buttons!
         */
        if (treeNode instanceof ContactNode
            && !(((ContactNode) treeNode).getContactDescriptor() instanceof
                    ShowMoreContact))
        {
                this.setBorder(BorderFactory
                    .createEmptyBorder( TOP_BORDER,
                                        LEFT_BORDER,
                                        BOTTOM_BORDER,
                                        RIGHT_BORDER));
        }
        else // GroupNode || ShowMoreContact
        {
            this.setBorder(BorderFactory
                .createEmptyBorder( 0,
                                    LEFT_BORDER,
                                    0,
                                    RIGHT_BORDER));
        }
    }

    /**
     * Inializes button tool tips.
     */
    private void initButtonToolTips()
    {
        callButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.CALL_CONTACT"));
        callVideoButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.VIDEO_CALL"));
        desktopSharingButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.SHARE_DESKTOP"));
        chatButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_MESSAGE"));
        addContactButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));

        // We need to explicitly remove the buttons from the tooltip manager,
        // because we're going to manager the tooltip ourselves in the
        // DefaultTreeContactList class. We need to do this in order to have
        // a different tooltip for every button and for non button area.
        ToolTipManager ttManager = ToolTipManager.sharedInstance();
        ttManager.unregisterComponent(callButton);
        ttManager.unregisterComponent(callVideoButton);
        ttManager.unregisterComponent(desktopSharingButton);
        ttManager.unregisterComponent(chatButton);
        ttManager.unregisterComponent(addContactButton);
    }
}
