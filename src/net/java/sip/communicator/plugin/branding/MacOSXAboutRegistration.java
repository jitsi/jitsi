/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import com.apple.eawt.*;

/**
 * @author Lubomir Marinov
 */
@SuppressWarnings("deprecation")
public final class MacOSXAboutRegistration
{
    /**
     * Show the about dialog on Mac OS X.
     *
     * @return true if the Mac OS X application is not null
     */
    public static boolean run()
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.addAboutMenuItem();
            if (application.isAboutMenuItemPresent())
            {
                application.setEnabledAboutMenu(true);
                application.addApplicationListener(new ApplicationAdapter()
                {
                    public void handleAbout(ApplicationEvent event)
                    {
                        AboutWindowPluginComponent.actionPerformed();
                        event.setHandled(true);
                    }
                });
                return true;
            }
        }
        return false;
    }
}
