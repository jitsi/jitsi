package net.java.sip.communicator.impl.gui.main.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import net.java.sip.communicator.impl.gui.main.Status;
import net.java.sip.communicator.impl.gui.main.customcontrols.StatusIcon;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 * 
 * All look and feel related constants are stored here.
 */

public class Constants {
	private static Logger log = Logger.getLogger(Constants.class);

	/*========================================================================
	 * ------------------------ SIZE CONSTANTS --------------------------------
	 ========================================================================*/

	public static final int MAINFRAME_HEIGHT = 180;

	public static final int MAINFRAME_WIDTH = 50;

	public static final int CONTACTPANEL_HEIGHT = 20;

	public static final int CONTACTPANEL_WIDTH = 10;

	public static final int CONTACTPANEL_SELECTED_HEIGHT = 50;

	public static final int CONTACTPANEL_SELECTED_GRADIENT_SIZE = 10;

	public static final int CONTACTPANEL_GRADIENT_SIZE = 10;
	
	public static final int HISTORY_WINDOW_HEIGHT = 450;

	public static final int HISTORY_WINDOW_WIDTH = 450;
	
	public static final int OPTION_PANE_WIDTH = 330;
	
	public static final int OPTION_PANE_HEIGHT = 150;
	
	/*========================================================================
	 * -------------------- FONTS AND COLOR CONSTANTS ------------------------
	 ========================================================================*/
		
	public static final Color CONTACTPANEL_SELECTED_START_COLOR = 
													new Color(151, 169, 198);

	public static final Color CONTACTPANEL_SELECTED_END_COLOR = 
													new Color(177, 193, 218);

	public static final Color CONTACTPANEL_MOVER_START_COLOR = 
													new Color(220, 220, 220);

	public static final Color CONTACTPANEL_MOVER_END_COLOR = 
													new Color(255, 255, 255);

	public static final Color CONTACTPANEL_LINES_COLOR = 
													new Color(154, 154,	154);

	public static final Color MSG_WINDOW_BORDER_COLOR = 
													new Color(142, 160,	188);
	
	/*=========================================================================
	 * ---------------------- ORIENTATION CONSTANTS -------------------------
	 ========================================================================*/
	
	public static final String LEFT = "left";
	
	public static final String RIGHT = "right";
	
	public static final String BOTTOM = "bottom";
	
	public static final String TOP = "top";
	
	/*=========================================================================
	 * ------------------------------ ICONS ----------------------------------
	 ========================================================================*/
	
	public static final Image EMPTY_16x16_ICON = Constants
			.loadImage("../../resources/common/emptyIcon16x16.png");

	
	public static final Image QUICK_MENU_ADD_ICON = Constants
			.loadImage("../../resources/buttons/addContactIcon.png");

	public static final Image QUICK_MENU_CONFIGURE_ICON = Constants
			.loadImage("../../resources/buttons/configureIcon.png");

	public static final Image QUICK_MENU_SEARCH_ICON = Constants
			.loadImage("../../resources/buttons/searchIcon.png");
	
	public static final Image QUICK_MENU_INFO_ICON = Constants
			.loadImage("../../resources/buttons/infoIcon.png");
	
	public static final Image QUICK_MENU_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/quickMenuButtonBg.png");

	public static final Image QUICK_MENU_BUTTON_ROLLOVER_BG = Constants
			.loadImage("../../resources/buttons/quickMenuButtonRolloverBg.png");
	
	public static final Image CALL_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/callButton.png");
	
	public static final Image HANGUP_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/hangupButton.png");

	public static final Image CALL_ROLLOVER_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/callButtonRollover.png");

	public static final Image CALL_PRESSED_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/callButtonPressed.png");
	
	public static final Image HANGUP_ROLLOVER_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/hangupButtonRollover.png");

	public static final Image HANGUP_PRESSED_BUTTON_BG = Constants
			.loadImage("../../resources/buttons/hangupButtonPressed.png");
	
