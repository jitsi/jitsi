/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MetaContactRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener,
                Skinnable
{
    /**
     * An eclipse generated serial version unique ID
     */
    private static final long serialVersionUID = 3033031652970285857L;

    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(MetaContactRightButtonMenu.class);

    /**
     * The string shown over menu items indicating that an operation should be
     * done for all contained contacts.
     */
    private static final String allContactsString
        = GuiActivator.getResources().getI18NString("service.gui.ALL_CONTACTS");

    /**
     * String for call menu items.
     */
    private static final String callString
        = GuiActivator.getResources().getI18NString("service.gui.CALL");

    /**
     * The menu responsible for moving a contact to another group.
     */
    private final SIPCommMenu moveToMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_TO_GROUP"));

    /**
     * The menu responsible for moving a containing protocol contact to another
     * group.
     */
    private final SIPCommMenu moveSubcontactMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT"));

    /**
     * The menu responsible for removing a contact.
     */
    private final SIPCommMenu removeContactMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.REMOVE_CONTACT"));

    /**
     * The menu responsible for calling a contact.
     */
    private final SIPCommMenu callContactMenu = new SIPCommMenu(callString);

    /**
     * The menu responsible for adding a contact.
     */
    private final JMenuItem addContactItem = new JMenuItem();

    /**
     * The menu item responsible for calling a contact.
     */
    private final JMenuItem callItem = new JMenuItem(callString);

    /**
     * The video call menu item.
     */
    private final JMenuItem videoCallItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.VIDEO_CALL"));

    /**
     * The menu responsible for calling a contact with video.
     */
    private final SIPCommMenu videoCallMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.VIDEO_CALL"));

    /**
     * The menu responsible for full screen sharing when more than one contact
     * is available for sharing.
     */
    private final JMenuItem fullShareMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_FULL_SCREEN"));

    /**
     * The menu responsible for region screen sharing when more than one contact
     * is available for sharing.
     */
    private final JMenuItem regionShareMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_REGION"));

    /**
     * The menu responsible for full screen sharing when more than one contact
     * is available for sharing.
     */
    private final SIPCommMenu multiContactFullShareMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_FULL_SCREEN"));

    /**
     * The menu responsible for region screen sharing when more than one contact
     * is available for sharing.
     */
    private final SIPCommMenu multiContactRegionShareMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_REGION"));

    /**
     * The send message menu item.
     */
    private final JMenuItem sendMessageItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_MESSAGE"));

    /**
     * The send file menu item.
     */
    private final JMenuItem sendFileItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_FILE"));

    /**
     * The send SMS menu item.
     */
    private final JMenuItem sendSmsItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_SMS"));

    /**
     * The rename contact menu item.
     */
    private final JMenuItem renameContactItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_CONTACT"));

    /**
     * The view history menu item.
     */
    private final JMenuItem viewHistoryItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.VIEW_HISTORY"));

    /**
     * The <tt>MetaContact</tt> over which the right button was pressed.
     */
    private final MetaContact metaContact;

    /**
     * The prefix for the move menu.
     */
    private static final String moveToPrefix = "moveTo:";

    /**
     * The prefix for remove contact menu.
     */
    private static final String removeContactPrefix = "removeContact:";

    /**
     * The prefix for remove protocol contact menu.
     */
    private static final String moveSubcontactPrefix = "moveSubcontact:";

    /**
     * The prefix for call contact menu.
     */
    private static final String callContactPrefix = "callContact:";

    /**
     * The prefix for video call contact menu.
     */
    private static final String videoCallPrefix = "videoCall:";

    /**
     * The prefix for full screen desktop sharing menu.
     */
    private static final String fullDesktopSharingPrefix = "shareFullScreen:";

    /**
     * The prefix for region screen desktop sharing menu.
     */
    private static final String regionDesktopSharingPrefix = "shareRegionScreen:";

    /**
     * The contact to move when the move menu has been chosen.
     */
    private Contact contactToMove;

    /**
     * Indicates if all contacts should be moved when the move to menu is
     * pressed.
     */
    private boolean moveAllContacts = false;

    /**
     * The move dialog.
     */
    private MoveSubcontactMessageDialog moveDialog;

    /**
     * The main window.
     */
    private final MainFrame mainFrame;

    /**
     * The contact list component.
     */
    private final TreeContactList contactList;

    /**
     * Creates an instance of ContactRightButtonMenu.
     * @param contactItem The MetaContact for which the menu is opened
     */
    public MetaContactRightButtonMenu(  MetaContact contactItem)
    {
        super();

        this.mainFrame = GuiActivator.getUIService().getMainFrame();
        this.contactList = GuiActivator.getContactList();

        this.metaContact = contactItem;

        this.setLocation(getLocation());

        this.init();

        this.initMnemonics();

        loadSkin();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        addContactItem.setText(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT_TO")
                + " " + metaContact.getDisplayName());

        //Initialize moveTo menu.
        Iterator<MetaContactGroup> groups
            = GuiActivator.getContactListService().getRoot().getSubgroups();

        if(groups.hasNext())
        {
            JLabel infoLabel = new JLabel(
                GuiActivator.getResources()
                    .getI18NString("service.gui.SELECT_GROUP"));

            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));

            this.moveToMenu.add(infoLabel);
            this.moveToMenu.addSeparator();
        }

        while (groups.hasNext())
        {
            MetaContactGroup group = groups.next();

            JMenuItem menuItem = new JMenuItem(group.getGroupName());

            menuItem.setName(moveToPrefix + group.getMetaUID());
            menuItem.addActionListener(this);

            this.moveToMenu.add(menuItem);
        }

        //Initialize removeContact menu.
        Iterator<Contact> contacts = metaContact.getContacts();

        if (metaContact.getContactCount() > 1)
        {
            JMenuItem allItem = new JMenuItem(allContactsString);
            JMenuItem allItem1 = new JMenuItem(allContactsString);

            allItem.addActionListener(this);
            allItem1.addActionListener(this);

            allItem.setName(removeContactPrefix + "allContacts");
            allItem1.setName(moveSubcontactPrefix + "allContacts");

            this.removeContactMenu.add(allItem);
            this.moveSubcontactMenu.add(allItem1);
            this.removeContactMenu.addSeparator();
            this.moveSubcontactMenu.addSeparator();
        }

        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();

            String contactAddress = contact.getAddress();

            Icon protocolIcon = new ImageIcon(
                    createContactStatusImage(contact));

            this.removeContactMenu.add(
                createMenuItem( contactAddress,
                            removeContactPrefix + contact.getAddress()
                            + protocolProvider.getProtocolName(),
                            protocolIcon));

            this.moveSubcontactMenu.add(
                createMenuItem( contactAddress,
                            moveSubcontactPrefix + contact.getAddress()
                            + protocolProvider.getProtocolName(),
                            protocolIcon));

            // add all the contacts that support telephony to the call menu
            if (metaContact.getContactCount() > 1)
            {
                if (protocolProvider.getOperationSet(
                        OperationSetBasicTelephony.class) != null &&
                        hasContactCapabilities(contact,
                                OperationSetBasicTelephony.class))
                {
                    callContactMenu.add(
                        createMenuItem( contactAddress,
                                        callContactPrefix + contact.getAddress()
                                        + protocolProvider.getProtocolName(),
                                        protocolIcon));
                }

                if (protocolProvider.getOperationSet(
                        OperationSetVideoTelephony.class) != null &&
                        hasContactCapabilities(contact,
                                OperationSetVideoTelephony.class))
                {
                    videoCallMenu.add(
                        createMenuItem( contactAddress,
                                        videoCallPrefix + contact.getAddress()
                                        + protocolProvider.getProtocolName(),
                                        protocolIcon));
                }

                if (protocolProvider.getOperationSet(
                        OperationSetDesktopSharingServer.class) != null &&
                        hasContactCapabilities(contact,
                                OperationSetDesktopSharingServer.class))
                {
                    multiContactFullShareMenu.add(
                        createMenuItem( contactAddress,
                                        fullDesktopSharingPrefix
                                        + contact.getAddress()
                                        + protocolProvider.getProtocolName(),
                                        protocolIcon));

                    multiContactRegionShareMenu.add(
                        createMenuItem( contactAddress,
                                        regionDesktopSharingPrefix
                                        + contact.getAddress()
                                        + protocolProvider.getProtocolName(),
                                        protocolIcon));
                }
            }
        }

        this.add(sendMessageItem);

        if (metaContact.getDefaultContact(
            OperationSetSmsMessaging.class) != null)
        {
            this.add(sendSmsItem);
            sendSmsItem.addActionListener(this);
            sendSmsItem.setName("sendSms");
        }

        if (callContactMenu.getItemCount() > 1)
        {
            this.add(callContactMenu);
        }
        else
        {
            this.add(callItem);
            this.callItem.setName("call");
            this.callItem.addActionListener(this);
        }

        if (videoCallMenu.getItemCount() > 1)
        {
            this.add(videoCallMenu);
        }
        else
        {
            this.add(videoCallItem);
            this.videoCallItem.setName("videoCall");
            this.videoCallItem.addActionListener(this);
        }

        if (multiContactFullShareMenu.getItemCount() > 1)
        {
            add(multiContactFullShareMenu);
            add(multiContactRegionShareMenu);
        }
        else
        {
            fullShareMenuItem.setName("shareFullScreen");
            fullShareMenuItem.addActionListener(this);
            add(fullShareMenuItem);

            regionShareMenuItem.setName("shareRegion");
            regionShareMenuItem.addActionListener(this);
            add(regionShareMenuItem);
        }

        add(sendFileItem);

        addSeparator();

        add(moveToMenu);
        add(moveSubcontactMenu);

        addSeparator();

        if (!ConfigurationManager.isAddContactDisabled())
            add(addContactItem);

        addSeparator();

        if (!ConfigurationManager.isRemoveContactDisabled())
            add(removeContactMenu);

        add(renameContactItem);

        addSeparator();

        add(viewHistoryItem);

        initPluginComponents();

        sendMessageItem.setName("sendMessage");
        sendFileItem.setName("sendFile");
        moveToMenu.setName("moveToGroup");
        addContactItem.setName("addContact");
        renameContactItem.setName("renameContact");
        viewHistoryItem.setName("viewHistory");

        sendMessageItem.addActionListener(this);
        sendFileItem.addActionListener(this);
        renameContactItem.addActionListener(this);
        viewHistoryItem.addActionListener(this);
        addContactItem.addActionListener(this);

        // Disable all menu items that do nothing.
        if (metaContact.getDefaultContact(
            OperationSetFileTransfer.class) == null)
            this.sendFileItem.setEnabled(false);

        if (metaContact.getDefaultContact(
            OperationSetBasicTelephony.class) == null)
            this.callItem.setEnabled(false);

        if (metaContact.getDefaultContact(
            OperationSetVideoTelephony.class) == null)
            this.videoCallItem.setEnabled(false);

        if (metaContact.getDefaultContact(
            OperationSetDesktopSharingServer.class) == null)
        {
            fullShareMenuItem.setEnabled(false);
            regionShareMenuItem.setEnabled(false);
        }

        if (metaContact.getDefaultContact(
            OperationSetBasicInstantMessaging.class) == null)
            this.sendMessageItem.setEnabled(false);
    }

    /**
     * Initializes the call menu items.
     *
     * @param displayName the display name of the menu item
     * @param name the name of the menu item, used to distinguish it in action
     * events
     * @param icon the icon of the protocol
     *
     * @return the created menu item
     */
    private JMenuItem createMenuItem(String displayName,
                                String name,
                                Icon icon)
    {
        JMenuItem menuItem = new JMenuItem(displayName);
        menuItem.setIcon(icon);
        menuItem.setName(name);
        menuItem.addActionListener(this);

        return menuItem;
    }

    /**
     * Initializes plug-in components for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);

                component.setCurrentContact(metaContact);

                if (component.getComponent() == null)
                    continue;

                this.add((Component)component.getComponent());
            }
        }
        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Initializes menu items mnemonics.
     */
    private void initMnemonics()
    {
        this.sendMessageItem.setMnemonic(
            GuiActivator.getResources()
                .getI18nMnemonic("service.gui.SEND_MESSAGE"));

        char callMnemonic = GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CALL");

        if (callContactMenu.getItemCount() > 1)
        {
            this.callContactMenu.setMnemonic(callMnemonic);
        }
        else
        {
            this.callItem.setMnemonic(callMnemonic);
        }

        char videoCallMnemonic = GuiActivator.getResources()
            .getI18nMnemonic("service.gui.VIDEO_CALL");

        if (videoCallMenu.getItemCount() > 1)
        {
            this.videoCallMenu.setMnemonic(videoCallMnemonic);
        }
        else
        {
            this.videoCallItem.setMnemonic(videoCallMnemonic);
        }

        this.sendSmsItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SEND_SMS"));
        this.sendFileItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SEND_FILE"));
        this.moveToMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.MOVE_TO_GROUP"));
        this.addContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));
        this.removeContactMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.REMOVE_CONTACT"));
        this.renameContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.RENAME_CONTACT"));
        this.viewHistoryItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.VIEW_HISTORY"));
        this.moveSubcontactMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.MOVE_SUBCONTACT"));
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and performs the appropriate operations.
     * @param e the <tt>ActionEvent</tt>, which notified us of the action
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        Contact contact;

        if (itemName.equals(addContactItem.getName()))
        {
            AddContactDialog dialog
                = new AddContactDialog(mainFrame, metaContact);

            // Try to select the preferred protocol provider.
            ProtocolProviderService protocolProvider
                = GuiActivator.getPreferredAccount();

            if (protocolProvider != null)
                dialog.setSelectedAccount(protocolProvider);

            dialog.setVisible(true);
        }
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact);
        }
        else if (itemName.equalsIgnoreCase("sendSms"))
        {
            Contact defaultSmsContact
                = metaContact.getDefaultContact(OperationSetSmsMessaging.class);

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact, defaultSmsContact, true);
        }
        else if (itemName.equals("call"))
        {
            contact = metaContact.getDefaultContact(
                    OperationSetBasicTelephony.class);

            CallManager.createCall(
                contact.getProtocolProvider(), contact);
        }
        else if (itemName.equals("videoCall"))
        {
            contact = metaContact.getDefaultContact(
                    OperationSetVideoTelephony.class);

            CallManager.createVideoCall(
                contact.getProtocolProvider(), contact.getAddress());
        }
        else if (itemName.equals("shareFullScreen"))
        {
            contact = metaContact.getDefaultContact(
                    OperationSetVideoTelephony.class);

            CallManager.createDesktopSharing(
                contact.getProtocolProvider(), contact.getAddress());
        }
        else if (itemName.equals("shareRegion"))
        {
            contact = metaContact.getDefaultContact(
                    OperationSetVideoTelephony.class);

            CallManager.createRegionDesktopSharing(
                contact.getProtocolProvider(), contact.getAddress());
        }
        else if (itemName.equals("sendFile"))
        {
            SipCommFileChooser scfc = GenericFileDialog.create(
                null, "Send file...",
                SipCommFileChooser.LOAD_FILE_OPERATION,
                ConfigurationManager.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationManager.setSendFileLastDir(
                        selectedFile.getParent());

                // Obtain the corresponding chat panel.
                ChatPanel chatPanel
                    = GuiActivator.getUIService().
                        getChatWindowManager().getContactChat(metaContact, true);

                chatPanel.sendFile(selectedFile);

                GuiActivator.getUIService().
                    getChatWindowManager().openChat(chatPanel, true);
            }

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact);
        }
        else if (itemName.equals("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, metaContact);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (itemName.equals("viewHistory"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            if(historyWindowManager
                .containsHistoryWindowForContact(metaContact))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(metaContact);

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(this.metaContact);

                historyWindowManager
                    .addHistoryWindowForContact(metaContact, history);

                history.setVisible(true);
            }
        }
        else if (itemName.startsWith(moveToPrefix))
        {
            MetaContactListManager.moveMetaContactToGroup(
                metaContact, itemName.substring(moveToPrefix.length()));
        }
        else if (itemName.startsWith(removeContactPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(removeContactPrefix.length()));

            if(contact != null)
            {
                MetaContactListManager.removeContact(contact);
            }
            else
            {
                MetaContactListManager.removeMetaContact(metaContact);
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(moveSubcontactPrefix.length()));

            contactList.addContactListListener(this);
            contactList.setGroupClickConsumed(true);

            // FIXME: set the special cursor while moving a subcontact
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            this.moveDialog = new MoveSubcontactMessageDialog(mainFrame, this);

            // Be sure we allow open/close groups in the contactlist if
            // user cancels the action
            this.moveDialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        contactList.setGroupClickConsumed(false);
                    }
                });
            this.moveDialog.setVisible(true);

            if(contact != null)
            {
                this.contactToMove = contact;
            }
            else
            {
                this.moveAllContacts = true;
            }
        }
        else if (itemName.startsWith(callContactPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(callContactPrefix.length()));

            CallManager.createCall(
                contact.getProtocolProvider(), contact);
        }
        else if (itemName.startsWith(videoCallPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(videoCallPrefix.length()));

            CallManager.createVideoCall(contact.getProtocolProvider(),
                                        contact.getAddress());
        }
        else if (itemName.startsWith(fullDesktopSharingPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(fullDesktopSharingPrefix.length()));

            CallManager.createDesktopSharing(   contact.getProtocolProvider(),
                                                contact.getAddress());
        }
        else if (itemName.startsWith(regionDesktopSharingPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(regionDesktopSharingPrefix.length()));

            CallManager.createRegionDesktopSharing(
                                                contact.getProtocolProvider(),
                                                contact.getAddress());
        }
    }

    /**
     * Obtains the <tt>Contact</tt> corresponding to the given address
     * identifier.
     *
     * @param itemID The address of the <tt>Contact</tt>.
     * @return the <tt>Contact</tt> corresponding to the given address
     * identifier.
     */
    private Contact getContactFromMetaContact(String itemID)
    {
        Iterator<Contact> i = metaContact.getContacts();

        while (i.hasNext())
        {
            Contact contact = i.next();

            String id = contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName();

            if (itemID.equals(id))
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Indicates that a group has been selected during a move operation. Moves
     * the selected contact to the selected group.
     * @param evt the <tt>ContactListEvent</tt> has
     */
    public void groupClicked(ContactListEvent evt)
    {
        this.moveDialog.dispose();

        UIGroup sourceGroup = evt.getSourceGroup();

        // TODO: may be show a warning message to tell the user that she should
        // select another group.
        if (!(sourceGroup instanceof MetaUIGroup))
            return;

        MetaContactGroup metaGroup
            = (MetaContactGroup) sourceGroup.getDescriptor();

        contactList.removeContactListListener(this);

        // FIXME: unset the special cursor after a subcontact has been moved
        //guiContactList.setCursor(
        //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if(moveAllContacts)
        {
            MetaContactListManager
                .moveMetaContactToGroup(metaContact, metaGroup);
        }
        else if(contactToMove != null)
        {
            MetaContactListManager
                .moveContactToGroup(contactToMove, metaGroup);
        }

        contactList.setGroupClickConsumed(false);
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the chosen sub-contact when a meta contact is selected.
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        UIContact descriptor = evt.getSourceContact();
        // We're only interested in MetaContacts here.
        if (!(descriptor instanceof MetaUIContact))
            return;

        this.moveContact((MetaContact) descriptor.getDescriptor());
    }

    /**
     * Moves the previously chosen sub-contact in the given toMetaContact.
     *
     * @param toMetaContact the MetaContact, where to move the previously
     * chosen sub-contact.
     */
    private void moveContact(MetaContact toMetaContact)
    {
        this.moveDialog.dispose();

        if(toMetaContact.equals(metaContact))
        {
            new ErrorDialog(this.mainFrame,
                GuiActivator.getResources()
                    .getI18NString("service.gui.MOVE_SUBCONTACT"),
                GuiActivator.getResources()
                    .getI18NString("service.gui.MOVE_SUBCONTACT_FAILED"),
                ErrorDialog.WARNING)
                    .showDialog();
        }
        else
        {
            contactList.removeContactListListener(this);

            // FIXME: unset the special cursor after a subcontact has been moved
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if(moveAllContacts)
            {
                MetaContactListManager
                    .moveMetaContactToMetaContact(metaContact, toMetaContact);
            }
            else if(contactToMove != null)
            {
                MetaContactListManager
                    .moveContactToMetaContact(contactToMove, toMetaContact);
            }
        }
    }

    /**
     * Adds the according plug-in component to this container.
     * @param event received event
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(!c.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
            return;

        Object constraints
            = UIServiceImpl.getBorderLayoutConstraintsFromContainer(
                c.getConstraints());

        if (c.getComponent() == null)
            return;

        if (constraints == null)
            this.add((Component) c.getComponent());
        else
            this.add((Component) c.getComponent(), constraints);

        c.setCurrentContact(metaContact);

        this.repaint();
    }

    /**
     * Removes the according plug-in component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }

    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the protocol contact for which to create the image
     * @return the indexed status image
     */
    public Image createContactStatusImage(Contact protoContact)
    {
        return
            ImageLoader.getIndexedProtocolImage(
                ImageUtils.getBytesInImage(
                    protoContact.getPresenceStatus().getStatusIcon()),
                protoContact.getProtocolProvider());
    }

    /**
     * Reloads skin related information.
     */
    public void loadSkin()
    {
        callItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

        videoCallItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.VIDEO_CALL)));

        videoCallMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.VIDEO_CALL)));

        fullShareMenuItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_SHARING)));

        regionShareMenuItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.REGION_DESKTOP_SHARING)));

        multiContactFullShareMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_SHARING)));

        multiContactRegionShareMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.REGION_DESKTOP_SHARING)));

        sendMessageItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

        sendFileItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_FILE_16x16_ICON)));

        sendSmsItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

        renameContactItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));

        viewHistoryItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

        moveToMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MOVE_TO_GROUP_16x16_ICON)));

        addContactItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        removeContactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON)));

        moveSubcontactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MOVE_CONTACT_ICON)));

        callContactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));
    }

    /**
     * Returns <tt>true</tt> if <tt>Contact</tt> supports the specified
     * <tt>OperationSet</tt>, <tt>false</tt> otherwise.
     *
     * @param contact contact to check
     * @param opSet <tt>OperationSet</tt> to search for
     * @return Returns <tt>true</tt> if <tt>Contact</tt> supports the specified
     * <tt>OperationSet</tt>, <tt>false</tt> otherwise.
     */
    private boolean hasContactCapabilities(
            Contact contact, Class<? extends OperationSet> opSet)
    {
        OperationSetContactCapabilities capOpSet =
            contact.getProtocolProvider().
                getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet == null)
        {
            // assume contact has OpSet capabilities
            return true;
        }
        else
        {
            if(capOpSet.getOperationSet(contact, opSet) != null)
            {
                return true;
            }
        }

        return false;
    }
}
