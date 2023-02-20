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
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.Desktop.*;

/**
 * @author Lubomir Marinov
 */
public final class AppQuitRegistration
{
    public static boolean run(FileMenu fileMenu)
    {
        if (Desktop.isDesktopSupported())
        {
            var desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Action.APP_QUIT_HANDLER))
            {
                desktop.setQuitHandler((e, response) ->
                {
                    fileMenu.closeActionPerformed();

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
                    synchronized(AppQuitRegistration.class)
                    {
                        try
                        {
                            AppQuitRegistration.class.wait(15000);
                        }
                        catch (InterruptedException ex)
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                    /*
                     * Free the event dispatch thread before performing the
                     * quit (System.exit), shutdown timer may also have started
                     * the quit and is waiting to free the threads which
                     * we may be blocking.
                     */
                    var t = new Thread(response::performQuit);
                    t.setName("Quit handler continuation thread");
                    t.setDaemon(true);
                    t.start();
                });

                return true;
            }
        }

        return false;
    }
}
