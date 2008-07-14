/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.io.*;

import javax.swing.text.html.StyleSheet;

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
        = new Color(
            BrandingActivator.getResources().getColor("splashScreenTitleColor"));

    /**
     * Text color used in the about window and the splash screen.
     */
    public static final String TEXT_COLOR
        = BrandingActivator.getResources().getColorString("splashScreenTextColor");


    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * The default <tt>Font</tt> object used through this ui implementation.
     */
    public static final Font FONT
        = new Font( BrandingActivator.getResources().getSettingsString("fontName"),
                Font.PLAIN,
                BrandingActivator.getResources().getSettingsInt("fontSize"));

    /**
     * Temporary method to load the css style used in the chat window.
     * 
     * @param style
     */
    public static void loadSimpleStyle(StyleSheet style)
    {
        InputStream is = BrandingActivator.getResources().
            getSettingsInputStream("textStyle");

        Reader r = new BufferedReader(new InputStreamReader(is));
        try
        {
            style.loadRules(r, null);
            r.close();
        } catch (IOException e)
        {
        }
    }
}
