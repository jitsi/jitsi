/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List; // disambiguation

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;

/**
 * UI shortcut.
 *
 * @author Sebastien Vincent
 */
public class UIShortcut
    implements GlobalShortcutListener
{
    /**
     * Keybindings service.
     */
    private KeybindingsService keybindingsService =
        GlobalShortcutActivator.getKeybindingsService();

    /**
     * Callback when an shortcut is typed
     *
     * @param evt <tt>GlobalShortcutEvent</tt>
     */
    public void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke keystroke = evt.getKeyStroke();
        GlobalKeybindingSet set = keybindingsService.getGlobalBindings();

        for(Map.Entry<String, List<AWTKeyStroke>> entry :
            set.getBindings().entrySet())
        {
            for(AWTKeyStroke ks : entry.getValue())
            {
                if(ks == null)
                    continue;

                if(entry.getKey().equals("contactlist") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    ExportedWindow window =
                        GlobalShortcutActivator.getUIService().
                            getExportedWindow(ExportedWindow.MAIN_WINDOW);

                    if(window == null)
                        return;

                    if(!window.isVisible())
                    {
                        window.bringToFront();
                        window.setVisible(true);
                        if(window instanceof Window)
                        {
                            ((Window)window).setAlwaysOnTop(true);
                            ((Window)window).setAlwaysOnTop(false);
                        }
                    }
                    else
                    {
                        window.setVisible(false);
                    }
                }
            }
        }
    }
}
