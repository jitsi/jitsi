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
import java.util.*;

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
    private static Logger logger = Logger.getLogger(Constants.class);

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
     * ===================================================================
     * ------------------------ MESSAGE TYPES ---------------------------
     * ===================================================================
     */

    /**
     * The message type representing outgoing messages.
     */
    public static final String OUTGOING_MESSAGE = "OutgoingMessage";

    /**
     * The message type representing incoming messages.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    /**
     * The message type representing status messages.
     */
    public static final String STATUS_MESSAGE = "StatusMessage";

    /**
     * The message type representing action messages. These are message specific
     * for IRC, but could be used in other protocols also.
     */
    public static final String ACTION_MESSAGE = "ActionMessage";

    /**
     * The message type representing system messages.
     */
    public static final String SYSTEM_MESSAGE = "SystemMessage";

    /**
     * The message type representing sms messages.
     */
    public static final String SMS_MESSAGE = "SmsMessage";

    /**
     * The message type representing error messages.
     */
    public static final String ERROR_MESSAGE = "ErrorMessage";

    /**
     * The history incoming message type.
     */
    public static final String HISTORY_INCOMING_MESSAGE = "HistoryIncomingMessage";

    /**
     * The history outgoing message type.
     */
    public static final String HISTORY_OUTGOING_MESSAGE = "HistoryOutgoingMessage";

    /**
     * The size of the buffer that indicates how many messages will be stored
     * in the conversation area in the chat window.
     */
    public static final int CHAT_BUFFER_SIZE = 3000;

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
    private static final ArrayList specialCharsList = new ArrayList();
    static{
        specialCharsList.add(new Integer(KeyEvent.VK_PLUS));
        specialCharsList.add(new Integer(KeyEvent.VK_MINUS));
        specialCharsList.add(new Integer(KeyEvent.VK_SPACE));
        specialCharsList.add(new Integer(KeyEvent.VK_ENTER));
        specialCharsList.add(new Integer(KeyEvent.VK_LEFT));
        specialCharsList.add(new Integer(KeyEvent.VK_RIGHT));
    };

    /**
     * Checks if the given char is in the list of application special chars.
     *
     * @param charCode The char code.
     */
    public static boolean isSpecialChar(int charCode) {
        if(specialCharsList.contains(new Integer(charCode)))
            return true;
        else
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

        File is = new File(Constants.class.getClassLoader()
            .getResource(DEFAULT_STYLE_PATH + "/TotallyClear").toString());
    }

    /**
     * Temporary method to load the css style used in the chat window.
     * @param style
     */
    public static void loadSimpleStyle(StyleSheet style) {

        InputStream is = GuiActivator.getResources()
            .getSettingsInputStream("service.gui.HTML_TEXT_STYLE");

        Reader r = new BufferedReader(new InputStreamReader(is));
        try {
            style.loadRules(r, null);
            r.close();
        } catch (IOException e) {
            logger.error("Failed to load css style.", e);
        }
    }
}
