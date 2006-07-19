/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.java.sip.communicator.util.Logger;

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
    public static final ImageID TAB_BG = new ImageID("TAB_BG");

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    public static final ImageID SELECTED_TAB_BG 
        = new ImageID("SELECTED_TAB_BG");

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

    /////////////////////// OptionPane icons /////////////////////////////

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane warning message. 
     */
    public static final ImageID WARNING_ICON = new ImageID("WARNING_ICON");

    /*------------------------------------------------------------------------
     * ============================APPLICATION ICONS =========================
     * -----------------------------------------------------------------------
     */
    /**
     * An empty 16x16 icon used for alignment.
     */
    public static final ImageID EMPTY_16x16_ICON 
        = new ImageID("EMPTY_16x16_ICON");

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
     * <tt>QuickMenu</tt> and in the <tt>AppearanceConfigurationForm</tt>.
     */
    public static final ImageID QUICK_MENU_SEARCH_ICON 
        = new ImageID("QUICK_MENU_SEARCH_ICON");

    /**
     * The icon on the "Info" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID QUICK_MENU_INFO_ICON 
        = new ImageID("QUICK_MENU_INFO_ICON");

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
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO 
        = new ImageID("DEFAULT_USER_PHOTO");

    /**
     * The image used in the chat window, when a contact has no photo
     * specified.
     */
    public static final ImageID DEFAULT_CHAT_USER_PHOTO 
        = new ImageID("DEFAULT_CHAT_USER_PHOTO");

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
     * The image used as a separator in all toolbars.
     */
    public static final ImageID TOOLBAR_DIVIDER 
        = new ImageID("TOOLBAR_DIVIDER");
    
    /**
     * The image used for decoration of the "Add contact" window.
     */
    public static final ImageID ADD_CONTACT_WIZARD_ICON 
        = new ImageID("ADD_CONTACT_WIZARD_ICON");
    
    /**
     * The image used for decoration of the "Rename contact" window.
     */
    public static final ImageID RENAME_DIALOG_ICON 
        = new ImageID("RENAME_DIALOG_ICON");
    

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
     * "Italic" button rollover image in the <tt>EditTextToolBar</tt> in the
     * <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_ITALIC_ROLLOVER_BUTTON
        = new ImageID("TEXT_ITALIC_ROLLOVER_BUTTON");

    /**
     * "Underline" button rollover image in the <tt>EditTextToolBar</tt> in
     * the <tt>ChatWindow</tt>.
     */
    public static final ImageID TEXT_UNDERLINED_ROLLOVER_BUTTON
        = new ImageID("TEXT_UNDERLINED_ROLLOVER_BUTTON");
    
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
     * Quit icon.
     */
    public static final ImageID QUIT_ICON = new ImageID("QUIT_ICON");

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
     * Info 16x16 image.
     */
    public static final ImageID INFO_16x16_ICON 
        = new ImageID("INFO_16x16_ICON");

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
     * The background image of <tt>LoginWindow</tt> and <tt>WelcomeWindow</tt>
     * frames.
     */
    public static final ImageID LOGIN_WINDOW_LOGO = new ImageID(
            "LOGIN_WINDOW_LOGO");

    /*
     * =========================================================================
     * --------------------- PROTOCOLS STATUS ICONS ---------------------------
     * ========================================================================
     */
    /**
     * The ICQ logo 16x16 icon.
     */
    public static final ImageID ICQ_LOGO = new ImageID("ICQ_LOGO");

    /**
     * The ICQ "connecting" 16x16 animated icon.
     */
    public static final ImageID ICQ_CONNECTING = new ImageID("ICQ_CONNECTING");

    /**
     * The ICQ "free for chat" 16x16 icon.
     */
    public static final ImageID ICQ_FF_CHAT_ICON = new ImageID(
            "ICQ_FF_CHAT_ICON");

    /**
     * The ICQ "away" 16x16 icon.
     */
    public static final ImageID ICQ_AWAY_ICON = new ImageID("ICQ_AWAY_ICON");

    /**
     * The ICQ "not available" 16x16 icon.
     */
    public static final ImageID ICQ_NA_ICON = new ImageID("ICQ_NA_ICON");

    /**
     * The ICQ "do not disturb" 16x16 icon.
     */
    public static final ImageID ICQ_DND_ICON = new ImageID("ICQ_DND_ICON");

    /**
     * The ICQ "occupied" 16x16 icon.
     */
    public static final ImageID ICQ_OCCUPIED_ICON = new ImageID(
            "ICQ_OCCUPIED_ICON");

    /**
     * The ICQ "offline" 16x16 icon.
     */
    public static final ImageID ICQ_OFFLINE_ICON = new ImageID(
            "ICQ_OFFLINE_ICON");

    /**
     * The ICQ "invisible" 16x16 icon.
     */
    public static final ImageID ICQ_INVISIBLE_ICON = new ImageID(
            "ICQ_INVISIBLE_ICON");

    /**
     * The MSN logo 16x16 icon.
     */
    public static final ImageID MSN_LOGO = new ImageID("MSN_LOGO");

    /**
     * The AIM logo 16x16 icon.
     */
    public static final ImageID AIM_LOGO = new ImageID("AIM_LOGO");

    /**
     * The Yahoo logo 16x16 icon.
     */
    public static final ImageID YAHOO_LOGO = new ImageID("YAHOO_LOGO");

    /**
     * The Jabber logo 16x16 icon.
     */
    public static final ImageID JABBER_LOGO = new ImageID("JABBER_LOGO");

    /**
     * The Skype logo 16x16 icon.
     */
    public static final ImageID SKYPE_LOGO = new ImageID("SKYPE_LOGO");

    /**
     * The SIP logo 16x16 icon.
     */
    public static final ImageID SIP_LOGO = new ImageID("SIP_LOGO");

    /**
     * The SIP online 16x16 icon.
     */
    public static final ImageID SIP_ONLINE_ICON 
        = new ImageID("SIP_ONLINE_ICON");

    /**
     * The SIP offline 16x16 icon.
     */
    public static final ImageID SIP_OFFLINE_ICON = new ImageID(
            "SIP_OFFLINE_ICON");

    /**
     * The SIP invisible 16x16 icon.
     */
    public static final ImageID SIP_INVISIBLE_ICON = new ImageID(
            "SIP_INVISIBLE_ICON");

    /**
     * The SIP away 16x16 icon.
     */
    public static final ImageID SIP_AWAY_ICON = new ImageID("SIP_AWAY_ICON");

    /**
     * The SIP "not availabled" 16x16 icon.
     */
    public static final ImageID SIP_NA_ICON = new ImageID("SIP_NA_ICON");

    /**
     * The SIP "do not disturb" 16x16 icon.
     */
    public static final ImageID SIP_DND_ICON = new ImageID("SIP_DND_ICON");

    /**
     * The SIP "occupied" 16x16 icon.
     */
    public static final ImageID SIP_OCCUPIED_ICON = new ImageID(
            "SIP_OCCUPIED_ICON");

    /**
     * The SIP "chat" 16x16 icon.
     */
    public static final ImageID SIP_CHAT_ICON = new ImageID("SIP_CHAT_ICON");

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
                "8-)", "8)" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY3, new String[] {
                ":-*", ":*" }));

        defaultPackList.add(new Smiley(ImageLoader.SMILEY4, new String[] {
                ":-0", ":0" }));

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
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImage getImage(ImageID imageID) {
        BufferedImage image = null;

        if (loadedImages.containsKey(imageID)) {

            image = (BufferedImage) loadedImages.get(imageID);
        } else {
            String path = Images.getString(imageID.getId());
            try {
                image = ImageIO.read(ImageLoader.class.getClassLoader()
                        .getResource(path));

                loadedImages.put(imageID, image);

            } catch (IOException e) {
                log.error("Failed to load image:" + path, e);
            }
        }

        return image;
    }

    /**
     * Loads an animated gif image.
     * @param imageID The image identifier.
     * @return A BufferedImage array containing the animated image.
     */
    public static BufferedImage[] getAnimatedImage(ImageID imageID) {

        String path = Images.getString(imageID.getId());

        URL url = ImageLoader.class.getClassLoader().getResource(path);

        Iterator readers = ImageIO.getImageReadersBySuffix("gif");

        ImageReader reader = (ImageReader) readers.next();

        ImageInputStream iis;

        BufferedImage[] images = null;

        try {
            iis = ImageIO.createImageInputStream(url.openStream());

            reader.setInput(iis);

            final int numImages;

            numImages = reader.getNumImages(true);

            images = new BufferedImage[numImages];

            for (int i = 0; i < numImages; ++i) {
                images[i] = reader.read(i);
            }

        } catch (IOException e) {
            log.error("Failed to load image:" + path, e);
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
