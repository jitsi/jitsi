/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

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
    public SIPCommPopupMenu()
    {
        // Hides the popup menu when the parent window loses focus.
        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt)
            {
                Window parentWindow;

                Component parent = getParent();

                // If this is a submenu get the invoker first.
                if (parent instanceof JPopupMenu)
                    parentWindow = SwingUtilities.getWindowAncestor(
                        ((JPopupMenu) parent).getInvoker());
                else
                    parentWindow
                        = SwingUtilities.getWindowAncestor(getInvoker());

                if (!parentWindow.isActive())
                {
                    setVisible(false);
                }

                parentWindow.addWindowFocusListener(new WindowFocusListener()
                {
                    public void windowLostFocus(WindowEvent e)
                    {
                        if (SIPCommPopupMenu.this != null
                            && SIPCommPopupMenu.this.isVisible())
                            SIPCommPopupMenu.this.setVisible(false);
                    }

                    public void windowGainedFocus(WindowEvent e) {}
                });
            }
        });
    }
}
