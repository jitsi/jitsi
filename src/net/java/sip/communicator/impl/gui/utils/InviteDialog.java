/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The invite dialog is a widget that shows a list of contacts, from which the
 * user could pick in order to create a conference chat or call.
 *
 * @author Yana Stamcheva
 */
public class InviteDialog
    extends SIPCommDialog
{
    private final JTextArea reasonArea = new JTextArea();

    private final JButton inviteButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.INVITE"));

    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final DefaultListModel contactListModel = new DefaultListModel();

    private final DefaultListModel selectedContactListModel
        = new DefaultListModel();

    /**
     * Constructs an <tt>InviteDialog</tt>, by specifying the initial list of
     * contacts available for invite.
     *
     * @param title the title to show on the top of this dialog
     * @param metaContacts the list of contacts available for invite
     */
    public InviteDialog(String title, java.util.List<MetaContact> metaContacts)
    {
        this(title);

        // Initialize contacts list.
        for(MetaContact metaContact : metaContacts)
        {
            this.addMetaContact(metaContact);
        }
    }

    /**
     * Constructs an <tt>InviteDialog</tt>.
     * @param title the title to show on the top of this dialog
     */
    public InviteDialog (String title)
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

        this.reasonArea.setBorder(BorderFactory.createTitledBorder(
            GuiActivator.getResources()
                .getI18NString("service.gui.INVITE_REASON")));

        JTextArea infoTextArea = new JTextArea();

        infoTextArea.setText(GuiActivator.getResources()
            .getI18NString("service.gui.INVITE_CONTACT_MSG"));

        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setLineWrap(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);

        JLabel iconLabel = new JLabel(new ImageIcon(
                ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));

        northPanel.add(iconLabel, BorderLayout.WEST);
        northPanel.add(infoTextArea, BorderLayout.CENTER);

        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonsPanel.add(inviteButton);
        buttonsPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(inviteButton);
        inviteButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.INVITE"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        final DefaultContactList contactList = new DefaultContactList();
        final DefaultContactList selectedContactList = new DefaultContactList();

        contactList.setModel(contactListModel);
        selectedContactList.setModel(selectedContactListModel);

        contactList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() > 1)
                {
                    Object[] metaContacts = contactList.getSelectedValues();

                    moveContactsFromLeftToRight(metaContacts);
                }
            }
        });

        selectedContactList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() > 1)
                {
                    Object[] metaContacts
                        = selectedContactList.getSelectedValues();

                    moveContactsFromRightToLeft(metaContacts);
                }
            }
        });

        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(contactList);
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(
            SIPCommBorders.getRoundBorder());

        JScrollPane selectedListScrollPane = new JScrollPane();

        selectedListScrollPane.setOpaque(false);
        selectedListScrollPane.getViewport().setOpaque(false);
        selectedListScrollPane.getViewport().add(selectedContactList);
        selectedListScrollPane.getViewport().setBorder(null);
        selectedListScrollPane.setViewportBorder(null);
        selectedListScrollPane.setBorder(
            SIPCommBorders.getRoundBorder());

        JPanel listPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        listPanel.setPreferredSize(new Dimension(400, 200));

        listPanel.add(contactListScrollPane);
        listPanel.add(selectedListScrollPane);
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
                Object[] metaContacts = contactList.getSelectedValues();

                if (metaContacts != null && metaContacts.length > 0)
                    moveContactsFromLeftToRight(metaContacts);
            }
        });

        removeContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Object[] metaContacts = selectedContactList.getSelectedValues();

                if (metaContacts != null && metaContacts.length > 0)
                    moveContactsFromRightToLeft(metaContacts);
            }
        });

        TransparentPanel centerPanel = new TransparentPanel(new BorderLayout());

        centerPanel.add(listPanel, BorderLayout.CENTER);
        centerPanel.add(addRemoveButtonsPanel, BorderLayout.SOUTH);

        TransparentPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(reasonArea, BorderLayout.CENTER);
        southPanel.add(buttonsPanel, BorderLayout.SOUTH);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Adds the given <tt>metaContact</tt> to the left list of contacts
     * available for invite.
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    public void addMetaContact(MetaContact metaContact)
    {
        contactListModel.addElement(metaContact);
    }

    /**
     * Removes the given <tt>metaContact</tt> from the left list of contacts
     * available for invite.
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    public void removeMetaContact(MetaContact metaContact)
    {
        contactListModel.removeElement(metaContact);
    }

    /**
     * Removes all <tt>MetaContact</tt>-s from the left list of contacts
     * available for invite.
     */
    public void removeAllMetaContacts()
    {
        contactListModel.removeAllElements();
    }

    /**
     * Returns an enumeration of the list of selected contacts.
     * @return an enumeration of the list of selected contacts.
     */
    public Enumeration<MetaContact> getSelectedMetaContacts()
    {
        return (Enumeration<MetaContact>) selectedContactListModel.elements();
    }

    /**
     * Returns the reason of this invite, if the user has specified one.
     * @return the reason of this invite
     */
    public String getReason()
    {
        return reasonArea.getText();
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Invite" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addInviteButtonListener(ActionListener l)
    {
        this.inviteButton.addActionListener(l);
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
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Moves contacts from the left list to the right.
     *
     * @param metaContacts the contacts to move.
     */
    private void moveContactsFromLeftToRight(Object[] metaContacts)
    {
        for (Object metaContact : metaContacts)
        {
            contactListModel.removeElement(metaContact);

            selectedContactListModel.addElement(metaContact);
        }
    }

    /**
     * Moves a contact from the right list to the left.
     *
     * @param metaContacts the contact to move.
     */
    private void moveContactsFromRightToLeft(Object[] metaContacts)
    {
        for (Object metaContact : metaContacts)
        {
            selectedContactListModel.removeElement(metaContact);

            contactListModel.addElement(metaContact);
        }
    }
}
