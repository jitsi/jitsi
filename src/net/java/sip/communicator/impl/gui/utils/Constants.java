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
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class Constants
{
    private static final Logger logger = Logger.getLogger(Constants.class);

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
     * Background color for even records in call history.
     */
    public static Color CALL_HISTORY_EVEN_ROW_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.CALL_HISTORY_EVEN_ROW_COLOR"));

    /**
     * Background color for chat room contact rows.
     */
    public static Color CHAT_ROOM_ROW_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.CHAT_ROOM_ROW_COLOR"));

    /**
     * The start color used to paint a gradient selected background of some
     * components.
     */
    public static Color SELECTED_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIST_SELECTION_COLOR"));

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static Color SELECTED_GRADIENT_COLOR
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.LIST_SELECTION_COLOR_GRADIENT"));

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color GRADIENT_DARK_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.GRADIENT_DARK_COLOR"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color GRADIENT_LIGHT_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.GRADIENT_LIGHT_COLOR"));

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static Color BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.BORDER_COLOR"));

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static Color LIST_SELECTION_BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIST_SELECTION_BORDER_COLOR"));

    /**
     * The color used to paint the background of contact list groups.
     */
    public static Color CONTACT_LIST_GROUP_BG_COLOR
        = new Color(GuiActivator.getResources()
                .getColor("service.gui.CONTACT_LIST_GROUP_ROW"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color CONTACT_LIST_GROUP_BG_GRADIENT_COLOR
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
        LookAndFeel laf = UIManager.getLookAndFeel();
        Font font = null;
        String fontName = null;
        int fontSize = 0;

        if (laf != null)
        {
            String lafClassName = laf.getClass().getName();

            if ("com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(
                    lafClassName))
            {
                Object desktopPropertyValue
                    = Toolkit.getDefaultToolkit().getDesktopProperty(
                            "win.messagebox.font");

                if (desktopPropertyValue instanceof Font)
                    font = (Font) desktopPropertyValue;
            }
            else if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(
                    lafClassName))
            {
                font = UIManager.getDefaults().getFont("Panel.font");
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
            else if(connectivity < PresenceStatus.EXTENDED_AWAY_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_DND_ICON);
            }
            else if(connectivity  < PresenceStatus.AWAY_THRESHOLD)
            {
                String statusName = "";
                if(status.getStatusName() != null)
                    statusName = Pattern.compile("\\p{Space}").matcher(
                            status.getStatusName()).replaceAll("");
                if(statusName.equalsIgnoreCase("OnThePhone"))
                    return ImageLoader
                        .getImage(ImageLoader.USER_USER_ON_THE_PHONE_ICON);
                else if(statusName.equalsIgnoreCase("InAMeeting"))
                    return ImageLoader
                        .getImage(ImageLoader.USER_USER_IN_A_MEETING_ICON);
                else
                    return ImageLoader
                        .getImage(ImageLoader.USER_EXTENDED_AWAY_ICON);
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
            else if(connectivity <
                ChatRoomPresenceStatus.CHAT_ROOM_ONLINE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_FFC_ICON);
            }
            else if(connectivity <
                ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_ROOM_ONLINE_ICON);
            }
            else if(connectivity == MessageSourceContactPresenceStatus
                                        .MSG_SRC_CONTACT_ONLINE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.MSG_SRC_CONTACT_ONLINE_ICON);
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_ROOM_OFFLINE_ICON);
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
     * Returns the image corresponding to the given presence status.
     * @param status The presence status.
     * @return the image corresponding to the given presence status.
     */
    public static BufferedImage getMessageStatusIcon(PresenceStatus status)
    {
        if(status != null)
        {
            int connectivity = status.getStatus();

            if(connectivity < PresenceStatus.ONLINE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_OFFLINE_ICON);
            }
            else if(connectivity < PresenceStatus.EXTENDED_AWAY_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_DND_ICON);
            }
            else if(connectivity == PresenceStatus.EXTENDED_AWAY_THRESHOLD)
            {
                // the special status On The Phone is state
                // between DND and EXTENDED AWAY states.
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_ON_THE_PHONE_ICON);
            }
            else if(connectivity < PresenceStatus.AWAY_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_EXTENDED_AWAY_ICON);
            }
            else if(connectivity < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_AWAY_ICON);
            }
            else if(connectivity
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_ONLINE_ICON);
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_FFC_ICON);
            }
            else
            {
                return ImageLoader
                    .getImage(ImageLoader.CHAT_BUTTON_OFFLINE_ICON);
            }
        }
        else
        {
            return ImageLoader
                .getImage(ImageLoader.CHAT_BUTTON_SMALL_WHITE);
        }
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

    /**
     * Reloads constants.
     */
    public static void reload()
    {
        CALL_HISTORY_EVEN_ROW_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.CALL_HISTORY_EVEN_ROW_COLOR"));

        SELECTED_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIST_SELECTION_COLOR"));

        GRADIENT_DARK_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.GRADIENT_DARK_COLOR"));

        GRADIENT_LIGHT_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.GRADIENT_LIGHT_COLOR"));

        BORDER_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.BORDER_COLOR"));

        LIST_SELECTION_BORDER_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIST_SELECTION_BORDER_COLOR"));

        CONTACT_LIST_GROUP_BG_COLOR
            = new Color(GuiActivator.getResources()
                    .getColor("service.gui.CONTACT_LIST_GROUP_ROW"));

        CONTACT_LIST_GROUP_BG_GRADIENT_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.CONTACT_LIST_GROUP_GRADIENT"));
    }
}
