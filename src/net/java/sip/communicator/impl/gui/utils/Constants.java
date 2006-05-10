/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.text.html.StyleSheet;

import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;

/**
 * All look and feel related constants are stored here.
 * 
 * @author Yana Stamcheva
 */

public class Constants {

	/*
	 * ===================================================================
	 * ------------------------ SIZE CONSTANTS ---------------------------
	 * ===================================================================
	 */
	
	public static final int MAINFRAME_MIN_HEIGHT = 200;

	public static final int MAINFRAME_MIN_WIDTH = 80;

	public static final int CONTACTPANEL_HEIGHT = 20;

	public static final int CONTACTPANEL_WIDTH = 10;

	public static final int CONTACTPANEL_SELECTED_HEIGHT = 50;

	public static final int CONTACTPANEL_SELECTED_GRADIENT_SIZE = 5;

	public static final int CONTACTPANEL_GRADIENT_SIZE = 10;

	public static final int HISTORY_WINDOW_HEIGHT = 450;

	public static final int HISTORY_WINDOW_WIDTH = 450;

	public static final int OPTION_PANE_WIDTH = 330;

	public static final int OPTION_PANE_HEIGHT = 150;

	public static final int CHAT_BUFFER_SIZE = 100;

	public static final int CONFIG_FRAME_MAX_WIDTH = 800;

	public static final int CONFIG_FRAME_MAX_HEIGHT = 600;

	/*
	 * ======================================================================
	 * -------------------- FONTS AND COLOR CONSTANTS ------------------------
	 * ======================================================================
	 */

	public static final Color CONTACTPANEL_SELECTED_START_COLOR 
											= new Color(151, 169, 198);

	public static final Color CONTACTPANEL_SELECTED_END_COLOR 
											= new Color(209, 212, 225);

	public static final Color CONTACTPANEL_MOVER_START_COLOR 
											= new Color(230, 230, 230);

	public static final Color CONTACTPANEL_MOVER_END_COLOR 
											= new Color(255, 255, 255);

	public static final Color CONTACTPANEL_LINES_COLOR 
											= new Color(154, 154, 154);

	public static final Color MSG_WINDOW_BORDER_COLOR 
											= new Color(142, 160, 188);

	public static final Color CONTACTPANEL_BORDER_COLOR 
											= new Color(131, 149, 178);

	public static final Color TOOLBAR_SEPARATOR_COLOR 
											= new Color(200, 200, 200);
	
	public static final Color TRANSPARENT_WHITE_COLOR 
											= new Color(255, 255, 255, 60);
	/*
	 * ======================================================================
	 * ---------------------- MESSAGE WINDOW FONT CONSTANTS ------------------
	 * ======================================================================
	 */

	public static final String FONT_NAME = "Verdana";

	public static final String FONT_SIZE = "10";

	public static final String FONT_IN_MSG_COLOR = "#ef7b1e";

	public static final String FONT_OUT_MSG_COLOR = "#2e538b";

	public static final String FONT_CHAT_HEADER_COLOR = "c6d0e1";

	public static final Font FONT 
        = new Font(	Constants.FONT_NAME, 
					Font.PLAIN,
					new Integer(Constants.FONT_SIZE).intValue());
	
	/*
	 * ======================================================================
	 * ------------------------ STATUS LABELS --------------------------------
	 * ======================================================================
	 */

    public static final IcqStatusEnum ONLINE_STATUS 
                                        = IcqStatusEnum.ONLINE;

	public static final IcqStatusEnum OFFLINE_STATUS 
                                        = IcqStatusEnum.OFFLINE;

	public static final IcqStatusEnum OCCUPIED_STATUS 
                                        = IcqStatusEnum.OCCUPIED;

	public static final IcqStatusEnum CHAT_STATUS 
                                        = IcqStatusEnum.FREE_FOR_CHAT;

	public static final IcqStatusEnum AWAY_STATUS 
                                        = IcqStatusEnum.AWAY;

	public static final IcqStatusEnum NA_STATUS 
                                        = IcqStatusEnum.NOT_AVAILABLE;

	public static final IcqStatusEnum INVISIBLE_STATUS 
                                        = IcqStatusEnum.INVISIBLE;

	public static final IcqStatusEnum DND_STATUS 
                                        = IcqStatusEnum.DO_NOT_DISTURB;

