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

    private static Logger logger = Logger.getLogger(CommunicatorMain.class
            .getName());

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
     * Indicates whether the message automatic popup is enabled.
     */
    public static boolean AUTO_POPUP_NEW_MESSAGE = true;
    
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
    public static final int CHAT_BUFFER_SIZE = 100;

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
     * ------------------------ STATUS LABELS --------------------------------
     * ======================================================================
     */
    /**
     * The "online" presence status.
     */
    public static final IcqStatusEnum ONLINE_STATUS = IcqStatusEnum.ONLINE;

    /**
     * The "offline" presence status.
     */
    public static final IcqStatusEnum OFFLINE_STATUS = IcqStatusEnum.OFFLINE;

    /**
     * The "occupied" presence status.
     */
    public static final IcqStatusEnum OCCUPIED_STATUS = IcqStatusEnum.OCCUPIED;

    /**
     * The "chat" presence status.
     */
    public static final IcqStatusEnum CHAT_STATUS = IcqStatusEnum.FREE_FOR_CHAT;

    /**
     * The "away" presence status.
     */
    public static final IcqStatusEnum AWAY_STATUS = IcqStatusEnum.AWAY;

    /**
     * The "not available" presence status.
     */
    public static final IcqStatusEnum NA_STATUS = IcqStatusEnum.NOT_AVAILABLE;

    /**
     * The "invisible" presence status.
     */
    public static final IcqStatusEnum INVISIBLE_STATUS = IcqStatusEnum.INVISIBLE;

    /**
     * The "do not disturb" presence status.
     */
    public static final IcqStatusEnum DND_STATUS = IcqStatusEnum.DO_NOT_DISTURB;

    /**
     * A set of all statuses and corresponging icons.
     */
    private static final Hashtable mainStatusSet = new Hashtable();
    static {
        mainStatusSet.put(Constants.ONLINE_STATUS, ImageLoader
                .getImage(ImageLoader.USER_ONLINE_ICON));
        mainStatusSet.put(Constants.OCCUPIED_STATUS, ImageLoader
                .getImage(ImageLoader.USER_OCCUPIED_ICON));
        mainStatusSet.put(Constants.NA_STATUS, ImageLoader
                .getImage(ImageLoader.USER_NA_ICON));
        mainStatusSet.put(Constants.DND_STATUS, ImageLoader
                .getImage(ImageLoader.USER_DND_ICON));
        mainStatusSet.put(Constants.CHAT_STATUS, ImageLoader
                .getImage(ImageLoader.USER_FFC_ICON));
        mainStatusSet.put(Constants.AWAY_STATUS, ImageLoader
                .getImage(ImageLoader.USER_AWAY_ICON));
        mainStatusSet.put(Constants.OFFLINE_STATUS, ImageLoader
                .getImage(ImageLoader.USER_OFFLINE_ICON));
        mainStatusSet.put(Constants.INVISIBLE_STATUS, ImageLoader
                .getImage(ImageLoader.USER_ONLINE_ICON));
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
     * Gets the protocol logo icon in size 16x16.
     * @param protocolName The protocol name.
     * @return the protocol logo image.
     */
    public static Image getProtocolIcon(String protocolName) {

        Image protocolIcon = null;

        if (protocolName.equals(Constants.SIP)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.SIP_ONLINE_ICON);
        } else if (protocolName.equals(Constants.ICQ)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.ICQ_LOGO);
        } else if (protocolName.equals(Constants.MSN)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.MSN_LOGO);
        } else if (protocolName.equals(Constants.AIM)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.AIM_LOGO);

        } else if (protocolName.equals(Constants.YAHOO)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.YAHOO_LOGO);

        } else if (protocolName.equals(Constants.JABBER)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.JABBER_LOGO);

        } else if (protocolName.equals(Constants.SKYPE)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.SKYPE_LOGO);
        }

        return protocolIcon;
    }
    
    /**
     * Gets the protocol logo icon in size 32x32.
     * @param protocolName The protocol name.
     * @return the protocol logo image.
     */
    public static Image getProtocolBigIcon(String protocolName) {

        Image protocolIcon = null;

        if (protocolName.equals(Constants.SIP)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.SIP_ONLINE_ICON);
        } else if (protocolName.equals(Constants.ICQ)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.ICQ_32x32);
        } else if (protocolName.equals(Constants.MSN)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.MSN_32x32);
        } else if (protocolName.equals(Constants.AIM)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.AIM_32x32);

        } else if (protocolName.equals(Constants.YAHOO)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.YAHOO_32x32);

        } else if (protocolName.equals(Constants.JABBER)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.JABBER_32x32);

        } else if (protocolName.equals(Constants.SKYPE)) {

            protocolIcon = ImageLoader.getImage(ImageLoader.SKYPE_32x32);
        }

        return protocolIcon;
    }

    /**
     * Gets the protocol connecting animated icon.
     * @param protocolName The name of the protocol.
     * @return the animated icon in the form of array of all images that it
     * contains.
     */
    public static BufferedImage[] getProtocolAnimatedIcon(String protocolName) {
        
        if (protocolName.equals(Constants.SIP)) {
            return null;
        }
        else if (protocolName.equals(Constants.ICQ)){
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else if (protocolName.equals(Constants.MSN)) {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else if (protocolName.equals(Constants.AIM)) {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else if (protocolName.equals(Constants.YAHOO)) {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else if (protocolName.equals(Constants.JABBER)) {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else if (protocolName.equals(Constants.SKYPE)) {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
        else {
            return ImageLoader.getAnimatedImage(
                    ImageLoader.ICQ_CONNECTING);
        }
    }
    
    /**
     * Returns a set of <tt>Status</tt> objects for a given protocol. Each
     * <tt>Status</tt> contains a status icon and a status text. 
     * 
     * @param protocolName The name of the protocol.
     * @return a set of <tt>Status</tt> objects for a given protocol. Each
     * <tt>Status</tt> contains a status icon and a status text.
     */
    public static Map getProtocolStatusIcons(String protocolName) {
        Map protocolStatusList = new Hashtable();

        if (protocolName.equals(Constants.SIP)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_ONLINE_ICON));

            protocolStatusList.put(CHAT_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_CHAT_ICON));

            protocolStatusList.put(AWAY_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_AWAY_ICON));

            protocolStatusList.put(OCCUPIED_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_OCCUPIED_ICON));

            protocolStatusList.put(NA_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_NA_ICON));

            protocolStatusList.put(DND_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_DND_ICON));

            protocolStatusList.put(OFFLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_OFFLINE_ICON));

            protocolStatusList.put(INVISIBLE_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_INVISIBLE_ICON));

        } else if (protocolName.equals(Constants.ICQ)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_LOGO));

            protocolStatusList.put(CHAT_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_FF_CHAT_ICON));

            protocolStatusList.put(AWAY_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_AWAY_ICON));

            protocolStatusList.put(NA_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_NA_ICON));

            protocolStatusList.put(DND_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_DND_ICON));

            protocolStatusList.put(OCCUPIED_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_OCCUPIED_ICON));

            protocolStatusList.put(OFFLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_OFFLINE_ICON));

            protocolStatusList.put(INVISIBLE_STATUS, ImageLoader
                    .getImage(ImageLoader.ICQ_INVISIBLE_ICON));

        } else if (protocolName.equals(Constants.MSN)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.MSN_LOGO));

        } else if (protocolName.equals(Constants.AIM)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.AIM_LOGO));

        } else if (protocolName.equals(Constants.YAHOO)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.YAHOO_LOGO));

        } else if (protocolName.equals(Constants.JABBER)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.JABBER_LOGO));
 
            protocolStatusList.put(CHAT_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_CHAT_ICON));

            protocolStatusList.put(AWAY_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_AWAY_ICON));

            protocolStatusList.put(OCCUPIED_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_OCCUPIED_ICON));

            protocolStatusList.put(NA_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_NA_ICON));

            protocolStatusList.put(DND_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_DND_ICON));

            protocolStatusList.put(OFFLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_OFFLINE_ICON));

            protocolStatusList.put(INVISIBLE_STATUS, ImageLoader
                    .getImage(ImageLoader.SIP_INVISIBLE_ICON));


        } else if (protocolName.equals(Constants.SKYPE)) {

            protocolStatusList.put(ONLINE_STATUS, ImageLoader
                    .getImage(ImageLoader.SKYPE_LOGO));
        }

        return protocolStatusList;
    }

    /**
     * Returns the image corresponding to the given presence status.
     * @param status The presence status. 
     * @return the image corresponding to the given presence status.
     */
    public static BufferedImage getStatusIcon(PresenceStatus status) {
        return (BufferedImage) mainStatusSet.get(status);
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

    /**
     * Returns the default sound used when user receives a message.
     * @return the default sound used when user receives a message.
     */
    public static AudioClip getDefaultAudio() {
        AudioClip audio = Applet
                .newAudioClip(Constants.class
                        .getClassLoader()
                        .getResource(
        "net/java/sip/communicator/impl/gui/resources/sounds/ship-sink.wav"));

        return audio;
    }
}
