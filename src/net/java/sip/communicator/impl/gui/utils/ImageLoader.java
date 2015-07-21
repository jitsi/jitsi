/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Stores and loads images used throughout this UI implementation.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 * @author Hristo Terezov
 */
public class ImageLoader
{
    /**
     * The <tt>Logger</tt> used by the <tt>ImageLoader</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ImageLoader.class);

    /**
     * The image loader service implementation.
     */
    private static ImageLoaderServiceImpl imageLoaderService = null;

    /**
     * The SIP Communicator logo 16x16 icon.
     */
    public static final ImageID SIP_COMMUNICATOR_LOGO
        = new ImageID("service.gui.SIP_COMMUNICATOR_LOGO");

    /*------------------------------------------------------------------------
     * =========================LOOK AND FEEL IMAGES==========================
     * -----------------------------------------------------------------------
     */
    /**
     * The background image of a button.
     */
    public static final ImageID BUTTON
        = new ImageID("service.gui.lookandfeel.BUTTON");

    /**
     * The rollover image of a button.
     */
    public static final ImageID BUTTON_ROLLOVER
        = new ImageID("service.gui.lookandfeel.BUTTON_ROLLOVER");

    /**
     * The pressed toggle button background image.
     */
    public static final ImageID TOGGLE_BUTTON_PRESSED
        = new ImageID("service.gui.lookandfeel.TOGGLE_BUTTON_PRESSED");

    /**
     * The image used for a horizontal split.
     */
    public static final ImageID SPLITPANE_HORIZONTAL
        = new ImageID("service.gui.lookandfeel.SPLITPANE_HORIZONTAL");

    /**
     * The image used for a vertical split.
     */
    public static final ImageID SPLITPANE_VERTICAL
        = new ImageID("service.gui.lookandfeel.SPLITPANE_VERTICAL");

    /**
     * The image used for the "thumb" of a vertical scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_VERTICAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_VERTICAL");

    /**
     * The image used for the "thumb" of a horizontal scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HORIZONTAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a horizontal scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HANDLE_HORIZONTAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_THUMB_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a vertical scrollbar.
     */
    public static final ImageID SCROLLBAR_THUMB_HANDLE_VERTICAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_THUMB_VERTICAL");

