/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.imageio.stream.*;

import net.java.sip.communicator.util.*;

/**
 * Stores and loads images used throughout this ui implementation.
 *
 * @author Yana Stamcheva
 */
public class ImageLoader {

    private static Logger log = Logger.getLogger(ImageLoader.class);

    /**
     * Stores all already loaded images.
     */
    private static Hashtable loadedImages = new Hashtable();

    /**
     * The SIP Communicator logo 16x16 icon.
     */
    public static final ImageID SIP_COMMUNICATOR_LOGO
        = new ImageID("SIP_COMMUNICATOR_LOGO");

    /*------------------------------------------------------------------------
     * =========================LOOK AND FEEL IMAGES==========================
     * -----------------------------------------------------------------------
     */
    /**
     * The background image of a button.
     */
    public static final ImageID BUTTON = new ImageID("BUTTON");

    /**
     * The rollover image of a button.
     */
    public static final ImageID BUTTON_ROLLOVER
        = new ImageID("BUTTON_ROLLOVER");

    /**
     * The pressed toggle button background image. 
     */
    public static final ImageID TOGGLE_BUTTON_PRESSED
        = new ImageID("TOGGLE_BUTTON_PRESSED");

    /**
     * The toggle button background image.
     */
    public static final ImageID TOGGLE_BUTTON
        = new ImageID("TOGGLE_BUTTON");
    
    /**
     * The image used for a horizontal split.
     */
    public static final ImageID SPLITPANE_HORIZONTAL
        = new ImageID("SPLITPANE_HORIZONTAL");

    /**
     * The image used for a vertical split.
     */
    public static final ImageID SPLITPANE_VERTICAL
        = new ImageID("SPLITPANE_VERTICAL");

