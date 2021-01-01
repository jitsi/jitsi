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
package net.java.sip.communicator.impl.osdependent.systemtray.appindicator;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.systemtray.*;

/**
 * Jitsi system tray abstraction for libappindicator.
 *
 * @author Ingo Bauersachs
 */
public class AppIndicatorTray extends SystemTray
{
    private boolean dynamicMenu;

    public AppIndicatorTray(boolean dynamicMenu) throws Exception
    {
        this.dynamicMenu = dynamicMenu;
        try
        {
            // pre-initialize the JNA libraries before attempting to use them
            AppIndicator1.INSTANCE.toString();
            Gtk.INSTANCE.toString();
            Gobject.INSTANCE.toString();
            Gtk.INSTANCE.gtk_init(0, null);
        }
        catch (Throwable t)
        {
            throw new Exception("AppIndicator1 tray icon not available", t);
        }
    }

    @Override
    public void addTrayIcon(TrayIcon trayIcon)
    {
        ((AppIndicatorTrayIcon) trayIcon).createTray();
    }

    @Override
    public TrayIcon createTrayIcon(ImageIcon icon, String tooltip, Object popup)
    {
        return new AppIndicatorTrayIcon(icon, tooltip, (JPopupMenu) popup);
    }

    @Override
    public boolean useSwingPopupMenu()
    {
        // we want icons
        return true;
    }

    @Override
    public boolean supportsDynamicMenu()
    {
        return dynamicMenu;
    }
}
