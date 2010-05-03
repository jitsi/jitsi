/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 */
public class MetaContactRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener
{
    /**
     * An eclipse generated serial version unique ID
     */
    private static final long serialVersionUID = 3033031652970285857L;

    private final Logger logger
        = Logger.getLogger(MetaContactRightButtonMenu.class);

    private static final String allContactsString
        = GuiActivator.getResources().getI18NString("service.gui.ALL_CONTACTS");

    private static final String moveToString = GuiActivator.getResources()
        .getI18NString("service.gui.MOVE_TO_GROUP");

    private static final String moveSubcontactString
        = GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT");

    private static final String removeContactString
        = GuiActivator.getResources()
            .getI18NString("service.gui.REMOVE_CONTACT");

    private static final String callString
        = GuiActivator.getResources().getI18NString("service.gui.CALL");

    private static final String sendMessageString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_MESSAGE");

    private static final String sendFileString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_FILE");

    private static final String renameContactString
        = GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_CONTACT");

    private static final String viewHistoryString
        = GuiActivator.getResources().getI18NString("service.gui.VIEW_HISTORY");

    private static final String sendSmsString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_SMS");

    private final SIPCommMenu moveToMenu = new SIPCommMenu(moveToString);

    private final SIPCommMenu moveSubcontactMenu
        = new SIPCommMenu(moveSubcontactString);

    private final SIPCommMenu removeContactMenu
        = new SIPCommMenu(removeContactString);

    private final SIPCommMenu callContactMenu = new SIPCommMenu(callString);

    private final JMenuItem addContactItem = new JMenuItem();

    private final JMenuItem callItem = new JMenuItem(
        callString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

    private final JMenuItem sendMessageItem = new JMenuItem(
        sendMessageString,
        new ImageIcon(ImageLoader
            .getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private final JMenuItem sendFileItem = new JMenuItem(
        sendFileString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_FILE_16x16_ICON)));

    private final JMenuItem sendSmsItem = new JMenuItem(
        sendSmsString,
        new ImageIcon(ImageLoader
            .getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private final JMenuItem renameContactItem = new JMenuItem(
        renameContactString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));

    private final JMenuItem viewHistoryItem = new JMenuItem(
        viewHistoryString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

    private final MetaContact contactItem;

    private static final String moveToPrefix = "moveTo:";

    private static final String removeContactPrefix = "removeContact:";

    private static final String moveSubcontactPrefix = "moveSubcontact:";

    private static final String callContactPrefix = "callContact:";

    private Contact contactToMove;

    private boolean moveAllContacts = false;

    private MoveSubcontactMessageDialog moveDialog;

    private final MainFrame mainFrame;

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

        this.contactItem = contactItem;

        this.setLocation(getLocation());

        this.init();

        this.initMnemonics();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        addContactItem.setText(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT_TO")
                + " " + contactItem.getDisplayName());

        this.moveToMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.GROUPS_16x16_ICON)));

