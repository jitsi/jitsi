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

                if (!parentWindow.isActive())
                {
                    setVisible(false);
                }

                parentWindow.addWindowListener(new WindowAdapter()
                {
                    public void windowDeactivated(WindowEvent e)
                    {
                        if (SIPCommPopupMenu.this != null
                                && SIPCommPopupMenu.this.isVisible())
                            SIPCommPopupMenu.this.setVisible(false);
                    }
                });
            }
        });
    }
}
