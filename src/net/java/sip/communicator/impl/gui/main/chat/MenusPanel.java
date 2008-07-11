/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>MenusPanel</tt> is the panel, containing all toolbars in the chat
 * window.
 * 
 * @author Yana Stamcheva
 */
public class MenusPanel
    extends JPanel
{
    private MessageWindowMenuBar menuBar;

    private MainToolBar mainToolBar;

    private ChatWindow parentWindow;

    /**
     * Creates an instance and constructs the <tt>MenusPanel</tt>.
     * 
     * @param parentWindow The parent <tt>ChatWindow</tt> for this panel.
     */
    public MenusPanel(ChatWindow parentWindow)
    {
        super();

        this.parentWindow = parentWindow;

        boolean isToolBarExtended
            = new Boolean(ApplicationProperties
                    .getProperty("isToolBarExteneded")).booleanValue();

        if (isToolBarExtended)
            mainToolBar = new ExtendedMainToolBar(this.parentWindow);
        else
            mainToolBar = new MainToolBar(this.parentWindow);

        menuBar = new MessageWindowMenuBar(this.parentWindow);

        this.setLayout(new BorderLayout(5, 5));

        parentWindow.setJMenuBar(menuBar);
        this.add(mainToolBar, BorderLayout.CENTER);
    }

    /**
     * Adds a new toolbar to this <tt>MenusPanel</tt>.
     * 
     * @param toolBar The toolbar to add.
     */
    public void addToolBar(JToolBar toolBar)
    {
        this.add(toolBar);
    }

    /**
     * Returns the <tt>MainToolBar</tt>.
     * 
     * @return the <tt>MainToolBar</tt>.
     */
    public MainToolBar getMainToolBar()
    {
        return mainToolBar;
    }

    /**
     * Returns the <tt>MessageWindowMenuBar</tt>.
     * 
     * @return the <tt>MessageWindowMenuBar</tt>
     */
    public MessageWindowMenuBar getMainMenuBar()
    {
        return menuBar;
    }
}
