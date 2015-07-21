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
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

import java.util.*;
import java.util.List;
/**
 * The invite dialog is a widget that shows a list of contacts, from which the
 * user could pick in order to create a conference chat or call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class InviteDialog
    extends SIPCommDialog
    implements  Skinnable,
                ContactListContainer
{
    private static final long serialVersionUID = 0L;

    /**
     * Text area where user can specify a reason for the invitation.
     */
    private final JTextArea reasonArea = new JTextArea();

    /**
     * The button, which performs the invite.
     */
    protected final JButton inviteButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.INVITE"));

    /**
     * The button, which cancels the operation.
     */
    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The search field.
     */
    private final SearchField searchField;

    /**
     * The source contact list.
     */
    protected ContactList srcContactList;

    /**
     * The destination contact list.
     */
    protected ContactList destContactList;

    /**
     * The invite contact transfer handler.
     */
    private InviteContactTransferHandler inviteContactTransferHandler;

    /**
     * Currently selected protocol provider.
     */
    private ProtocolProviderService currentProvider;

    /**
     * Icon label.
     */
    private final JLabel iconLabel;

    /**
     * The description text.
     */
    protected final JTextArea infoTextArea;

    /**
     * Constructs an <tt>InviteDialog</tt>.
     *
     * @param title the title to show on the top of this dialog
     */
    public InviteDialog (String title, boolean enableReason)
    {
        this.setModal(false);

        this.setTitle(title);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel northPanel
            = new TransparentPanel(new BorderLayout(10, 10));

        mainPanel.setPreferredSize(new Dimension(450, 350));

        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

        infoTextArea = new JTextArea();

        infoTextArea.setText(GuiActivator.getResources()
            .getI18NString("service.gui.INVITE_CONTACT_MSG"));

        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setLineWrap(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);

        iconLabel = new JLabel(new ImageIcon(
                ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));

        northPanel.add(iconLabel, BorderLayout.WEST);
        northPanel.add(infoTextArea, BorderLayout.CENTER);

        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonsPanel.add(inviteButton);
        buttonsPanel.add(cancelButton);

        inviteButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.INVITE"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        Component contactListComponent = createSrcContactListComponent();

        ContactListSearchFilter inviteFilter
            = new InviteContactListFilter(srcContactList);

        srcContactList.setDefaultFilter(inviteFilter);

        searchField = new SearchField(null, inviteFilter, false, false);
        searchField.setPreferredSize(new Dimension(200, 25));
        searchField.setContactList(srcContactList);
        searchField.addFocusListener(new FocusAdapter()
        {
            /**
             * Removes all other selections.
             * @param e the <tt>FocusEvent</tt> that notified us
             */
            @Override
            public void focusGained(FocusEvent e)
            {
                srcContactList.removeSelection();
            }
        });

        TransparentPanel leftPanel
            = new TransparentPanel(new BorderLayout(5, 5));
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(contactListComponent);

        JPanel listPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        listPanel.setPreferredSize(new Dimension(400, 200));

        listPanel.add(leftPanel);
        listPanel.add(createDestContactListComponent());
        listPanel.setOpaque(false);

        // Add remove buttons panel.
        JPanel addRemoveButtonsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        addRemoveButtonsPanel.setOpaque(false);

        JButton addContactButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.ADD"));

        JButton removeContactButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.REMOVE"));

        addRemoveButtonsPanel.add(addContactButton);
        addRemoveButtonsPanel.add(removeContactButton);

        addContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<UIContact> selectedContacts
                    = srcContactList.getSelectedContacts();

                if (selectedContacts != null && selectedContacts.size() > 0)
                    moveContactsFromLeftToRight(selectedContacts.iterator());
            }
        });

        removeContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<UIContact> selectedContacts
                    = destContactList.getSelectedContacts();

                if (selectedContacts != null && selectedContacts.size() > 0)
                    moveContactsFromRightToLeft(selectedContacts.iterator());
            }
        });

        TransparentPanel centerPanel = new TransparentPanel(new BorderLayout());

        centerPanel.add(listPanel, BorderLayout.CENTER);
        centerPanel.add(addRemoveButtonsPanel, BorderLayout.SOUTH);

        TransparentPanel southPanel
            = new TransparentPanel(new BorderLayout());
        southPanel.add(buttonsPanel, BorderLayout.SOUTH);

        if (enableReason)
        {
            this.reasonArea.setBorder(BorderFactory.createTitledBorder(
                GuiActivator.getResources()
                    .getI18NString("service.gui.INVITE_REASON")));

            southPanel.add(reasonArea, BorderLayout.CENTER);
        }

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        initTransferHandler();

        KeyboardFocusManager keyManager
            = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        ContactListSearchKeyDispatcher clKeyDispatcher
                        = new ContactListSearchKeyDispatcher(   keyManager,
                                                                searchField,
                                                                this);

        clKeyDispatcher.setContactList(srcContactList);

        keyManager.addKeyEventDispatcher(clKeyDispatcher);
    }

    /**
     * Returns the reason of this invite, if the user has specified one.
     *
     * @return the reason of this invite
     */
    public String getReason()
    {
        return reasonArea.getText();
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Invite" button.
     *
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addInviteButtonListener(ActionListener l)
    {
        this.inviteButton.addActionListener(l);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Cancel" button.
     *
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addCancelButtonListener(ActionListener l)
    {
        this.cancelButton.addActionListener(l);
    }

    /**
     * Closes this dialog by clicking on the "Cancel" button.
     *
     * @param isEscaped indicates if this <tt>close</tt> is provoked by an
     * escape
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Sets the current provider selected for this invite dialog.
     *
     * @param protocolProvider the protocol provider selected for this invite
     * dialog
     */
    protected void setCurrentProvider(ProtocolProviderService protocolProvider)
    {
        this.currentProvider = protocolProvider;

        inviteContactTransferHandler.setBackupProvider(currentProvider);
    }

    /**
     * Moves contacts from the left list to the right.
     *
     * @param contacts an Iterator over a list of <tt>UIContact</tt>s
     */
    private void moveContactsFromLeftToRight(Iterator<UIContact> contacts)
    {
        while (contacts.hasNext())
        {
            moveContactFromLeftToRight(contacts.next());
        }
    }

    /**
     * Moves the given <tt>UIContact</tt> from left list to the right.
     *
     * @param uiContact the contact to move
     */
    protected void moveContactFromLeftToRight(UIContact uiContact)
    {
        destContactList.addContact(
            new InviteUIContact(uiContact, currentProvider), null, false, false);
    }

    /**
     * Moves contacts from the right list to the left.
     *
     * @param contacts an Iterator over a list of <tt>UIContact</tt>s
     */
    protected void moveContactsFromRightToLeft(Iterator<UIContact> contacts)
    {
        while (contacts.hasNext())
        {
            moveContactFromRightToLeft(contacts.next());
        }
    }

    /**
     * Moves the given <tt>UIContact</tt> from left list to the right.
     *
     * @param uiContact the contact to move
     */
    private void moveContactFromRightToLeft(UIContact uiContact)
    {
        destContactList.removeContact(uiContact);
    }

    /**
     * Reloads icon for icon label.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));
    }

    /**
     * Creates the source contact list component.
     *
     * @return the created contact list component
     */
    private Component createSrcContactListComponent()
    {
        srcContactList
            = GuiActivator.getUIService().createContactListComponent(this);

        srcContactList.setDragEnabled(true);
        srcContactList.setRightButtonMenuEnabled(false);
        srcContactList.setContactButtonsVisible(false);
        srcContactList.setMultipleSelectionEnabled(true);
        srcContactList.addContactListListener(new ContactListListener()
        {
            public void groupSelected(ContactListEvent evt) {}

            public void groupClicked(ContactListEvent evt) {}

            public void contactSelected(ContactListEvent evt) {}

            public void contactClicked(ContactListEvent evt)
            {
                if (evt.getClickCount() > 1)
                    moveContactFromLeftToRight(evt.getSourceContact());
            }
        });

        // By default we set the current filter to be the presence filter.
        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(srcContactList.getComponent());
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(null);

        return contactListScrollPane;
    }

    /**
     * Creates the destination contact list component.
     *
     * @return the created contact list component
     */
    private Component createDestContactListComponent()
    {
        destContactList
            = GuiActivator.getUIService().createContactListComponent(this);

        destContactList.removeAllContactSources();
        destContactList.setContactButtonsVisible(false);
        destContactList.setRightButtonMenuEnabled(false);
        destContactList.setMultipleSelectionEnabled(true);
        destContactList.addContactListListener(new ContactListListener()
        {
            public void groupSelected(ContactListEvent evt) {}

            public void groupClicked(ContactListEvent evt) {}

            public void contactSelected(ContactListEvent evt) {}

            public void contactClicked(ContactListEvent evt)
            {
                if (evt.getClickCount() > 1)
                    moveContactFromRightToLeft(evt.getSourceContact());
            }
        });

        // By default we set the current filter to be the presence filter.
        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(destContactList.getComponent());
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(null);

        return contactListScrollPane;
    }

    /**
     * Called when the ENTER key was typed when this container was the focused
     * container. Performs the appropriate actions depending on the current
     * state of the contained contact list.
     */
    public void enterKeyTyped()
    {
        List<UIContact> selectedContacts = srcContactList.getSelectedContacts();

        if (selectedContacts == null)
            return;

        moveContactsFromLeftToRight(selectedContacts.iterator());
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this
     * container was the focused container. Performs the appropriate actions
     * depending on the current state of the contained contact list.
     */
    public void ctrlEnterKeyTyped() {}

    /**
     * Initializes the transfer handler.
     */
    private void initTransferHandler()
    {
        inviteContactTransferHandler
            = new InviteContactTransferHandler(
                destContactList,
                InviteContactTransferHandler.DEST_TRANSFER_HANDLER,
                true);

        InviteContactTransferHandler srcContactTransferHandler
            = new InviteContactTransferHandler(
                srcContactList,
                InviteContactTransferHandler.SOURCE_TRANSFER_HANDLER,
                true);

        if (srcContactList.getComponent() instanceof JComponent)
            ((JComponent) srcContactList).setTransferHandler(
                srcContactTransferHandler);

        if (destContactList.getComponent() instanceof JComponent)
            ((JComponent) destContactList).setTransferHandler(
                inviteContactTransferHandler);
    }

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    public String getCurrentSearchText()
    {
        return searchField.getText();
    }

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText()
    {
        searchField.setText("");
    }

    /**
     * Returns <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise
     */
    public boolean isMenuSelected()
    {
        // This dialog has no menu bar so it will never be selected
        return false;
    }
    
    @Override
    public void dispose()
    {
        searchField.dispose();
        super.dispose();
    }
}
