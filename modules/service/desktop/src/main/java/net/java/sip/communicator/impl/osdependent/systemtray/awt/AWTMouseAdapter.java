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
import javax.swing.event.*;

import org.jitsi.util.*;

/**
 * Extended mouse adapter to show the JPopupMenu in Java 6. Based on: <a href=
 * "https://community.oracle.com/blogs/ixmal/2006/05/03/using-jpopupmenu-trayicon">
 * Using JPopupMenu in TrayIcon Blog</a> and <a href=
 * "https://community.oracle.com/blogs/alexfromsun/2008/02/14/jtrayicon-update">
 * JTrayIcon update Blog</a>.
 *
 * Use a hidden JWindow (JDialog for Windows) to manage the JPopupMenu.
 *
 * @author Damien Roth
 */
class AWTMouseAdapter
    extends MouseAdapter
{
    private JPopupMenu popup = null;
    private Window hiddenWindow = null;

    public AWTMouseAdapter(JPopupMenu p)
    {
        this.popup = p;
        this.popup.addPopupMenuListener(new PopupMenuListener()
        {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {}

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                if (hiddenWindow != null)
                {
                    hiddenWindow.dispose();
                    hiddenWindow = null;
                }
            }

            public void popupMenuCanceled(PopupMenuEvent e)
            {
                if (hiddenWindow != null)
                {
                    hiddenWindow.dispose();
                    hiddenWindow = null;
                }
            }
        });
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        showPopupMenu(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        showPopupMenu(e);
    }

    private void showPopupMenu(MouseEvent e)
    {
        if (e.isPopupTrigger() && popup != null)
        {
            if (hiddenWindow == null)
            {
                if (OSUtils.IS_WINDOWS)
                {
                    hiddenWindow = new JDialog((Frame) null);
                    ((JDialog) hiddenWindow).setUndecorated(true);
                }
                else
                    hiddenWindow = new JWindow((Frame) null);

                hiddenWindow.setAlwaysOnTop(true);
                Dimension size = popup.getPreferredSize();

                Point centerPoint = GraphicsEnvironment
                                        .getLocalGraphicsEnvironment()
                                            .getCenterPoint();

                if(e.getY() > centerPoint.getY())
                    hiddenWindow
                        .setLocation(e.getX(), e.getY() - size.height);
                else
                    hiddenWindow
                        .setLocation(e.getX(), e.getY());

                hiddenWindow.setVisible(true);

                popup.show(
                        ((RootPaneContainer)hiddenWindow).getContentPane(),
                        0, 0);

                // popup works only for focused windows
                hiddenWindow.toFront();
            }
        }
    }
}