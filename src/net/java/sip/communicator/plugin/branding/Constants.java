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
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.io.*;

import javax.swing.text.html.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 */
public class Constants
{
    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */

    /**
     * Dark blue color used in the about window and the splash screen.
     */
    public static final Color TITLE_COLOR
        = new Color(BrandingActivator.getResources()
                .getColor("service.gui.SPLASH_SCREEN_TITLE_COLOR"));

    /**
     * Text color used in the about window and the splash screen.
     */
    public static final String TEXT_COLOR
        = BrandingActivator.getResources()
            .getColorString("service.gui.SPLASH_SCREEN_TEXT_COLOR");


    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * Temporary method to load the css style used in the chat window.
     *
     * @param style
     */
    public static void loadSimpleStyle(StyleSheet style)
    {
        InputStream is = BrandingActivator.getResources().
            getSettingsInputStream("service.gui.HTML_TEXT_STYLE");

        Reader r = new BufferedReader(new InputStreamReader(is));
        try
        {
            style.loadRules(r, null);
            r.close();
        }
        catch (IOException e)
        {
        }
    }
}
