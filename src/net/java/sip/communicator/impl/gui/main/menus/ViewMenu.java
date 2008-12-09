/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>ViewMenu</tt> is a menu in the main application menu bar.
 * 
 * @author Yana Stamcheva
 */
public class ViewMenu
    extends SIPCommMenu
{

    private Logger logger = Logger.getLogger(ViewMenu.class.getName());

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>ViewMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ViewMenu(MainFrame mainFrame) {

        super(GuiActivator.getResources().getI18NString("service.gui.VIEW"));

        this.setOpaque(false);

        this.setForeground(
            new Color(GuiActivator.getResources().
                getColor("service.gui.MAIN_MENU_FOREGROUND")));

        this.mainFrame = mainFrame;

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.VIEW"));
    }
}