	public static final Image STATUS_SELECTOR_BOX = Constants
			.loadImage("../../resources/buttons/combobox.png");

	public static final Image BUTTON_BG = Constants
			.loadImage("../../resources/buttons/dialButtonBg.png");

	public static final Image BUTTON_ROLLOVER_BG = Constants
			.loadImage("../../resources/buttons/dialButtonRolloverBg.png");
	
	public static final Image ONE_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/one.png");
	
	public static final Image TWO_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/two.png");
	
	public static final Image THREE_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/three.png");
	
	public static final Image FOUR_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/four.png");
	
	public static final Image FIVE_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/five.png");
	
	public static final Image SIX_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/six.png");

	public static final Image SEVEN_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/seven.png");

	public static final Image EIGHT_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/eight.png");

	public static final Image NINE_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/nine.png");

	public static final Image STAR_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/star.png");
			
	public static final Image ZERO_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/zero.png");
	
	public static final Image DIEZ_DIAL_BUTTON = Constants
			.loadImage("../../resources/buttons/diez.png");
		
	public static final Image DEFAULT_USER_PHOTO = Constants
			.loadImage("../../resources/common/personPhoto.png");
	
	public static final Image DEFAULT_CHAT_USER_PHOTO = Constants
			.loadImage("../../resources/common/personPhotoChat.png");
	
	public static final Image CALL_PANEL_MINIMIZE_BUTTON = Constants
			.loadImage("../../resources/buttons/callPanelMinimizeButton.png");

	public static final Image CALL_PANEL_RESTORE_BUTTON = Constants
			.loadImage("../../resources/buttons/callPanelRestoreButton.png");
	
	public static final Image CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/callPanelMinimizeButtonRollover.png");
	
	public static final Image CALL_PANEL_RESTORE_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/callPanelRestoreButtonRollover.png");
	
	public static final Image ADD_TO_CHAT_BUTTON = Constants
			.loadImage("../../resources/buttons/addToChat.png");
	
	public static final Image ADD_TO_CHAT_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/addToChatRollover.png");
	
	public static final Image ADD_TO_CHAT_ICON = Constants
			.loadImage("../../resources/buttons/addToChatIcon.png");

	public static final Image TOOLBAR_DIVIDER = Constants
			.loadImage("../../resources/buttons/toolbarDivider.png");
	
	public static final Image RIGHT_ARROW_ICON = Constants
			.loadImage("../../resources/common/rightArrow.png");
	
	public static final Image RIGHT_ARROW_ROLLOVER_ICON = Constants
			.loadImage("../../resources/common/rightArrowRollover.png");

	public static final Image BOTTOM_ARROW_ICON = Constants
			.loadImage("../../resources/common/bottomArrow.png");

	public static final Image BOTTOM_ARROW_ROLLOVER_ICON = Constants
			.loadImage("../../resources/common/bottomArrowRollover.png");

	///////////////////////// Edit Text Toolbar icons //////////////////////////
	
	public static final Image ALIGN_LEFT_BUTTON = Constants
			.loadImage("../../resources/buttons/alignLeft.png");
	
	public static final Image ALIGN_RIGHT_BUTTON = Constants
			.loadImage("../../resources/buttons/alignRight.png");
	
	public static final Image ALIGN_CENTER_BUTTON = Constants
			.loadImage("../../resources/buttons/alignCenter.png");
	
	public static final Image ALIGN_LEFT_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/alignLeftRollover.png");

	public static final Image ALIGN_RIGHT_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/alignRightRollover.png");
	
	public static final Image ALIGN_CENTER_ROLLOVER_BUTTON = Constants
			.loadImage("../../resources/buttons/alignCenterRollover.png");
	
	public static final Image TEXT_BOLD_BUTTON = Constants
		.loadImage("../../resources/buttons/textBold.png");
	
	public static final Image TEXT_ITALIC_BUTTON = Constants
		.loadImage("../../resources/buttons/textItalic.png");
	
