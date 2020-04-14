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
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ProtocolProviderSelectorBox</tt> represents the call via menu in the
 * chat window. The menu contains all protocol providers.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Sebastien Vincent
 */
public class ProtocolProviderSelectorBox
    extends SIPCommMenuBar
    implements  ActionListener,
                Skinnable
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderSelectorBox.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The providers list associated with its <tt>JMenuItem</tt>.
     */
    private final Map<ProtocolProviderService, JMenuItem> providerMenuItems =
        new Hashtable<ProtocolProviderService, JMenuItem>();

    /**
     * The menu.
     */
    private final SIPCommMenu menu = new SelectorMenu();

    /**
     * The last selected provider.
     */
    private ProtocolProviderService lastSelectedProvider = null;

    /**
     * Creates an instance of <tt>ProtocolProviderSelectorBox</tt>.
     *
     * @param providers list of <tt>ProtocolProviderService</tt>
     */
    public ProtocolProviderSelectorBox(
        Iterator<ProtocolProviderService> providers)
    {
        setPreferredSize(new Dimension(30, 28));
        setMaximumSize(new Dimension(30, 28));
        setMinimumSize(new Dimension(30, 28));

        this.menu.setPreferredSize(new Dimension(30, 45));
        this.menu.setMaximumSize(new Dimension(30, 45));

        this.add(menu);

        this.setBorder(null);
        this.menu.setBorder(null);
        this.menu.setOpaque(false);
        this.setOpaque(false);

        // as a default disable the menu, it will be enabled as soon as we add
        // a valid menu item
        this.menu.setEnabled(false);

        ProtocolProviderService defaultProvider = null;
        while(providers.hasNext())
        {
            ProtocolProviderService provider = providers.next();
            if(defaultProvider == null)
                defaultProvider = provider;
            addProtocolProviderService(provider);
        }

        setSelected(defaultProvider);
    }

    /**
     * Sets the menu to enabled or disabled. The menu is enabled, as soon as it
     * contains one or more items. If it is empty, it is disabled.
     */
    private void updateEnableStatus()
    {
        this.menu.setEnabled(this.menu.getItemCount() > 0);
    }

    /**
     * Adds the given provider to the "call via" menu.
     * Only add those that support telephony.
     *
     * @param provider The provider to add.
     */
    public void addProtocolProviderService(ProtocolProviderService provider)
    {
        if (provider.getOperationSet(OperationSetBasicTelephony.class) != null)
        {
            Image img = createProviderImage(provider);

            JMenuItem menuItem = new JMenuItem(
                        "Via: " + provider.getAccountID().getDisplayName(),
                        new ImageIcon(img));

            menuItem.addActionListener(this);
            this.providerMenuItems.put(provider, menuItem);

            this.menu.add(menuItem);

            updateEnableStatus();
        }
    }

    /**
     * Removes the given provider from the "call via" menu. This method is
     * used to update the "call via" menu when a protocol contact is moved or
     * removed from the contact list.
     *
     * @param provider the provider to be removed
     */
    public void removeProtocolProviderService(ProtocolProviderService provider)
    {
        this.menu.remove(providerMenuItems.get(provider));
        this.providerMenuItems.remove(provider);

        updateEnableStatus();
    }

    /**
     * The listener of the provider selector box.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        for (Map.Entry<ProtocolProviderService, JMenuItem> providerMenuItem
                : providerMenuItems.entrySet())
        {
            ProtocolProviderService provider = providerMenuItem.getKey();

            if (providerMenuItem.getValue().equals(menuItem))
            {
                this.setSelected(provider, (ImageIcon) menuItem.getIcon());

                return;
            }
        }

        if (logger.isDebugEnabled())
            logger.debug( "Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + providerMenuItems.size()+") is : "
                      + providerMenuItems);
    }

    /**
     * Obtains the status icon for the given provider and
     * adds to it the account index information.
     *
     * @param provider The provider for which to create the image.
     * @return The indexed status image.
     */
    public Image createProviderImage(ProtocolProviderService provider)
    {
        return
            ImageLoader.getIndexedProtocolImage(
                ImageUtils.getBytesInImage(
                    provider.getProtocolIcon().getIcon(
                        ProtocolIcon.ICON_SIZE_16x16)),
                provider);
    }

    /**
     * In the "call via" menu selects the given contact and sets the given icon
     * to the "call via" menu button.
     *
     * @param provider the protocol provider
     * @param icon the provider icon
     */
    private void setSelected(ProtocolProviderService provider, ImageIcon icon)
    {
        lastSelectedProvider = provider;
        SelectedObject selectedObject = new SelectedObject(icon, provider);

        this.menu.setSelected(selectedObject);
    }

    /**
     * Sets the selected protocol provider.
     *
     * @param provider the protocol provider to select
     */
    public void setSelected(ProtocolProviderService provider)
    {
        this.setSelected(provider,
                new ImageIcon(createProviderImage(provider)));
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

    /**
     * Returns the selected provider.
     *
     * @return the selected provider.
     */
    public ProtocolProviderService getSelectedProvider()
    {
        return lastSelectedProvider;
    }

    private class SelectorMenu extends SIPCommMenu
    {
        private static final long serialVersionUID = 0L;

        Image image = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g.drawImage(image,
                getWidth() - image.getWidth(this) - 1,
                (getHeight() - image.getHeight(this) - 1)/2,
                this);
        }
    }
}
