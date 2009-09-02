/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 */
public class Constants
{
    private static final Logger logger = Logger.getLogger(Constants.class);

    /**
     * Indicates that the user is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * Indicates that the user is disconnected.
     */
    public static final String OFFLINE_STATUS = "Offline";

    /**
     * Indicates that the user is away.
     */
    public static final String AWAY_STATUS = "Away";

    /**
     * Indicates that the user is connected and eager to communicate.
     */
    public static final String FREE_FOR_CHAT_STATUS = "FreeForChat";

    /**
     * Indicates that the user is connected and eager to communicate.
     */
    public static final String DO_NOT_DISTURB_STATUS = "DoNotDisturb";

    /*
     * ===================================================================
     * ------------------------ CONFIG CONSTANTS ---------------------------
     * ===================================================================
     */
    /**
     * Indicates whether to ask for confirmation when user tries to delete
     * a contact.
     */
    public static boolean REMOVE_CONTACT_ASK = true;

    /**
     * Indicates whether to ask for confirmation when user tries to delete
     * a contact.
     */
    public static boolean REMOVE_GROUP_ASK = true;

    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */
    /**
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_IN_CALL_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.HISTORY_INCOMING_CALL_BACKGROUND"));

    /**
     * The color used to paint the background of an outgoing call history
     * record.
     */
    public static final Color HISTORY_OUT_CALL_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.HISTORY_OUTGOING_CALL_BACKGROUND"));

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIST_SELECTION_COLOR"));

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color GRADIENT_DARK_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.GRADIENT_DARK_COLOR"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color GRADIENT_LIGHT_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.GRADIENT_LIGHT_COLOR"));

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.BORDER_COLOR"));

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static final Color LIST_SELECTION_BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIST_SELECTION_BORDER_COLOR"));

    /**
     * The color used to paint the background of contact list groups.
     */
    public static final Color CONTACT_LIST_GROUP_BG_COLOR
        = new Color(GuiActivator.getResources()
                .getColor("service.gui.CONTACT_LIST_GROUP_ROW"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color CONTACT_LIST_GROUP_BG_GRADIENT_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.CONTACT_LIST_GROUP_GRADIENT"));

    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * The default <tt>Font</tt> object used through this ui implementation.
     */
    public static final Font FONT;

    static
    {
        Font font = null;
        String fontName = null;
        int fontSize = 0;

        LookAndFeel laf = UIManager.getLookAndFeel();
        if ((laf != null)
                && "com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(
                        laf.getClass().getName()))
        {
            Object desktopPropertyValue
                = Toolkit.getDefaultToolkit().getDesktopProperty(
                        "win.messagebox.font");

            if (desktopPropertyValue instanceof Font)
            {
                font = (Font) desktopPropertyValue;
                fontName = font.getFontName();
                fontSize = font.getSize();
            }
        }

        FONT
            = (font == null)
                ? new Font(
                        (fontName == null) ? "Verdana" : fontName,
                        Font.PLAIN,
                        (fontSize == 0) ? 12 : fontSize)
                : font;
    }

    /*
     * ======================================================================
     * ------------------------ PROTOCOL NAMES -------------------------------
     * ======================================================================
     */
    /**
     * The ICQ protocol.
     */
    public static final String ICQ = "ICQ";

    /**
     * The MSN protocol.
     */
    public static final String MSN = "MSN";

    /**
     * The AIM protocol.
     */
    public static final String AIM = "AIM";

    /**
     * The Yahoo protocol.
     */
    public static final String YAHOO = "Yahoo";

    /**
     * The Jabber protocol.
     */
    public static final String JABBER = "Jabber";

    /**
     * The Skype protocol.
     */
    public static final String SKYPE = "Skype";

    /**
     * The Gibberish protocol.
     */
    public static final String GIBBERISH = "Gibberish";


    /**
     * The SIP protocol.
     */
    public static final String SIP = "SIP";

    /*
     * ======================================================================
     * ------------------------ OTHER CONSTANTS ------------------------------
     * ======================================================================
     */
    /**
     * The default path, where chat window styles could be found.
     */
    public static final String DEFAULT_STYLE_PATH
        = "resources/styles";

    /*
     * ======================================================================
     * ------------------------ SPECIAL CHARS LIST --------------------------
     * ======================================================================
     */
    /**
     * A list of all special chars that should be escaped for some reasons.
     */
    private static final int[] specialChars = new int[]
    {
        KeyEvent.VK_PLUS,
        KeyEvent.VK_MINUS,
        KeyEvent.VK_SPACE,
        KeyEvent.VK_ENTER,
        KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT
    };

    /**
     * Checks if the given char is in the list of application special chars.
     *
     * @param charCode The char code.
     */
    public static boolean isSpecialChar(int charCode) {
        for (int specialChar : specialChars) {
            if (specialChar == charCode)
                return true;
        }
        return false;
    }

    /**
     * Returns the image corresponding to the given presence status.
     * @param status The presence status.
     * @return the image corresponding to the given presence status.
     */
    public static BufferedImage getStatusIcon(PresenceStatus status)
    {
        if(status != null)
        {
            int connectivity = status.getStatus();

            if(connectivity < PresenceStatus.ONLINE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_OFFLINE_ICON);
            }
            else if(connectivity < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_AWAY_ICON);
            }
            else if(connectivity
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_ONLINE_ICON);
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_FFC_ICON);
            }
            else
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_OFFLINE_ICON);
            }
        }
        else
        {
            return ImageLoader
                .getImage(ImageLoader.USER_OFFLINE_ICON);
        }
    }

    /**
     * Returns the image corresponding to the given status.
     * @param status ONLINE_STATUS or OFFLINE_STATUS
     * @return the image corresponding to the given status.
     */
    public static BufferedImage getStatusIcon(String status)
    {
        if (status.equals(ONLINE_STATUS))
            return ImageLoader
                .getImage(ImageLoader.USER_ONLINE_ICON);
        else if (status.equals(OFFLINE_STATUS))
            return ImageLoader
                .getImage(ImageLoader.USER_OFFLINE_ICON);
        else if (status.equals(AWAY_STATUS))
            return ImageLoader
                .getImage(ImageLoader.USER_AWAY_ICON);
        else if (status.equals(FREE_FOR_CHAT_STATUS))
            return ImageLoader
                .getImage(ImageLoader.USER_FFC_ICON);
        else
            return ImageLoader
                .getImage(ImageLoader.USER_OFFLINE_ICON);
    }

    /**
     * Loads a chat window style.
     */
    public static void loadAdiumStyle(){

        new File(Constants.class.getClassLoader()
            .getResource(DEFAULT_STYLE_PATH + "/TotallyClear").toString());
    }

    /**
     * Temporary method to load the css style used in the chat window.
     * @param styleSheet style sheet
     * @param defaultFont default font
     */
    public static void loadSimpleStyle(StyleSheet styleSheet, Font defaultFont)
    {
        Reader r =
            new BufferedReader(
                new InputStreamReader(
                    GuiActivator.getResources().getSettingsInputStream(
                        "service.gui.HTML_TEXT_STYLE")));

        if (defaultFont != null)
            styleSheet.addRule(
                "body, div, h1, h2, h3, h4, h5, h6, h7, p, td, th { "
                    + "font-family: "
                    + defaultFont.getName()
                    + "; font-size: "
                    + defaultFont.getSize()
                    + "pt; }");

        try
        {
            styleSheet.loadRules(r, null);
        }
        catch (IOException ex)
        {
            logger.error("Failed to load CSS stream.", ex);
        }
        finally
        {
            try
            {
                r.close();
            }
            catch (IOException ex)
            {
                logger.error("Failed to close CSS stream.", ex);
            }
        }
    }
}