	public static final Image TEXT_UNDERLINED_BUTTON = Constants
		.loadImage("../../resources/buttons/textUnderlined.png");

	public static final Image TEXT_BOLD_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/textBoldRollover.png");
	
	public static final Image TEXT_ITALIC_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/textItalicRollover.png");
	
	public static final Image TEXT_UNDERLINED_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/textUnderlinedRollover.png");
	
	/////////////////////////// Main Toolbar icons ////////////////////////////
	
	public static final Image MSG_TOOLBAR_BUTTON_BG = Constants
		.loadImage("../../resources/buttons/msgToolbarBg.png");
	
	public static final Image MSG_TOOLBAR_ROLLOVER_BUTTON_BG = Constants
		.loadImage("../../resources/buttons/msgToolBarRolloverBg.png");

	public static final Image COPY_ICON = Constants
		.loadImage("../../resources/buttons/copy.png");

	public static final Image CUT_ICON = Constants
		.loadImage("../../resources/buttons/cut.png");

	public static final Image PASTE_ICON = Constants
		.loadImage("../../resources/buttons/paste.png");
		
	public static final Image SMILIES_ICON = Constants
		.loadImage("../../resources/buttons/smily.png");
	
	public static final Image SAVE_ICON = Constants
		.loadImage("../../resources/buttons/save.png");

	public static final Image PRINT_ICON = Constants
		.loadImage("../../resources/buttons/print.png");
	
	public static final Image CLOSE_ICON = Constants
		.loadImage("../../resources/buttons/close.png");
	
	public static final Image QUIT_ICON = Constants
		.loadImage("../../resources/buttons/quit.png");
	
	public static final Image PREVIOUS_ICON = Constants
		.loadImage("../../resources/buttons/previous.png");

	public static final Image NEXT_ICON = Constants
		.loadImage("../../resources/buttons/next.png");

	public static final Image HISTORY_ICON = Constants
		.loadImage("../../resources/buttons/history.png");

	public static final Image SEND_FILE_ICON = Constants
		.loadImage("../../resources/buttons/sendFile.png");
	
	public static final Image FONT_ICON = Constants
		.loadImage("../../resources/buttons/fontIcon.png");
	
	/////////////////////// Chat contact icons ////////////////////////////////
	
	public static final Image CHAT_CONTACT_INFO_BUTTON = Constants
		.loadImage("../../resources/buttons/chatInfoButton.png");
	
	public static final Image CHAT_CONTACT_INFO_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/chatInfoButtonRollover.png");
	
	public static final Image CHAT_CONTACT_CALL_BUTTON = Constants
		.loadImage("../../resources/buttons/chatCallButton.png");

	public static final Image CHAT_CONTACT_CALL_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/chatCallButtonRollover.png");
	
	public static final Image CHAT_CONTACT_SEND_FILE_BUTTON = Constants
		.loadImage("../../resources/buttons/chatSendFile.png");
	
	public static final Image CHAT_SEND_FILE_ROLLOVER_BUTTON = Constants
		.loadImage("../../resources/buttons/chatSendFileRollover.png");
	
	/////////////////////// Optionpane icons /////////////////////////////
	
	public static final Image WARNING_ICON = Constants
		.loadImage("../../resources/common/warning.png");
	
	////////////////////// RightButton menu icons ////////////////////////
	
	public static final Image SEND_MESSAGE_16x16_ICON = Constants
		.loadImage("../../resources/common/sendMessage16x16.png");
	
	public static final Image DELETE_16x16_ICON = Constants
		.loadImage("../../resources/common/delete16x16.png");
	
	public static final Image HISTORY_16x16_ICON = Constants
		.loadImage("../../resources/common/history16x16.png");
	
	public static final Image SEND_FILE_16x16_ICON = Constants
		.loadImage("../../resources/common/sendFile16x16.png");
	
	public static final Image GROUPS_16x16_ICON = Constants
		.loadImage("../../resources/common/groups16x16.png");
	
