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
                     *
                     * (2011-06-10) Changed to true, we tell that quit is handled
                     * as otherwise will stop OS from logout or shutdown and
                     * a notification will be shown to user to inform about it.
                     *
                     * (2011-07-12) Wait before answering to the OS or we will
                     * end too quickly. 15sec is the time our shutdown timer
                     * waits before force the shutdown.
                     */

                    synchronized(this)
                    {
                        try
                        {
                            wait(15000);
                        }catch (InterruptedException ex){}
                    }

                    event.setHandled(true);
                }
            });
            return true;
        }
        return false;
    }
}
