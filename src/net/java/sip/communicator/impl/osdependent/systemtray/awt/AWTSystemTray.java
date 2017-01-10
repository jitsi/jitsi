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
package net.java.sip.communicator.impl.osdependent.systemtray.awt;

import java.awt.*;

import javax.swing.*;

import org.jitsi.util.*;

import net.java.sip.communicator.impl.osdependent.systemtray.SystemTray;
import net.java.sip.communicator.impl.osdependent.systemtray.TrayIcon;

/**
 * Wrapper of the AWT SystemTray class.
 */
public class AWTSystemTray
    extends SystemTray
{
    private final java.awt.SystemTray impl;

    /**
     * Creates a new instance of this class.
     */
    public AWTSystemTray()
    {
        impl = java.awt.SystemTray.getSystemTray();
    }

    @Override
    public void addTrayIcon(TrayIcon trayIcon)
        throws IllegalArgumentException
    {
        try
        {
            impl.add(((AWTTrayIcon) trayIcon).getImpl());
        }
        catch (AWTException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public TrayIcon createTrayIcon(ImageIcon icon, String tooltip,
        Object popup)
    {
        return new AWTTrayIcon(icon.getImage(), tooltip, popup);
    }

    @Override
    public boolean useSwingPopupMenu()
    {
        // enable swing for Java 1.6 except for the mac version
        return !OSUtils.IS_MAC;
    }

    @Override
    public boolean supportsDynamicMenu()
    {
        return true;
    }
}