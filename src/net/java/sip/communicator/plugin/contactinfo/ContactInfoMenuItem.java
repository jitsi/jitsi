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
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

/**
 *
 * @author Adam Goldstein
 */
public class ContactInfoMenuItem
    extends AbstractPluginComponent
    implements ActionListener
{
    private AbstractButton menuItem = null;

    private MetaContact metaContact;

    /**
     * The button index, for now placed on last position.
     */
    private final static int CONTACT_INFO_BUTTON_IX = 50;

    /**
     * Creates a <tt>ContactInfoMenuItem</tt>.
     */
    public ContactInfoMenuItem(PluginComponentFactory parentFactory)
    {
        this(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU, parentFactory);
    }

    /**
     * Creates a <tt>ContactInfoMenuItem</tt>.
     */
    public ContactInfoMenuItem(Container container,
                               PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);
    }

    /**
     * Sets the currently selected <tt>MetaContact</tt>.
     * @param metaContact the currently selected meta contact
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     * @param contact the currently selected contact
     */
    @Override
    public void setCurrentContact(Contact contact)
    {
        if(metaContact == null)
        {
            // search for the metacontact
            MetaContactListService mcs =
                ContactInfoActivator.getContactListService();

            metaContact =
                mcs.findMetaContactByContact(contact);
        }
    }

    /**
     * Initializes and shows the contact details dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        if(metaContact == null)
            return;

        ContactInfoDialog cinfoDialog = new ContactInfoDialog(metaContact);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        cinfoDialog.setLocation(
            screenSize.width/2 - cinfoDialog.getWidth()/2,
            screenSize.height/2 - cinfoDialog.getHeight()/2);
        cinfoDialog.setVisible(true);
    }

    public Object getComponent()
    {
        return getMenuItem();
    }

    public String getName()
    {
        return getMenuItem().getText();
    }

    private AbstractButton getMenuItem()
    {
        if(menuItem == null)
        {
            if(getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
            {
                menuItem =
                    new SIPCommButton(null,
                            (Image)ContactInfoActivator.getImageLoaderService()
                                .getImage(new ImageID(
                                    "plugin.contactinfo.CONTACT_INFO_TOOLBAR")))
                    {
                        /**
                         * Returns the button index.
                         * @return the button index.
                         */
                        public int getIndex()
                        {
                            return CONTACT_INFO_BUTTON_IX;
                        }
                    };

                menuItem.setPreferredSize(new Dimension(25, 25));
                menuItem.setToolTipText(
                    Resources.getString("service.gui.CONTACT_INFO"));
            }
            else if(getContainer().equals(Container.CONTAINER_CALL_DIALOG))
            {
                menuItem =
                    new SIPCommButton(null,
                            (Image)ContactInfoActivator.getImageLoaderService()
                                .getImage(new ImageID(
                                    "plugin.contactinfo.CONTACT_INFO_CALL_WINDOW")))
                    {
                        /**
                         * Returns the button index.
                         * @return the button index.
                         */
                        public int getIndex()
                        {
                            return CONTACT_INFO_BUTTON_IX;
                        }
                    };
                menuItem.setPreferredSize(new Dimension(44, 38));
                menuItem.setToolTipText(
                    Resources.getString("service.gui.CONTACT_INFO"));
            }
            else
                menuItem =
                    new JMenuItem(
                            Resources.getString("service.gui.CONTACT_INFO"),
                            new ImageIcon(Resources.getImage(
                                "plugin.contactinfo.CONTACT_INFO_ICON")));
            menuItem.addActionListener(this);
        }

        return menuItem;
    }
}
