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
import java.io.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 */
public class ContactRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener
{
    /**
     * An eclipse generated serial version unique ID
     */
    private static final long serialVersionUID = 3033031652970285857L;

    private Logger logger = Logger.getLogger(ContactRightButtonMenu.class);

    private static final String allContactsString
        = GuiActivator.getResources().getI18NString("service.gui.ALL_CONTACTS");

    private static final String moveToString = GuiActivator.getResources()
        .getI18NString("service.gui.MOVE_TO_GROUP");

    private static final String moveSubcontactString = GuiActivator.getResources()
        .getI18NString("service.gui.MOVE_SUBCONTACT");

    private static final String addSubcontactString
        = GuiActivator.getResources()
            .getI18NString("service.gui.ADD_SUBCONTACT");

    private static final String removeContactString
        = GuiActivator.getResources()
            .getI18NString("service.gui.REMOVE_CONTACT");

    private static final String callString
        = GuiActivator.getResources().getI18NString("service.gui.CALL");

    private static final String sendMessageString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_MESSAGE");

    private String sendFileString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_FILE");

    private String renameContactString
        = GuiActivator.getResources().getI18NString("service.gui.RENAME_CONTACT");

    private String viewHistoryString
        = GuiActivator.getResources().getI18NString("service.gui.VIEW_HISTORY");

    private String sendSmsString
        = GuiActivator.getResources().getI18NString("service.gui.SEND_SMS");

    private SIPCommMenu moveToMenu = new SIPCommMenu(moveToString);

    private SIPCommMenu moveSubcontactMenu
        = new SIPCommMenu(moveSubcontactString);

    private SIPCommMenu addSubcontactMenu
        = new SIPCommMenu(addSubcontactString);

    private SIPCommMenu removeContactMenu
        = new SIPCommMenu(removeContactString);

    private SIPCommMenu callContactMenu = new SIPCommMenu(callString);

    private JMenuItem callItem = new JMenuItem(
        callString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

    private JMenuItem sendMessageItem = new JMenuItem(
        sendMessageString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private JMenuItem sendFileItem = new JMenuItem(
        sendFileString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_FILE_16x16_ICON)));

    private JMenuItem sendSmsItem = new JMenuItem(
        sendSmsString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private JMenuItem renameContactItem = new JMenuItem(
        renameContactString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));

    private JMenuItem viewHistoryItem = new JMenuItem(
        viewHistoryString,
        new ImageIcon(ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

    private MetaContact contactItem;

    private String moveToPrefix = "moveTo:";

    private String removeContactPrefix = "removeContact:";

    private String addSubcontactPrefix = "addSubcontact:";

    private String moveSubcontactPrefix = "moveSubcontact:";

    private String callContactPrefix = "callContact:";

    private Contact contactToMove;

    private boolean moveAllContacts = false;

    private MoveSubcontactMessageDialog moveDialog;

    private ContactList guiContactList;

    private MainFrame mainFrame;

    /**
     * Creates an instance of ContactRightButtonMenu.
     * @param contactList The contact list over which this menu is shown.
     * @param contactItem The MetaContact for which the menu is opened.
     */
    public ContactRightButtonMenu(  ContactList contactList,
                                    MetaContact contactItem)
    {
        super();

        this.mainFrame = GuiActivator.getUIService().getMainFrame();

        this.guiContactList = contactList;

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

        this.moveToMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.GROUPS_16x16_ICON)));

