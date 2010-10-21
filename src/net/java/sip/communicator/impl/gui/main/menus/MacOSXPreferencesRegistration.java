/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import com.apple.eawt.*;

/**
 * @author Lubomir Marinov
 */
@SuppressWarnings("deprecation")
public final class MacOSXPreferencesRegistration
{
    public static boolean run(final Object userData)
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.addPreferencesMenuItem();
            if (application.isPreferencesMenuItemPresent())
            {
                application.setEnabledPreferencesMenu(true);
                application.addApplicationListener(new ApplicationAdapter()
                {
                    public void handlePreferences(ApplicationEvent event)
                    {
                        ((ToolsMenu) userData).configActionPerformed();
                        event.setHandled(true);
                    }
                });
                return true;
            }
        }
        return false;
    }
}