	public static final Image INFO_16x16_ICON = Constants
		.loadImage("../../resources/common/userInfo16x16.png");
	
	public static final Image ADD_CONTACT_16x16_ICON = Constants
		.loadImage("../../resources/common/addContact16x16.png");
	
	public static final Image RENAME_16x16_ICON = Constants
		.loadImage("../../resources/common/rename16x16.png");
	
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
	
	public static final String SIP = "SIP";
	
	/*=========================================================================
	 * --------------------- PROTOCOLS STATUS ICONS ---------------------------
	 ========================================================================*/

	public static final Image ICQ_LOGO = Constants
			.loadImage("../../resources/protocols/icq/Icq16.png");

	public static final Image ICQ_FF_CHAT_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_ffc.png");

	public static final Image ICQ_AWAY_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_away.png");

	public static final Image ICQ_NA_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_na.png");

	public static final Image ICQ_DND_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_dnd.png");

	public static final Image ICQ_OCCUPIED_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_occupied.png");

	public static final Image ICQ_OFFLINE_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_offline.png");

	public static final Image ICQ_INVISIBLE_ICON = Constants
			.loadImage("../../resources/protocols/icq/cr16-action-icq_invisible.png");

	public static final Image MSN_LOGO = Constants
			.loadImage("../../resources/protocols/msn/Msn16.png");

	public static final Image AIM_LOGO = Constants
			.loadImage("../../resources/protocols/aim/Aim16.png");

	public static final Image YAHOO_LOGO = Constants
			.loadImage("../../resources/protocols/yahoo/Yahoo16.png");

	public static final Image JABBER_LOGO = Constants
			.loadImage("../../resources/protocols/jabber/Jabber16.png");

	public static final Image SKYPE_LOGO = Constants
			.loadImage("../../resources/protocols/skype/Skype16.png");

	public static final Image SIP_LOGO = Constants
			.loadImage("../../resources/protocols/sip/sc_logo16x16.png");
		 
	public static final Image SIP_ONLINE_ICON = Constants
			.loadImage("../../resources/protocols/sip/onlineStatus.png");
	
	public static final Image SIP_OFFLINE_ICON = Constants
			.loadImage("../../resources/protocols/sip/offlineStatus.png");
	
	public static final Image SIP_INVISIBLE_ICON = Constants
			.loadImage("../../resources/protocols/sip/invisibleStatus.png");
	
	public static final Image SIP_AWAY_ICON = Constants
			.loadImage("../../resources/protocols/sip/awayStatus.png");
	
	public static final Image SIP_NA_ICON = Constants
			.loadImage("../../resources/protocols/sip/naStatus.png");
	
	public static final Image SIP_DND_ICON = Constants
			.loadImage("../../resources/protocols/sip/dndStatus.png");
	
	public static final Image SIP_OCCUPIED_ICON = Constants
			.loadImage("../../resources/protocols/sip/occupiedStatus.png");
	
	public static final Image SIP_CHAT_ICON = Constants
			.loadImage("../../resources/protocols/sip/chatStatus.png");
	
	
	/*========================================================================
	 * ------------------------ USERS ICONS ------------------v---------------
	 ========================================================================*/
	
	public static final Image USER_ONLINE_ICON = Constants
	.loadImage("../../resources/protocols/sip/sc_user_online.png");
	
	/*========================================================================
	 * ------------------------ OTHER CONSTANTS ------------------------------
	 ========================================================================*/
	
	public static final int RIGHT_SHIFT_STATUS_ICON = 2;
	
	/**
	 * Gets protocol logo
	 */
	
