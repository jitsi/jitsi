/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the
 * right mouse button on a group in the contact list. Through this menu the
 * user could add a contact to a group.
 * 
 * @author Yana Stamcheva
 */
public class GroupRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                PluginComponentListener
    {

    private I18NString addContactString = Messages.getI18NString("addContact");
    
    private I18NString removeGroupString = Messages.getI18NString("removeGroup");
    
    private I18NString renameGroupString = Messages.getI18NString("renameGroup");
    
    private SIPCommMenu addContactMenu
        = new SIPCommMenu(addContactString.getText());
    
    private JMenuItem removeGroupItem = new JMenuItem(
        removeGroupString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON)));
    
    private JMenuItem renameGroupItem = new JMenuItem(
        renameGroupString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));
    
    private MetaContactGroup group;
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of GroupRightButtonMenu.
     * 
     * @param mainFrame The parent <tt>MainFrame</tt> window. 
     * @param group The <tt>MetaContactGroup</tt> for which the menu is opened.
     */
    public GroupRightButtonMenu(MainFrame mainFrame,
            MetaContactGroup group) {
        
        this.group = group;
        this.mainFrame = mainFrame;
        
        this.addContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.add(addContactMenu);
        
        Iterator providers = mainFrame.getProtocolProviders();
        while(providers.hasNext()) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)providers.next();
            
            String protocolName = pps.getProtocolName();
            
            AccountMenuItem menuItem = new AccountMenuItem(pps,
                    new ImageIcon(createAccountStatusImage(pps)));
            
            menuItem.setName(protocolName);
            menuItem.addActionListener(this);
            
            this.addContactMenu.add(menuItem);
        }
        
        this.addSeparator();
        
        this.add(renameGroupItem);
        this.add(removeGroupItem);
        
        this.renameGroupItem.setName("renameGroup");
        this.removeGroupItem.setName("removeGroup");
        
        
        this.addContactMenu.setMnemonic(addContactString.getMnemonic());
        
        this.renameGroupItem.setMnemonic(renameGroupString.getMnemonic());
        
        this.removeGroupItem.setMnemonic(removeGroupString.getMnemonic());
        
        this.renameGroupItem.addActionListener(this);
        this.removeGroupItem.addActionListener(this);
        
        this.initPluginComponents();
    }
    
    
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                UIService.CONTAINER_GROUP_RIGHT_BUTTON_MENU);
        
        if(pluginComponents.hasNext())
            this.addSeparator();
        
        while (pluginComponents.hasNext())
        {
            Component o = (Component)pluginComponents.next();
            
            this.add(o);
            
            if (o instanceof ContactAwareComponent)
                ((ContactAwareComponent)o).setCurrentContactGroup(group);
        }
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. The choosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the choosen account and show the
     * dialog, where the user could add the contact.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem item = (JMenuItem)e.getSource();
        String itemText = item.getText();
        String itemName = item.getName();
        
        if(itemName.equals("removeGroup"))
        {   
            if(group != null) 
                new RemoveGroupThread(group).start();                
        }
        else if(itemName.equals("renameGroup"))
        {
            
            RenameGroupDialog dialog = new RenameGroupDialog(
                    mainFrame, group);

            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - 200,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - 50
                    );

            dialog.setVisible(true);
            
            dialog.requestFocusInFiled();
        }
        else if(item instanceof AccountMenuItem)
        {
            ProtocolProviderService pps 
                = ((AccountMenuItem)item).getProtocolProvider();

            AddContactDialog dialog = new AddContactDialog(
                    mainFrame, group, pps);

            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2 
                        - 250,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2 
                        - 100
                    );

            dialog.showDialog();
        }
    }
    
    /**
     * Removes a group from the contact list in a separate thread.
     */
    private class RemoveGroupThread extends Thread
    {
        private MetaContactGroup group;
        
        public RemoveGroupThread(MetaContactGroup group) {
            this.group = group;
        }
        public void run() {
            if(Constants.REMOVE_CONTACT_ASK) {
                String message = "<HTML>Are you sure you want to remove <B>"
                    + this.group.getGroupName()
                    + "</B><BR>from your contact list?</html>";

                MessageDialog dialog = new MessageDialog(mainFrame,
                        message, Messages.getI18NString("remove").getText());

                dialog.setTitle(Messages.getI18NString("removeGroup").getText());
                int returnCode = dialog.showDialog();
                
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    mainFrame.getContactList()
                                .removeMetaContactGroup(group);                    
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    mainFrame.getContactList()
                            .removeMetaContactGroup(group);
                    
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                mainFrame.getContactList().removeMetaContactGroup(group);
            }
        }
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID()
                .equals(UIService.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
        {
            this.add(c);
            
            if (c instanceof ContactAwareComponent)
            {   
                ((ContactAwareComponent)c)
                    .setCurrentContactGroup(group);
            }
            
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID()
                .equals(UIService.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
        {
            this.remove(c);
        }
    }
    
    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param pps the protocol provider for which to create the image
     * @return the indexed status image
     */
    public Image createAccountStatusImage(ProtocolProviderService pps)
    {  
        Image statusImage;
        
        OperationSetPresence presence
            = this.mainFrame.getProtocolPresenceOpSet(pps);
        
        if(presence != null)
        {
            
            statusImage = ImageLoader.getBytesInImage(
                presence.getPresenceStatus().getStatusIcon()); 
        }
        else if (pps.isRegistered())
        {
            statusImage
                = ImageLoader.getBytesInImage(pps.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16));
        }
        else {
            statusImage
                =  LightGrayFilter.createDisabledImage(
                    ImageLoader.getBytesInImage(pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
        }
        
        int index = mainFrame.getProviderIndex(pps);

        Image img = null;
        if(index > 0) {
            BufferedImage buffImage = new BufferedImage(
                    22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D)buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index+1).toString(), 14, 8);

            img = buffImage;
        }
        else {
            img = statusImage;
        }
        return img;
    }
 
    /**
     * The <tt>AccountMenuItem</tt> is a <tt>JMenuItem</tt> that stores a
     * <tt>ProtocolProviderService</tt> in it.
     */
    private class AccountMenuItem extends JMenuItem
    {
        private ProtocolProviderService pps;
        
        public AccountMenuItem(ProtocolProviderService pps, Icon icon)
        {
            super(pps.getAccountID().getUserID(), icon);
            
            this.pps = pps;
        }
        
        public ProtocolProviderService getProtocolProvider()
        {
            return pps;
        }
    }
}
