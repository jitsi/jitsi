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
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

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
    private JTextArea reasonArea = new JTextArea();

    private JButton inviteButton = new JButton(
        GuiActivator.getResources().getI18NString("invite"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("cancel"));

    private DefaultListModel contactListModel = new DefaultListModel();
    
    private DefaultListModel selectedContactListModel = new DefaultListModel();
    
    private ChatPanel chatPanel;
    
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

        String title
            = Messages.getI18NString("inviteContactToChat").getText();

        this.setTitle(title);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        JPanel northPanel = new JPanel(new BorderLayout(10, 10));

        mainPanel.setPreferredSize(new Dimension(450, 350));

        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

        this.reasonArea.setBorder(BorderFactory.createTitledBorder(
            GuiActivator.getResources().getI18NString("inviteReason")));

        JTextArea infoTextArea = new JTextArea();

        infoTextArea.setText(
            Messages.getI18NString("inviteContactFormInfo").getText());

        infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        infoTextArea.setLineWrap(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);

        JLabel iconLabel = new JLabel(new ImageIcon(
                ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));

        northPanel.add(iconLabel, BorderLayout.WEST);
        northPanel.add(infoTextArea, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        inviteButton.addActionListener(this);
        cancelButton.addActionListener(this);

        buttonsPanel.add(inviteButton);
        buttonsPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(inviteButton);
        inviteButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("invite"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("cancel"));

        MainFrame mainFrame = GuiActivator.getUIService().getMainFrame();

        final ContactList contactList = new ContactList(mainFrame);

        final ContactList selectedContactList = new ContactList(mainFrame);

        contactList.setModel(contactListModel);
        selectedContactList.setModel(selectedContactListModel);

        contactList.setMouseMotionListener(null);
        contactList.setMouseListener(new MouseAdapter()
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

        selectedContactList.setMouseMotionListener(null);
        selectedContactList.setMouseListener(new MouseAdapter()
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

        contactListScrollPane.getViewport().add(contactList);
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(
            SIPCommBorders.getRoundBorder());

        JScrollPane selectedListScrollPane = new JScrollPane();

        selectedListScrollPane.getViewport().add(selectedContactList);
        selectedListScrollPane.getViewport().setBorder(null);
        selectedListScrollPane.setViewportBorder(null);
        selectedListScrollPane.setBorder(
            SIPCommBorders.getRoundBorder());

        JPanel listPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        listPanel.setPreferredSize(new Dimension(400, 200));

        listPanel.add(contactListScrollPane);
        listPanel.add(selectedListScrollPane);

        this.initContactListData();

        // Add remove buttons panel.
        JPanel addRemoveButtonsPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        JButton addContactButton = new JButton(
            GuiActivator.getResources().getI18NString("add"));

        JButton removeContactButton = new JButton(
            GuiActivator.getResources().getI18NString("remove"));

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

        JPanel centerPanel = new JPanel(new BorderLayout());

        centerPanel.add(listPanel, BorderLayout.CENTER);
        centerPanel.add(addRemoveButtonsPanel, BorderLayout.SOUTH);

        JPanel southPanel = new JPanel(new BorderLayout());
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
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();

        if(button.equals(inviteButton))
        {
            ArrayList selectedContactAddresses = new ArrayList();

            Enumeration selectedContacts
                = selectedContactListModel.elements();

            while (selectedContacts.hasMoreElements())
            {
                MetaContact metaContact
                    = (MetaContact) selectedContacts.nextElement();

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
