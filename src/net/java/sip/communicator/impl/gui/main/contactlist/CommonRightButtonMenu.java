/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the
 * right mouse button on a group in the contact list. Through this menu the
 * user could add a contact to a group.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CommonRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                Skinnable
{
    private final JMenuItem addContactItem = new JMenuItem(
        GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT") + "...");

    private final JMenuItem createGroupItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CREATE_GROUP"));

    private MainFrame mainFrame;

    /**
     * Creates an instance of GroupRightButtonMenu.
     * 
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public CommonRightButtonMenu(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        if (!ConfigurationManager.isAddContactDisabled())
            this.add(addContactItem);

        this.add(createGroupItem);

        this.addContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));
        this.createGroupItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CREATE_GROUP"));

        this.addContactItem.addActionListener(this);
        this.createGroupItem.addActionListener(this);

        loadSkin();
    }

    /**
     * Handles the <tt>ActionEvent</tt>. The chosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the chosen account and show the
     * dialog, where the user could add the contact.
     * @param e the <tt>ActionEvent</tt>, which notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem item = (JMenuItem) e.getSource();

        if(item.equals(createGroupItem))
        {
            CreateGroupDialog dialog = new CreateGroupDialog(mainFrame);

            dialog.setVisible(true);
        }
        else if(item.equals(addContactItem))
        {
            AddContactDialog dialog = new AddContactDialog(mainFrame);

            dialog.setVisible(true);
        }
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        addContactItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        createGroupItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));
    }
}
