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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A dialog with room provider's contacts on the left and contacts
 * that has members role set for this chat room on the right.
 *
 * @author Damian Minkov
 */
public class MembersListDialog
    extends InviteDialog
    implements ActionListener
{
    /**
     * The chat room.
     */
    private final ChatRoomWrapper chatRoomWrapper;

    /**
     * The contact source used to be able to add simple string contact ids.
     * We can grant members role to users which are not part of our contact
     * list.
     */
    private StringContactSourceServiceImpl currentStringContactSource;

    /**
     * Constructs an <tt>MembersListDialog</tt>.
     *
     * @param chatRoomWrapper the room
     * @param title the title to show on the top of this dialog
     * @param enableReason
     */
    public MembersListDialog(
        ChatRoomWrapper chatRoomWrapper, String title, boolean enableReason)
    {
        super(title, enableReason);

        this.chatRoomWrapper = chatRoomWrapper;

        // change invite button text
        inviteButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.SAVE"));
        // change description text
        infoTextArea.setText(GuiActivator.getResources().getI18NString(
            "service.gui.CHAT_ROOM_CONFIGURATION_MEMBERS_EDIT_DESCRIPTION"));
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        initContactListData(chatRoomWrapper.getParentProvider()
            .getProtocolProvider());

        this.addInviteButtonListener(this);

        this.addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * When save button is pressed.
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e)
    {
        Collection<UIContact> selectedContacts
            = destContactList.getContacts(null);

        List<String> selectedMembers = new ArrayList<String>();

        if (selectedContacts != null)
        {
            for(UIContact c : selectedContacts)
            {
                Iterator<UIContactDetail> contactDetailsIter = c
                    .getContactDetailsForOperationSet(
                        OperationSetMultiUserChat.class).iterator();

                if (contactDetailsIter.hasNext())
                {
                    UIContactDetail inviteDetail
                        = contactDetailsIter.next();
                    selectedMembers.add(inviteDetail.getAddress());
                }
            }

            // save
            chatRoomWrapper.getChatRoom()
                .setMembersWhiteList(selectedMembers);

            dispose();
        }
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the right.
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData(
        final ProtocolProviderService protocolProvider)
    {
        this.setCurrentProvider(protocolProvider);

        srcContactList.removeAllContactSources();

        ContactSourceService currentProviderContactSource
            = new ProtocolContactSourceServiceImpl(
                    protocolProvider,
                    OperationSetMultiUserChat.class);
        currentStringContactSource
            = new StringContactSourceServiceImpl(
                    protocolProvider,
                    OperationSetMultiUserChat.class);
        currentStringContactSource.setDisableDisplayDetails(false);

        srcContactList.addContactSource(currentProviderContactSource);
        srcContactList.addContactSource(currentStringContactSource);

        srcContactList.applyDefaultFilter();

        // load in new thread, obtaining white list maybe slow as it involves
        // network operations
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                List<String> membersWhiteList =
                    MembersListDialog.this.chatRoomWrapper
                        .getChatRoom().getMembersWhiteList();

                UIContactSource uiContactSource =
                    srcContactList.getContactSource(currentStringContactSource);

                for(String member : membersWhiteList)
                {
                    SourceContact newSourceContact =
                        currentStringContactSource.createSourceContact(member);

                    destContactList.addContact(
                        new InviteUIContact(
                            uiContactSource.createUIContact(newSourceContact),
                            protocolProvider),
                        null, false, false);
                }
            }
        }).start();
    }
}
