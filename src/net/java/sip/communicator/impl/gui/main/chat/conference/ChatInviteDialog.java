/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
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
    extends InviteDialog
{
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
        super(GuiActivator.getResources()
            .getI18NString("service.gui.INVITE_CONTACT_TO_CHAT"));

        this.chatPanel = chatPanel;

        this.initContactListData();

        this.addInviteButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                inviteContacts();
                dispose();
            }
        });

        this.addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     */
    private void initContactListData()
    {
        this.inviteChatTransport = chatPanel.findInviteChatTransport();

        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();

        Iterator<MetaContact> contactListIter = metaContactListService
            .findAllMetaContactsForProvider(
                inviteChatTransport.getProtocolProvider());

        while (contactListIter.hasNext())
        {
            MetaContact metaContact = contactListIter.next();

            if(TreeContactList.presenceFilter.isMatching(metaContact))
                this.addMetaContact(metaContact);
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts()
    {
        java.util.List<String> selectedContactAddresses =
            new ArrayList<String>();

        // Obtain selected contacts.
        Enumeration<MetaContact> selectedContacts = getSelectedMetaContacts();

        if (selectedContacts != null)
        {
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
        }

        // Obtain selected strings.
        Enumeration<String> selectedStrings = getSelectedStrings();
        if (selectedStrings != null)
        {
            while (selectedStrings.hasMoreElements())
            {
                selectedContactAddresses.add(selectedStrings.nextElement());
            }
        }

        // Invite all selected.
        if (selectedContactAddresses.size() > 0)
        {
            chatPanel.inviteContacts(   inviteChatTransport,
                                        selectedContactAddresses,
                                        this.getReason());
        }
    }
}
