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
            application.setQuitHandler(new QuitHandler()
            {
                public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent,
                                              final QuitResponse quitResponse)
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

                    /**
                     * Free the event dispatch thread before performing the
                     * quit (System.exit), shutdown timer may also has started
                     * the quit and is waiting to free the threads which
                     * we may be blocking.
                     */
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            quitResponse.performQuit();
                        }
                    }).start();
                }
            });

            return true;
        }
        return false;
    }
}
