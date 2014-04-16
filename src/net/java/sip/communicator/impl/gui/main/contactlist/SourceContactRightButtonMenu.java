/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The right button menu for external contact sources.
 * @see ExternalContactSource
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SourceContactRightButtonMenu
    extends SIPCommPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The source UI contact.
     */
    private final SourceUIContact sourceUIContact;

    /**
     * The source contact.
     */
    private final SourceContact sourceContact;

    /**
     * Call contact menu.
     */
    private SIPCommMenu callContactMenu;

    /**
     * Contact details menu.
     */
    private SIPCommMenu contactDetailsMenu;

    /**
     * Add contact component.
     */
    private Component addContactComponent;

    /**
     * Creates an instance of <tt>SourceContactRightButtonMenu</tt> by
     * specifying the <tt>SourceUIContact</tt>, for which this menu is created.
     * @param sourceUIContact the <tt>SourceUIContact</tt>, for which this menu
     * is created
     */
    public SourceContactRightButtonMenu(SourceUIContact sourceUIContact)
    {
        this.sourceUIContact = sourceUIContact;

        this.sourceContact = (SourceContact)sourceUIContact.getDescriptor();

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
        if (cDetail != null
            && sourceContact.getContactSource().getType()
                != ContactSourceService.RECENT_MESSAGES_TYPE)
        {
            add(initCallMenu());
        }

        // Only create the menu if the add contact functionality is enabled.
        if (!GuiActivator.getMUCService().isMUCSourceContact(sourceContact)
            && !ConfigurationUtils.isAddContactDisabled())
        {
            addContactComponent
                = TreeContactList.createAddContactMenu(sourceContact);
        }

        if (addContactComponent != null)
            add(addContactComponent);

        for(JMenuItem item :
            sourceUIContact.getContactCustomActionMenuItems(true))
        {
            add(item);
        }
    }

    /**
     * Initializes the call menu.
     * @return the call menu
     */
    private Component initCallMenu()
    {
        callContactMenu = new SIPCommMenu(
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
            callContactItem.setText(detail.getDetail());
            callContactItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    List<ProtocolProviderService> providers
                        = AccountUtils.getOpSetRegisteredProviders(
                            OperationSetBasicTelephony.class,
                            detail.getPreferredProtocolProvider(
                                OperationSetBasicTelephony.class),
                            detail.getPreferredProtocol(
                                OperationSetBasicTelephony.class));

                    int providersCount = providers.size();

                    if (providers == null || providersCount <= 0)
                    {
                        new ErrorDialog(null,
                            GuiActivator.getResources().getI18NString(
                                "service.gui.CALL_FAILED"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                            .showDialog();
                        return;
                    }
                    else if (providersCount > 1)
                    {
                        ChooseCallAccountDialog dialog
                            = new ChooseCallAccountDialog(
                                detail.getDetail(),
                                OperationSetBasicTelephony.class, providers);
                        dialog.setUIContact(sourceUIContact);
                        dialog.setVisible(true);
                    }
                    else // providersCount == 1
                    {
                        CallManager.createCall(
                            providers.get(0),
                            detail.getDetail(),
                            sourceUIContact);
                    }
                }
            });
            callContactItem.setEnabled(detail.getSupportedOperationSets().
                    contains(OperationSetBasicTelephony.class));
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

    /**
     * Reloads icons for menu items.
     */
    public void loadSkin()
    {
        callContactMenu.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.CALL_16x16_ICON)));

        if(addContactComponent instanceof JMenuItem)
        {
            ((JMenuItem) addContactComponent).setIcon(new ImageIcon(ImageLoader
                        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));
        }
    }
}
