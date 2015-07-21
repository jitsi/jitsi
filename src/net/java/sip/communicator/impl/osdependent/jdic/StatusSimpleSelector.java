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
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;

/**
 * The <tt>StatusSimpleSelector</tt> is a submenu which allow to select a status
 * for a protocol provider which does not support the OperationSetPresence
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class StatusSimpleSelector
    implements ActionListener,
               ItemListener
{

    /**
     * The protocol provider
     */
    private final ProtocolProviderService provider;

    private final Object menu;

    /**
     * Creates an instance of <tt>StatusSimpleSelector</tt>
     *
     * @param protocolProvider the protocol provider
     * @param swing
     */
    public StatusSimpleSelector(
        ProtocolProviderService protocolProvider,
        boolean swing)
    {
        this.provider = protocolProvider;

        /* the parent item */
        String text = provider.getAccountID().getDisplayName();
        this.menu = swing ? new JMenu(text) : new Menu(text);

        /* the menu itself */
        createMenuItem("service.gui.ONLINE", "online");
        createMenuItem("service.gui.OFFLINE", "offline");

        updateStatus();
    }

    private void createMenuItem(String textKey, String name)
    {
        String text = Resources.getString(textKey);
        if (menu instanceof JMenu)
        {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
            menuItem.setName(name);
            menuItem.addActionListener(this);
            ((JMenu) menu).add(menuItem);
        }
        else
        {
            CheckboxMenuItem menuItem = new CheckboxMenuItem(text);
            menuItem.setName(name);
            menuItem.addItemListener(this);
            ((Menu) menu).add(menuItem);
        }
    }

    public Object getMenu()
    {
        return menu;
    }

    /**
     * Change the status of the protocol according to
     * the menu item selected
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
        changeStatus(evt.getSource());
    }

    /**
     * Changes status corresponding the seelcted object.
     * @param source the source object selected
     */
    private void changeStatus(Object source)
    {
        String itemName;
        if (source instanceof Component)
            itemName = ((Component) source).getName();
        else
            itemName = ((MenuComponent) source).getName();

        if(itemName.equals("online"))
        {
            OsDependentActivator.getGlobalStatusService()
                .publishStatus(provider, GlobalStatusEnum.ONLINE);
        }
        else
        {
            OsDependentActivator.getGlobalStatusService()
                .publishStatus(provider, GlobalStatusEnum.OFFLINE);
        }
    }

    public void updateStatus()
    {
        if (menu instanceof AbstractButton)
        {
            byte[] iconBytes
                = provider
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if ((iconBytes != null) && (iconBytes.length > 0))
            {
                ImageIcon icon = new ImageIcon(iconBytes);

                if (!provider.isRegistered())
                    icon
                        = new ImageIcon(
                                LightGrayFilter
                                    .createDisabledImage(icon.getImage()));
                ((AbstractButton) menu).setIcon(icon);
            }
        }

        String onlineText = Resources.getString("service.gui.ONLINE");
        String offlineText = Resources.getString("service.gui.OFFLINE");
        if(menu instanceof Menu)
        {
            Menu theMenu = (Menu) menu;
            for(int i =0; i < theMenu.getItemCount(); i++)
            {
                MenuItem item = theMenu.getItem(i);

                if(item instanceof CheckboxMenuItem)
                {
                    if(item.getLabel().equals(onlineText))
                    {
                        if(provider.isRegistered())
                            ((CheckboxMenuItem)item).setState(true);
                        else
                            ((CheckboxMenuItem)item).setState(false);
                    }
                    else if(item.getLabel().equals(offlineText))
                    {
                        if(provider.isRegistered())
                            ((CheckboxMenuItem)item).setState(false);
                        else
                            ((CheckboxMenuItem)item).setState(true);
                    }
                }
            }
        }
        else if(menu instanceof JMenu)
        {
            JMenu theMenu = (JMenu) menu;
            for(int i =0; i < theMenu.getItemCount(); i++)
            {
                JMenuItem item = theMenu.getItem(i);

                if(item instanceof JCheckBoxMenuItem)
                {
                    if(item.getText().equals(onlineText))
                    {
                        if(provider.isRegistered())
                            item.setSelected(true);
                        else
                            item.setSelected(false);
                    }
                    else if(item.getText().equals(offlineText))
                    {
                        if(provider.isRegistered())
                            item.setSelected(false);
                        else
                            item.setSelected(true);
                    }
                }
            }
        }
    }

    /**
     * Listens for changes in item state (CheckboxMenuItem)s.
     * @param e the event.
     */
    public void itemStateChanged(ItemEvent e)
    {
        Object soutceItem = e.getSource();
        if(e.getStateChange() == ItemEvent.SELECTED)
        {
            if(soutceItem instanceof CheckboxMenuItem)
            {
                changeStatus(soutceItem);
            }
        }
        else if(e.getStateChange() == ItemEvent.DESELECTED)
        {
            if(soutceItem instanceof CheckboxMenuItem)
            {
                ((CheckboxMenuItem)soutceItem).setState(true);
            }
        }
    }
}
