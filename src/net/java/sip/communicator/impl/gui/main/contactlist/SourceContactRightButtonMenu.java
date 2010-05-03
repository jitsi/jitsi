/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The right button menu for external contact sources.
 * @see ExternalContactSource
 */
public class SourceContactRightButtonMenu
    extends JPopupMenu
{
    private final SourceContact sourceContact;

    /**
     * Creates an instance of <tt>SourceContactRightButtonMenu</tt> by
     * specifying the <tt>SourceContact</tt>, for which this menu is created.
     * @param sourceContact the <tt>SourceContact</tt>, for which this menu is
     * created
     */
    public SourceContactRightButtonMenu(SourceContact sourceContact)
    {
        this.sourceContact = sourceContact;

        this.initItems();
    }

    /**
     * Initializes menu items.
     */
    private void initItems()
    {
        ContactDetail cDetail = sourceContact
            .getPreferredContactDetail(OperationSetBasicTelephony.class);

        if (cDetail != null)
            add(initCallMenu());
    }

    /**
     * Initializes the call menu.
     * @return the call menu
     */
    private Component initCallMenu()
    {
        SIPCommMenu callContactMenu = new SIPCommMenu(
            GuiActivator.getResources().getI18NString("service.gui.CALL"));
        callContactMenu.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.CALL_16x16_ICON)));

        Iterator<ContactDetail> details
            = sourceContact.getContactDetails(OperationSetBasicTelephony.class)
                .iterator();

        while (details.hasNext())
        {
            final ContactDetail detail = details.next();
            // add all the contacts that support telephony to the call menu
            JMenuItem callContactItem = new JMenuItem();
            callContactItem.setText(detail.getContactAddress());
            callContactItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ProtocolProviderService protocolProvider
                        = detail.getPreferredProtocolProvider(
                            OperationSetBasicTelephony.class);

                    if (protocolProvider != null)
                        CallManager.createCall( protocolProvider,
                                                detail.getContactAddress());
                    else
                        CallManager.createCall(detail.getContactAddress());
                }
            });
            callContactMenu.add(callContactItem);
        }
        return callContactMenu;
    }

//    private Component initIMMenu()
//    {
//        SIPCommMenu callContactMenu = new SIPCommMenu(
//            GuiActivator.getResources().getI18NString(
//                "service.gui.SEND_MESSAGE"));
//        callContactMenu.setIcon(new ImageIcon(ImageLoader
//            .getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));
//
//        Iterator<ContactDetail> details
//            = sourceContact.getContactDetails(
//                OperationSetBasicInstantMessaging.class).iterator();
//
//        while (details.hasNext())
//        {
//            final ContactDetail detail = details.next();
//            // add all the contacts that support telephony to the call menu
//            JMenuItem callContactItem = new JMenuItem();
//            callContactItem.setName(detail.getContactAddress());
//            callContactItem.addActionListener(new ActionListener()
//            {
//                public void actionPerformed(ActionEvent e)
//                {
//                    ProtocolProviderService protocolProvider
//                        = detail.getPreferredProtocolProvider(
//                            OperationSetBasicInstantMessaging.class);
//
//                    if (protocolProvider != null)
//                        CallManager.createCall( protocolProvider,
//                                                detail.getContactAddress());
//                    else
//                        GuiActivator.getUIService().getChatWindowManager()
//                            .startChat(contactItem);
//                }
//            });
//            callContactMenu.add(callContactItem);
//        }
//        return callContactMenu;
//    }
}