	public static Image getProtocolIcon(String protocolName){
		
		Image protocolIcon = null;
		
		if (protocolName.equals (Constants.SIP)) {
			
			protocolIcon = Constants.SIP_ONLINE_ICON;			
		} 
		else if (protocolName.equals (Constants.ICQ)) {
			
			protocolIcon = Constants.ICQ_LOGO;			
		} 
		else if (protocolName.equals (Constants.MSN)) {
			
			protocolIcon = Constants.MSN_LOGO;			
		} 
		else if (protocolName.equals (Constants.AIM)) {
			
			protocolIcon = Constants.AIM_LOGO;
			
		} 
		else if (protocolName.equals (Constants.YAHOO)) {
			
			protocolIcon = Constants.YAHOO_LOGO;
			
		} 
		else if (protocolName.equals (Constants.JABBER)) {
			
			protocolIcon = Constants.JABBER_LOGO;
			
		} 
		else if (protocolName.equals (Constants.SKYPE)) {
			
			protocolIcon = Constants.SKYPE_LOGO;
		}
		
		return protocolIcon;
	}
	/**
	 * Gets all protocol statuses, including status and text.
	 * 
	 * @param protocolName
	 * @return an ArrayList of all status Icons for the given protocol. 
	 */
	
	public static ArrayList getProtocolStatusIcons (String protocolName) {
		ArrayList protocolStatusList = new ArrayList ();

		if (protocolName.equals (Constants.SIP)) {
			
			protocolStatusList.add (new Status (ONLINE_STATUS, 
					new StatusIcon (Constants.SIP_ONLINE_ICON)));
			
			protocolStatusList.add (new Status (CHAT_STATUS, 
					new StatusIcon (Constants.SIP_CHAT_ICON)));
			
			protocolStatusList.add (new Status (AWAY_STATUS, 
					new StatusIcon (Constants.SIP_AWAY_ICON)));
			
			protocolStatusList.add (new Status (OCCUPIED_STATUS, 
					new StatusIcon (Constants.SIP_OCCUPIED_ICON)));
			
			protocolStatusList.add (new Status (NA_STATUS, 
					new StatusIcon (Constants.SIP_NA_ICON)));
			
			protocolStatusList.add (new Status (DND_STATUS, 
					new StatusIcon (Constants.SIP_DND_ICON)));
			
			protocolStatusList.add (new Status (OFFLINE_STATUS, 
					new StatusIcon (Constants.SIP_OFFLINE_ICON)));
			
			protocolStatusList.add (new Status (INVISIBLE_STATUS, 
					new StatusIcon (Constants.SIP_INVISIBLE_ICON)));
			
		} else if (protocolName.equals (Constants.ICQ)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO)));
			
			protocolStatusList.add (new Status(CHAT_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO,
									Constants.ICQ_FF_CHAT_ICON)));
			
			protocolStatusList.add(new Status(AWAY_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO,
									Constants.ICQ_AWAY_ICON)));
			
			protocolStatusList.add(new Status(NA_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO,
									Constants.ICQ_NA_ICON)));
			
			protocolStatusList.add(new Status(DND_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO,
									Constants.ICQ_DND_ICON)));
			
			protocolStatusList.add(new Status(OCCUPIED_STATUS, 
					new StatusIcon (Constants.ICQ_LOGO,
									Constants.ICQ_OCCUPIED_ICON)));
			
			protocolStatusList.add(new Status(OFFLINE_STATUS, 
					new StatusIcon (Constants.ICQ_OFFLINE_ICON)));
			
			protocolStatusList.add(new Status(INVISIBLE_STATUS, 
					new StatusIcon (Constants.ICQ_INVISIBLE_ICON)));
			
		} else if (protocolName.equals (Constants.MSN)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.MSN_LOGO)));
			
		} else if (protocolName.equals (Constants.AIM)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.AIM_LOGO)));
			
		} else if (protocolName.equals (Constants.YAHOO)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.YAHOO_LOGO)));
			
		} else if (protocolName.equals (Constants.JABBER)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.JABBER_LOGO)));
			
		} else if (protocolName.equals (Constants.SKYPE)) {
			
			protocolStatusList.add (new Status(ONLINE_STATUS, 
					new StatusIcon (Constants.SKYPE_LOGO)));
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

			image = ImageIO.read(Constants.class.getResource(path));

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