        this.addContactItem.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.removeContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.DELETE_16x16_ICON)));

        this.moveSubcontactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.MOVE_CONTACT_ICON)));

        this.callContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.CALL_16x16_ICON)));

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
        Iterator<Contact> contacts = contactItem.getContacts();

        if (contactItem.getContactCount() > 1)
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

            String contactDisplayName = contact.getDisplayName();

            JMenuItem contactItem = new JMenuItem(contactDisplayName);
            JMenuItem contactItem1 = new JMenuItem(contactDisplayName);

            Icon protocolIcon = new ImageIcon(
                    createContactStatusImage(contact));

            contactItem.setIcon(protocolIcon);
            contactItem1.setIcon(protocolIcon);

            contactItem.setName(removeContactPrefix + contact.getAddress()
                    + protocolProvider.getProtocolName());

            contactItem1.setName(moveSubcontactPrefix + contact.getAddress()
                    + protocolProvider.getProtocolName());

            contactItem.addActionListener(this);
            contactItem1.addActionListener(this);

            this.removeContactMenu.add(contactItem);
            this.moveSubcontactMenu.add(contactItem1);

            // add all the contacts that support telephony to the call menu
            if (contact.getProtocolProvider().getOperationSet(
                OperationSetBasicTelephony.class) != null)
            {
                JMenuItem callContactItem = new JMenuItem(contactDisplayName);
                callContactItem.setIcon(protocolIcon);
                callContactItem.setName(callContactPrefix + contact.getAddress()
                        + protocolProvider.getProtocolName());
                callContactItem.addActionListener(this);
                this.callContactMenu.add(callContactItem);
            }

            // FIXME Why is OperationSetWebContactInfo requested and not used?
            protocolProvider.getOperationSet(OperationSetWebContactInfo.class);
        }

        this.add(sendMessageItem);
        this.add(sendSmsItem);
        if (callContactMenu.getItemCount() > 1)
        {
            this.add(callContactMenu);
        }
        else
        {
            this.add(callItem);
        }
        this.add(sendFileItem);

        this.addSeparator();

        this.add(moveToMenu);
        this.add(moveSubcontactMenu);

        this.addSeparator();

        this.add(addContactItem);

        this.addSeparator();

        this.add(removeContactMenu);
        this.add(renameContactItem);

        this.addSeparator();

        this.add(viewHistoryItem);

        this.initPluginComponents();

        this.sendMessageItem.setName("sendMessage");
        this.callItem.setName("call");
        this.sendSmsItem.setName("sendSms");
        this.sendFileItem.setName("sendFile");
        this.moveToMenu.setName("moveToGroup");
        this.addContactItem.setName("addContact");
        this.renameContactItem.setName("renameContact");
        this.viewHistoryItem.setName("viewHistory");

        this.sendMessageItem.addActionListener(this);
        this.callItem.addActionListener(this);
        this.sendSmsItem.addActionListener(this);
        this.sendFileItem.addActionListener(this);
        this.renameContactItem.addActionListener(this);
        this.viewHistoryItem.addActionListener(this);
        this.addContactItem.addActionListener(this);

        // Disable all menu items that do nothing.
        if (contactItem.getDefaultContact(OperationSetFileTransfer.class)
                == null)
            this.sendFileItem.setEnabled(false);

        if (contactItem.getDefaultContact(OperationSetBasicTelephony.class)
                == null)
            this.callItem.setEnabled(false);

        if (contactItem.getDefaultContact(OperationSetBasicInstantMessaging.class)
                == null)
            this.sendMessageItem.setEnabled(false);

        if (contactItem.getDefaultContact(OperationSetSmsMessaging.class)
                == null)
            this.sendSmsItem.setEnabled(false);
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

                component.setCurrentContact(contactItem);

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
        if (callContactMenu.getItemCount() > 1)
        {
            this.callContactMenu.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CALL"));
        }
        else
        {
            this.callItem.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CALL"));
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
                = new AddContactDialog(mainFrame, contactItem);

            dialog.setVisible(true);
        }
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            GuiActivator.getUIService().getChatWindowManager()
                .startChat(contactItem);
        }
        else if (itemName.equalsIgnoreCase("sendSms"))
        {
            Contact defaultSmsContact
                = contactItem.getDefaultContact(OperationSetSmsMessaging.class);

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(contactItem, defaultSmsContact, true);
        }
        else if (itemName.equalsIgnoreCase("call"))
        {
            contact = contactItem.getDefaultContact(
                    OperationSetBasicTelephony.class);

            callContact(contact);
        }
        else if (itemName.equalsIgnoreCase("sendFile"))
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

                GuiActivator.getUIService().
                getChatWindowManager().getSelectedChat().
                    sendFile(selectedFile);
            }

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(contactItem);
        }
        else if (itemName.equalsIgnoreCase("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, contactItem);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (itemName.equalsIgnoreCase("viewHistory"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            if(historyWindowManager
                .containsHistoryWindowForContact(contactItem))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(contactItem);

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(this.contactItem);

                historyWindowManager
                    .addHistoryWindowForContact(contactItem, history);

                history.setVisible(true);
            }
        }
        else if (itemName.startsWith(moveToPrefix))
        {
            MetaContactListManager.moveMetaContactToGroup(
                contactItem, itemName.substring(moveToPrefix.length()));
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
                MetaContactListManager.removeMetaContact(contactItem);
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

            callContact(contact);
        }
    }

    /**
     * Calls the given contact
     * @param contact the contact to call
     */
    private void callContact(Contact contact)
    {
        CallManager.createCall(
            contact.getProtocolProvider(), contact);
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
        Iterator<Contact> i = contactItem.getContacts();

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
                .moveMetaContactToGroup(contactItem, metaGroup);
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

        if(toMetaContact.equals(contactItem))
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
                    .moveMetaContactToMetaContact(contactItem, toMetaContact);
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
     * @param event 
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

        c.setCurrentContact(contactItem);

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
            ImageLoader.badgeImageWithProtocolIndex(
                ImageLoader.getBytesInImage(
                    protoContact.getPresenceStatus().getStatusIcon()),
                protoContact.getProtocolProvider());
    }
}