    /**
     * The image used for the "thumb" of a vertical scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_VERTICAL
        = new ImageID("SCROLLBAR_VERTICAL");

    /**
     * The image used for the "thumb" of a horizontal scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HORIZONTAL
        = new ImageID("SCROLLBAR_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a horizontal scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HANDLE_HORIZONTAL
        = new ImageID("SCROLLBAR_THUMB_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a vertical scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HANDLE_VERTICAL
        = new ImageID("SCROLLBAR_THUMB_VERTICAL");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    public static final ImageID TAB_LEFT_BG = new ImageID("TAB_LEFT_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    public static final ImageID TAB_MIDDLE_BG = new ImageID("TAB_MIDDLE_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    public static final ImageID TAB_RIGHT_BG = new ImageID("TAB_RIGHT_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    public static final ImageID SELECTED_TAB_LEFT_BG
        = new ImageID("SELECTED_TAB_LEFT_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    public static final ImageID SELECTED_TAB_MIDDLE_BG
        = new ImageID("SELECTED_TAB_MIDDLE_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    public static final ImageID SELECTED_TAB_RIGHT_BG
        = new ImageID("SELECTED_TAB_RIGHT_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a closable tab.
     */
    public static final ImageID CLOSABLE_TAB_BG
        = new ImageID("CLOSABLE_TAB_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a closable selected tab.
     */
    public static final ImageID SELECTED_CLOSABLE_TAB_BG
        = new ImageID("SELECTED_CLOSABLE_TAB_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint a close
     * button on a tab.
     */
    public static final ImageID CLOSE_TAB_ICON = new ImageID("CLOSE_TAB_ICON");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint a rollover
     * close button on a tab.
     */
    public static final ImageID CLOSE_TAB_SELECTED_ICON
        = new ImageID("CLOSE_TAB_SELECTED_ICON");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * used to delete text in text fields and editable combo boxes.
     */
    public static final ImageID DELETE_TEXT_ICON
        = new ImageID("DELETE_TEXT_ICON");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the rollover
     * icon used to delete text in text fields and editable combo boxes.
     */
    public static final ImageID DELETE_TEXT_ROLLOVER_ICON
        = new ImageID("DELETE_TEXT_ROLLOVER_ICON");

    /////////////////////// OptionPane icons /////////////////////////////

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane warning message.
     */
    public static final ImageID WARNING_ICON = new ImageID("WARNING_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane error message.
     */
    public static final ImageID ERROR_ICON = new ImageID("ERROR_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane info message.
     */
    public static final ImageID INFO_ICON = new ImageID("INFO_ICON");

    /*------------------------------------------------------------------------
     * ============================APPLICATION ICONS =========================
     * -----------------------------------------------------------------------
     */
    
    /**
     * The background of the main window and chat window.
     */
    public static final ImageID MAIN_WINDOW_BACKGROUND
        = new ImageID("MAIN_WINDOW_BACKGROUND");

    /**
     * The background of the about window.
     */
    public static final ImageID ABOUT_WINDOW_BG
        = new ImageID("ABOUT_WINDOW_BG");

    /**
     * The icon on the "Add contact" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID ACCOUNT_ICON
        = new ImageID("ACCOUNT_ICON");

    /**
     * The icon on the "Add contact" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_ADD_ICON
        = new ImageID("QUICK_MENU_ADD_ICON");

    /**
     * The icon on the "Configure" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_CONFIGURE_ICON
        = new ImageID("QUICK_MENU_CONFIGURE_ICON");

    /**
     * The icon on the "Hide/Show offline contacts" button in the
     * <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_SHOW_OFFLINE_ICON
        = new ImageID("QUICK_MENU_SHOW_OFFLINE_ICON");

    /**
     * The icon on the "Hide/Show offline contacts" button in the
     * <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_HIDE_OFFLINE_ICON
        = new ImageID("QUICK_MENU_HIDE_OFFLINE_ICON");

    /**
     * The icon on the "Info" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_INFO_ICON
        = new ImageID("QUICK_MENU_INFO_ICON");

    /**
     * The icon on the "Sound" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_SOUND_ON_ICON
        = new ImageID("QUICK_MENU_SOUND_ON_ICON");

    /**
     * The icon on the "Sound" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_SOUND_OFF_ICON
        = new ImageID("QUICK_MENU_SOUND_OFF_ICON");

    /**
     * The background image of a <tt>QuickMenu</tt> button.
     */
    public static final ImageID QUICK_MENU_BUTTON_BG
        = new ImageID("QUICK_MENU_BUTTON_BG");

    /**
     * The background rollover image of a <tt>QuickMenu</tt> button.
     */
    public static final ImageID QUICK_MENU_BUTTON_ROLLOVER_BG
        = new ImageID("QUICK_MENU_BUTTON_ROLLOVER_BG");

    /**
     * The call button image.
     */
    public static final ImageID CALL_BUTTON_BG
        = new ImageID("CALL_BUTTON_BG");

    /**
     * The hangup button image.
     */
    public static final ImageID HANGUP_BUTTON_BG
        = new ImageID("HANGUP_BUTTON_BG");

    /**
     * The call button mouse over image.
     */
    public static final ImageID CALL_ROLLOVER_BUTTON_BG
        = new ImageID("CALL_ROLLOVER_BUTTON_BG");

    /**
     * The hangup button mouse over image.
     */
    public static final ImageID HANGUP_ROLLOVER_BUTTON_BG
        = new ImageID("HANGUP_ROLLOVER_BUTTON_BG");

    /**
     * The hangup button pressed image.
     */
    public static final ImageID CALL_BUTTON_PRESSED_BG
        = new ImageID("CALL_BUTTON_PRESSED_BG");

    /**
     * The hangup button pressed image.
     */
    public static final ImageID HANGUP_BUTTON_PRESSED_BG
        = new ImageID("HANGUP_BUTTON_PRESSED_BG");

    /**
     * The background image for the <tt>StatusSelectorBox</tt>.
     */
    public static final ImageID STATUS_SELECTOR_BOX
        = new ImageID("STATUS_SELECTOR_BOX");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON
        = new ImageID("ONE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON
        = new ImageID("TWO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON
        = new ImageID("THREE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON
        = new ImageID("FOUR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON
        = new ImageID("FIVE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON
        = new ImageID("SIX_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON
        = new ImageID("SEVEN_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON
        = new ImageID("EIGHT_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON
        = new ImageID("NINE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON
        = new ImageID("STAR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON
        = new ImageID("ZERO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON
        = new ImageID("DIEZ_DIAL_BUTTON");

    /**
     * A dial button icon. The icon shown in the CallParticipant panel. 
     */
    public static final ImageID DIAL_BUTTON
        = new ImageID("DIAL_BUTTON");

    /**
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO
        = new ImageID("DEFAULT_USER_PHOTO");


    /**
     * The minimize button icon in the <tt>CallPanel</tt>.
     */
    public static final ImageID CALL_PANEL_MINIMIZE_BUTTON
        = new ImageID("CALL_PANEL_MINIMIZE_BUTTON");

    /**
     * The restore button icon in the <tt>CallPanel</tt>.
     */
    public static final ImageID CALL_PANEL_RESTORE_BUTTON
        = new ImageID("CALL_PANEL_RESTORE_BUTTON");

    /**
     * The minimize rollover button icon in the <tt>CallPanel</tt>.
     */
    public static final ImageID CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON
        = new ImageID("CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON");

    /**
     * The restore rollover button icon in the <tt>CallPanel</tt>.
     */
    public static final ImageID CALL_PANEL_RESTORE_ROLLOVER_BUTTON
        = new ImageID("CALL_PANEL_RESTORE_ROLLOVER_BUTTON");

    /**
     * The background image of the "Add contact to chat" button in the
     * chat window.
     */
    public static final ImageID ADD_TO_CHAT_BUTTON
        = new ImageID("ADD_TO_CHAT_BUTTON");

    /**
     * The background rollover image of the "Add contact to chat" button in
     * the chat window.
     */
    public static final ImageID ADD_TO_CHAT_ROLLOVER_BUTTON
        = new ImageID("ADD_TO_CHAT_ROLLOVER_BUTTON");

    /**
     * The icon image of the "Add contact to chat" button in the
     * chat window.
     */
    public static final ImageID ADD_TO_CHAT_ICON
        = new ImageID("ADD_TO_CHAT_ICON");

    /**
     * The image used for decoration of the "Add contact" window.
     */
    public static final ImageID ADD_CONTACT_WIZARD_ICON
        = new ImageID("ADD_CONTACT_WIZARD_ICON");

    /**
     * The image used for decoration of the "Add group" window.
     */
    public static final ImageID ADD_GROUP_ICON
        = new ImageID("ADD_GROUP_ICON");

    /**
     * The image used for decoration of the "Rename contact" window.
     */
    public static final ImageID RENAME_DIALOG_ICON
        = new ImageID("RENAME_DIALOG_ICON");
    
    /**
     * The image used for decoration of the "reason" dialog. The "reason" dialog
     * is used wherever user should specify a reason for the operation he's
     * trying to do.
     */
    public static final ImageID REASON_DIALOG_ICON
        = new ImageID("REASON_DIALOG_ICON");

    /**
     * The image used for decoration of the "Open in browser" item in
     * the right button click menu in chat window.
     */
    public static final ImageID BROWSER_ICON
        = new ImageID("BROWSER_ICON");

    /**
     * The image used for decoration of all windows concerning the process of
     * authorization.
     */
    public static final ImageID AUTHORIZATION_ICON
        = new ImageID("AUTHORIZATION_ICON");

    /**
     * The image used for decoration of incoming calls in the call list panel.
     */
    public static final ImageID INCOMING_CALL_ICON
        = new ImageID("INCOMING_CALL");

    /**
     * The image used for decoration of outgoing calls in the call list panel.
     */
    public static final ImageID OUTGOING_CALL_ICON
        = new ImageID("OUTGOING_CALL");

    /**
     * The image used in the right button menu for the move contact item.
     */
    public static final ImageID MOVE_CONTACT_ICON
        = new ImageID("MOVE_CONTACT");

    /**
     * The image used for error messages in the chat window.
     */
    public static final ImageID EXCLAMATION_MARK
        = new ImageID("EXCLAMATION_MARK");

    /**
     * The image used for about window background.
     */
    public static final ImageID ABOUT_WINDOW_BACKGROUND
        = new ImageID("ABOUT_WINDOW_BACKGROUND");

    /**
     * The image used for opened groups.
     */
    public static final ImageID OPENED_GROUP
        = new ImageID("OPENED_GROUP");

    /**
     * The image used for closed groups.
     */
    public static final ImageID CLOSED_GROUP
        = new ImageID("CLOSED_GROUP");
    
    /**
     * The image used for chat rooms.
     */
    public static final ImageID CHAT_ROOM_16x16_ICON
        = new ImageID("CHAT_ROOM_16x16_ICON");
    
    /**
     * The image used for multi user chat servers.
     */
    public static final ImageID CHAT_SERVER_16x16_ICON
        = new ImageID("CHAT_SERVER_16x16_ICON");
    
    /**
     * The image used to indicate in the contact list that a message is received
     * from a certain contact.
     */
    public static final ImageID MESSAGE_RECEIVED_ICON
        = new ImageID("MESSAGE_RECEIVED_ICON");
    
    /**
     * The image used to set to the chat room "join" right button menu.
     */
    public static final ImageID JOIN_ICON
        = new ImageID("JOIN_ICON");

    /**
     * The image used to set to the chat room "join as" right button menu.
     */
    public static final ImageID JOIN_AS_ICON
        = new ImageID("JOIN_AS_ICON");

    /**
     * The image used to set to the chat room "leave" right button menu.
     */
    public static final ImageID LEAVE_ICON
        = new ImageID("LEAVE_ICON");

    /**
     * Background image of the dial button.
     */
    public static final ImageID DIAL_BUTTON_BG
        = new ImageID("DIAL_BUTTON_BG");

    /**
     * Background image when rollover on the dial button.
     */
    public static final ImageID DIAL_BUTTON_ROLLOVER_BG
        = new ImageID("DIAL_BUTTON_ROLLOVER_BG");

    /**
     * Icon used in the chat window for the "Send as SMS" option.
     */
    public static final ImageID SEND_SMS_ICON
        = new ImageID("SEND_SMS_ICON");

    /**
     * Tool bar background image.
     */
    public static final ImageID TOOL_BAR_BACKGROUND
        = new ImageID("TOOL_BAR_BACKGROUND");

    /**
     * Main menu background image.
     */
    public static final ImageID MENU_BACKGROUND
        = new ImageID("MENU_BACKGROUND");

    /**
     * Title bar background image.
     */
    public static final ImageID WINDOW_TITLE_BAR
        = new ImageID("WINDOW_TITLE_BAR");

    // ///////////////////// Edit Text Toolbar icons //////////////////////////

    /**
     * "Left align" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_LEFT_BUTTON
        = new ImageID("ALIGN_LEFT_BUTTON");

    /**
     * "Right align" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_RIGHT_BUTTON
        = new ImageID("ALIGN_RIGHT_BUTTON");

    /**
     * "Center align" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_CENTER_BUTTON
        = new ImageID("ALIGN_RIGHT_BUTTON");

    /**
     * "Left align" button rollover image in the <tt>EditTextToolBar</tt> in
     * the <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_LEFT_ROLLOVER_BUTTON
        = new ImageID("ALIGN_LEFT_ROLLOVER_BUTTON");

    /**
     * "Right align" button rollover image in the <tt>EditTextToolBar</tt> in
     * the <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_RIGHT_ROLLOVER_BUTTON
        = new ImageID("ALIGN_RIGHT_ROLLOVER_BUTTON");

    /**
     * "Center align" button rollover image in the <tt>EditTextToolBar</tt> in
     * the <tt>ChatWindow</tt>.
     */
    public static final ImageID ALIGN_CENTER_ROLLOVER_BUTTON
        = new ImageID("ALIGN_CENTER_ROLLOVER_BUTTON");

    /**
     * "Bold" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_BOLD_BUTTON
        = new ImageID("TEXT_BOLD_BUTTON");

    /**
     * "Italic" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_ITALIC_BUTTON
        = new ImageID("TEXT_ITALIC_BUTTON");

    /**
     * "Underline" button image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_UNDERLINED_BUTTON
        = new ImageID("TEXT_UNDERLINED_BUTTON");

    /**
     * "Bold" button rollover image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_BOLD_ROLLOVER_BUTTON
        = new ImageID("TEXT_BOLD_ROLLOVER_BUTTON");

    /**
     * "Italic" button roll-over image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_ITALIC_ROLLOVER_BUTTON
        = new ImageID("TEXT_ITALIC_ROLLOVER_BUTTON");

    /**
     * "Underline" button roll-over image in the <tt>EditTextToolBar</tt> in
     * the <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_UNDERLINED_ROLLOVER_BUTTON
        = new ImageID("TEXT_UNDERLINED_ROLLOVER_BUTTON");

    /**
     * The icon shown in the invite dialog.
     */
    public static final ImageID INVITE_DIALOG_ICON
        = new ImageID("INVITE_DIALOG_ICON");

    /**
     * The icon shown between the global status button and other protocol status
     * buttons. 
     */
    public static final ImageID STATUS_SEPARATOR_ICON
        = new ImageID("STATUS_SEPARATOR_ICON");

    // ///////////////////////// Main Toolbar icons ////////////////////////////

    /**
     * The background image of a button in one of the <tt>ChatWindow</tt>
     * toolbars.
     */
    public static final ImageID CHAT_TOOLBAR_BUTTON_BG
        = new ImageID("MSG_TOOLBAR_BUTTON_BG");

    /**
     * The background rollover image of a button in one of the
     * <tt>ChatWindow</tt> toolbars.
     */
    public static final ImageID CHAT_TOOLBAR_ROLLOVER_BUTTON_BG
        = new ImageID("MSG_TOOLBAR_ROLLOVER_BUTTON_BG");

    /**
     * Copy icon.
     */
    public static final ImageID COPY_ICON = new ImageID("COPY_ICON");

    /**
     * Cut icon.
     */
    public static final ImageID CUT_ICON = new ImageID("CUT_ICON");

    /**
     * Paste icon.
     */
    public static final ImageID PASTE_ICON = new ImageID("PASTE_ICON");

    /**
     * Smiley icon, used for the "Smiley" button in the <tt>MainToolBar</tt>.
     */
    public static final ImageID SMILIES_ICON = new ImageID("SMILIES_ICON");

    /**
     * Save icon.
     */
    public static final ImageID SAVE_ICON = new ImageID("SAVE_ICON");

    /**
     * Print icon.
     */
    public static final ImageID PRINT_ICON = new ImageID("PRINT_ICON");

    /**
     * Close icon.
     */
    public static final ImageID CLOSE_ICON = new ImageID("CLOSE_ICON");

    /**
     * Left flash icon.
     */
    public static final ImageID PREVIOUS_ICON = new ImageID("PREVIOUS_ICON");

    /**
     * Right flash icon.
     */
    public static final ImageID NEXT_ICON = new ImageID("NEXT_ICON");

    /**
     * Clock icon.
     */
    public static final ImageID HISTORY_ICON = new ImageID("HISTORY_ICON");

    /**
     * Send file icon.
     */
    public static final ImageID SEND_FILE_ICON = new ImageID("SEND_FILE_ICON");

    /**
     * Font icon.
     */
    public static final ImageID FONT_ICON = new ImageID("FONT_ICON");

    // ///////////////////// Chat contact icons ////////////////////////////////

    /**
     * A special "info" icon used in the <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_CONTACT_INFO_BUTTON
        = new ImageID("CHAT_CONTACT_INFO_BUTTON");

    /**
     * A special "info" rollover icon used in the <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_CONTACT_INFO_ROLLOVER_BUTTON
        = new ImageID("CHAT_CONTACT_INFO_ROLLOVER_BUTTON");

    /**
     * A special "call" icon used in the <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_CONTACT_CALL_BUTTON
        = new ImageID("CHAT_CONTACT_CALL_BUTTON");

    /**
     * A special "call" rollover icon used in the <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_CONTACT_CALL_ROLLOVER_BUTTON
        = new ImageID("CHAT_CONTACT_CALL_ROLLOVER_BUTTON");

    /**
     * A special "send file" icon used in the <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_CONTACT_SEND_FILE_BUTTON
        = new ImageID("CHAT_CONTACT_SEND_FILE_BUTTON");

    /**
     * A special "send file" rollover icon used in the
     * <tt>ChatContactPanel</tt>.
     */
    public static final ImageID CHAT_SEND_FILE_ROLLOVER_BUTTON
        = new ImageID("CHAT_SEND_FILE_ROLLOVER_BUTTON");


    ////////////////////////////// 16x16 icons ////////////////////////////////
    /**
     * Send message 16x16 image.
     */
    public static final ImageID SEND_MESSAGE_16x16_ICON
        = new ImageID("SEND_MESSAGE_16x16_ICON");

    /**
     * Call 16x16 image.
     * //TODO : change to an appropriate logo
     */
    public static final ImageLoader.ImageID CALL_16x16_ICON
            = new ImageID("CALL_16x16_ICON");

    /**
     * Delete 16x16 image.
     */
    public static final ImageID DELETE_16x16_ICON
        = new ImageID("DELETE_16x16_ICON");

    /**
     * History 16x16 image.
     */
    public static final ImageID HISTORY_16x16_ICON
        = new ImageID("HISTORY_16x16_ICON");

    /**
     * Send file 16x16 image.
     */
    public static final ImageID SEND_FILE_16x16_ICON
        = new ImageID("SEND_FILE_16x16_ICON");

    /**
     * Groups 16x16 image.
     */
    public static final ImageID GROUPS_16x16_ICON
        = new ImageID("GROUPS_16x16_ICON");


    /**
     * Add contact 16x16 image.
     */
    public static final ImageID ADD_CONTACT_16x16_ICON
        = new ImageID("ADD_CONTACT_16x16_ICON");

    /**
     * Rename 16x16 image.
     */
    public static final ImageID RENAME_16x16_ICON
        = new ImageID("RENAME_16x16_ICON");


    ///////////////////////////////////////////////////////////////////////////

    /**
     * Contact list cell "more info" button.
     */
    public static final ImageID MORE_INFO_ICON = new ImageID("MORE_INFO_ICON");

    /**
     * Toolbar drag area icon.
     */
    public static final ImageID TOOLBAR_DRAG_ICON = new ImageID(
            "TOOLBAR_DRAG_ICON");

    /**
     * The background image of the <tt>AuthenticationWindow</tt>.
     */
    public static final ImageID AUTH_WINDOW_BACKGROUND = new ImageID(
            "AUTH_WINDOW_BACKGROUND");

    /**
     * The icon used to indicate a search.
     */
    public static final ImageID SEARCH_ICON = new ImageID("SEARCH_ICON");
        
    /**
     * The icon used to indicate a search.
     */
    public static final ImageID SEARCH_ICON_16x16
        = new ImageID("SEARCH_ICON_16x16");
    
    /*
     * =======================================================================
     * ------------------------ USERS' ICONS ---------------------------------
     * =======================================================================
     */

    /**
     * Contact "online" icon.
     */
    public static final ImageID USER_ONLINE_ICON = new ImageID(
            "USER_ONLINE_ICON");

    /**
     * Contact "offline" icon.
     */
    public static final ImageID USER_OFFLINE_ICON = new ImageID(
            "USER_OFFLINE_ICON");

    /**
     * Contact "away" icon.
     */
    public static final ImageID USER_AWAY_ICON = new ImageID("USER_AWAY_ICON");

    /**
     * Contact "not available" icon.
     */
    public static final ImageID USER_NA_ICON = new ImageID("USER_NA_ICON");

    /**
     * Contact "free for chat" icon.
     */
    public static final ImageID USER_FFC_ICON = new ImageID("USER_FFC_ICON");

    /**
     * Contact "do not disturb" icon.
     */
    public static final ImageID USER_DND_ICON = new ImageID("USER_DND_ICON");

    /**
     * Contact "occupied" icon.
     */
    public static final ImageID USER_OCCUPIED_ICON = new ImageID(
            "USER_OCCUPIED_ICON");

    /*
     * =====================================================================
     * ---------------------------- SMILIES --------------------------------
     * =====================================================================
     */

    public static final ImageID SMILEY1 = new ImageID("SMILEY1");

    public static final ImageID SMILEY2 = new ImageID("SMILEY2");

    public static final ImageID SMILEY3 = new ImageID("SMILEY3");

    public static final ImageID SMILEY4 = new ImageID("SMILEY4");

    public static final ImageID SMILEY5 = new ImageID("SMILEY5");

    public static final ImageID SMILEY6 = new ImageID("SMILEY6");

    public static final ImageID SMILEY7 = new ImageID("SMILEY7");

    public static final ImageID SMILEY8 = new ImageID("SMILEY8");

    public static final ImageID SMILEY9 = new ImageID("SMILEY9");

    public static final ImageID SMILEY10 = new ImageID("SMILEY10");

    public static final ImageID SMILEY11 = new ImageID("SMILEY11");

    public static final ImageID SMILEY12 = new ImageID("SMILEY12");


    /**
     * Load default smilies pack.
     *
     * @return the ArrayList of all smilies.
     */
    public static ArrayList getDefaultSmiliesPack() {

        ArrayList defaultPackList = new ArrayList();

        defaultPackList.add(new Smiley(ImageLoader.SMILEY1, new String[] {
                "$-)", "$)" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY2, new String[] {
                "B-)", "B)" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY3, new String[] {
                ":-*", ":*" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY4, new String[] {
                ":-0" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY5, new String[] {
                ":-((", ":((" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY6, new String[] {
                ":-~", ":~" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY7, new String[] {
                ":-|", ":|" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY8, new String[] {
                ":-P", ":P", ":-p", ":p" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY9, new String[] {
                ":-))", ":))" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY10, new String[] {
                ":-(", ":(" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY11, new String[] {
                ":-)", ":)" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY12, new String[] {
                ";-)", ";)" }));

        return defaultPackList;
    }

    /**
     * Returns a Smiley object for a given smiley string.
     * @param smileyString One of :-), ;-), etc.
     * @return A Smiley object for a given smiley string.
     */
    public static Smiley getSmiley(String smileyString) {
        ArrayList smiliesList = getDefaultSmiliesPack();

        for (int i = 0; i < smiliesList.size(); i++) {

            Smiley smiley = (Smiley) smiliesList.get(i);

            String[] smileyStrings = smiley.getSmileyStrings();

            for (int j = 0; j < smileyStrings.length; j++) {

                String srcString = smileyStrings[j];

                if (srcString.equals(smileyString))
                    return smiley;
            }
        }
        return null;
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImage getImage(ImageID imageID)
    {
        BufferedImage image = null;

        if (loadedImages.containsKey(imageID))
        {
            image = (BufferedImage) loadedImages.get(imageID);
        }
        else
        {
            String path = Images.getString(imageID.getId());

            if (path == null)
                return null;

            try
            {
                image = ImageIO.read(ImageLoader.class.getClassLoader()
                        .getResource(path));

                loadedImages.put(imageID, image);
            }
            catch (Exception exc)
            {
                log.error("Failed to load image:" + path, exc);
            }
        }

        return image;
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(ImageID imageID) {
        byte[] image = new byte[100000];

        String path = Images.getString(imageID.getId());
        try {
            Images.class.getClassLoader()
                    .getResourceAsStream(path).read(image);

        } catch (IOException e) {
            log.error("Failed to load image:" + path, e);
        }

        return image;
    }

    /**
     * Loads an image from a given bytes array.
     * @param imageBytes The bytes array to load the image from.
     * @return The image for the given bytes array.
     */
    public static Image getBytesInImage(byte[] imageBytes)
    {
        Image image = null;
        try
        {
            image = ImageIO.read(
                    new ByteArrayInputStream(imageBytes));
            
        }        
        catch (Exception e)
        {
            log.error("Failed to convert bytes to image.", e);
        }
        return image;
    }

    /**
     * Loads an animated gif image.
     * @param animatedImage the animated image buffer
     * @return a <tt>BufferedImage</tt> array containing the animated image.
     */
    public static BufferedImage[] getAnimatedImage(byte[] animatedImage)
    {
        Iterator readers = ImageIO.getImageReadersBySuffix("gif");

        ImageReader reader = (ImageReader) readers.next();

        ImageInputStream iis;

        BufferedImage[] images = null;

        try {
            iis = ImageIO.createImageInputStream(
                new ByteArrayInputStream(animatedImage));

            reader.setInput(iis);

            int numImages = reader.getNumImages(true);

            if(numImages == 0)
                return null;
            
            images = new BufferedImage[numImages];

            for (int i = 0; i < numImages; ++i) {
                images[i] = reader.read(i);
            }

        } catch (IOException e) {
            log.error("Failed to load image.", e);
        } finally {
            log.logExit();
        }
        return images;
    }

    /**
     * Represents the Image Identifier.
     */
    public static class ImageID {
        private String id;

        private ImageID(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Returns the path string of an already loaded image, otherwise null.
     *
     * @param image
     *            The image wich path to return.
     * @return The path string of an already loaded image, otherwise null.
     */
    public static String getImagePath(Image image) {

        String path = null;

        Iterator i = ImageLoader.loadedImages.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();

            if (entry.getValue().equals(image)) {
                String imageID = ((ImageID) entry.getKey()).getId();

                path = Images.getString(imageID);
            }
        }

        return path;
    }
}
