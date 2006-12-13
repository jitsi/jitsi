/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
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
                PluginComponentListener,
                ComponentListener,
                ListSelectionListener {

    private Logger logger = Logger.getLogger(QuickMenu.class.getName());
    
    private JButton infoButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_INFO_ICON)));

    private JButton configureButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON)));

    private JButton searchButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON)));

    private JButton addButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.QUICK_MENU_ADD_ICON)));

    private ConfigurationManager configDialog;

    private MainFrame mainFrame;

    private int movedDownButtons = 0;
    
    /**
     * Create an instance of the <tt>QuickMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public QuickMenu(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
                
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        this.setFloatable(true);

        this.infoButton.setPreferredSize(new Dimension(28, 28));
        this.configureButton.setPreferredSize(new Dimension(28, 28));
        this.searchButton.setPreferredSize(new Dimension(28, 28));
        this.addButton.setPreferredSize(new Dimension(28, 28));

        this.infoButton.setToolTipText(
            Messages.getI18NString("userInfo").getText());
        this.configureButton.setToolTipText(
            Messages.getI18NString("configure").getText());
        this.searchButton.setToolTipText(
            Messages.getI18NString("showOfflineUsers").getText());
        this.addButton.setToolTipText(
            Messages.getI18NString("addContact").getText());

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
        
        this.addButton.addComponentListener(this);
        this.configureButton.addComponentListener(this);
        this.searchButton.addComponentListener(this);
        this.infoButton.addComponentListener(this);
    }
    
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                UIService.CONTAINER_MAIN_TOOL_BAR);
        
        if(pluginComponents.hasNext())
            this.addSeparator();
        
        while (pluginComponents.hasNext())
        {
            Component c = (Component)pluginComponents.next();
            
            this.add(c);
                        
            if (c instanceof ContactAwareComponent)
            {
                Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();
                            
                if(selectedValue instanceof MetaContact)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContactGroup((MetaContactGroup)selectedValue);
                }
            }
        }
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
            
            wizard.showDialog(false);
        }
        else if (buttonName.equals("config")) {

            configDialog = GuiActivator.getUIService().getConfigurationManager();

            configDialog.showWindow();
        }
        else if (buttonName.equals("search")) {

            ContactList contactList = mainFrame.getContactListPanel()
                .getContactList();

            ContactListModel listModel
                = (ContactListModel) contactList.getModel();
            
            Object selectedObject = null;
            int currentlySelectedIndex = contactList.getSelectedIndex();
            if(currentlySelectedIndex != -1) {
                selectedObject
                    = listModel.getElementAt(currentlySelectedIndex);
            }

            listModel.setShowOffline(!listModel.showOffline());

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
        else if (buttonName.equals("info")) {
            MetaContact selectedMetaContact =
                (MetaContact) mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

            if(selectedMetaContact != null) {
                OperationSetWebContactInfo wContactInfo = null;
                
                Iterator protocolContacts = selectedMetaContact.getContacts();
                
                while(protocolContacts.hasNext()) {
                    Contact protoContact = (Contact) protocolContacts.next();
                    
                    wContactInfo = mainFrame.getWebContactInfo(
                        protoContact.getProtocolProvider());
                    
                    if(wContactInfo != null)
                        break;
                }
                
                if(wContactInfo != null) {
                    Contact defaultContact = selectedMetaContact
                        .getDefaultContact();

                    CrossPlatformBrowserLauncher.openURL(
                            wContactInfo.getWebContactInfo(defaultContact)
                                .toString());
                }
                else {
                    new ErrorDialog(mainFrame,
                        Messages.getI18NString("selectContactSupportingInfo")
                            .getText(),
                        Messages.getI18NString("warning").getText(),
                        ErrorDialog.WARNING).showDialog();
                }
            }            
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event) {
        Component c = (Component) event.getSource();
        
        this.add(c);
        
        if (c instanceof ContactAwareComponent)
        {
            Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();
            
            if(selectedValue instanceof MetaContact)
            {
                ((ContactAwareComponent)c)
                    .setCurrentContact((MetaContact)selectedValue);
            }
            else if(selectedValue instanceof MetaContactGroup)
            {
                ((ContactAwareComponent)c)
                    .setCurrentContactGroup((MetaContactGroup)selectedValue);
            }
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event) {
        Component c = (Component) event.getSource();
        
        this.remove(c);
    }

    public void componentHidden(ComponentEvent e)
    {}

    /**
     * Implements ComponentListener.componentMoved method in order to resize
     * the toolbar when buttons are aligned on more than one row.
     */
    public void componentMoved(ComponentEvent e)
    {        
        int compCount = this.getComponentCount();
        int buttonHeight = this.infoButton.getHeight();

        int biggestY = 0;
        for (int i = 0; i < compCount; i ++)
        {
            Component c = this.getComponent(i);
            
            if(c instanceof JButton)
            {
                if(c.getY() > biggestY)
                    biggestY = c.getY();
            }
        }
        
        this.setPreferredSize(
            new Dimension(this.getWidth(), biggestY + buttonHeight));
        
        ((JPanel)this.getParent()).revalidate();
        ((JPanel)this.getParent()).repaint();
    }

    public void componentResized(ComponentEvent e)
    {}

    public void componentShown(ComponentEvent e)
    {}
    
    public void valueChanged(ListSelectionEvent e)
    {   
        if((e.getFirstIndex() != -1 || e.getLastIndex() != -1))
        {
            Iterator pluginComponents = GuiActivator.getUIService()
                .getComponentsForContainer(
                    UIService.CONTAINER_MAIN_TOOL_BAR);
            
            while (pluginComponents.hasNext())
            {
                Component c = (Component)pluginComponents.next();
                    
                if(!(c instanceof ContactAwareComponent))
                    continue;
                
                Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();
                            
                if(selectedValue instanceof MetaContact)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContactGroup(
                            (MetaContactGroup)selectedValue);
                }
            }            
        }
    }
}
