/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import com.apple.eawt.*;

/**
 * @author Lubomir Marinov
 */
public final class MacOSXPreferencesRegistration
{
    public static boolean run(final Object userData)
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.setPreferencesHandler(new PreferencesHandler()
            {
                public void handlePreferences(
                    AppEvent.PreferencesEvent preferencesEvent)
                {
                    ((ToolsMenu) userData).configActionPerformed();
                }
            });
            return true;
        }
        return false;
    }
}