    /*
     * =======================================================================
     * ------------------------ OPTION PANE ICONS ----------------------------
     * =======================================================================
     */
    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane warning message.
     */
    public static final ImageID WARNING_ICON
        = new ImageID("service.gui.icons.WARNING_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane error message.
     */
    public static final ImageID ERROR_ICON
        = new ImageID("service.gui.icons.ERROR_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane info message.
     */
    public static final ImageID INFO_ICON
        = new ImageID("service.gui.icons.INFO_ICON");

    /*------------------------------------------------------------------------
     * ============================APPLICATION ICONS =========================
     * -----------------------------------------------------------------------
     */

    /**
     * The background of the main window and chat window.
     */
    public static final ImageID MAIN_WINDOW_BACKGROUND
        = new ImageID("service.gui.MAIN_WINDOW_BACKGROUND");

    /**
     * The add account icon used in the file menu.
     */
    public static final ImageID ADD_ACCOUNT_MENU_ICON
        = new ImageID("service.gui.icons.ADD_ACCOUNT_MENU_ICON");

    /**
     * The background of the main window and chat window.
     */
    public static final ImageID DOWN_ARROW_ICON
        = new ImageID("service.gui.icons.DOWN_ARROW_ICON");

    /**
     * Closed group icon.
     */
    public static final ImageID RIGHT_ARROW_ICON
        = new ImageID("service.gui.icons.RIGHT_ARROW_ICON");

    /**
     * The edit icon that is shown when account is right clicked on.
     */
    public static final ImageID ACCOUNT_EDIT_ICON
        = new ImageID("service.gui.buttons.ACCOUNT_EDIT_ICON");

    /**
     * The call button image.
     */
    public static final ImageID INCOMING_CALL_BUTTON_BG
        = new ImageID("service.gui.buttons.INCOMING_CALL_BUTTON_BG");

    /**
     * The call button image.
     */
    public static final ImageID INCOMING_CALL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.INCOMING_CALL_BUTTON_ROLLOVER");

    /**
     * The call button image.
     */
    public static final ImageID INCOMING_CALL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.INCOMING_CALL_BUTTON_PRESSED");

    /**
     * The merge call button image.
     */
    public static final ImageID MERGE_CALL_BUTTON_BG
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_BG");

    /**
     * The merge call button image.
     */
    public static final ImageID MERGE_CALL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_ROLLOVER");

    /**
     * The merge call button image.
     */
    public static final ImageID MERGE_CALL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_PRESSED");

    /**
     * The video call button image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_BG
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_BG");

    /**
     * The video call button image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_ROLLOVER");

    /**
     * The video call button image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_PRESSED");

    /**
     * The background image for a button in contact list that is shown on the
     * left of the button toolbar.
     */
    public static final ImageID CONTACT_LIST_BUTTON_BG_LEFT
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_LEFT");

    /**
     * The background image for a button in contact list that is shown on the
     * right of the button toolbar.
     */
    public static final ImageID CONTACT_LIST_BUTTON_BG_RIGHT
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_RIGHT");

    /**
     * The background image for a button in contact list that is shown in the
     * middle of other buttons.
     */
    public static final ImageID CONTACT_LIST_BUTTON_BG_MIDDLE
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_MIDDLE");

    /**
     * The background image for a button in contact list if there's only one
     * button shown.
     */
    public static final ImageID CONTACT_LIST_ONE_BUTTON_BG
        = new ImageID("service.gui.buttons.CONTACT_LIST_ONE_BUTTON_BG");

    /**
     * The separator image for the button toolbar in the contact list.
     */
    public static final ImageID CONTACT_LIST_BUTTON_SEPARATOR
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_SEPARATOR");

    /**
     * The mobile indicator for contacts in contact list.
     */
    public static final ImageID CONTACT_LIST_MOBILE_INDICATOR
        = new ImageID("service.gui.buttons.CONTACT_LIST_MOBILE_INDICATOR");

    /**
     * The call button small image.
     */
    public static final ImageID CALL_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL");

    /**
     * The call button small pressed image.
     */
    public static final ImageID CALL_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_ROLLOVER");

    /**
     * The call button small pressed image.
     */
    public static final ImageID CALL_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_PRESSED");

    /**
     * The desktop sharing button small image.
     */
    public static final ImageID DESKTOP_BUTTON_SMALL
        = new ImageID("service.gui.buttons.DESKTOP_BUTTON_SMALL");

    /**
     * The desktop sharing button small pressed image.
     */
    public static final ImageID DESKTOP_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.DESKTOP_BUTTON_SMALL_ROLLOVER");

    /**
     * The desktop sharing button small pressed image.
     */
    public static final ImageID DESKTOP_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.DESKTOP_BUTTON_SMALL_PRESSED");

    /**
     * The desktop sharing button in the call window.
     */
    public static final ImageID CALL_DESKTOP_BUTTON
        = new ImageID("service.gui.buttons.CALL_DESKTOP_BUTTON");

    /**
     * The call button small image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL");

    /**
     * The call button small pressed image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL_ROLLOVER");

    /**
     * The call button small pressed image.
     */
    public static final ImageID CALL_VIDEO_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL_PRESSED");

    /**
     * The add contact button small image, shown when an external source contact
     * is selected.
     */
    public static final ImageID ADD_CONTACT_BUTTON_SMALL
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL");

    /**
     * The add contact button small pressed image, shown when an external source
     * contact is selected and add contact button is pressed.
     */
    public static final ImageID ADD_CONTACT_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL_ROLLOVER");

    /**
     * The add contact button small pressed image, shown when an external source
     * contact is selected and add contact button is pressed.
     */
    public static final ImageID ADD_CONTACT_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL_PRESSED");

    /**
     * The web button image.
     */
    public static final ImageID WEB_BUTTON
        = new ImageID("service.gui.buttons.WEB_BUTTON");

    /**
     * The call web pressed image.
     */
    public static final ImageID WEB_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.WEB_BUTTON_ROLLOVER");

    /**
     * The web button pressed image.
     */
    public static final ImageID WEB_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.WEB_BUTTON_PRESSED");

    /**
     * The chat button small image.
     */
    public static final ImageID CHAT_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL");

    /**
     * The chat button small image white on transparent version.
     */
    public static final ImageID CHAT_BUTTON_SMALL_WHITE
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_WHITE");

    /**
     * The icon used to separate buttons in the call toolbar.
     */
    public static final ImageID CALL_TOOLBAR_SEPARATOR
        = new ImageID("service.gui.icons.CALL_TOOLBAR_SEPARATOR");

    /**
     * The icon used to separate buttons in the call toolbar.
     */
    public static final ImageID CALL_TOOLBAR_DARK_SEPARATOR
        = new ImageID("service.gui.icons.CALL_TOOLBAR_DARK_SEPARATOR");

    /**
     * The chat call button image.
     */
    public static final ImageID CHAT_CALL
        = new ImageID("service.gui.buttons.CHAT_CALL");

    /**
     * The chat room call button image.
     */
    public static final ImageID CHAT_ROOM_CALL
        = new ImageID("service.gui.buttons.CHAT_ROOM_CALL");

    /**
     * The chat video call button image.
     */
    public static final ImageID CHAT_VIDEO_CALL
        = new ImageID("service.gui.buttons.CHAT_VIDEO_CALL");

    /**
     * The chat room video call button image.
     */
    public static final ImageID CHAT_ROOM_VIDEO_CALL
        = new ImageID("service.gui.buttons.CHAT_ROOM_VIDEO_CALL");

    /**
     * The chat call button image.
     */
    public static final ImageID CHAT_DESKTOP_SHARING
        = new ImageID("service.gui.buttons.CHAT_DESKTOP_SHARING");

    /**
     * The call history button image.
     */
    public static final ImageID CALL_HISTORY_BUTTON
        = new ImageID("service.gui.buttons.CALL_HISTORY_BUTTON");

    /**
     * The call history pressed button image.
     */
    public static final ImageID CALL_HISTORY_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.CALL_HISTORY_BUTTON_PRESSED");

    /**
     * The call history button missed call notification image.
     */
    public static final ImageID CALL_HISTORY_BUTTON_NOTIFICATION
        = new ImageID("service.gui.icons.CALL_HISTORY_BUTTON_NOTIFICATION");

    /**
     * The chat button small pressed image.
     */
    public static final ImageID CHAT_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_PRESSED");

    /**
     * The chat button small pressed image.
     */
    public static final ImageID CHAT_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_ROLLOVER");

    /**
     * The hangup button image.
     */
    public static final ImageID HANGUP_BUTTON_BG
        = new ImageID("service.gui.buttons.HANGUP_BUTTON_BG");

    /**
     * The hangup button image.
     */
    public static final ImageID HANGUP_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.HANGUP_BUTTON_ROLLOVER");

    /**
     * The hangup button image.
     */
    public static final ImageID HANGUP_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.HANGUP_BUTTON_PRESSED");

    /**
     * The background image for all setting buttons in the call panel.
     */
    public static final ImageID CALL_SETTING_BUTTON_BG
        = new ImageID("service.gui.buttons.CALL_SETTING_BUTTON_BG");

    /**
     * The background image for all pressed setting buttons in the call panel.
     */
    public static final ImageID CALL_SETTING_BUTTON_PRESSED_BG
        = new ImageID("service.gui.buttons.CALL_SETTING_BUTTON_PRESSED_BG");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID ONE_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID TWO_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID THREE_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID FOUR_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID FIVE_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID SIX_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID SEVEN_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID EIGHT_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID NINE_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID STAR_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID ZERO_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon.
     */
    public static final ImageID DIEZ_DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID DIAL_BUTTON
        = new ImageID("service.gui.buttons.DIAL_BUTTON");

    /**
     * A dial button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID ADD_TO_CALL_BUTTON
        = new ImageID("service.gui.buttons.ADD_TO_CALL_BUTTON");

    /**
     * A put-on/off-hold button icon. The icon shown in the CallPeer
     * panel.
     */
    public static final ImageID HOLD_BUTTON
        = new ImageID("service.gui.buttons.HOLD_BUTTON");

    /**
     * The merge call button image. The icon shown in the CallPeer panel.
     */
    public static final ImageID MERGE_CALL_BUTTON
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON");

    /**
     * A put-on/off-hold button icon. The icon shown in the CallPeer
     * panel.
     */
    public static final ImageID HOLD_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.HOLD_BUTTON_PRESSED");

    /**
     * The icon shown when the status of the call is "On hold".
     */
    public static final ImageID HOLD_STATUS_ICON
        = new ImageID("service.gui.icons.HOLD_STATUS_ICON");

    /**
     * The icon shown when the status of the call is "Mute".
     */
    public static final ImageID MUTE_STATUS_ICON
        = new ImageID("service.gui.icons.MUTE_STATUS_ICON");

    /**
     * A mute button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID MUTE_BUTTON
        = new ImageID("service.gui.buttons.MUTE_BUTTON");

    /**
     * A record button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID RECORD_BUTTON
        = new ImageID("service.gui.buttons.RECORD_BUTTON");

    /**
     * A record button pressed icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID RECORD_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.RECORD_BUTTON_PRESSED");

    /**
     * A local video button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID LOCAL_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON");

    /**
     * A local video button pressed icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID LOCAL_VIDEO_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_PRESSED");

    /**
     * A show/hide local video button icon. The icon shown in the CallPeer
     * panel.
     */
    public static final ImageID SHOW_LOCAL_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.SHOW_LOCAL_VIDEO_BUTTON");

    /**
     * A show/hide local video button pressed icon. The icon shown in the
     * CallPeer panel.
     */
    public static final ImageID SHOW_LOCAL_VIDEO_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.SHOW_LOCAL_VIDEO_BUTTON_PRESSED");

    /**
     * A show/hide local video button icon. The icon shown in the CallPeer
     * panel.
     */
    public static final ImageID SHOW_HIDE_PEERS_BUTTON
        = new ImageID("service.gui.buttons.SHOW_HIDE_PEERS_BUTTON");

    /**
     * A show/hide local video button pressed icon. The icon shown in the
     * CallPeer panel.
     */
    public static final ImageID SHOW_HIDE_PEERS_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.SHOW_HIDE_PEERS_BUTTON_PRESSED");

    /**
     * The resize video button.
     */
    public static final ImageID HD_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.HD_VIDEO_BUTTON");

    /**
     * The resize video button.
     */
    public static final ImageID SD_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.SD_VIDEO_BUTTON");

    /**
     * The resize video button.
     */
    public static final ImageID LO_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.LO_VIDEO_BUTTON");

    /**
     * A call-transfer button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID TRANSFER_CALL_BUTTON
        = new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON");

    /**
     * A call-park button icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID PARK_CALL_BUTTON
        = new ImageID("service.gui.buttons.PARK_CALL_BUTTON");

    /**
     * The secure button on icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_BUTTON_ON
        = new ImageID("service.gui.buttons.SECURE_BUTTON_ON");

    /**
     * The secure button off icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_BUTTON_OFF
        = new ImageID("service.gui.buttons.SECURE_BUTTON_OFF");

    /**
     * The secure button pending icon.
     */
    public static final ImageID SECURE_BUTTON_PENDING
        = new ImageID("service.gui.buttons.SECURE_BUTTON_PENDING");

    /**
     * The conference secure button off icon.
     */
    public static final ImageID SECURE_OFF_CONF_CALL
        = new ImageID("service.gui.buttons.SECURE_OFF_CONF_CALL");

    /**
     * The secure button on icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_AUDIO_ON
        = new ImageID("service.gui.buttons.SECURE_AUDIO_ON");

    /**
     * The secure button off icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_AUDIO_OFF =
        new ImageID("service.gui.buttons.SECURE_AUDIO_OFF");

    /**
     * The secure button on icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_VIDEO_ON =
        new ImageID("service.gui.buttons.SECURE_VIDEO_ON");

    /**
     * The secure button off icon. The icon shown in the CallPeer panel.
     */
    public static final ImageID SECURE_VIDEO_OFF =
        new ImageID("service.gui.buttons.SECURE_VIDEO_OFF");

    /**
     * The security button: encrypted and SAS verified, encrypted only,
     * security off.
     */
    public static final ImageID ENCR_VERIFIED
        = new ImageID("service.gui.buttons.ENCR_VERIFIED");

    /**
     * The button icon of the Enter Full Screen command. The icon shown in the
     * CallPeer panel.
     */
    public static final ImageID ENTER_FULL_SCREEN_BUTTON =
        new ImageID("service.gui.buttons.ENTER_FULL_SCREEN_BUTTON");

    /**
     * The button icon of the Exit Full Screen command. The icon shown in the
     * CallPeer panel.
     */
    public static final ImageID EXIT_FULL_SCREEN_BUTTON =
        new ImageID("service.gui.buttons.EXIT_FULL_SCREEN_BUTTON");

    /**
     * The call information button icon used in the call panel.
     */
    public static final ImageID CALL_INFO =
        new ImageID("service.gui.buttons.CALL_INFO");

    /**
     * The call information button icon used in the call panel.
     */
    public static final ImageID CRM =
        new ImageID("service.gui.buttons.CRM");

    /**
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO
        = new ImageID("service.gui.DEFAULT_USER_PHOTO");

    /**
     * The image used, when a contact is unauthorized.
     */
    public static final ImageID UNAUTHORIZED_CONTACT_PHOTO
        = new ImageID("service.gui.icons.UNAUTHORIZED_CONTACT_PHOTO");

    /**
     * Re-request authorization menu item icon.
     */
    public static final ImageID UNAUTHORIZED_CONTACT_16x16
        = new ImageID("service.gui.icons.UNAUTHORIZED_CONTACT_16x16");

    /**
     * The icon image of the "Add contact to chat" button in the
     * chat window.
     */
    public static final ImageID ADD_TO_CHAT_ICON
        = new ImageID("service.gui.icons.ADD_TO_CHAT_ICON");

    /**
     * The image used for decoration of the "Add group" window.
     */
    public static final ImageID ADD_GROUP_ICON
        = new ImageID("service.gui.icons.ADD_GROUP_ICON");

    /**
     * The image used for decoration of the "Rename contact" window.
     */
    public static final ImageID RENAME_DIALOG_ICON
        = new ImageID("service.gui.icons.RENAME_DIALOG_ICON");

    /**
     * The image used for decoration of the "Open in browser" item in
     * the right button click menu in chat window.
     */
    public static final ImageID BROWSER_ICON
        = new ImageID("service.gui.icons.BROWSER_ICON");

    /**
     * The image used for decoration of all windows concerning the process of
     * authorization.
     */
    public static final ImageID AUTHORIZATION_ICON
        = new ImageID("service.gui.icons.AUTHORIZATION_ICON");

    /**
     * The image used in the right button menu for the move contact item.
     */
    public static final ImageID MOVE_CONTACT_ICON
        = new ImageID("service.gui.icons.MOVE_CONTACT");

    /**
     * The image used in the right button menu for the move to group item.
     */
    public static final ImageID MOVE_TO_GROUP_16x16_ICON
        = new ImageID("service.gui.icons.MOVE_TO_GROUP_16x16_ICON");

    /**
     * The image used for error messages in the chat window.
     */
    public static final ImageID EXCLAMATION_MARK
        = new ImageID("service.gui.icons.EXCLAMATION_MARK");

    /**
     * The image used for opened groups.
     */
    public static final ImageID OPENED_GROUP_ICON
        = new ImageID("service.gui.icons.OPENED_GROUP");

    /**
     * The image used for closed groups.
     */
    public static final ImageID CLOSED_GROUP_ICON
        = new ImageID("service.gui.icons.CLOSED_GROUP");

    /**
     * The image used for chat rooms presence status.
     */
    public static final ImageID CHAT_ROOM_OFFLINE_ICON
        = new ImageID("service.gui.icons.CHAT_ROOM_ICON");

    /**
     * The image used for chat rooms presence status.
     */
    public static final ImageID CHAT_ROOM_MENU_ICON
        = new ImageID("service.gui.icons.CHAT_ROOM_16x16_ICON");

    /**
     * The image used for joined chat rooms.
     */
    public static final ImageID CHAT_ROOM_ONLINE_ICON
        = new ImageID("service.gui.icons.CHAT_ROOM_JOINED_ICON");

    /**
     * The image used for multi user chat servers.
     */
    public static final ImageID CHAT_SERVER_16x16_ICON
        = new ImageID("service.gui.icons.CHAT_SERVER_16x16_ICON");

    /**
     * The image used to indicate in the contact list that a message is received
     * from a certain contact.
     */
    public static final ImageID MESSAGE_RECEIVED_ICON
        = new ImageID("service.gui.icons.MESSAGE_RECEIVED_ICON");

    /**
     * The image used to set to the chat room "join" right button menu.
     */
    public static final ImageID JOIN_ICON
        = new ImageID("service.gui.icons.JOIN_ICON");

    /**
     * The image used to set to the chat room "join as" right button menu.
     */
    public static final ImageID JOIN_AS_ICON
        = new ImageID("service.gui.icons.JOIN_AS_ICON");

    /**
     * The image can be used for message source contact presence status.
     */
    public static final ImageID MSG_SRC_CONTACT_ONLINE_ICON
        = new ImageID("service.gui.icons.SMS_STATUS_ICON");

    /**
     * The image used to set to the chat room "leave" right button menu.
     */
    public static final ImageID LEAVE_ICON
        = new ImageID("service.gui.icons.LEAVE_ICON");

    /**
     * The image used to set to the chat room "remove" right button menu.
     */
    public static final ImageID REMOVE_CHAT_ICON
        = new ImageID("service.gui.icons.REMOVE_CHAT_ICON");

    /**
     * Background image of the dial button.
     */
    public static final ImageID DIAL_BUTTON_BG
        = new ImageID("service.gui.buttons.DIAL_BUTTON_BG");

    /**
     * Main menu background image.
     */
    public static final ImageID MENU_BACKGROUND
        = new ImageID("service.gui.MENU_BACKGROUND");

    /**
     * Title bar background image.
     */
    public static final ImageID WINDOW_TITLE_BAR
        = new ImageID("service.gui.WINDOW_TITLE_BAR");

    /**
     * Title bar background image.
     */
    public static final ImageID WINDOW_TITLE_BAR_BG
        = new ImageID("service.gui.WINDOW_TITLE_BAR_BG");

    /**
     * The default icon used in file transfer ui.
     */
    public static final ImageID DEFAULT_FILE_ICON
        = new ImageID("service.gui.icons.DEFAULT_FILE_ICON");

    /**
     * The tools icon shown in conference calls.
     */
    public static final ImageID CALL_PEER_TOOLS
        = new ImageID("service.gui.buttons.CALL_PEER_TOOLS");

    /**
     * The icon used for the chat room configuration button.
     */
    public static final ImageID CHAT_ROOM_CONFIG
        = new ImageID("service.gui.buttons.CHAT_ROOM_CONFIG");

    /**
     * The icon used for the chat room configuration button.
     */
    public static final ImageID CHAT_ROOM_MEMBERS_LIST_CONFIG
        = new ImageID("service.gui.buttons.CHAT_ROOM_MEMBERS_LIST_CONFIG");

    /**
     * The video call menu item icon.
     */
    public static final ImageID VIDEO_CALL
        = new ImageID("service.gui.icons.VIDEO_CALL_16x16_ICON");

    /**
     * The desktop sharing menu item icon.
     */
    public static final ImageID DESKTOP_SHARING
        = new ImageID("service.gui.icons.DESKTOP_SHARING_16x16_ICON");

    /**
     * The desktop sharing menu item icon.
     */
    public static final ImageID REGION_DESKTOP_SHARING
        = new ImageID("service.gui.icons.REGION_SHARING_16x16_ICON");

    /**
     * The volume control button icon.
     */
    public static final ImageID VOLUME_CONTROL_BUTTON
        = new ImageID("service.gui.buttons.VOLUME_CONTROL");

    /**
     * The dial button shown in contact list.
     */
    public static final ImageID CONTACT_LIST_DIAL_BUTTON
        = new ImageID("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON");

    /**
     * The dial pad call button background.
     */
    public static final ImageID DIAL_PAD_CALL_BUTTON_BG
        = new ImageID("service.gui.buttons.DIAL_PAD_CALL_BUTTON_BG");

    /**
     * The conference video indicator.
     */
    public static final ImageID CONFERENCE_VIDEO_INDICATOR
        = new ImageID("service.gui.icons.CONFERENCE_VIDEO_INDICATOR");

    /*
     * =======================================================================
     * ------------------------ EDIT TOOLBAR ICONS ---------------------------
     * =======================================================================
     */
    /**
     * Add not in contact list contact icon.
     */
    public static final ImageID ADD_CONTACT_CHAT_ICON
        = new ImageID("service.gui.icons.ADD_CONTACT_CHAT_ICON");

    /**
     * The icon shown in the invite dialog.
     */
    public static final ImageID INVITE_DIALOG_ICON
        = new ImageID("service.gui.icons.INVITE_DIALOG_ICON");

    /**
     * The icon shown in the invite dialog.
     */
    public static final ImageID CLOSE_VIDEO
        = new ImageID("service.gui.buttons.CLOSE_VIDEO");

    /*
     * =======================================================================
     * ------------------------ MAIN TOOLBAR ICONS ---------------------------
     * =======================================================================
     */
    /**
     * The background image of a button in one of the <tt>ChatWindow</tt>
     * toolbars.
     */
    public static final ImageID CHAT_TOOLBAR_BUTTON_BG
        = new ImageID("MSG_TOOLBAR_BUTTON_BG");

    /**
     * Copy icon.
     */
    public static final ImageID COPY_ICON
        = new ImageID("service.gui.icons.COPY_ICON");

    /**
     * Cut icon.
     */
    public static final ImageID CUT_ICON
        = new ImageID("service.gui.icons.CUT_ICON");

    /**
     * Paste icon.
     */
    public static final ImageID PASTE_ICON
        = new ImageID("service.gui.icons.PASTE_ICON");

    /**
     * Smiley icon, used for the "Smiley" button in the <tt>MainToolBar</tt>.
     */
    public static final ImageID SMILIES_ICON
        = new ImageID("service.gui.icons.SMILIES_ICON");

    /**
     * Close icon.
     */
    public static final ImageID CLOSE_ICON
        = new ImageID("service.gui.icons.CLOSE_ICON");

    /**
     * Left flash icon.
     */
    public static final ImageID PREVIOUS_ICON
        = new ImageID("service.gui.icons.PREVIOUS_ICON");

    /**
     * Right flash icon.
     */
    public static final ImageID NEXT_ICON
        = new ImageID("service.gui.icons.NEXT_ICON");

    /**
     * Clock icon.
     */
    public static final ImageID HISTORY_ICON
        = new ImageID("service.gui.icons.HISTORY_ICON");

    /**
     * Clock icon on.
     */
    public static final ImageID HISTORY_ICON_ON
        = new ImageID("service.gui.icons.HISTORY_ICON_ON");

    /**
     * Clock icon off.
     */
    public static final ImageID HISTORY_ICON_OFF
        = new ImageID("service.gui.icons.HISTORY_ICON_OFF");

    /**
     * Send file icon.
     */
    public static final ImageID SEND_FILE_ICON
        = new ImageID("service.gui.icons.SEND_FILE_ICON");

    /**
     * Font icon.
     */
    public static final ImageID FONT_ICON
        = new ImageID("service.gui.icons.FONT_ICON");

    /**
     * Invite to conference icon
     */
    public static final ImageID CONFERENCE_ICON
        = new ImageID("service.gui.icons.CONFERENCE_CALL");

    /*
     * =======================================================================
     * ------------------------ CHAT CONTACT ICONS ---------------------------
     * =======================================================================
     */
    public static final ImageID CHAT_CONFIGURE_ICON
        = new ImageID("service.gui.icons.CHAT_CONFIGURE_ICON");

    /*
     * =======================================================================
     * ------------------------- 16x16 ICONS ---------------------------------
     * =======================================================================
     */
    /**
     * Send message 16x16 image.
     */
    public static final ImageID SEND_MESSAGE_16x16_ICON
        = new ImageID("service.gui.icons.SEND_MESSAGE_16x16_ICON");

    /**
     * Call 16x16 image.
     * //TODO : change to an appropriate logo
     */
    public static final ImageID CALL_16x16_ICON
            = new ImageID("service.gui.icons.CALL_16x16_ICON");

    /**
     * Delete 16x16 image.
     */
    public static final ImageID DELETE_16x16_ICON
        = new ImageID("service.gui.icons.DELETE_16x16_ICON");

    /**
     * History 16x16 image.
     */
    public static final ImageID HISTORY_16x16_ICON
        = new ImageID("service.gui.icons.HISTORY_16x16_ICON");

    /**
     * Send file 16x16 image.
     */
    public static final ImageID SEND_FILE_16x16_ICON
        = new ImageID("service.gui.icons.SEND_FILE_16x16_ICON");

    /**
     * Groups 16x16 image.
     */
    public static final ImageID GROUPS_16x16_ICON
        = new ImageID("service.gui.icons.GROUPS_16x16_ICON");

    /**
     * Add contact 16x16 image.
     */
    public static final ImageID ADD_CONTACT_16x16_ICON
        = new ImageID("service.gui.icons.ADD_CONTACT_16x16_ICON");

    /**
     * Quit 16x16 image.
     */
    public static final ImageID QUIT_16x16_ICON
        = new ImageID("service.gui.icons.QUIT_16x16_ICON");

    /**
     * Rename 16x16 image.
     */
    public static final ImageID RENAME_16x16_ICON
        = new ImageID("service.gui.icons.RENAME_16x16_ICON");

    /**
     * Rename 16x16 image.
     */
    public static final ImageID CHAT_ROOM_REVOKE_VOICE
        = new ImageID("service.gui.icons.CHAT_ROOM_REVOKE_VOICE");

    /**
     * Toolbar drag area icon.
     */
    public static final ImageID TOOLBAR_DRAG_ICON = new ImageID(
            "service.gui.icons.TOOLBAR_DRAG_ICON");

    /**
     * The icon used to indicate a search.
     */
    public static final ImageID SEARCH_ICON
        = new ImageID("service.gui.icons.SEARCH_ICON");

    /**
     * The icon used to indicate a search.
     */
    public static final ImageID SEARCH_ICON_16x16
        = new ImageID("service.gui.icons.SEARCH_ICON_16x16");

    /*
     * =======================================================================
     * ------------------------ USERS' ICONS ---------------------------------
     * =======================================================================
     */

    /**
     * Contact "online" icon.
     */
    public static final ImageID USER_ONLINE_ICON
        = new ImageID("service.gui.statusicons.USER_ONLINE_ICON");

    /**
     * Contact "offline" icon.
     */
    public static final ImageID USER_OFFLINE_ICON
        = new ImageID("service.gui.statusicons.USER_OFFLINE_ICON");

    /**
     * Contact "away" icon.
     */
    public static final ImageID USER_AWAY_ICON
        = new ImageID("service.gui.statusicons.USER_AWAY_ICON");

    /**
     * Contact "away" icon.
     */
    public static final ImageID USER_EXTENDED_AWAY_ICON
        = new ImageID("service.gui.statusicons.USER_EXTENDED_AWAY_ICON");

    /**
     * Contact "free for chat" icon.
     */
    public static final ImageID USER_FFC_ICON
        = new ImageID("service.gui.statusicons.USER_FFC_ICON");

    /**
     * Contact "do not disturb" icon.
     */
    public static final ImageID USER_DND_ICON
        = new ImageID("service.gui.statusicons.USER_DND_ICON");

    /**
     * Contact "on the phone" icon.
     */
    public static final ImageID USER_USER_ON_THE_PHONE_ICON
        = new ImageID("service.gui.statusicons.USER_ON_THE_PHONE_ICON");

    /**
     * Contact "In a meeting" icon.
     */
    public static final ImageID USER_USER_IN_A_MEETING_ICON
        = new ImageID("service.gui.statusicons.USER_IN_MEETING_ICON");


    /**
     * Chat button "online" icon.
     */
    public static final ImageID CHAT_BUTTON_ONLINE_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_ONLINE_ICON");

    /**
     * Chat button "offline" icon.
     */
    public static final ImageID CHAT_BUTTON_OFFLINE_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_OFFLINE_ICON");

    /**
     * Chat button "away" icon.
     */
    public static final ImageID CHAT_BUTTON_AWAY_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_AWAY_ICON");

    /**
     * Chat button "away" icon.
     */
    public static final ImageID CHAT_BUTTON_EXTENDED_AWAY_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_EXTENDED_AWAY_ICON");

    /**
     * Chat button "free for chat" icon.
     */
    public static final ImageID CHAT_BUTTON_FFC_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_FFC_ICON");

    /**
     * Chat button "do not disturb" icon.
     */
    public static final ImageID CHAT_BUTTON_DND_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_DND_ICON");

    /**
     * Chat button "on the phone" icon.
     */
    public static final ImageID CHAT_BUTTON_ON_THE_PHONE_ICON
        = new ImageID("service.gui.buttons.CHAT_BUTTON_ON_THE_PHONE_ICON");

   /**
    * Owner chatroom member.
    */
    public static final ImageID CHATROOM_MEMBER_OWNER
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_OWNER");

    /**
     * Admin chatroom member.
     */
    public static final ImageID CHATROOM_MEMBER_ADMIN
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_ADMIN");

    /**
     * Moderator chatroom member.
     */
    public static final ImageID CHATROOM_MEMBER_MODERATOR
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_MODERATOR");

    /**
     * Standard chatroom member.
     */
    public static final ImageID CHATROOM_MEMBER_STANDARD
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_STANDARD");

    /**
     * Guest chatroom member.
     */
    public static final ImageID CHATROOM_MEMBER_GUEST
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_GUEST");

    /**
     * Silent chatroom member.
     */
    public static final ImageID CHATROOM_MEMBER_SILENT
        = new ImageID("service.gui.icons.CHATROOM_MEMBER_SILENT");

    /**
     * Change room icon.
     */
    public static final ImageID CHANGE_ROOM_SUBJECT_ICON_16x16
        = new ImageID("service.gui.icons.CHANGE_ROOM_SUBJECT_16x16");

    /**
     * Change nickname icon
     */
    public static final ImageID CHANGE_NICKNAME_ICON_16x16
        = new ImageID("service.gui.icons.CHANGE_NICKNAME_16x16");

    /**
     * Change nickname icon
     */
    public static final ImageID CHANGE_NICKNAME_ICON
        = new ImageID("service.gui.icons.CHANGE_NICKNAME");

    /**
     * Ban icon.
     */
    public static final ImageID BAN_ICON_16x16
        = new ImageID("service.gui.icons.BAN_16x16");

    /**
     * Kick icon.
     */
    public static final ImageID KICK_ICON_16x16
        = new ImageID("service.gui.icons.KICK_16x16");

    public static final ImageID MICROPHONE
        = new ImageID("service.gui.soundlevel.MICROPHONE");

    public static final ImageID HEADPHONE
        = new ImageID("service.gui.soundlevel.HEADPHONE");

    public static final ImageID SOUND_SETTING_BUTTON_BG
        = new ImageID("service.gui.soundlevel.SOUND_SETTING_BUTTON_BG");

    public static final ImageID SOUND_SETTING_BUTTON_PRESSED
        = new ImageID("service.gui.soundlevel.SOUND_SETTING_BUTTON_PRESSED");

    public static final ImageID AUTO_ANSWER_CHECK
                = new ImageID("service.gui.icons.AUTO_ANSWER_CHECK");

    /**
     * Returns the imageLoaderService instance, if missing query osgi for it.
     * @return the imageLoaderService.
     */
    private static ImageLoaderServiceImpl getImageLoaderService()
    {
        if(imageLoaderService == null)
        {
            imageLoaderService = (ImageLoaderServiceImpl)
                ServiceUtils.getService(
                    GuiActivator.bundleContext,
                    ImageLoaderService.class);
        }

        return imageLoaderService;
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImage getImage(ImageID imageID)
    {
        return getImageLoaderService().getImage(imageID);
    }

    /**
     * Returns the URI corresponding to the image with the given image
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the URI corresponding to the image with the given image
     * identifier
     */
    public static String getImageUri(ImageID imageID)
    {
        URL imageURL = GuiActivator.getResources().getImageURL(imageID.getId());

        try
        {
            if (imageURL != null)
                return imageURL.toURI().toString();
        }
        catch (URISyntaxException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to parse image URL to URI.", e);
        }

        return null;
    }

    /**
     * Obtains the indexed status image for the given protocol provider.
     *
     * @param pps the protocol provider for which to create the image
     *
     * @return the indexed status image
     */
    public static ImageIcon getAccountStatusImage(ProtocolProviderService pps)
    {
        OperationSetPresence presence
            = pps.getOperationSet(OperationSetPresence.class);
        byte[] protocolStatusIcon
            = (presence == null)
                ? null
                : presence.getPresenceStatus().getStatusIcon();
        Image statusImage;

        if (presence != null && protocolStatusIcon != null)
        {
            statusImage = ImageUtils.getBytesInImage(protocolStatusIcon);
        }
        else
        {
            byte[] bytes
                = pps.getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16);

            statusImage
                = (bytes == null) ? null : ImageUtils.getBytesInImage(bytes);
            if (!pps.isRegistered() && (statusImage != null))
            {
                statusImage
                    = LightGrayFilter.createDisabledImage(statusImage);
            }
        }

        return
            (statusImage == null)
                ? null
                : new ImageIcon(getIndexedProtocolImage(statusImage, pps));
    }

    /**
     * Returns an icon for the given protocol image with an index allowing to
     * distinguish different accounts from the same protocol.
     *
     * @param image the initial image to badge with an index
     * @param pps the protocol provider service corresponding to the account,
     * containing the index.
     * @return an icon for the given protocol image with an index allowing to
     * distinguish different accounts from the same protocol.
     */
    public static ImageIcon getIndexedProtocolIcon( Image image,
                                                    ProtocolProviderService pps)
    {
        return new ImageIcon(getIndexedProtocolImage(image, pps));
    }

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param image the initial image to badge with an index
     * @param pps the protocol provider service corresponding to the account,
     * containing the index.
     * @return the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     */
    public static Image getIndexedProtocolImage(
            Image image, ProtocolProviderService pps)
    {
        int index
            = GuiActivator.getUIService().getMainFrame().getProviderIndex(pps);
        Image badged;

        if (index > 0)
        {
            BufferedImage buffImage =
                new BufferedImage(22, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) buffImage.getGraphics();

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(image, 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.drawString(Integer.toString(index + 1), 14, 8);

            badged = buffImage;
        }
        else
            badged = image;
        return badged;
    }

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param bgImage the background image
     * @param topImage the image that should be painted on the top of the
     * background image
     * @param x the x coordinate of the top image
     * @param y the y coordinate of the top image
     * @return the result merged image
     */
    public static Image getImage(Image bgImage, Image topImage, int x, int y)
    {
        int width = (x + topImage.getWidth(null) > bgImage.getWidth(null))
                        ? x + topImage.getWidth(null)
                        : bgImage.getWidth(null);

        int height = (y + topImage.getHeight(null) > bgImage.getHeight(null))
                        ? y + topImage.getHeight(null)
                        : bgImage.getHeight(null);

        BufferedImage buffImage
            = new BufferedImage(width,
                                height,
                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImage.getGraphics();

        AntialiasingManager.activateAntialiasing(g);
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(topImage, x, y, null);

        return buffImage;
    }

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param bgImage the background image
     * @param text the text that should be painted on the top of the
     * background image
     * @return the result merged image
     */
    public static Image getImage(Image bgImage, String text, Component c)
    {
        BufferedImage buffImage
            = new BufferedImage(bgImage.getWidth(c),
                                bgImage.getHeight(c),
                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImage.getGraphics();

        AntialiasingManager.activateAntialiasing(g);
        g.setColor(Color.WHITE);
        g.setFont(c.getFont().deriveFont(Font.BOLD, 9));
        g.drawImage(bgImage, 0, 0, null);

        FontMetrics fontMetrics = g.getFontMetrics();
        int fontHeight = fontMetrics.getHeight();
        int textWidth = fontMetrics.stringWidth(text);

        g.drawString(
                text,
                (bgImage.getWidth(null) - textWidth)/2 + 1,
                (bgImage.getHeight(null) - fontHeight)/2 + fontHeight - 3);

        return buffImage;
    }

    /**
     * Loads an image icon from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIcon getImageForPath(String imagePath)
    {
        InputStream is = null;

        try
        {
            // try to load path it maybe valid url
            is = new URL(imagePath).openStream();
        }
        catch (Exception e)
        {}

        if(is == null)
            is = GuiActivator.getResources()
                    .getImageInputStreamForPath(imagePath);

        // If we didn't find the icon corresponding to the given path, we have
        // nothing more to do here.
        if (is == null)
            return null;

        byte[] icon = null;
        try
        {
            icon = new byte[is.available()];
            is.read(icon);
        }
        catch (IOException e)
        {
            logger.error("Failed to load icon: " + imagePath, e);
        }
        return new ImageIcon(icon);
    }

    /**
     * Clears the images cache.
     */
    public static void clearCache()
    {
        getImageLoaderService().clearCache();
    }
}
