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

import javax.imageio.ImageIO;

import net.java.sip.communicator.impl.gui.main.customcontrols.StatusIcon;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 * 
 * All look and feel related constants are stored here.
 */

public class Constants {

	/*
	 * ========================================================================
	 * ------------------------ SIZE CONSTANTS --------------------------------
	 * ========================================================================
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
	 * ========================================================================
	 * -------------------- FONTS AND COLOR CONSTANTS ------------------------
	 * ========================================================================
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
	 * =========================================================================
	 * ---------------------- MESSAGE WINDOW FONT CONSTANTS ---------------------
	 * =========================================================================
	 */

	public static final String FONT_NAME = "Verdana";

	public static final String FONT_SIZE = "10";

	public static final String FONT_IN_MSG_COLOR = "#ef7b1e";

	public static final String FONT_OUT_MSG_COLOR = "#2e538b";

	public static final String FONT_CHAT_HEADER_COLOR = "c6d0e1";

	public static final Font FONT = new Font(	Constants.FONT_NAME, 
												Font.PLAIN,
												new Integer(Constants.FONT_SIZE).intValue());
	
	/*
	 * =========================================================================
	 * ------------------------ STATUS LABELS ---------------------------------
	 * ========================================================================
	 */

	public static final String ONLINE_STATUS = "Online";

	public static final String OFFLINE_STATUS = "Offline";

	public static final String OCCUPIED_STATUS = "Occupied";

	public static final String CHAT_STATUS = "Free for chat";

	public static final String AWAY_STATUS = "Away";

	public static final String NA_STATUS = "Not available";

	public static final String INVISIBLE_STATUS = "Invisible";

	public static final String DND_STATUS = "Do not disturb";

	/*
	 * =========================================================================
	 * ------------------------ PROTOCOL NAMES --------------------------------
	 * ========================================================================
	 */

	public static final String ICQ = "ICQ";

	public static final String MSN = "MSN";

	public static final String AIM = "AIM";

	public static final String YAHOO = "Yahoo";

	public static final String JABBER = "Jabber";

	public static final String SKYPE = "Skype";

	public static final String SIP = "SIP";

	/*
	 * ========================================================================
	 * ------------------------ OTHER CONSTANTS ------------------------------
	 * ========================================================================
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

	public static ArrayList getProtocolStatusIcons(String protocolName) {
		ArrayList protocolStatusList = new ArrayList();

		if (protocolName.equals(Constants.SIP)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_ONLINE_ICON))));

			protocolStatusList.add(new SelectorBoxItem(CHAT_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_CHAT_ICON))));

			protocolStatusList.add(new SelectorBoxItem(AWAY_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_AWAY_ICON))));

			protocolStatusList.add(new SelectorBoxItem(OCCUPIED_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_OCCUPIED_ICON))));

			protocolStatusList.add(new SelectorBoxItem(NA_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_NA_ICON))));

			protocolStatusList.add(new SelectorBoxItem(DND_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_DND_ICON))));

			protocolStatusList.add(new SelectorBoxItem(OFFLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_OFFLINE_ICON))));

			protocolStatusList.add(new SelectorBoxItem(INVISIBLE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SIP_INVISIBLE_ICON))));

		} else if (protocolName.equals(Constants.ICQ)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO))));

			protocolStatusList.add(new SelectorBoxItem(CHAT_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO), 
					ImageLoader.getImage(ImageLoader.ICQ_FF_CHAT_ICON))));

			protocolStatusList.add(new SelectorBoxItem(AWAY_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO), 
					ImageLoader.getImage(ImageLoader.ICQ_AWAY_ICON))));

			protocolStatusList.add(new SelectorBoxItem(NA_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO), 
					ImageLoader.getImage(ImageLoader.ICQ_NA_ICON))));

			protocolStatusList.add(new SelectorBoxItem(DND_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO), 
					ImageLoader.getImage(ImageLoader.ICQ_DND_ICON))));

			protocolStatusList.add(new SelectorBoxItem(OCCUPIED_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_LOGO), 
					ImageLoader.getImage(ImageLoader.ICQ_OCCUPIED_ICON))));

			protocolStatusList.add(new SelectorBoxItem(OFFLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_OFFLINE_ICON))));

			protocolStatusList.add(new SelectorBoxItem(INVISIBLE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.ICQ_INVISIBLE_ICON))));

		} else if (protocolName.equals(Constants.MSN)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.MSN_LOGO))));

		} else if (protocolName.equals(Constants.AIM)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.AIM_LOGO))));

		} else if (protocolName.equals(Constants.YAHOO)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.YAHOO_LOGO))));

		} else if (protocolName.equals(Constants.JABBER)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.JABBER_LOGO))));

		} else if (protocolName.equals(Constants.SKYPE)) {

			protocolStatusList.add(new SelectorBoxItem(ONLINE_STATUS, new StatusIcon(
					ImageLoader.getImage(ImageLoader.SKYPE_LOGO))));
		}

		return protocolStatusList;
	}
	
}
