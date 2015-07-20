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
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.impl.gui.*;

/**
 * Defines the CSS style of an incoming chat message elements.
 *
 * @author Yana Stamcheva
 */
public class IncomingMessageStyle
{
    /**
     * The incoming message background image path.
     */
    private final static String INCOMING_MESSAGE_IMAGE_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_BACKGROUND").toString();

    /**
     * The incoming message right image path.
     */
    private final static String INCOMING_MESSAGE_IMAGE_RIGHT_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_BACKGROUND_RIGHT")
                .toString();

    /**
     * The incoming message indicator image path.
     */
    private final static String INCOMING_MESSAGE_INDICATOR_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_INDICATOR").toString();

    /**
     * The incoming message round border image path.
     */
    private final static String INCOMING_MESSAGE_CURVES_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_CURVES").toString();

    /**
     * The incoming message top image path.
     */
    private final static String INCOMING_MESSAGE_CURVES_TOP_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_CURVES_TOP").toString();

    /**
     * The foreground color of history messages.
     */
    private final static String HISTORY_FOREGROUND_COLOR
        = GuiActivator.getResources().getColorString(
            "service.gui.CHAT_HISTORY_FOREGROUND");

    /**
     * Creates the global message style.
     *
     * @return the style attribute defining the global message style.
     */
    public static String createMessageStyle()
    {
        return "style=\""
                + "width:100%;"
                + "opacity:0.96;"
                + "\"";
    }

    public static String createSingleMessageStyle(  boolean isHistory,
                                                    boolean isEdited,
                                                    boolean isSimpleTheme)
    {
        StringBuffer styleBuff = new StringBuffer();

        if (isSimpleTheme)
            styleBuff.append("style=\"");
        else
            styleBuff.append("style=\"padding-left:10px;");

        if (isEdited)
        {
            styleBuff.append("font-style:italic;");

            if (isHistory)
                styleBuff.append("color:#" + HISTORY_FOREGROUND_COLOR + ";\"");
            else
                styleBuff.append("\"");
        }
        else
        {
            if (isHistory)
                styleBuff.append("color:#" + HISTORY_FOREGROUND_COLOR + ";\"");
            else
                styleBuff.append("\"");
        }

        return styleBuff.toString().replaceFirst(";\"", "\"");
    }

    /**
     * Creates the style of the table bubble right element.
     *
     * @return the style of the table bubble right element
     */
    public static String createTableBubbleMessageRightStyle()
    {
        return "style=\""
                + "width:8px;"
                + " background-image: url('"
                    +INCOMING_MESSAGE_IMAGE_RIGHT_PATH+"');"
                + " background-repeat: repeat-y;"
                + " background-position: top left;"
                + "\"";
    }

    /**
     * Creates the style of the table bubble (wrapping the message table).
     *
     * @return the style of the table bubble
     */
    public static String createTableBubbleStyle()
    {
        return "style=\""
                + "width:100%;"
                + " position:relative;"
                + "\"";
    }

    /**
     * Creates the style of the message table bubble.
     *
     * @return the style of the message table bubble
     */
    public static String createTableBubbleMessageStyle()
    {
        return "style=\""
                + "font-size:10px;"
                + " background-image: url('"+INCOMING_MESSAGE_IMAGE_PATH+"');"
                + " background-repeat: repeat-y;"
                + " background-position: top left;"
                + "\"";
    }

    /**
     * Creates the style of the table buuble bottom left corner.
     *
     * @return the style of the table buuble bottom left corner
     */
    public static String createTableBubbleBlStyle()
    {
        return "style=\""
                + "height:10px;"
                + " background-image: url('"+INCOMING_MESSAGE_CURVES_PATH+"');"
                + " background-repeat: no-repeat;"
                + " background-position: 0px -20px;"
                + "\"";
    }

    /**
     * Creates the style of the table buuble bottom right corner.
     *
     * @return the style of the table buuble bottom right corner
     */
    public static String createTableBubbleBrStyle()
    {
        return "style=\""
                + "width:8px;"
                + " height:10px;"
                + " background-image: url('"+INCOMING_MESSAGE_CURVES_PATH+"');"
                + " background-repeat: no-repeat;"
                + " background-position: -2999px -20px;"
                + "\"";
    }

    /**
     * Creates the style of the table buuble top left corner.
     *
     * @return the style of the table buuble top left corner
     */
    public static String createTableBubbleTlStyle()
    {
        return "style=\""
                + "height:23px;"
                + " background-image: url('"
                    +INCOMING_MESSAGE_CURVES_TOP_PATH+"');"
                + " background-repeat: no-repeat;"
                + " background-position: top left;"
                + "\"";
    }

    /**
     * Creates the style of the table buuble top right corner.
     *
     * @return the style of the table buuble top right corner
     */
    public static String createTableBubbleTrStyle()
    {
        return "style=\""
                + "width:6px;"
                + " height:23px;"
                + " background-image: url('"
                    +INCOMING_MESSAGE_CURVES_TOP_PATH+"');"
                + " background-repeat: no-repeat;"
                + " background-position: -2999px 0px;"
                + "\"";
    }

    /**
     * Creates the style of the indicator pointing to the avatar image.
     *
     * @return the style of the indicator pointing to the avatar image
     */
    public static String createIndicatorStyle()
    {
        return "style =\""
                + "width:9px;"
                + " height:19px;"
                + " background-image: url('"
                + INCOMING_MESSAGE_INDICATOR_PATH+"');"
                + " background-repeat: no-repeat;"
                + " background-position: top right;"
                + "\"";
    }

    /**
     * Creates the style of the avatar image.
     *
     * @return the style of the avatar image
     */
    public static String createAvatarStyle()
    {
        return "style=\"width:26px;"
                + " height:26px;"
                + " float:left;\"";
    }

    /**
     * Creates the header style.
     *
     * @return the header style.
     */
    public static String createHeaderStyle()
    {
        return "style=\"padding-top: 4px;"
                + " padding-left: 10px;\"";
    }
}
