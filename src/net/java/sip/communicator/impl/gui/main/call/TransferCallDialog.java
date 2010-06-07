/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a <code>Dialog</code> which allows specifying the target contact
 * address of a transfer-call operation.
 * 
 * @author Yana Stamcheva
 */
public class TransferCallDialog
    extends OneChoiceInviteDialog
{
    /**
     * Creates a <tt>TransferCallDialog</tt> by specifying the peer to transfer
     * @param peer the peer to transfer
     */
    public TransferCallDialog(final CallPeer peer)
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER_CALL_TITLE"));

        this.initContactListData(peer.getProtocolProvider());

        this.setInfoText(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER_CALL_MSG"));
        this.setOkButtonText(GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER"));

        addOkButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String transferString = getSelectedString();

                if (transferString != null && transferString.length() > 0)
                    CallManager.transferCall(peer, transferString);
                else
                {
                    MetaContact metaContact = getSelectedMetaContact();

                    if (metaContact != null)
                    {
                        Iterator<Contact> contactsIter = metaContact
                            .getContactsForProvider(peer.getProtocolProvider());

                        if (contactsIter.hasNext())
                            CallManager.transferCall(peer,
                                contactsIter.next().getAddress());
                    }
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
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData(ProtocolProviderService protocolProvider)
    {
        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();

        Iterator<MetaContact> contactListIter = metaContactListService
            .findAllMetaContactsForProvider(protocolProvider);

        while (contactListIter.hasNext())
        {
            MetaContact metaContact = contactListIter.next();

            this.addMetaContact(metaContact);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.impl.gui.customcontrols.SIPCommDialog#close
     * (boolean)
     */
    protected void close(boolean isEscaped) {}
}
