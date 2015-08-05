/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.osgi.framework.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the right
 * mouse button on a group in the contact list. Through this menu the user could
 * add a contact to a group.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class GroupRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                Skinnable
{
    private final Logger logger = Logger.getLogger(GroupRightButtonMenu.class);

    private final JMenuItem addContactItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.ADD_CONTACT") + "...");

    private final JMenuItem removeGroupItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REMOVE_GROUP"));

    private final JMenuItem renameGroupItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.RENAME_GROUP"));

    private final MetaContactGroup group;

    private final MainFrame mainFrame;

    /**
     * Creates an instance of GroupRightButtonMenu.
     *
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     * @param group The <tt>MetaContactGroup</tt> for which the menu is opened.
     */
    public GroupRightButtonMenu(MainFrame mainFrame, MetaContactGroup group)
    {
        this.group = group;
        this.mainFrame = mainFrame;

        if (!ConfigurationUtils.isAddContactDisabled())
        {
            this.add(addContactItem);
            this.addSeparator();
        }

        if (!ConfigurationUtils.isGroupRenameDisabled())
        {
            this.add(renameGroupItem);
        }

        if (!ConfigurationUtils.isGroupRemoveDisabled())
        {
            this.add(removeGroupItem);
        }

        this.addContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));

        this.renameGroupItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.RENAME_GROUP"));

        this.removeGroupItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.REMOVE_GROUP"));

        this.addContactItem.addActionListener(this);
        this.renameGroupItem.addActionListener(this);
        this.removeGroupItem.addActionListener(this);

        loadSkin();

        this.initPluginComponents();
    }

    /**
     * Initializes all plugin components.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter =
            "(" + Container.CONTAINER_ID + "="
                + Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU.getID() + ")";

        try
        {
            serRefs =
                GuiActivator.bundleContext.getServiceReferences(
                    PluginComponentFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                PluginComponentFactory factory =
                    (PluginComponentFactory) GuiActivator.bundleContext
                        .getService(serRefs[i]);
                PluginComponent component =
                    factory.getPluginComponentInstance(this);

                component.setCurrentContactGroup(group);

                this.add((Component) component.getComponent());

                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. The chosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the chosen account and show the
     * dialog, where the user could add the contact.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem item = (JMenuItem) e.getSource();

        if (item.equals(removeGroupItem))
        {
            if (group != null)
                MetaContactListManager.removeMetaContactGroup(group);
        }
        else if (item.equals(renameGroupItem))
        {

            RenameGroupDialog dialog = new RenameGroupDialog(mainFrame, group);

            dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 50);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (item.equals(addContactItem))
        {
            AddContactDialog dialog
                = new AddContactDialog(mainFrame);

            dialog.setSelectedGroup(group);

            dialog.setVisible(true);
        }
    }

    /**
     * Indicates that a plugin component has been added to this container.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    /**
     * Indicates that a new plugin component has been added. Adds it to this
     * container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (!factory.getContainer().equals(
                Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
            return;

        PluginComponent c = factory.getPluginComponentInstance(this);

        this.add((Component) c.getComponent());

        c.setCurrentContactGroup(group);

        this.repaint();
    }

    /**
     * Indicates that a new plugin component has been removed. Removes it to
     * from this container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (factory.getContainer()
            .equals(Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
        {
            this.remove((Component)factory.getPluginComponentInstance(this)
                    .getComponent());
        }
    }

    /**
     * Reloads label icons.
     */
    public void loadSkin()
    {
        addContactItem.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        removeGroupItem.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.DELETE_16x16_ICON)));

        renameGroupItem.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.RENAME_16x16_ICON)));
    }
}
