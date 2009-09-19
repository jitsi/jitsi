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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;

/**
 * The <tt>MessageWindowMenuBar</tt> is the menu bar in the chat window where
 * all menus are added.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MessageWindowMenuBar
    extends JMenuBar
{
    private final FileMenu fileMenu;

    private final EditMenu editMenu;
    
    private final OptionsMenu optionsMenu;

    private final HelpMenu helpMenu;

    private final ChatWindow parentWindow;

    private final PluginContainer pluginContainer;

    /**
     * Creates an instance of <tt>MessageWindowMenuBar</tt>.
     * 
     * @param parentWindow The parent ChatWindow.
     */
    public MessageWindowMenuBar(ChatWindow parentWindow)
    {
        this.parentWindow = parentWindow;

        this.setForeground(
            new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAIN_MENU_FOREGROUND")));

        fileMenu = new FileMenu(this.parentWindow);

        editMenu = new EditMenu(this.parentWindow);
        
        optionsMenu = new OptionsMenu(this.parentWindow);

        helpMenu = new HelpMenu(this.parentWindow);

        fileMenu.setOpaque(false);
        editMenu.setOpaque(false);
        helpMenu.setOpaque(false);

        this.init();

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_CHAT_MENU_BAR)
                {

                    /*
                     * Overrides PluginContainer#addComponentToContainer(
                     * Component, JComponent). Keeps the Help menu last as it is
                     * its conventional place.
                     */
                    protected void addComponentToContainer(
                        Component component,
                        JComponent container)
                    {
                        container.add(component, getComponentIndex(helpMenu));
                    }
                };

        this.parentWindow.addChatChangeListener(new ChatChangeListener()
        {
            public void chatChanged(ChatPanel panel)
            {
                MetaContact contact =
                    GuiActivator.getUIService().getChatContact(panel);

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
        int menuCount = this.getMenuCount();

        for (int i = 0; i < menuCount; i++)
        {
            JMenu menu = this.getMenu(i);

            if (menu.isSelected())
            {
                return menu;
            }
        }
        return null;
    }

    /**
     * Paints the MENU_BACKGROUND image on the background of this container.
     * 
     * @param g the Graphics object that does the painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Image backgroundImage =
            ImageLoader.getImage(ImageLoader.MENU_BACKGROUND);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
    }
}
