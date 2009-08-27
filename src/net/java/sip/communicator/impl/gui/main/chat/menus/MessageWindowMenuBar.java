/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MessageWindowMenuBar</tt> is the menu bar in the chat window where
 * all menus are added.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MessageWindowMenuBar
    extends JMenuBar
    implements PluginComponentListener
{
    private final Logger logger = Logger.getLogger(MessageWindowMenuBar.class);

    private FileMenu fileMenu;

    private EditMenu editMenu;
    
    private OptionsMenu optionsMenu;

    private final HelpMenu helpMenu;

    private final ChatWindow parentWindow;

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

        this.initPluginComponents();
        
        this.parentWindow.addChatChangeListener(new ChatChangeListener()
        {
            public void chatChanged(ChatPanel panel)
            {
                MetaContact contact =
                    GuiActivator.getUIService().getChatContact(panel);

                for (Component c : getComponents())
                {
                    if (!(c instanceof PluginComponent))
                        continue;
                    
                    ((PluginComponent)c).setCurrentContact(contact);
                }
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
        GuiActivator.getUIService().removePluginComponentListener(this);
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

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_MENU_BAR.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

                this.add((Component)component.getComponent());
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_MENU_BAR))
        {
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_MENU_BAR))
        {
            this.remove((Component) c.getComponent());
        }
    }
}
