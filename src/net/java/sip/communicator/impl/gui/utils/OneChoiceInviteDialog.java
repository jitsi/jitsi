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
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * The invite dialog is a widget that shows a list of contacts, from which the
 * user could pick in order to create a conference chat or call.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class OneChoiceInviteDialog
    extends SIPCommDialog
    implements ContactListContainer
{
    /**
     * The information text area.
     */
    private final JTextArea infoTextArea;

    /**
     * The label containing the icon of this dialog.
     */
    private final JLabel infoIconLabel = new JLabel();

    /**
     * The OK button.
     */
    private final JButton okButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The contact list.
     */
    protected ContactList contactList;

    /**
     * The search field.
     */
    private final SearchField searchField;

    /**
     * Constructs an <tt>OneChoiceInviteDialog</tt>.
     * @param title the title to show on the top of this dialog
     */
    public OneChoiceInviteDialog (String title)
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

        infoTextArea = createInfoArea();

        northPanel.add(infoIconLabel, BorderLayout.WEST);
        northPanel.add(infoTextArea, BorderLayout.CENTER);

        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(okButton);
        okButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        Component contactListComponent = createContactListComponent();

        ContactListSearchFilter inviteFilter
            = new InviteContactListFilter(contactList);

        contactList.setDefaultFilter(inviteFilter);

        searchField = new SearchField(null, inviteFilter, false, false);
        searchField.setPreferredSize(new Dimension(200, 25));
        searchField.setContactList(contactList);
        searchField.addFocusListener(new FocusAdapter()
        {
            /**
             * Removes all other selections.
             * @param e the <tt>FocusEvent</tt> that notified us
             */
            @Override
            public void focusGained(FocusEvent e)
            {
                contactList.removeSelection();
            }
        });

        TransparentPanel listPanel = new TransparentPanel(new BorderLayout());
        listPanel.setBorder(SIPCommBorders.getRoundBorder());
        listPanel.add(contactListComponent);
        
        northPanel.add(searchField, BorderLayout.SOUTH);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        KeyboardFocusManager keyManager
            = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        ContactListSearchKeyDispatcher clKeyDispatcher
                        = new ContactListSearchKeyDispatcher(   keyManager,
                                                                searchField,
                                                                this);

        clKeyDispatcher.setContactList(contactList);

        keyManager.addKeyEventDispatcher(clKeyDispatcher);
    }

    /**
     * Returns an enumeration of the list of selected <tt>MetaContact</tt>s.
     * @return an enumeration of the list of selected <tt>MetaContact</tt>s
     */
    public UIContact getSelectedContact()
    {
        return contactList.getSelectedContact();
    }

    /**
     * Returns an enumeration of the list of selected Strings.
     * @return an enumeration of the list of selected Strings
     */
    public String getSelectedString()
    {
        return searchField.getText();
    }

    /**
     * Sets the information text explaining how to use the containing form.
     * @param text the text
     */
    public void setInfoText(String text)
    {
        infoTextArea.setText(text);
    }

    /**
     * Sets the icon shown in the left top corner of this dialog.
     * @param icon the icon
     */
    public void setIcon(Icon icon)
    {
        infoIconLabel.setIcon(icon);
    }

    /**
     * Sets the text of the ok button.
     * @param text the text of the ok button
     */
    public void setOkButtonText(String text)
    {
        okButton.setText(text);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Invite" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addOkButtonListener(ActionListener l)
    {
        this.okButton.addActionListener(l);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Cancel" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addCancelButtonListener(ActionListener l)
    {
        this.cancelButton.addActionListener(l);
    }

    /**
     * Closes this dialog by clicking on the "Cancel" button.
     * @param isEscaped indicates if this <tt>close</tt> is provoked by an
     * escape
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Creates the an info text area.
     * @return the created <tt>JTextArea</tt>
     */
    private JTextArea createInfoArea()
    {
        JTextArea infoTextArea = new JTextArea();

        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setLineWrap(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);

        return infoTextArea;
    }

    /**
     * Creates the contact list component.
     * @return the created contact list component
     */
    private Component createContactListComponent()
    {
        contactList
            = GuiActivator.getUIService().createContactListComponent(this);

        contactList.setContactButtonsVisible(false);
        contactList.setRightButtonMenuEnabled(false);
        contactList.addContactListListener(new ContactListListener()
        {
            public void groupSelected(ContactListEvent evt) {}

            public void groupClicked(ContactListEvent evt) {}

            public void contactSelected(ContactListEvent evt) {}

            public void contactClicked(ContactListEvent evt)
            {
                int clickCount = evt.getClickCount();

                if (clickCount > 1)
                {
                    okButton.doClick();
                }
            }
        });

        // By default we set the current filter to be the presence filter.
        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(contactList.getComponent());
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(null);

        return contactListScrollPane;
    }

    /**
     * Adds the given contact to this contact list.
     *
     * @param contact
     */
    protected void addContact(UIContact contact)
    {
        contactList.addContact(contact, null, true, false);
    }

    /**
     * Called when the ENTER key was typed when this container was the focused
     * container. Performs the appropriate actions depending on the current
     * state of the contained contact list.
     */
    public void enterKeyTyped()
    {
        UIContact selectedContact = contactList.getSelectedContact();

        if (selectedContact != null)
        {
            okButton.doClick();
        }
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
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this
     * container was the focused container. Performs the appropriate actions
     * depending on the current state of the contained contact list.
     */
    public void ctrlEnterKeyTyped() {}

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
