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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * A custom popup menu that detects parent focus lost.
 *
 * @author Yana Stamcheva
 */
public class SIPCommPopupMenu
    extends JPopupMenu
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructor.
     */
    public SIPCommPopupMenu()
    {
        // Hides the popup menu when the parent window loses focus.
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent evt)
            {
                final Window parentWindow;

                Component parent = getParent();

                // If this is a submenu get the invoker first.
                if (parent instanceof JPopupMenu)
                    parentWindow = SwingUtilities.getWindowAncestor(
                        ((JPopupMenu) parent).getInvoker());
                else
                    parentWindow
                        = SwingUtilities.getWindowAncestor(getInvoker());

                if (parentWindow != null)
                {
                    if (!parentWindow.isActive())
                        setVisible(false);

                    parentWindow.addWindowListener(new WindowAdapter()
                    {
                        @Override
                        public void windowDeactivated(WindowEvent e)
                        {
                            if (SIPCommPopupMenu.this != null
                                    && SIPCommPopupMenu.this.isVisible())
                                SIPCommPopupMenu.this.setVisible(false);
                        }

                        /**
                         * Invoked when a window has been closed.
                         * Remove the listener as we do not need it any more.
                         */
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            parentWindow.removeWindowListener(this);
                        }
                    });
                }
            }
        });
    }
}
