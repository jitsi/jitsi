/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import java.awt.event.*;
import java.awt.image.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell. The cell
 * border and background are repainted.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ContactListTreeCellRenderer
    extends JPanel
    implements  TreeCellRenderer,
                Icon
{
    private static final Color glowOuterHigh = new Color(223, 238, 249, 100);
    private static final Color glowOuterLow = new Color(219, 233, 243, 100);

    /**
     * The call button component name, used to distinguish it.
     */
    public static final String CALL_BUTTON_NAME = "CallButton";

    /**
     * The chat button component name, used to distinguish it.
     */
    public static final String CHAT_BUTTON_NAME = "ChatButton";

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
     * The icon used for opened groups.
     */
    private final ImageIcon openedGroupIcon =
        new ImageIcon(ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON));

    /**
     * The icon used for closed groups.
     */
    private final ImageIcon closedGroupIcon =
        new ImageIcon(ImageLoader.getImage(ImageLoader.RIGHT_ARROW_ICON));

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
    private final SIPCommButton callButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.CALL_BUTTON_SMALL),
        ImageLoader.getImage(ImageLoader.CALL_BUTTON_SMALL_PRESSED),
        null);

    /**
     * The chat button.
     */
    private final SIPCommButton chatButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.CHAT_BUTTON_SMALL),
        ImageLoader.getImage(ImageLoader.CHAT_BUTTON_SMALL_PRESSED),
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

    private final Image msgReceivedImage =
        ImageLoader.getImage(ImageLoader.MESSAGE_RECEIVED_ICON);

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
    private JTree tree;

    /**
     * Initializes the panel containing the node.
     */
    public ContactListTreeCellRenderer()
    {
        super(new GridBagLayout());

        this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2));

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

        this.displayDetailsLabel.setFont(getFont().deriveFont(9f));
        this.displayDetailsLabel.setForeground(Color.GRAY);

        this.rightLabel.setFont(rightLabel.getFont().deriveFont(9f));
        this.rightLabel.setHorizontalAlignment(JLabel.RIGHT);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 2));

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.VERTICAL;
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
        constraints.gridwidth = 2;
        this.add(nameLabel, constraints);

        rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridheight = 3;
        constraints.weightx = 0f;
        constraints.weighty = 1f;
        this.add(rightLabel, constraints);

        this.callButton.setName(CALL_BUTTON_NAME);
        this.chatButton.setName(CHAT_BUTTON_NAME);

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ChooseCallAccountPopupMenu chooseAccountDialog = null;

                if (treeNode != null && treeNode instanceof ContactNode)
                {
                    List<UIContactDetail> telephonyContacts
                        = ((ContactNode) treeNode).getContactDescriptor()
                            .getContactDetailsForOperationSet(
                                OperationSetBasicTelephony.class);

                    if (telephonyContacts.size() == 1)
                    {
                        UIContactDetail detail
                            = telephonyContacts.get(0);

                        ProtocolProviderService preferredProvider
                            = detail.getPreferredProtocolProvider(
                                OperationSetBasicTelephony.class);

                        if (preferredProvider != null)
                            CallManager.createCall(
                                preferredProvider,
                                detail.getAddress());
                        else
                        {
                            List<ProtocolProviderService> providers
                                = CallManager.getTelephonyProviders();

                            int providersCount = providers.size();

                            if (providersCount == 1)
                                CallManager.createCall(
                                    providers.get(0),
                                    detail.getAddress());
                            else if (providersCount > 1)
                                chooseAccountDialog
                                    = new ChooseCallAccountPopupMenu(
                                        tree, providers);
                        }
                    }
                    else if (telephonyContacts.size() > 1)
                    {
                        chooseAccountDialog
                            = new ChooseCallAccountPopupMenu(
                                tree,
                                telephonyContacts);
                    }

                    // If the choose dialog is created we're going to show it.
                    if (chooseAccountDialog != null)
                    {
                        Point location = new Point(callButton.getX(),
                            callButton.getY() + callButton.getHeight());

                        SwingUtilities.convertPointToScreen(
                            location, tree);

                        location.y = location.y
                            + tree.getPathBounds(tree.getSelectionPath()).y;
                        chooseAccountDialog
                            .showPopupMenu(location.x + 8, location.y - 8);
                    }
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
        this.tree = tree;
        this.row = row;
        this.isSelected = selected;
        this.treeNode = (TreeNode) value;

        this.rightLabel.setIcon(null);

        DefaultTreeContactList contactList = (DefaultTreeContactList) tree;

        if (value instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) value).getContactDescriptor();

            String displayName = contact.getDisplayName();

            if (displayName == null || displayName.length() < 1)
            {
                displayName = GuiActivator.getResources()
                    .getI18NString("service.gui.UNKNOWN");
            }

            this.nameLabel.setText(displayName);

            if(contactList.isContactActive(contact))
            {
                statusIcon.setImage(msgReceivedImage);
            }
            else
            {
                statusIcon = contact.getStatusIcon();
            }
            this.statusLabel.setIcon(statusIcon);

            this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));

            if (contactForegroundColor != null)
                this.nameLabel.setForeground(contactForegroundColor);

            // Initializes status message components if the given meta contact
            // contains a status message.
            this.initDisplayDetails(contact);

            this.initButtonsPanel(contact);

            ImageIcon avatar = isSelected
                ? contact.getAvatar(
                    isSelected, EXTENDED_AVATAR_WIDTH, EXTENDED_AVATAR_HEIGHT)
                : contact.getAvatar(
                    isSelected, AVATAR_WIDTH, AVATAR_HEIGHT);

            if (avatar != null)
                this.rightLabel.setIcon(avatar);
            this.rightLabel.setText("");

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
            this.remove(chatButton);

            this.statusLabel.setIcon(
                    expanded
                    ? openedGroupIcon
                    : closedGroupIcon);

            // We have no photo icon for groups.
            this.rightLabel.setIcon(null);
 
            if (groupItem.countChildContacts() >= 0)
                this.rightLabel.setText( groupItem.countOnlineChildContacts() 
                                        + "/" + groupItem.countChildContacts());

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
        if (!(treeNode instanceof GroupNode) && !isSelected)
            return;

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        Shape clipShape
            = GraphicUtils.createRoundedClipShape(
                getWidth(), getHeight(), 20, 20);

        // Clear the background to white
        g2.setColor(Color.WHITE);
        g2.fillRect(1, 1, getWidth(), getHeight());

        // Set the clip shape
        BufferedImage clipImage = GraphicUtils.createClipImage(g2, clipShape);
        Graphics2D clipG = clipImage.createGraphics();

        // Fill the shape with a gradient
        clipG.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        clipG.setComposite(AlphaComposite.SrcAtop);

        if (isSelected)
        {
            clipG.setPaint(new GradientPaint(0, 0,
                Color.WHITE, 0, getHeight(), Constants.SELECTED_COLOR));
            clipG.fill(clipShape);

            // Apply the border glow effect
            GraphicUtils.paintBorderGlow(
                clipG, 6, clipShape, glowOuterLow, glowOuterHigh);
        }
        else if (treeNode instanceof GroupNode)
        {
            clipG.setPaint(new GradientPaint(0, 0,
                Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR,
                0, getHeight(),
                Constants.CONTACT_LIST_GROUP_BG_COLOR));
            clipG.fill(clipShape);

            // Apply the border glow effect
            GraphicUtils.paintBorderGlow(
                clipG, 1, clipShape, Color.WHITE, Color.DARK_GRAY);
        }

        clipG.dispose();

        g2.drawImage(clipImage, 0, 0, null);

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
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
     * Returns the preferred size of this component.
     * @return the preferred size of this component
     */
    public Dimension getPreferredSize()
    {
        Dimension preferredSize = new Dimension();

        if (treeNode instanceof ContactNode)
            if (isSelected)
                preferredSize.height = 55;
            else
                preferredSize.height = 30;
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
            constraints.gridwidth = 2;
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
        this.remove(callButton);
        this.remove(chatButton);

        if (!isSelected)
            return;

        int statusMessageLabelHeight = 0;
        if (displayDetailsLabel.getText().length() > 0)
            statusMessageLabelHeight = 20;
        else
            statusMessageLabelHeight = 15;

         UIContactDetail imContact = uiContact.getDefaultContactDetail(
             OperationSetBasicInstantMessaging.class);

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

            int x = statusLabel.getWidth();

            chatButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight,
                28, 28);
        }

        UIContactDetail telephonyContact
            = uiContact.getDefaultContactDetail(OperationSetBasicTelephony.class);

        if (telephonyContact != null)
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

            int x = statusLabel.getWidth();

            if (imContact != null)
                x += callButton.getWidth();

            callButton.setBounds(x,
                nameLabel.getHeight() + statusMessageLabelHeight, 28, 28);
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
     * Returns the call button contained in the current cell.
     * @return the call button contained in the current cell
     */
    public JButton getCallButton()
    {
        return callButton;
    }
}