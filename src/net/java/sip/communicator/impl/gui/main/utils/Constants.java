/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.imageio.ImageIO;

import net.java.sip.communicator.impl.gui.main.customcontrols.StatusIcon;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 * 
 * All look and feel related constants are stored here.
 */

public class Constants {

	/*
	 * =======================================================================
	 * ------------------------ SIZE CONSTANTS -------------------------------
	 * =======================================================================
	 */

	public static final int MAINFRAME_HEIGHT = 180;

	public static final int MAINFRAME_WIDTH = 120;

	public static final int MAINFRAME_MIN_HEIGHT = 300;

	public static final int MAINFRAME_MIN_WIDTH = 200;

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
	 * =======================================================================
	 * -------------------- FONTS AND COLOR CONSTANTS ------------------------
	 * =======================================================================
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
	 * =======================================================================
	 * ---------------------- MESSAGE WINDOW FONT CONSTANTS ------------------
	 * =======================================================================
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
	 * =======================================================================
	 * ------------------------ STATUS LABELS --------------------------------
	 * =======================================================================
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

	/*
	 * =======================================================================
	 * ------------------------ PROTOCOL NAMES -------------------------------
	 * =======================================================================
	 */

	public static final String ICQ = "ICQ";

	public static final String MSN = "MSN";

	public static final String AIM = "AIM";

	public static final String YAHOO = "Yahoo";

	public static final String JABBER = "Jabber";

	public static final String SKYPE = "Skype";

	public static final String SIP = "SIP";

	/*
	 * =======================================================================
	 * ------------------------ OTHER CONSTANTS ------------------------------
	 * =======================================================================
	 */

	public static final int RIGHT_SHIFT_STATUS_ICON = 2;

	/**
	 * Gets protocol logo
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
	
}
