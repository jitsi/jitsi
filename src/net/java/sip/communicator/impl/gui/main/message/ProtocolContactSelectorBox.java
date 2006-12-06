/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class ProtocolContactSelectorBox
    extends JMenuBar
    implements ActionListener
{
    private static final Logger logger
        = Logger.getLogger(ProtocolContactSelectorBox.class);

    private ChatSendPanel sendPanel;
    private Hashtable contactsTable = new Hashtable();
    
    private SIPCommMenu menu = new SIPCommMenu();

    public ProtocolContactSelectorBox(ChatSendPanel sendPanel)
    {
        this.sendPanel = sendPanel;
        
        this.add(menu);
    }

    public void addContact(Contact contact)
    {
        Image img = createContactStatusImage(contact);

        JMenuItem menuItem = new JMenuItem(
                    contact.getDisplayName(),
                    new ImageIcon(img));

        menuItem.addActionListener(this);
        this.contactsTable.put(contact, menuItem);
        this.menu.add(menuItem);
    }

    /**
     * The listener of the protocol contact's selector box.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        ChatPanel chatPanel = sendPanel.getChatPanel();
        MainFrame mainFrame = chatPanel.getChatWindow().getMainFrame();

        Enumeration i = contactsTable.keys();
        while(i.hasMoreElements()) {
            Contact protocolContact = (Contact) i.nextElement();

            if (contactsTable.get(protocolContact).equals(menuItem)) {

                OperationSetBasicInstantMessaging im
                    = mainFrame.getProtocolIM(
                                protocolContact.getProtocolProvider());

                OperationSetTypingNotifications tn
                    = mainFrame.getTypingNotifications(
                            protocolContact.getProtocolProvider());

                chatPanel.setImOperationSet(im);
                chatPanel.setTnOperationSet(tn);

                chatPanel.setProtocolContact(protocolContact);

                this.setSelected(protocolContact, (ImageIcon)menuItem.getIcon());

                return;
            }
        }
        logger.debug( "Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + contactsTable.size()+") is : "
                      + contactsTable);
    }

    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the proto contact for which to create the image
     * @return the indexed status image
     */
    public Image createContactStatusImage(Contact protoContact)
    {
        Image statusImage = ImageLoader.getBytesInImage(
                protoContact.getPresenceStatus().getStatusIcon());

        int index = sendPanel.getChatPanel().getChatWindow().getMainFrame()
            .getProviderIndex(protoContact.getProtocolProvider());

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
     * Updates the protocol contact status.
     * @param protoContact the protocol contact to update
     */
    public void updateContactStatus(Contact protoContact)
    {
        JMenuItem menuItem = (JMenuItem)contactsTable.get(protoContact);
        Icon icon = new ImageIcon(createContactStatusImage(protoContact));

        menuItem.setIcon(icon);
        if(menu.getSelectedObject().equals(protoContact))
        {
            this.menu.setIcon(icon);
        }
    }

    public void setSelected(Contact protoContact, ImageIcon icon)
    {
        this.menu.setSelected(protoContact, icon);
        
        String tooltipText;
        
        if(!protoContact.getDisplayName().equals(protoContact.getAddress()))
            tooltipText = protoContact.getDisplayName()
                + " (" + protoContact.getAddress() + ")";
        else
            tooltipText = protoContact.getDisplayName();
        
        this.menu.setToolTipText(tooltipText);
    }
    
    /**
     * Sets the selected contact to the given proto contact.
     * @param protoContact the proto contact to select
     */
    public void setSelected(Contact protoContact)
    {
        this.setSelected(protoContact,
                new ImageIcon(createContactStatusImage(protoContact)));        
    }
    
    /**
     * Returns the protocol menu.
     * 
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }
}
