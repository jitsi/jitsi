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

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 * @author Thomas Hofer
 * @author Lyubomir Marinov
 */
public class HelpMenu
    extends SIPCommMenu
    implements ActionListener
{
    /**
     * The <tt>PluginContainer</tt> which implements the logic related to
     * dealing with <tt>PluginComponent</tt>s on behalf of this
     * <tt>HelpMenu</tt>.
     */
    private final PluginContainer pluginContainer;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     *
     * @param mainFrame the parent window
     */
    public HelpMenu(MainFrame mainFrame)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        setMnemonic(resources.getI18nMnemonic("service.gui.HELP"));
        setText(resources.getI18NString("service.gui.HELP"));

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_HELP_MENU);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev)
    {
    }
}
