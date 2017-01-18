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
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.systemtray.TrayIcon;

/**
 * Wrapper of the AWT TrayIcon class.
 */
public class AWTTrayIcon
    implements TrayIcon
{
    private final java.awt.TrayIcon impl;

    /**
     * Creates a new instance of this class.
     * 
     * @param image the <tt>Image</tt> to be used
     * @param tooltip the string to be used as tooltip text; if the value is
     *            <tt>null</tt> no tooltip is shown
     * @param popup the menu to be used for the tray icon's popup menu; if the
     *            value is <tt>null</tt> no popup menu is shown
     */
    public AWTTrayIcon(Image image, String tooltip,
        Object popup)
    {
        if (popup instanceof JPopupMenu)
        {
            impl = new java.awt.TrayIcon(image, tooltip);
            impl.addMouseListener(new AWTMouseAdapter((JPopupMenu)popup));
        }
        else if (popup instanceof PopupMenu)
        {
            impl = new java.awt.TrayIcon(image, tooltip, (PopupMenu)popup);
        }
        else if (popup == null)
        {
            impl = new java.awt.TrayIcon(image, tooltip);
        }
        else
        {
            throw new IllegalArgumentException("Invalid popup menu type");
        }
    }

    public void setDefaultAction(final Object menuItem)
    {
        // clear all previous listeners
        ActionListener[] previous = impl.getActionListeners();
        for (ActionListener l : previous)
        {
            impl.removeActionListener(l);
        }

        // get the new handlers
        final ActionListener[] listeners;
        if (menuItem instanceof JMenuItem)
        {
            listeners = ((JMenuItem) menuItem).getActionListeners();
        }
        else if (menuItem instanceof MenuItem)
        {
            listeners = ((MenuItem) menuItem).getActionListeners();
        }
        else
        {
            return;
        }

        // create a custom handler to fake that the source is the menu item
        impl.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for (ActionListener l : listeners)
                {
                    l.actionPerformed(new ActionEvent(menuItem,
                        e.getID(), e.getActionCommand()));
                }
            }
        });
    }

    public void addBalloonActionListener(ActionListener listener)
    {
        // java.awt.TrayIcon doesn't support addBalloonActionListener()
    }

    public void displayMessage(String caption, String text,
                               java.awt.TrayIcon.MessageType messageType)
        throws NullPointerException
    {
        impl.displayMessage(caption, text, messageType);
    }

    public void setIcon(ImageIcon icon) throws NullPointerException
    {
        impl.setImage(icon.getImage());
    }

    public void setIconAutoSize(boolean autoSize)
    {
        impl.setImageAutoSize(autoSize);
    }

    java.awt.TrayIcon getImpl()
    {
        return impl;
    }
}