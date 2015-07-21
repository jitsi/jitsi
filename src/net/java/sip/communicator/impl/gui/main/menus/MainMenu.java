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
package net.java.sip.communicator.impl.gui.main.menus;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;

import org.jitsi.service.resources.*;

/**
 * The main menu bar. This is the menu bar that appears on top of the main
 * window. It contains a file menu, tools menu, view menu and help menu.
 * <p>
 * Note that this container allows also specifying a custom background by
 * modifying the menuBackground.png in the resources/images/impl/gui/common
 * folder.
 * </p>
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MainMenu
    extends SIPCommWindowMenuBar
{
    /**
     * Initializes a new <tt>MainMenu</tt> instance.
     */
    public MainMenu(MainFrame mainFrame)
    {
        super("service.gui.MAIN_MENU_FOREGROUND");

        addMenu(new FileMenu(mainFrame), "service.gui.FILE");
        addMenu(new ToolsMenu(), "service.gui.TOOLS");
        addMenu(new HelpMenu(mainFrame), "service.gui.HELP");
    }

    private void addMenu(JMenu menu, String key)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        menu.setText(resources.getI18NString(key));
        menu.setMnemonic(resources.getI18nMnemonic(key));

        add(menu);
    }

    /**
     * Determines whether there are selected menus.
     *
     * @return <tt>true</tt> if there are selected menus;otherwise,
     * <tt>false</tt>
     */
    public boolean hasSelectedMenus()
    {
        for (int i = 0, menuCount = getMenuCount(); i < menuCount; i++)
            if (getMenu(i).isSelected())
                return true;
        return false;
    }
}
