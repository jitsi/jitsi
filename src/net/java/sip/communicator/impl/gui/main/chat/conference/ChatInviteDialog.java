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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
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
        srcContactList.setDefaultFilter(
            new ChatInviteContactListFilter(srcContactList));

        srcContactList.applyDefaultFilter();
    }
    
    /**
     * The <tt>ChatInviteContactListFilter</tt> is 
     * <tt>InviteContactListFilter</tt> which doesn't list contact that don't 
     * have persistable addresses ( for example private messaging contacts are 
     * not listed).
     */
    private class ChatInviteContactListFilter extends InviteContactListFilter
    {
        /**
         * The Multi User Chat operation set instance.
         */
        private OperationSetMultiUserChat opSetMUC;
        
        /**
         * Creates an instance of <tt>InviteContactListFilter</tt>.
         *
         * @param sourceContactList the contact list to filter
         */
        public ChatInviteContactListFilter(ContactList sourceContactList)
        {
            super(sourceContactList);
            opSetMUC = inviteChatTransport
                .getProtocolProvider().getOperationSet(
                    OperationSetMultiUserChat.class);
        }
        
        @Override public boolean isMatching(UIContact uiContact) 
        {
            SourceContact contact = (SourceContact)uiContact.getDescriptor();
            if(opSetMUC.isPrivateMessagingContact(
                contact.getContactAddress()))
            {
                return false;
            }
            return true;
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts()
    {
        Collection<String> selectedContactAddresses = new ArrayList<String>();

        // Obtain selected contacts.
        Collection<UIContact> contacts = destContactList.getContacts(null);
        if(contacts == null)
            return;

        Iterator<UIContact> selectedContacts = contacts.iterator();

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