        this.addSubcontactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.removeContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.DELETE_16x16_ICON)));

        this.moveSubcontactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.MOVE_CONTACT_ICON)));

        this.callContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.CALL_16x16_ICON)));

        //Initialize the addSubcontact menu.
        Iterator<ProtocolProviderService> providers 
                = mainFrame.getProtocolProviders();

        if(providers.hasNext())
        {
            JLabel infoLabel = new JLabel(
                GuiActivator.getResources()
                    .getI18NString("service.gui.SELECT_ACCOUNT"));

            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));

            this.addSubcontactMenu.add(infoLabel);
            this.addSubcontactMenu.addSeparator();
        }

        while (providers.hasNext())
        {
            ProtocolProviderService pps = providers.next();

            String protocolName = pps.getProtocolName();

            ProviderAwareMenuItem menuItem 
                = new ProviderAwareMenuItem(pps, 
                    pps.getAccountID().getDisplayName(),
                    ImageLoader.getAccountStatusImage(pps));

            menuItem.setName(addSubcontactPrefix + protocolName);
            menuItem.addActionListener(this);

            this.addSubcontactMenu.add(menuItem);
        }

        //Initialize moveTo menu.
        Iterator<MetaContactGroup> groups = this.mainFrame.getAllGroups();

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

        this.add(addSubcontactMenu);

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
        this.addSubcontactMenu.setName("addSubcontact");
        this.renameContactItem.setName("renameContact");
        this.viewHistoryItem.setName("viewHistory");

        this.sendMessageItem.addActionListener(this);
        this.callItem.addActionListener(this);
        this.sendSmsItem.addActionListener(this);
        this.sendFileItem.addActionListener(this);
        this.renameContactItem.addActionListener(this);
        this.viewHistoryItem.addActionListener(this);

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
                    .bundleContext.getService(serRefs[i]);;

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
        this.addSubcontactMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_SUBCONTACT"));
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
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        Contact contact = null;

        if (itemName.startsWith(addSubcontactPrefix))
        {
            ProtocolProviderService pps
                = ((ProviderAwareMenuItem)menuItem).getProvider();

            if(pps != null)
            {
                AddContactDialog dialog
                    = new AddContactDialog(mainFrame, contactItem, pps);
                Dimension screenSize
                    = Toolkit.getDefaultToolkit().getScreenSize();

                dialog
                    .setLocation(
                        screenSize.width/2 - 250,
                        screenSize.height/2 - 100);
                dialog.showDialog();
            }
        }
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            ContactListPane clistPanel = mainFrame.getContactListPanel();
            SwingUtilities.invokeLater(clistPanel.new RunMessageWindow(
                    contactItem));
        }
        else if (itemName.equalsIgnoreCase("sendSms"))
        {
            Contact defaultSmsContact
                = contactItem.getDefaultContact(OperationSetSmsMessaging.class);

            ContactListPane clistPanel = mainFrame.getContactListPanel();
            SwingUtilities.invokeLater(clistPanel.new RunMessageWindow(
                    contactItem, defaultSmsContact, true));
        }
        else if (itemName.equalsIgnoreCase("call"))
        {
            contact = contactItem.getDefaultContact(
                    OperationSetBasicTelephony.class);

            callContact(contact);
        }
        else if (itemName.equalsIgnoreCase("sendFile"))
        {
            ContactListPane clistPanel = mainFrame.getContactListPanel();
            SwingUtilities.invokeLater(
                clistPanel.new RunMessageWindow(contactItem)
                    {
                        public void run()
                        {
                            super.run();

                            JFileChooser fileChooser = new JFileChooser(
                                ConfigurationManager.getSendFileLastDir());

                            int result = fileChooser.showOpenDialog(
                                GuiActivator.getUIService().
                                    getChatWindowManager().getSelectedChat().
                                        getChatWindow());

                            if (result == JFileChooser.APPROVE_OPTION)
                            {
                                File selectedFile = fileChooser.getSelectedFile();

                                ConfigurationManager
                                    .setSendFileLastDir(selectedFile.getParent());

                                GuiActivator.getUIService().
                                    getChatWindowManager().getSelectedChat().
                                        sendFile(selectedFile);
                            }
                        }
                    });
        }
        else if (itemName.equalsIgnoreCase("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, contactItem);

            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - 200,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - 50
                    );

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
            MetaContactGroup group
                = mainFrame.getGroupByID(
                        itemName.substring(moveToPrefix.length()));

            try
            {
                if(group != null)
                {
                    mainFrame.getContactList().
                        moveMetaContact(contactItem, group);
                }
            }
            catch (Exception ex)
            {
                new ErrorDialog(
                        mainFrame,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MOVE_TO_GROUP"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MOVE_CONTACT_ERROR"),
                        ex).showDialog();
            }
        }
        else if (itemName.startsWith(removeContactPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(removeContactPrefix.length()));

            if(contact != null)
            {
                new RemoveContactThread(contact).start();
            }
            else
            {
                new RemoveAllContactsThread().start();
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix))
        {
            contact = getContactFromMetaContact(
                    itemName.substring(moveSubcontactPrefix.length()));

            guiContactList.addExcContactListListener(this);
            guiContactList.setDisableOpenClose(true);

            // FIXME: set the special cursor while moving a subcontact
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            this.moveDialog = new MoveSubcontactMessageDialog(mainFrame, this);

            // Be sure we allow open/close groups in the contactlist if
            // user cancels the action
            this.moveDialog.addWindowListener(new WindowAdapter()
                {
                    public void windowClosed(WindowEvent e)
                    {
                        guiContactList.setDisableOpenClose(false);
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

            String id =
                contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName();

            if (itemID.equals(id))
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private class RemoveContactThread extends Thread
    {
        private Contact contact;
        public RemoveContactThread(Contact contact)
        {
            this.contact = contact;
        }

        public void run()
        {
            try
            {
                if(Constants.REMOVE_CONTACT_ASK)
                {
                    String message = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{contact.getDisplayName()});

                    MessageDialog dialog = new MessageDialog(
                        mainFrame,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE_CONTACT"),
                        message,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        mainFrame.getContactList().removeContact(contact);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        mainFrame.getContactList().removeContact(contact);

                        Constants.REMOVE_CONTACT_ASK = false;
                    }
                }
                else {
                    mainFrame.getContactList().removeContact(contact);
                }
            }
            catch (Exception ex)
            {
                new ErrorDialog(mainFrame,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_CONTACT"),
                                ex.getMessage(),
                                ex)
                            .showDialog();
            }
        }
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private class RemoveAllContactsThread extends Thread
    {
        public void run()
        {
            if(Constants.REMOVE_CONTACT_ASK)
            {
                String message
                    = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{contactItem.getDisplayName()});

                MessageDialog dialog = new MessageDialog(mainFrame,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT"),
                    message,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE"));

                int returnCode = dialog.showDialog();

                if (returnCode == MessageDialog.OK_RETURN_CODE)
                {
                    mainFrame.getContactList().removeMetaContact(contactItem);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                {
                    mainFrame.getContactList().removeMetaContact(contactItem);

                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else
            {
                mainFrame.getContactList().removeMetaContact(contactItem);
            }
        }
    }

    public void groupSelected(ContactListEvent evt)
    {
        this.moveDialog.dispose();

        MetaContactGroup sourceGroup = evt.getSourceGroup();

        guiContactList.removeExcContactListListener(this);

        // FIXME: unset the special cursor after a subcontact has been moved
        //guiContactList.setCursor(
        //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if(moveAllContacts)
        {
            mainFrame.getContactList()
                .moveMetaContact(contactItem, sourceGroup);
        }
        else if(contactToMove != null)
        {
            new MoveSubcontactThread(sourceGroup).start();
        }

        guiContactList.setDisableOpenClose(false);
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the chosen sub-contact when a meta contact is selected.
     */
    public void contactClicked(ContactListEvent evt)
    {
        this.moveContact(evt.getSourceContact());
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the chosen sub-contact when a meta contact is selected.
     */
    public void protocolContactClicked(ContactListEvent evt)
    {
        this.moveContact(evt.getSourceContact());
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
        else {
            guiContactList.removeExcContactListListener(this);

            // FIXME: unset the special cursor after a subcontact has been moved
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if(moveAllContacts)
            {
                new MoveAllSubcontactsThread(toMetaContact).start();
            }
            else if(contactToMove != null)
            {
                new MoveSubcontactThread(toMetaContact).start();
            }
        }
    }

    /**
     * Moves the previously chosen contact in the given meta group or meta
     * contact.
     */
    private class MoveSubcontactThread extends Thread
    {
        private MetaContact metaContact;

        private MetaContactGroup metaGroup;

        public MoveSubcontactThread(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        public MoveSubcontactThread(MetaContactGroup metaGroup)
        {
            this.metaGroup = metaGroup;
        }

        public void run()
        {
            if(metaContact != null)
            {
                mainFrame.getContactList()
                    .moveContact(contactToMove, metaContact);
            }
            else {
                mainFrame.getContactList()
                    .moveContact(contactToMove, metaGroup);
            }
        }
    }

    /**
     * Moves all sub-contacts contained in the previously selected meta contact
     * in the given meta contact.
     */
    private class MoveAllSubcontactsThread extends Thread
    {
        private MetaContact metaContact;

        public MoveAllSubcontactsThread(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        public void run()
        {
            Iterator<Contact> i = contactItem.getContacts();

            while(i.hasNext())
            {
                Contact contact = i.next();
                mainFrame.getContactList()
                    .moveContact(contact, metaContact);
            }
        }
    }

    /**
     * Adds the according plug-in component to this container.
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
    
    /**
     * A menu item that performs an action related to a specific protocol
     * provider.
     *
     */
    private static class ProviderAwareMenuItem extends JMenuItem
    {
        /**
         * An eclipse generated serialVersionUID.
         */
        private static final long serialVersionUID = 6343418726839985645L;
        
        private ProtocolProviderService provider = null;
        
        /**
         * Initializes the menu item and stores a reference to the specified 
         * provider.
         * 
         * @param provider the provider that we are related to
         * @param text the text string for this menu
         * @param icon the icon to display when showing this menu
         */
        public ProviderAwareMenuItem(ProtocolProviderService provider,
                                     String text,
                                     Icon icon)
        {
            super(text, icon);
            
            this.provider = provider;
        }
        
        /**
         * Returns a reference to the <tt>ProtocolProviderService</tt> that 
         * this item is related to.
         * 
         * @return a reference to the <tt>ProtocolProviderService</tt> that 
         * this item is related to.
         */
        public ProtocolProviderService getProvider()
        {
            return provider;
        }
    }
}
