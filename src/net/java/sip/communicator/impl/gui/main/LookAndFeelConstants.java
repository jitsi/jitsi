package net.java.sip.communicator.impl.gui.main;

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import net.java.sip.communicator.impl.gui.main.customcontrols.StatusIcon;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 * 
 * All look and feel related constants are stored here.
 */

public class LookAndFeelConstants {
	private static Logger log = Logger.getLogger(LookAndFeelConstants.class);

	/*========================================================================
	 * ------------------------ SIZE CONSTANTS --------------------------------
	 ========================================================================*/

	public static final int MAINFRAME_HEIGHT = 200;

	public static final int MAINFRAME_WIDTH = 30;

	public static final int CONTACTPANEL_HEIGHT = 25;

	public static final int CONTACTPANEL_WIDTH = 10;

	public static final int CONTACTPANEL_SELECTED_HEIGHT = 50;

	public static final int CONTACTPANEL_SELECTED_GRADIENT_SIZE = 10;

	public static final int CONTACTPANEL_GRADIENT_SIZE = 10;

	
	/*========================================================================
	 * ------------------------ COLOR CONSTANTS -------------------------------
	 ========================================================================*/
	
	public static final Color CONTACTPANEL_SELECTED_START_COLOR = 
													new Color(166, 207, 239);

	public static final Color CONTACTPANEL_SELECTED_END_COLOR = 
													new Color(255, 255, 255);

	public static final Color CONTACTPANEL_MOVER_START_COLOR = 
													new Color(210, 210, 210);

	// public static final Color CONTACTPANEL_MOVER_START_COLOR = 
	//												new Color(244, 235, 143);
	
	public static final Color CONTACTPANEL_MOVER_END_COLOR = 
													new Color(255, 255, 255);

	public static final Color CONTACTPANEL_LINES_COLOR = 
													new Color(154, 154,	154);

	
	/*=========================================================================
	 * ------------------------------ ICONS ----------------------------------
	 ========================================================================*/
	
	public static final Image QUICK_MENU_ADD_ICON = LookAndFeelConstants
			.loadImage("../resources/buttons/addContactIcon.png");

	public static final Image QUICK_MENU_CONFIGURE_ICON = LookAndFeelConstants
			.loadImage("../resources/buttons/configureIcon.png");

	public static final Image QUICK_MENU_BUTTON_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/quickMenuButtonBg.gif");

	public static final Image QUICK_MENU_BUTTON_ROLLOVER_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/quickMenuButtonRolloverBg.gif");

	public static final Image CALL_BUTTON_ICON = LookAndFeelConstants
			.loadImage("../resources/buttons/callIcon.png");

	public static final Image HANG_UP_BUTTON_ICON = LookAndFeelConstants
			.loadImage("../resources/buttons/hangupIcon.png");

	public static final Image CALL_BUTTON_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/call.gif");

	public static final Image HANGUP_BUTTON_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/hangUp.gif");

	public static final Image CALL_ROLLOVER_BUTTON_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/callRollover.gif");

	public static final Image HANGUP_ROLLOVER_BUTTON_BG = LookAndFeelConstants
			.loadImage("../resources/buttons/hangUpRollover.gif");

	public static final Image STATUS_SELECTOR_BOX = LookAndFeelConstants
			.loadImage("../resources/buttons/combobox.png");

	
	/*=========================================================================
	 * ------------------------ STATUS LABELS ---------------------------------
	 ========================================================================*/
	
	public static final String ONLINE_STATUS = "Online";

	public static final String OFFLINE_STATUS = "Offline";

	public static final String OCCUPIED_STATUS = "Occupied";

	public static final String CHAT_STATUS = "Free for chat";

	public static final String AWAY_STATUS = "Away";

	public static final String NA_STATUS = "Not available";

	public static final String INVISIBLE_STATUS = "Invisible";

	public static final String DND_STATUS = "Do not disturb";

	/*=========================================================================
	 * ------------------------ PROTOCOL NAMES --------------------------------
	 ========================================================================*/
	
