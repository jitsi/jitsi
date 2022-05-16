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
package net.java.sip.communicator.impl.osdependent.macosx;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.gui.*;

import com.apple.eawt.*;

/**
 * MacOSX specific dock icon, which will add a dock icon listener in order to
 * show the application each time user clicks on the dock icon.
 *
 * @author Yana Stamcheva
 */
public class MacOSXDockIcon
{
    /**
     * Adds a dock icon listener in order to show the application each time user
     * clicks on the dock icon.
     */
    public static void addDockIconListener()
    {
        Application application = Application.getApplication();
        if (application != null)
        {

            application.addAppEventListener(new AppReOpenedListener()
            {
                public void appReOpened(AppEvent.AppReOpenedEvent appReOpenedEvent)
                {
                    UIService uiService = OsDependentActivator.getUIService();

                    if (uiService != null && !uiService.isVisible())
                        uiService.setVisible(true);
                }
            });
        }
    }
}
