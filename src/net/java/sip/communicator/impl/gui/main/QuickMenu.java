/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.Wizard;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationDialogImpl;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactList;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListModel;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactWizardPage1;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactWizardPage2;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactWizardPage3;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.NewContact;
import net.java.sip.communicator.impl.gui.utils.BrowserLauncher;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListException;
import net.java.sip.communicator.service.gui.ConfigurationDialog;
import net.java.sip.communicator.service.gui.event.PluginComponentEvent;
import net.java.sip.communicator.service.gui.event.PluginComponentListener;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetWebContactInfo;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * The <tt>QuickMenu</tt> is the toolbar on the top of the main
 * application window. It provides quick access to the "User info" window, the 
 * "Configuration" window, the "Add contact" window and the "Hide/Open offline
 * contacts" window.
 * <p>
 * Note that this class implements the <tt>PluginComponentListener</tt>. This
 * means that this toolbar is a plugable container and could contain plugin
 * components.
 * 
 * @author Yana Stamcheva 
 */
public class QuickMenu extends SIPCommToolBar implements ActionListener,
        PluginComponentListener {

    private JButton infoButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_INFO_ICON)));

    private JButton configureButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON)));

    private JButton searchButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_SEARCH_ICON)));

    private JButton addButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_ADD_ICON)));

    private ConfigurationDialog configDialog;
    
    private MainFrame mainFrame;

    /**
     * Create an instance of the <tt>QuickMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public QuickMenu(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        this.setFloatable(false);
        
        this.infoButton.setPreferredSize(new Dimension(28, 28));
        this.configureButton.setPreferredSize(new Dimension(28, 28));
        this.searchButton.setPreferredSize(new Dimension(28, 28));
        this.addButton.setPreferredSize(new Dimension(28, 28));

        this.infoButton.setToolTipText(Messages.getString("userInfo"));
        this.configureButton.setToolTipText(Messages.getString("configure"));
        this.searchButton
                .setToolTipText(Messages.getString("showOfflineUsers"));
        this.addButton.setToolTipText(Messages.getString("addContact"));

        this.init();
    }

    /**
     * Initialize the <tt>QuickMenu</tt> by adding the buttons.
     */
    private void init() {
        this.add(addButton);
        this.add(configureButton);
        this.add(infoButton);
        this.add(searchButton);

        this.addButton.setName("add");
        this.configureButton.setName("config");
        this.searchButton.setName("search");
        this.infoButton.setName("info");

        this.addButton.addActionListener(this);
        this.configureButton.addActionListener(this);
        this.searchButton.addActionListener(this);
        this.infoButton.addActionListener(this);
                
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on one of
     * the buttons in this toolbar.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("add")) {

            Wizard wizard = new Wizard();
            wizard.getDialog().setTitle(Messages.getString("addContactWizard"));
           
            NewContact newContact = new NewContact();
            
            AddContactWizardPage1 page1 
                = new AddContactWizardPage1(wizard, newContact, 
                        mainFrame.getProtocolProviders());
            
            wizard.registerWizardPage(AddContactWizardPage1.IDENTIFIER, page1);

            AddContactWizardPage2 page2 
                = new AddContactWizardPage2(wizard, newContact,
                        mainFrame.getAllGroups());
            
            wizard.registerWizardPage(AddContactWizardPage2.IDENTIFIER, page2);
            
            AddContactWizardPage3 page3 
                = new AddContactWizardPage3(newContact);
            
            wizard.registerWizardPage(AddContactWizardPage3.IDENTIFIER, page3);
            
            wizard.setCurrentPage(AddContactWizardPage1.IDENTIFIER);
            
            wizard.getDialog().setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2 
                        - 250,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2 
                        - 100
                    );
                        
            int returnCode = wizard.showModalDialog();
            
            if(returnCode == 0) {
                ArrayList ppList = newContact.getProtocolProviders();
                ArrayList groupList = newContact.getGroups();
                
                for(int i = 0; i < ppList.size(); i ++) {
                    ProtocolProviderService pps 
                        = (ProtocolProviderService)ppList.get(i);
                    
                    for(int j = 0; j < groupList.size(); j++) {
                        MetaContactGroup group
                            = (MetaContactGroup)groupList.get(j);
                        
                        try {
                            mainFrame.getContactList()
                                .createMetaContact(
                                    pps, group, newContact.getUin());
                        }
                        catch (MetaContactListException ex) {
                            JOptionPane.showMessageDialog(mainFrame,
                                Messages.getString(
                                        "addContactError", newContact.getUin()),
                                Messages.getString(
                                        "addContactErrorTitle"),
                                JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }
            else if(returnCode == 1) {
                wizard.getDialog().dispose();
            }            
        } 
        else if (buttonName.equals("config")) {

            configDialog = GuiActivator.getUIService().getConfigurationDialog();
            
            configDialog.showDialog();
        } 
        else if (buttonName.equals("search")) {

            ContactList contactList = mainFrame.getTabbedPane()
                .getContactListPanel().getContactList();
            
            ContactListModel listModel 
                = (ContactListModel) contactList.getModel();

            if (listModel.showOffline()) {
                listModel.setShowOffline(false);                
                listModel.removeOfflineContacts();
            } 
            else {
                
                int currentlySelectedIndex = contactList.getSelectedIndex();
                Object selectedObject 
                    = listModel.getElementAt(currentlySelectedIndex);

                listModel.setShowOffline(true);
                listModel.addOfflineContacts();
                
                if (selectedObject != null) {
                    if (selectedObject instanceof MetaContact) {
                        contactList.setSelectedIndex(
                            listModel.indexOf((MetaContact) selectedObject));
                    }
                    else {
                        contactList.setSelectedIndex(
                            listModel.indexOf(
                                    (MetaContactGroup) selectedObject));
                    }
                }
            }
        } 
        else if (buttonName.equals("info")) {
            MetaContact selectedMetaContact = 
                (MetaContact) mainFrame.getTabbedPane().getContactListPanel()
                    .getContactList().getSelectedValue();
            
            if(selectedMetaContact != null) {
                Contact defaultContact = selectedMetaContact
                    .getDefaultContact();
                
                ProtocolProviderService defaultProvider
                    = defaultContact.getProtocolProvider();
                
                OperationSetWebContactInfo wContactInfo 
                    = mainFrame.getWebContactInfo(defaultProvider);
                
                BrowserLauncher.openURL(
                        wContactInfo.getWebContactInfo(defaultContact)
                            .toString());
            }
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event) {
        //TODO Implement pluginComponentAdded.
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event) {
        //TODO Implement pluginComponentRemoved.
    }
}
