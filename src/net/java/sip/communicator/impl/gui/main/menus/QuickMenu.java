/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
public class QuickMenu
    extends SIPCommToolBar 
    implements  ActionListener,
                PluginComponentListener {

    private Logger logger = Logger.getLogger(QuickMenu.class.getName());
    
    private JButton infoButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_INFO_ICON)));

    private JButton configureButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON)));

    private JButton searchButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_SEARCH_ICON)));

    private JButton addButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_ADD_ICON)));

    private ConfigurationManager configDialog;

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

            AddContactWizard wizard = new AddContactWizard(mainFrame);
            
            wizard.showModalDialog();            
        }
        else if (buttonName.equals("config")) {

            configDialog = GuiActivator.getUIService().getConfigurationManager();

            configDialog.showDialog();
        }
        else if (buttonName.equals("search")) {

            ContactList contactList = mainFrame.getContactListPanel()
                .getContactList();

            ContactListModel listModel
                = (ContactListModel) contactList.getModel();

            if (listModel.showOffline()) {                
                listModel.setShowOffline(false);
                listModel.removeOfflineContacts();
            }
            else {
                Object selectedObject = null;
                int currentlySelectedIndex = contactList.getSelectedIndex();
                if(currentlySelectedIndex != -1) {
                    selectedObject
                        = listModel.getElementAt(currentlySelectedIndex);
                }

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
                (MetaContact) mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

            if(selectedMetaContact != null) {
                Contact defaultContact = selectedMetaContact
                    .getDefaultContact();

                ProtocolProviderService defaultProvider
                    = defaultContact.getProtocolProvider();

                OperationSetWebContactInfo wContactInfo
                    = mainFrame.getWebContactInfo(defaultProvider);

                CrossPlatformBrowserLauncher.openURL(
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
