/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 */
public class HelpMenu
    extends SIPCommMenu
    implements ActionListener,
               PluginComponentListener
{

    private Logger logger = Logger.getLogger(HelpMenu.class.getName());

    private I18NString aboutString = Messages.getI18NString("about");

    private ChatWindow chatWindow;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public HelpMenu(ChatWindow chatWindow) {

        super(Messages.getI18NString("help").getText());

        this.chatWindow = chatWindow;

        this.setForeground(new Color(
            ColorProperties.getColor("chatMenuBarForeground")));

        this.setMnemonic(Messages.getI18NString("help").getMnemonic());

        this.initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
                .getComponentsForContainer(UIService.CONTAINER_CHAT_HELP_MENU);

        while (pluginComponents.hasNext())
        {
            Component o = (Component) pluginComponents.next();

            this.add(o);
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     */
    public void actionPerformed(ActionEvent e)
    {
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();

        if (event.getContainerID().equals(UIService.CONTAINER_CHAT_HELP_MENU))
        {
            this.add(c);

            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();

        if (event.getContainerID().equals(UIService.CONTAINER_CHAT_HELP_MENU))
        {
            this.remove(c);
        }
    }

}
