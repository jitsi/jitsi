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
import java.awt.Container;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>AddContactDialog</tt> is the dialog containing the form for adding
 * a contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AddContactDialog
    extends SIPCommDialog
    implements  ExportedWindow,
                ActionListener,
                WindowFocusListener,
                Skinnable
{
    private JLabel accountLabel;

    private JComboBox accountCombo;

    private JLabel groupLabel;

    private JComboBox groupCombo;

    private JLabel contactAddressLabel;

    private JLabel displayNameLabel;

    private JTextField contactAddressField;

    private JTextField displayNameField;

    private JButton addButton;

    private JButton cancelButton;

    private MetaContact metaContact;

    /**
     * Whether dialog is initialized.
     */
    private boolean initialized = false;

    /**
     * Image label.
     */
    private JLabel imageLabel;

    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     *
     * @param parentWindow the parent window of this dialog
     */
    public AddContactDialog(Frame parentWindow)
    {
        super(parentWindow);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));
    }

    /**
     * Creates an <tt>AddContactDialog</tt> by specifying the parent window and
     * a meta contact, to which to add the new contact.
     * @param parentWindow the parent window
     * @param metaContact the meta contact, to which to add the new contact
     */
    public AddContactDialog(Frame parentWindow, MetaContact metaContact)
    {
        this(parentWindow);

        this.metaContact = metaContact;

        this.setTitle(GuiActivator.getResources()
                        .getI18NString("service.gui.ADD_CONTACT_TO")
                         + " " + metaContact.getDisplayName());
    }

    /**
     * Selects the given protocol provider in the account combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to select
     */
    public void setSelectedAccount(ProtocolProviderService protocolProvider)
    {
        if(!initialized)
            init();

        accountCombo.setSelectedItem(protocolProvider);
    }

    /**
     * Selects the given <tt>group</tt> in the group combo box.
     * @param group the <tt>MetaContactGroup</tt> to select
     */
    public void setSelectedGroup(MetaContactGroup group)
    {
        if(!initialized)
            init();

        groupCombo.setSelectedItem(group);
    }

    /**
     * Sets the address of the contact to add.
     * @param contactAddress the address of the contact to add
     */
    public void setContactAddress(String contactAddress)
    {
        if(!initialized)
            init();

        contactAddressField.setText(contactAddress);
    }

    /**
     * Sets the display name of the contact to add.
     * @param displayName the display name of the contact to add
     */
    public void setDisplayName(String displayName)
    {
        if(!initialized)
            init();

        displayNameField.setText(displayName);
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        this.accountLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.SELECT_ACCOUNT") + ": ");

        this.accountCombo = new JComboBox();

        this.groupLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.SELECT_GROUP") + ": ");

        this.contactAddressLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.CONTACT_NAME") + ": ");

        this.displayNameLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.DISPLAY_NAME") + ": ");

        this.contactAddressField = new JTextField();

        this.displayNameField = new JTextField();

        this.addButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.ADD"));

        this.cancelButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

        this.imageLabel = new JLabel();

        this.groupCombo = createGroupCombo(this);

        if(metaContact != null)
        {
            groupCombo.setEnabled(false);

            groupCombo.setSelectedItem(metaContact.getParentMetaContactGroup());
        }

        TransparentPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        TransparentPanel fieldsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        initAccountCombo();
        accountCombo.setRenderer(new AccountComboRenderer());

        // we have an empty choice and one account
        if(accountCombo.getItemCount() > 2
            || (accountCombo.getItemCount() == 2
                && !ConfigurationUtils
                        .isHideAccountSelectionWhenPossibleEnabled()))
        {
            labelsPanel.add(accountLabel);
            fieldsPanel.add(accountCombo);
        }

        labelsPanel.add(groupLabel);
        fieldsPanel.add(groupCombo);

        labelsPanel.add(contactAddressLabel);
        fieldsPanel.add(contactAddressField);

        labelsPanel.add(displayNameLabel);
        fieldsPanel.add(displayNameField);

        contactAddressField.getDocument().addDocumentListener(
            new DocumentListener()
            {
                public void changedUpdate(DocumentEvent e) {}

                public void insertUpdate(DocumentEvent e)
                {
                    updateAddButtonState(false);
                }

                public void removeUpdate(DocumentEvent e)
                {
                    updateAddButtonState(false);
                }
            });

        TransparentPanel dataPanel = new TransparentPanel(new BorderLayout());

        dataPanel.add(labelsPanel, BorderLayout.WEST);
        dataPanel.add(fieldsPanel);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(20, 10));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(imageLabel, BorderLayout.WEST);
        mainPanel.add(dataPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonsPanel(), BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel, BorderLayout.CENTER);

        if(ConfigurationUtils.isHideAccountSelectionWhenPossibleEnabled())
            this.setPreferredSize(new Dimension(450, 205));
        else
            this.setPreferredSize(new Dimension(450, 250));

        this.setResizable(false);
        this.addWindowFocusListener(this);

        // All items are now instantiated and could safely load the skin.
        loadSkin();

        this.initialized = true;
    }

    /**
     * Creates the buttons panel.
     * @return the created buttons panel
     */
    private Container createButtonsPanel()
    {
        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.ADD"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        buttonsPanel.add(addButton);
        buttonsPanel.add(cancelButton);

        // Disable the add button so that it would be clear for the user that
        // they need to choose an account and enter a contact id first.
        addButton.setEnabled(false);

        return buttonsPanel;
    }

    /**
     * Initializes account combo box.
     */
    private void initAccountCombo()
    {
        Iterator<ProtocolProviderService> providers
            = AccountUtils.getRegisteredProviders().iterator();

        accountCombo.addItem(GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_ACCOUNT"));

        accountCombo.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                updateAddButtonState(true);
            }
        });

        while (providers.hasNext())
        {
            ProtocolProviderService provider = providers.next();

            if(provider.getAccountID().isHidden())
                continue;

            OperationSet opSet
                = provider.getOperationSet(OperationSetPresence.class);

            if (opSet == null)
                continue;

            OperationSetPersistentPresencePermissions opSetPermissions
                = provider.getOperationSet(
                    OperationSetPersistentPresencePermissions.class);
            if(opSetPermissions != null)
            {
                // let's check whether we can edit something
                if(opSetPermissions.isReadOnly())
                    continue;
            }

            accountCombo.addItem(provider);

            if (provider.getAccountID().isPreferredProvider())
                accountCombo.setSelectedItem(provider);
        }

        // if we have only select account option and only one account
        // select the available account
        if(accountCombo.getItemCount() == 2)
            accountCombo.setSelectedIndex(1);
    }

    /**
     * Initializes groups combo box.
     */
    public static JComboBox createGroupCombo(final Dialog parentDialog)
    {
        final JComboBox groupCombo = new JComboBox();

        groupCombo.setRenderer(new GroupComboRenderer());

        updateGroupItems(groupCombo, null);

        final String newGroupString = GuiActivator.getResources()
            .getI18NString("service.gui.CREATE_GROUP");

        groupCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (groupCombo.getSelectedItem() != null
                    && groupCombo.getSelectedItem().equals(newGroupString))
                {
                    CreateGroupDialog dialog
                        = new CreateGroupDialog(parentDialog, false);
                    dialog.setModal(true);
                    dialog.setVisible(true);

                    MetaContactGroup newGroup = dialog.getNewMetaGroup();

                    if (newGroup != null)
                    {
                        groupCombo.insertItemAt(newGroup,
                                groupCombo.getItemCount() - 2);
                        groupCombo.setSelectedItem(newGroup);
                    }
                    else
                        groupCombo.setSelectedIndex(0);
                }
            }
        });

        return groupCombo;
    }

    /**
     * Update the group items in the combo supplied, by checking
     * and the edit permissions
     */
    private static void updateGroupItems(JComboBox groupCombo,
                                         ProtocolProviderService provider)
    {
        OperationSetPersistentPresencePermissions opsetPermissions = null;
        OperationSetPersistentPresence opsetPresence;

        boolean isRootReadOnly = false;

        Object selectedItem = groupCombo.getSelectedItem();

        if(provider != null)
        {
            groupCombo.removeAllItems();

            opsetPermissions = provider.getOperationSet(
                OperationSetPersistentPresencePermissions.class);
            opsetPresence = provider.getOperationSet(
                OperationSetPersistentPresence.class);

            if(opsetPermissions != null
                && opsetPresence != null)
                isRootReadOnly =  opsetPermissions.isReadOnly(
                    opsetPresence.getServerStoredContactListRoot());
        }

        if(!isRootReadOnly)
        {
            groupCombo.addItem(GuiActivator.getContactListService().getRoot());
        }

        Iterator<MetaContactGroup> groupList
            = GuiActivator.getContactListService().getRoot().getSubgroups();

        while (groupList.hasNext())
        {
            MetaContactGroup group = groupList.next();

            if (!group.isPersistent())
                continue;

            if(provider != null && opsetPermissions != null)
            {
                Iterator<ContactGroup> protoGroupsIter =
                    group.getContactGroupsForProvider(provider);
                boolean foundWritableGroup = false;
                while(protoGroupsIter.hasNext())
                {
                    ContactGroup gr = protoGroupsIter.next();
                    if(!opsetPermissions.isReadOnly(gr))
                    {
                        foundWritableGroup = true;
                        break;
                    }
                }

                if(!foundWritableGroup)
                    continue;
            }

            groupCombo.addItem(group);
        }

        final String newGroupString = GuiActivator.getResources()
            .getI18NString("service.gui.CREATE_GROUP");

        if (!ConfigurationUtils.isCreateGroupDisabled()
            && !isRootReadOnly)
        {
            groupCombo.addItem(newGroupString);
        }

        if(selectedItem != null)
            groupCombo.setSelectedItem(selectedItem);
    }

    /**
     * Indicates that the "Add" buttons has been pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if (button.equals(addButton))
        {
            final ProtocolProviderService protocolProvider
                = (ProtocolProviderService) accountCombo.getSelectedItem();
            final String contactAddress = contactAddressField.getText().trim();
            final String displayName = displayNameField.getText();

            if (!protocolProvider.isRegistered())
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.ADD_CONTACT_NOT_CONNECTED"),
                    ErrorDialog.WARNING)
                .showDialog();

                return;
            }

            if (displayName != null && displayName.length() > 0)
            {
                addRenameListener(  protocolProvider,
                                    metaContact,
                                    contactAddress,
                                    displayName);
            }

            if (metaContact != null)
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        GuiActivator.getContactListService()
                            .addNewContactToMetaContact(
                                protocolProvider,
                                metaContact,
                                contactAddress);
                    }
                }.start();
            }
            else
            {
                ContactListUtils.addContact( protocolProvider,
                                            (MetaContactGroup) groupCombo
                                                .getSelectedItem(),
                                            contactAddress);
            }
        }
        dispose();
    }

    /**
     * Overwrites the dispose method in order to clean instances
     * of this window before closing it.
     */
    @Override
    public void dispose()
    {
        super.dispose();

        this.getContentPane().removeAll();

        this.accountLabel = null;

        this.accountCombo = null;

        this.groupLabel = null;

        this.contactAddressLabel = null;

        this.displayNameLabel = null;

        this.contactAddressField = null;

        this.displayNameField = null;

        this.addButton = null;

        this.cancelButton = null;

        this.imageLabel = null;

        this.groupCombo = null;

        this.initialized = false;
    }

    /**
     * Overwrites the setVisible method in order to init window before opening
     * it.
     * @param isVisible indicates if the dialog should be visible
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        if(!initialized)
            init();

        super.setVisible(isVisible);
    }

    /**
     * Indicates that this dialog is about to be closed.
     * @param isEscaped indicates if the dialog is closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Indicates that the window has gained the focus. Requests the focus in
     * the text field.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    public void windowGainedFocus(WindowEvent e)
    {
        if(!initialized)
            init();

        this.contactAddressField.requestFocus();
    }

    public void windowLostFocus(WindowEvent e) {}

    /**
     * A custom renderer displaying accounts in a combo box.
     */
    private static class AccountComboRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            if (value instanceof String)
            {
                setIcon(null);
                setText((String) value);
            }
            else if (value instanceof ProtocolProviderService)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) value;

                if (provider != null)
                {
                    Image protocolImg
                        = ImageUtils.getBytesInImage(provider.getProtocolIcon()
                            .getIcon(ProtocolIcon.ICON_SIZE_16x16));

                    if (protocolImg != null)
                        this.setIcon(ImageLoader.getIndexedProtocolIcon(
                                protocolImg, provider));

                    this.setText(provider.getAccountID().getDisplayName());
                }
            }

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    /**
     * A custom renderer displaying groups in a combo box.
     */
    private static class GroupComboRenderer
        extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            if (value instanceof String)
            {
                this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 0, 0)));
                this.setText((String) value);
            }
            else
            {
                this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
                MetaContactGroup group = (MetaContactGroup) value;

                if (group == null
                    || group.equals(GuiActivator
                            .getContactListService().getRoot()))
                {
                    this.setText(GuiActivator.getResources()
                        .getI18NString("service.gui.SELECT_NO_GROUP"));
                }
                else
                    this.setText(group.getGroupName());
            }

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    /**
     * Brings this window to the front.
     */
    public void bringToFront()
    {
        toFront();
    }

    /**
     * Returns this exported window identifier.
     * @return the identifier of this window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ADD_CONTACT_WINDOW;
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * This window can't be maximized.
     */
    public void maximize() {}

    /**
     * This window can't be minimized.
     */
    public void minimize() {}

    /**
     * This method can be called to pass any params to the exported window. This
     * method will be automatically called by
     * {@link UIService#getExportedWindow(WindowID, Object[])} in order to set
     * the parameters passed.
     *
     * @param windowParams the parameters to pass.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * Updates the state of the add button.
     */
    private void updateAddButtonState(boolean updateGroups)
    {
        String contactAddress = contactAddressField.getText();

        Object selectedItem = accountCombo.getSelectedItem();
        if (selectedItem instanceof ProtocolProviderService
            && contactAddress != null && contactAddress.length() > 0)
            addButton.setEnabled(true);
        else
            addButton.setEnabled(false);

        if(updateGroups && selectedItem instanceof ProtocolProviderService)
            updateGroupItems(groupCombo,
                (ProtocolProviderService)accountCombo.getSelectedItem());
    }

    /**
     * Reloads resources for this component.
     */
    public void loadSkin()
    {
        if(initialized)
        {
            imageLabel.setIcon(GuiActivator.getResources().getImage(
                    "service.gui.icons.ADD_CONTACT_DIALOG_ICON"));

            imageLabel.setVerticalAlignment(JLabel.TOP);
        }
    }

    /**
     * Adds a rename listener.
     *
     * @param protocolProvider the protocol provider to which the contact was
     * added
     * @param metaContact the <tt>MetaContact</tt> if the new contact was added
     * to an existing meta contact
     * @param contactAddress the address of the newly added contact
     * @param displayName the new display name
     */
    private void addRenameListener(
                                final ProtocolProviderService protocolProvider,
                                final MetaContact metaContact,
                                final String contactAddress,
                                final String displayName)
    {
        GuiActivator.getContactListService().addMetaContactListListener(
            new MetaContactListAdapter()
            {
                @Override
                public void metaContactAdded(MetaContactEvent evt)
                {
                    if (evt.getSourceMetaContact().getContact(
                            contactAddress, protocolProvider) != null)
                    {
                        renameContact(evt.getSourceMetaContact(), displayName);
                    }
                }

                @Override
                public void protoContactAdded(ProtoContactEvent evt)
                {
                    if (metaContact != null
                        && evt.getNewParent().equals(metaContact))
                    {
                        renameContact(metaContact, displayName);
                    }
                }
            });
    }

    /**
     * Renames the given meta contact.
     *
     * @param metaContact the <tt>MetaContact</tt> to rename
     * @param displayName the new display name
     */
    private void renameContact( final MetaContact metaContact,
                                final String displayName)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                GuiActivator.getContactListService()
                    .renameMetaContact( metaContact,
                                        displayName);
            }
        }.start();
    }
}
