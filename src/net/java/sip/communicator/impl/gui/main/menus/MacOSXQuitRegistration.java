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
public final class MacOSXQuitRegistration
{
    public static boolean run(final Object userData)
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.addApplicationListener(new ApplicationAdapter()
            {
                public void handleQuit(ApplicationEvent event)
                {
                    ((FileMenu) userData).closeActionPerformed();
                    event.setHandled(true);
                }
            });
            return true;
        }
        return false;
    }
}
