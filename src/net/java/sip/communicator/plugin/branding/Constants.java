/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.splashscreen;

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
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_DATE_COLOR = new Color(255, 201, 102);

    /**
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_IN_CALL_COLOR = new Color(249, 255, 197);

    /**
     * The color used to paint the background of an outgoing call history
     * record.
     */
    public static final Color HISTORY_OUT_CALL_COLOR = new Color(243, 244, 247);

    /**
     * The start color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_START_COLOR = new Color(151, 169, 198);

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_END_COLOR = new Color(209, 212, 225);

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color MOVER_START_COLOR = new Color(230, 230, 230);

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color MOVER_END_COLOR = new Color(255, 255, 255);

    /**
     * Gray color used to paint some borders, like the button border for
     * example.
     */
    public static final Color GRAY_COLOR = new Color(154, 154, 154);

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BLUE_GRAY_BORDER_COLOR = new Color(142, 160, 188);

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static final Color BLUE_GRAY_BORDER_DARKER_COLOR = new Color(131,
            149, 178);

    /**
     * Light gray color used in the look and feel.
     */
    public static final Color LIGHT_GRAY_COLOR = new Color(200, 200, 200);

    /**
     * Dark blue color used in the About Window.
     */
    public static final Color DARK_BLUE = new Color(23, 65, 125);

    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * The name of the font used in this ui implementation.
     */
    public static final String FONT_NAME = "Verdana";

    /**
     * The size of the font used in this ui implementation.
     */
    public static final String FONT_SIZE = "12";

    /**
     * The default <tt>Font</tt> object used through this ui implementation.
     */
    public static final Font FONT = new Font(Constants.FONT_NAME, Font.PLAIN,
            new Integer(Constants.FONT_SIZE).intValue());

    /**
     * Temporary method to load the css style used in the chat window.
     * 
     * @param style
     */
    public static void loadSimpleStyle(StyleSheet style)
    {

        InputStream is = Constants.class
                .getResourceAsStream("resources/defaultStyle.css");

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
