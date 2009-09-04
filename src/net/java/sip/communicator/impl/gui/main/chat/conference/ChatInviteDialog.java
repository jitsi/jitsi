/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The invite dialog is the one shown when the user clicks on the conference
 * button in the chat toolbar.
 *
 * @author Yana Stamcheva
 */
public class ChatInviteDialog
    extends SIPCommDialog
    implements ActionListener
{
    private final JTextArea reasonArea = new JTextArea();

    private final JButton inviteButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.INVITE"));

    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final DefaultListModel contactListModel = new DefaultListModel();

    private final DefaultListModel selectedContactListModel
        = new DefaultListModel();

    private final ChatPanel chatPanel;

    private ChatTransport inviteChatTransport;

    /**
     * Constructs the <tt>ChatInviteDialog</tt>.
     *
     * @param chatPanel the <tt>ChatPanel</tt> corresponding to the
     * <tt>ChatRoom</tt>, where the contact is invited.
     */
    public ChatInviteDialog (ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;

        this.setModal(false);

        String title = GuiActivator.getResources()
            .getI18NString("service.gui.INVITE_CONTACT_TO_CHAT");

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

        inviteButton.addActionListener(this);
        cancelButton.addActionListener(this);

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
                    MetaContact metaContact
                        = (MetaContact) contactList.getSelectedValue();

                    moveContactFromLeftToRight(metaContact);
                }
            }
        });

        selectedContactList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() > 1)
                {
                    MetaContact metaContact
                        = (MetaContact) selectedContactList.getSelectedValue();

                    moveContactFromRightToLeft(metaContact);
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

        this.initContactListData();

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
                MetaContact metaContact
                    = (MetaContact) contactList.getSelectedValue();

                if (metaContact != null)
                    moveContactFromLeftToRight(metaContact);
            }
        });

        removeContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MetaContact metaContact
                    = (MetaContact) selectedContactList.getSelectedValue();

                if (metaContact != null)
                    moveContactFromRightToLeft(metaContact);
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
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     */
    private void initContactListData()
    {
        this.inviteChatTransport = chatPanel.findInviteChatTransport();

        MetaContactListService metaContactListService
            = GuiActivator.getMetaContactListService();

        Iterator<MetaContact> contactListIter = metaContactListService
            .findAllMetaContactsForProvider(
                inviteChatTransport.getProtocolProvider());

        while (contactListIter.hasNext())
        {
            MetaContact metaContact = contactListIter.next();

            contactListModel.addElement(metaContact);
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks
     * on one of the buttons.
     */
    @SuppressWarnings("unchecked") //legacy DefaultListModel code.
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();

        if(button.equals(inviteButton))
        {
            java.util.List<String> selectedContactAddresses =
                new ArrayList<String>();

            Enumeration<MetaContact> selectedContacts
                = (Enumeration<MetaContact>) selectedContactListModel.elements();

            while (selectedContacts.hasMoreElements())
            {
                MetaContact metaContact
                    = selectedContacts.nextElement();

                Iterator<Contact> contactsIter = metaContact
                    .getContactsForProvider(
                        inviteChatTransport.getProtocolProvider());

                // We invite the first protocol contact that corresponds to the
                // invite provider.
                if (contactsIter.hasNext())
                {
                    Contact inviteContact = contactsIter.next();

                    selectedContactAddresses.add(inviteContact.getAddress());
                }
            }

            chatPanel.inviteContacts(   inviteChatTransport,
                                        selectedContactAddresses,
                                        reasonArea.getText());
        }

        this.dispose();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Moves a contact from the left list to the right.
     *
     * @param metaContact the contact to move.
     */
    private void moveContactFromLeftToRight(MetaContact metaContact)
    {
        contactListModel.removeElement(metaContact);

        selectedContactListModel.addElement(metaContact);
    }

    /**
     * Moves a contact from the right list to the left.
     *
     * @param metaContact the contact to move.
     */
    private void moveContactFromRightToLeft(MetaContact metaContact)
    {
        selectedContactListModel.removeElement(metaContact);

        contactListModel.addElement(metaContact);
    }
}