	private static final Hashtable mainStatusSet =new Hashtable();
    static{
    	mainStatusSet.put(Constants.ONLINE_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON));
    	mainStatusSet.put(Constants.OCCUPIED_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_OCCUPIED_ICON));
    	mainStatusSet.put(Constants.NA_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_NA_ICON));
    	mainStatusSet.put(Constants.DND_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_DND_ICON));
    	mainStatusSet.put(Constants.CHAT_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_FFC_ICON));
    	mainStatusSet.put(Constants.AWAY_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_AWAY_ICON));
    	mainStatusSet.put(Constants.OFFLINE_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON));
    	mainStatusSet.put(Constants.INVISIBLE_STATUS, 
    			ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON));
    }

	/*
	 * ======================================================================
	 * ------------------------ PROTOCOL NAMES -------------------------------
	 * ======================================================================
	 */

	public static final String ICQ = "ICQ";

	public static final String MSN = "MSN";

	public static final String AIM = "AIM";

	public static final String YAHOO = "Yahoo";

	public static final String JABBER = "Jabber";

	public static final String SKYPE = "Skype";

	public static final String SIP = "SIP";

	/*
	 * ======================================================================
	 * ------------------------ OTHER CONSTANTS ------------------------------
	 * ======================================================================
	 */

	public static final int RIGHT_SHIFT_STATUS_ICON = 2;
	
	public static final boolean TABBED_CHAT_WINDOW = true;
	

	/**
	 * Gets protocol logo icon.
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
	 * Gets all protocol statuses, including status and text.
	 * 
	 * @param protocolName
	 * @return an ArrayList of all status Icons for the given protocol.
	 */

	public static Map getProtocolStatusIcons(String protocolName) {
		Map protocolStatusList = new Hashtable();

		if (protocolName.equals(Constants.SIP)) {

			protocolStatusList.put(ONLINE_STATUS,
                    ImageLoader.getImage(ImageLoader.SIP_ONLINE_ICON));

			protocolStatusList.put(CHAT_STATUS,
                    ImageLoader.getImage(ImageLoader.SIP_CHAT_ICON));
                        
			protocolStatusList.put(AWAY_STATUS, 
                    ImageLoader.getImage(ImageLoader.SIP_AWAY_ICON));

			protocolStatusList.put(OCCUPIED_STATUS,
					ImageLoader.getImage(ImageLoader.SIP_OCCUPIED_ICON));

			protocolStatusList.put(NA_STATUS,
					ImageLoader.getImage(ImageLoader.SIP_NA_ICON));

			protocolStatusList.put(DND_STATUS,
					ImageLoader.getImage(ImageLoader.SIP_DND_ICON));

			protocolStatusList.put(OFFLINE_STATUS,
					ImageLoader.getImage(ImageLoader.SIP_OFFLINE_ICON));

			protocolStatusList.put(INVISIBLE_STATUS,
					ImageLoader.getImage(ImageLoader.SIP_INVISIBLE_ICON));

		} else if (protocolName.equals(Constants.ICQ)) {

			protocolStatusList.put(ONLINE_STATUS,
					ImageLoader.getImage(ImageLoader.ICQ_LOGO));

			protocolStatusList.put(CHAT_STATUS,  
					ImageLoader.getImage(ImageLoader.ICQ_FF_CHAT_ICON));

			protocolStatusList.put(AWAY_STATUS,  
					ImageLoader.getImage(ImageLoader.ICQ_AWAY_ICON));

			protocolStatusList.put(NA_STATUS,  
					ImageLoader.getImage(ImageLoader.ICQ_NA_ICON));

			protocolStatusList.put(DND_STATUS,  
					ImageLoader.getImage(ImageLoader.ICQ_DND_ICON));

			protocolStatusList.put(OCCUPIED_STATUS,  
					ImageLoader.getImage(ImageLoader.ICQ_OCCUPIED_ICON));

			protocolStatusList.put(OFFLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.ICQ_OFFLINE_ICON));

			protocolStatusList.put(INVISIBLE_STATUS, 
					ImageLoader.getImage(ImageLoader.ICQ_INVISIBLE_ICON));

		} else if (protocolName.equals(Constants.MSN)) {

			protocolStatusList.put(ONLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.MSN_LOGO));

		} else if (protocolName.equals(Constants.AIM)) {

			protocolStatusList.put(ONLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.AIM_LOGO));

		} else if (protocolName.equals(Constants.YAHOO)) {

			protocolStatusList.put(ONLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.YAHOO_LOGO));

		} else if (protocolName.equals(Constants.JABBER)) {

			protocolStatusList.put(ONLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.JABBER_LOGO));

		} else if (protocolName.equals(Constants.SKYPE)) {

			protocolStatusList.put(ONLINE_STATUS, 
					ImageLoader.getImage(ImageLoader.SKYPE_LOGO));
		}

		return protocolStatusList;
	}

	public static Image getStatusIcon(PresenceStatus status) {
		return (Image)mainStatusSet.get(status);
	}

    /**
     * Temporary method to load the css style used in the chat window.
     * 
     * @param style
     */
    public static void loadStyle(StyleSheet style){        
        
        InputStream is = Constants.class.getClassLoader()
            .getResourceAsStream
            ("net/java/sip/communicator/impl/gui/resources/styles/defaultStyle.css");
        
        Reader r = new BufferedReader(new InputStreamReader(is));
        try {
            style.loadRules(r, null);
            r.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static AudioClip getDefaultAudio() {
        AudioClip audio = Applet.newAudioClip(Constants.class.getClassLoader()
        .getResource("net/java/sip/communicator/impl/gui/resources/sounds/ship-sink.wav"));
        
        return audio;
	}
}
