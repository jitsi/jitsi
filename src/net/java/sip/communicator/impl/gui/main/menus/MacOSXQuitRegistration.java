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

                    /*
                     * Tell Mac OS X that it shouldn't terminate the
                     * application. We've already initiated the quit and we'll
                     * eventually complete it i.e. we'll honor the request of
                     * Mac OS X to quit.
                     */
                    event.setHandled(false);
                }
            });
            return true;
        }
        return false;
    }
}