	public static final String ICQ = "ICQ";

	public static final String MSN = "MSN";

	public static final String AIM = "AIM";

	public static final String YAHOO = "Yahoo";

	public static final String JABBER = "Jabber";

	public static final String SKYPE = "Skype";
	
	/*=========================================================================
	 * --------------------- PROTOCOLS STATUS ICONS ---------------------------
	 ========================================================================*/

	public static final Image ICQ_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/Icq16.png");

	public static final Image ICQ_FF_CHAT_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_ffc.png");

	public static final Image ICQ_AWAY_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_away.png");

	public static final Image ICQ_NA_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_na.png");

	public static final Image ICQ_DND_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_dnd.png");

	public static final Image ICQ_OCCUPIED_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_occupied.png");

	public static final Image ICQ_OFFLINE_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_offline.png");

	public static final Image ICQ_INVISIBLE_ICON = LookAndFeelConstants
			.loadImage("../resources/protocols/icq/cr16-action-icq_invisible.png");

	public static final Image MSN_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/msn/Msn16.png");

	public static final Image AIM_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/aim/Aim16.png");

	public static final Image YAHOO_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/yahoo/Yahoo16.png");

	public static final Image JABBER_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/jabber/Jabber16.png");

	public static final Image SKYPE_LOGO = LookAndFeelConstants
			.loadImage("../resources/protocols/skype/Skype16.png");

	
	/**
	 * Gets all protocol statuses, including status and text.
	 * 
	 * @param protocolName
	 * @return an ArrayList of all status Icons for the given protocol. 
	 */
	
	public static ArrayList getProtocolIcons (String protocolName) {
		ArrayList protocolStatusList = new ArrayList ();

		if (protocolName.equals (LookAndFeelConstants.ICQ)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO)));
			
			protocolStatusList.add (new Status(CHAT_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO,
									LookAndFeelConstants.ICQ_FF_CHAT_ICON)));
			
			protocolStatusList.add(new Status(AWAY_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO,
									LookAndFeelConstants.ICQ_AWAY_ICON)));
			
			protocolStatusList.add(new Status(NA_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO,
									LookAndFeelConstants.ICQ_NA_ICON)));
			
			protocolStatusList.add(new Status(DND_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO,
									LookAndFeelConstants.ICQ_DND_ICON)));
			
			protocolStatusList.add(new Status(OCCUPIED_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_LOGO,
									LookAndFeelConstants.ICQ_OCCUPIED_ICON)));
			
			protocolStatusList.add(new Status(OFFLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_OFFLINE_ICON)));
			
			protocolStatusList.add(new Status(INVISIBLE_STATUS, 
					new StatusIcon (LookAndFeelConstants.ICQ_INVISIBLE_ICON)));
			
		} else if (protocolName.equals (LookAndFeelConstants.MSN)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.MSN_LOGO)));
			
		} else if (protocolName.equals (LookAndFeelConstants.AIM)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.AIM_LOGO)));
			
		} else if (protocolName.equals (LookAndFeelConstants.YAHOO)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.YAHOO_LOGO)));
			
		} else if (protocolName.equals (LookAndFeelConstants.JABBER)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.JABBER_LOGO)));
			
		} else if (protocolName.equals (LookAndFeelConstants.SKYPE)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (LookAndFeelConstants.SKYPE_LOGO)));
		}
		
		return protocolStatusList;
	}

	/**
	 * Loads an image from a given path.
	 */
	
	private static Image loadImage(String path) {
		Image image = null;

		try {
			log.logEntry();

			if (log.isTraceEnabled()) {
				log.trace("Loading image : " + path + "...");
			}

			image = ImageIO.read(LookAndFeelConstants.class.getResource(path));

			if (log.isTraceEnabled()) {
				log.trace("Loading image : " + path + "... [ DONE ]");
			}

		} catch (IOException e) {
			log.error("Failed to load image:" + path, e);
		} finally {
			log.logExit();
		}

		return image;
	}
}
