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
import java.util.List;

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

        // Call menu.
        if (cDetail != null)
            add(initCallMenu());

        Component addContactComponent = initAddContactMenu();
        if (addContactComponent != null)
            add(addContactComponent);
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

    /**
     * Initializes the add contact menu item.
     * @return the add contact menu item
     */
    private Component initAddContactMenu()
    {
        Component addContactComponent = null;

        List<ContactDetail> details = sourceContact.getContactDetails();

        if (details.size() == 1)
        {
            addContactComponent
                = new JMenuItem(GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT"),
                    new ImageIcon(ImageLoader
                        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

            final ContactDetail detail = details.get(0);

            ((JMenuItem) addContactComponent)
                .addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    showAddContactDialog(detail);
                }
            });
        }
        // If we have more than one details we would propose a separate menu
        // item for each one of them.
        else if (details.size() > 1)
        {
            addContactComponent
                = new JMenu(GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT"));

            Iterator<ContactDetail> detailsIter = details.iterator();

            while (detailsIter.hasNext())
            {
                final ContactDetail detail = detailsIter.next();

                JMenuItem addMenuItem
                    = new JMenuItem(detail.getContactAddress());
                ((JMenu) addContactComponent).add(addMenuItem);

                addMenuItem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        showAddContactDialog(detail);
                    }
                });
            }
        }
        return addContactComponent;
    }

    /**
     * Creates and shows an <tt>AddContactDialog</tt> with a predefined
     * <tt>contactAddress</tt> and <tt>protocolProvider</tt>.
     * @param contactDetail the contact detail to be added
     */
    private void showAddContactDialog(ContactDetail contactDetail)
    {
        AddContactDialog dialog = new AddContactDialog(
            GuiActivator.getUIService().getMainFrame());

        // Try to obtain a preferred provider.
        ProtocolProviderService preferredProvider = null;
        List<Class<? extends OperationSet>> opSetClasses
            = contactDetail.getSupportedOperationSets();
        if (opSetClasses != null
            && opSetClasses.size() > 0)
        {
            preferredProvider
                = contactDetail.getPreferredProtocolProvider(
                    opSetClasses.get(0));
        }
        if (preferredProvider != null)
            dialog.setSelectedAccount(preferredProvider);

        dialog.setContactAddress(contactDetail.getContactAddress());
        dialog.setVisible(true);
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
