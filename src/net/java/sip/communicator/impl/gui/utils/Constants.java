/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.applet.*;
import java.io.*;
import java.util.*;
import java.util.Map;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 */

public class Constants {

    private static Logger logger = Logger.getLogger(Constants.class);

    public static final String ONLINE_STATUS = "Online";

    public static final String OFFLINE_STATUS = "Offline";

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

    /**
     * Indicates the number of messages to show in the chat area when a new
     * chat is opened.
     */
    public static int CHAT_HISTORY_SIZE = 10;

    /*
     * ===================================================================
     * ------------------------ MESSAGE TYPES ---------------------------
     * ===================================================================
     */

    /**
     * The outging message type.
     */
    public static final String OUTGOING_MESSAGE = "OutgoingMessage";

    /**
     * The incoming message type.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    /**
     * The system message type.
     */
    public static final String SYSTEM_MESSAGE = "SystemMessage";

    /**
     * The error message type.
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

    /*
     * ===================================================================
     * ------------------------ SIZE CONSTANTS ---------------------------
     * ===================================================================
     */

    /**
     * The minimum height of the main application window.
     */
    public static final int MAINFRAME_MIN_HEIGHT = 200;

    /**
     * The minimum width of the main application window.
     */
    public static final int MAINFRAME_MIN_WIDTH = 80;

    /**
     * The size of the gradient used for painting the selected background of
     * some components.
     */
    public static final int SELECTED_GRADIENT_SIZE = 5;

    /**
     * The size of the gradient used for painting the background of some
     * components.
     */
    public static final int GRADIENT_SIZE = 10;

    /**
     * The height of the <tt>HistoryWindow</tt>.
     */
    public static final int HISTORY_WINDOW_HEIGHT = 450;

    /**
     * The width of the <tt>HistoryWindow</tt>.
     */
    public static final int HISTORY_WINDOW_WIDTH = 450;

    /**
     * The width of a <tt>MessageDialog</tt>.
     */
    public static final int MSG_DIALOG_WIDTH = 330;

    /**
     * The height of a <tt>MessageDialog</tt>.
     */
    public static final int MSG_DIALOG_HEIGHT = 150;

    /**
     * The size of the buffer that indicates how many messages will be stored
     * in the conversation area in the chat window.
     */
    public static final int CHAT_BUFFER_SIZE = 50;

    /**
     * The maximum width of the <tt>ConfigurationFrame</tt>.
     */
    public static final int CONFIG_FRAME_MAX_WIDTH = 800;

    /**
     * The maximum height of the <tt>ConfigurationFrame</tt>.
     */
    public static final int CONFIG_FRAME_MAX_HEIGHT = 600;

    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */

    /**
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_DATE_COLOR
        = new Color(255, 201, 102);

    /**
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_IN_CALL_COLOR
        = new Color(249, 255, 197);

    /**
     * The color used to paint the background of an outgoing call history
     * record.
     */
    public static final Color HISTORY_OUT_CALL_COLOR
        = new Color(243, 244, 247);

    /**
     * The start color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_START_COLOR
        = new Color(151, 169, 198);

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_END_COLOR
        = new Color(209, 212, 225);

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color MOVER_START_COLOR = new Color(230,
            230, 230);

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color MOVER_END_COLOR = new Color(255,
            255, 255);

    /**
     * Gray color used to paint some borders, like the button border for
     * example.
     */
    public static final Color GRAY_COLOR = new Color(154, 154,
            154);

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BLUE_GRAY_BORDER_COLOR = new Color(142, 160, 188);

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static final Color BLUE_GRAY_BORDER_DARKER_COLOR = new Color(131, 149,
            178);

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
     * Indicates whether the application is in mode "group messages in one
     * window".
     */
    public static final boolean TABBED_CHAT_WINDOW = true;

    /**
     * The default path, where chat window styles could be found.
     */
    public static final String DEFAULT_STYLE_PATH
        = "net/java/sip/communicator/impl/gui/resources/styles";

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
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESSHOLD)
            {
                return ImageLoader
                    .getImage(ImageLoader.USER_ONLINE_ICON);
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE) {
                return ImageLoader
                    .getImage(ImageLoader.USER_FFC_ICON);
            }
            else {
                return ImageLoader
                    .getImage(ImageLoader.USER_OFFLINE_ICON);
            }
        }
        else {
            return ImageLoader
                .getImage(ImageLoader.USER_OFFLINE_ICON);
        }
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

        InputStream is = Constants.class
                .getClassLoader()
                .getResourceAsStream(DEFAULT_STYLE_PATH + "/defaultStyle.css");

        Reader r = new BufferedReader(new InputStreamReader(is));
        try {
            style.loadRules(r, null);
            r.close();
        } catch (IOException e) {
            logger.error("Failed to load css style.", e);
        }
    }
}
