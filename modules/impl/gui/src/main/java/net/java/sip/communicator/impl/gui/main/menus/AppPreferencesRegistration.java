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
public final class AppPreferencesRegistration
{
    public static boolean run(ToolsMenu toolsMenu)
    {
        if (Desktop.isDesktopSupported())
        {
            var desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Action.APP_PREFERENCES))
            {
                desktop.setPreferencesHandler(e ->
                {
                    toolsMenu.configActionPerformed();
                });
                return true;
            }
        }

        return false;
    }
}
