/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container; // disambiguation
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>MessageWindowMenuBar</tt> is the menu bar in the chat window where
 * all menus are added.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MessageWindowMenuBar
    extends SIPCommWindowMenuBar
{
    private final FileMenu fileMenu;

    private final EditMenu editMenu;

    private final OptionsMenu optionsMenu;

    private final HelpMenu helpMenu;

    private final PluginContainer pluginContainer;

    /**
     * Initializes a new <tt>MessageWindowMenuBar</tt> instance.
     * 
     * @param parentWindow the ChatWindow which is to be the parent of the new
     * instance
     */
    public MessageWindowMenuBar(ChatWindow parentWindow)
    {
        super("service.gui.CHAT_MENU_FOREGROUND");

        fileMenu = new FileMenu(parentWindow);
        editMenu = new EditMenu(parentWindow);
        optionsMenu = new OptionsMenu(parentWindow);
        helpMenu = new HelpMenu(parentWindow);

        this.init();

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_CHAT_MENU_BAR)
                {

                    /*
                     * Overrides PluginContainer#addComponentToContainer(
                     * Component, JComponent). Keeps the Help menu last as it is
                     * its conventional place.
                     */
                    @Override
                    protected void addComponentToContainer(
                        Component component,
                        JComponent container)
                    {
                        /*
                         * Apply the opaque property in order to prevent plugin
                         * menus from looking different than the built-in menus.
                         */
                        if (component instanceof SIPCommMenu)
                            ((SIPCommMenu) component).setOpaque(false);

                        container.add(component, getComponentIndex(helpMenu));
                    }
                };

        parentWindow.addChatChangeListener(new ChatChangeListener()
        {
            public void chatChanged(ChatPanel panel)
            {
                MetaContact contact
                    = GuiActivator.getUIService().getChatContact(panel);

                for (PluginComponent c : pluginContainer.getPluginComponents())
                    c.setCurrentContact(contact);
            }
        });
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        pluginContainer.dispose();
        helpMenu.dispose();
    }

    /**
     * Initializes the menu bar, by adding all contained menus.
     */
    private void init()
    {
        this.add(fileMenu);
        this.add(editMenu);
        this.add(optionsMenu);
        this.add(helpMenu);
    }

    /**
     * Returns the currently selected menu.
     */
    public JMenu getSelectedMenu()
    {
        for (int i = 0, menuCount = getMenuCount(); i < menuCount; i++)
        {
            JMenu menu = this.getMenu(i);

            if (menu.isSelected())
                return menu;
        }
        return null;
    }
}
