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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a <tt>Dialog</tt> which allows specifying the target contact
 * address of a transfer-call operation.
 *
 * @author Yana Stamcheva
 */
public class TransferCallDialog
    extends OneChoiceInviteDialog
{
    /**
     * The peer to transfer.
     */
    private final CallPeer transferPeer;

    /**
     * Creates a <tt>TransferCallDialog</tt> by specifying the peer to transfer
     * @param peer the peer to transfer
     */
    public TransferCallDialog(final CallPeer peer)
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER_CALL_TITLE"));

        this.transferPeer = peer;

        this.initContactListData(peer.getProtocolProvider());

        this.setInfoText(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER_CALL_MSG"));
        this.setOkButtonText(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER"));

        this.setMinimumSize(new Dimension(300, 300));

        addOkButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIContact uiContact = getSelectedContact();

                if (uiContact != null)
                {
                    transferToContact(uiContact);
                }

                setVisible(false);
                dispose();
            }
        });
        addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        });
    }

    /**
     * Initializes contact list sources.
     */
    private void initContactSources()
    {
        DemuxContactSourceService demuxCSService
             = GuiActivator.getDemuxContactSourceService();

        // If the DemuxContactSourceService isn't registered we use the default
        // contact source set.
        if (demuxCSService == null)
            return;

        Iterator<UIContactSource> sourcesIter
            = new ArrayList<UIContactSource>(
                contactList.getContactSources()).iterator();

        contactList.removeAllContactSources();

        while (sourcesIter.hasNext())
        {
            ContactSourceService contactSource
                = sourcesIter.next().getContactSourceService();

            contactList.addContactSource(
                demuxCSService.createDemuxContactSource(contactSource));
        }
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     *
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData(ProtocolProviderService protocolProvider)
    {
        initContactSources();

        contactList.addContactSource(
            new ProtocolContactSourceServiceImpl(
                protocolProvider, OperationSetBasicTelephony.class));
        contactList.addContactSource(
            new StringContactSourceServiceImpl(
                protocolProvider, OperationSetBasicTelephony.class));

        contactList.applyDefaultFilter();
    }

    /**
     * Transfer the transfer peer to the given <tt>UIContact</tt>.
     *
     * @param uiContact the contact to transfer to
     */
    private void transferToContact(UIContact uiContact)
    {
        UIContactDetail contactDetail = uiContact
            .getDefaultContactDetail(
                OperationSetBasicTelephony.class);

        CallManager.transferCall(   transferPeer,
                                    contactDetail.getAddress());
    }
}
