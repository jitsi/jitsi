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
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
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
            .getI18NString("service.gui.INVITE_CONTACT_TO_CHAT"), true);

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

        srcContactList.addContactSource(
            new ProtocolContactSourceServiceImpl(
                inviteChatTransport.getProtocolProvider(),
                OperationSetMultiUserChat.class));

        srcContactList.applyDefaultFilter();
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts()
    {
        Collection<String> selectedContactAddresses = new ArrayList<String>();

        // Obtain selected contacts.
        Iterator<UIContact> selectedContacts
            = destContactList.getContacts(null).iterator();

        if (selectedContacts != null)
        {
            while (selectedContacts.hasNext())
            {
                UIContact uiContact = selectedContacts.next();

                Iterator<UIContactDetail> contactsIter
                    = uiContact.getContactDetailsForOperationSet(
                        OperationSetMultiUserChat.class).iterator();

                // We invite the first protocol contact that corresponds to the
                // invite provider.
                if (contactsIter.hasNext())
                {
                    UIContactDetail inviteDetail = contactsIter.next();

                    selectedContactAddresses.add(inviteDetail.getAddress());
                }
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
