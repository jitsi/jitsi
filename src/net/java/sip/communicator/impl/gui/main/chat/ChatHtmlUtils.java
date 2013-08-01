/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.text.*;
import java.util.*;

import javax.swing.text.html.HTML.Tag;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ChatHtmlUtils
{
    /**
     * The name attribute.
     */
    public final static String NAME_ATTRIBUTE = "name";

    /**
     * The date attribute.
     */
    public final static String DATE_ATTRIBUTE = "date";

    /**
     * The name of the attribute containing the original chat message before
     * processing replacements.
     */
    public final static String ORIGINAL_MESSAGE_ATTRIBUTE = "original_message";

    /**
     * The message header identifier attribute.
     */
    public final static String MESSAGE_HEADER_ID = "messageHeader";

    /**
     * The message identifier attribute.
     */
    public final static String MESSAGE_TEXT_ID = "message";

    /**
     * The closing tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String END_PLAINTEXT_TAG = "</PLAINTEXT>";

    /**
     * The opening tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String START_PLAINTEXT_TAG = "<PLAINTEXT>";

    /**
     * The html text content type.
     */
    public static final String HTML_CONTENT_TYPE = "text/html";

    /**
     * The plain text content type.
     */
    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * The color used in html for name background.
     */
    public static final String MSG_NAME_BACKGROUND = "#efefef";

    /**
     * The color used in html for incoming message contact name foreground.
     */
    public static final String MSG_IN_NAME_FOREGROUND = "#488fe7";


    /**
     * Creates an incoming message tag.
     *
     * @param messageID the identifier
     * @param contactName the name of the contact sending the message
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param avatarPath the path to the avatar file
     * @param date the date, when the message was sent
     * @param message the message content
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @param isSimpleTheme indicates if the simple or the advance theme should
     * be used
     * @return the created incoming message tag
     */
    public static String createIncomingMessageTag(
        String messageID,
        String contactName,
        String contactDisplayName,
        String avatarPath,
        Date date,
        String message,
        String contentType,
        boolean isHistory,
        boolean isSimpleTheme)
    {
        if (isSimpleTheme)
            return createSimpleIncomingMessageTag(  messageID,
                                                    contactName,
                                                    contactDisplayName,
                                                    avatarPath,
                                                    date,
                                                    message,
                                                    contentType,
                                                    isHistory);
        else
            return createAdvancedIncomingMessageTag(messageID,
                                                    contactName,
                                                    contactDisplayName,
                                                    avatarPath,
                                                    date,
                                                    message,
                                                    contentType,
                                                    isHistory);
    }

    /**
     * Create an outgoing message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the account sending the message
     * @param contactDisplayName the display name of the account sending the
     * message
     * @param avatarPath the path to the avatar image
     * @param date the date, when the message was sent
     * @param message the content of the message
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @param isSimpleTheme indicates if the simple or the advance theme should
     * be used
     * @return the created outgoing message tag
     */
    public static String createOutgoingMessageTag(  String messageID,
                                                    String contactName,
                                                    String contactDisplayName,
                                                    String avatarPath,
                                                    Date date,
                                                    String message,
                                                    String contentType,
                                                    boolean isHistory,
                                                    boolean isSimpleTheme)
    {
        if (isSimpleTheme)
            return createSimpleOutgoingMessageTag(  messageID,
                                                    contactName,
                                                    contactDisplayName,
                                                    avatarPath,
                                                    date,
                                                    message,
                                                    contentType,
                                                    isHistory);
        else
            return createAdvancedOutgoingMessageTag(messageID,
                                                    contactName,
                                                    contactDisplayName,
                                                    avatarPath,
                                                    date,
                                                    message,
                                                    contentType,
                                                    isHistory);
    }

    /**
     * Creates the message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the sender
     * @param message the message content
     * @param contentType the content type (html or plain text)
     * @param date the date on which the message was sent
     * @param isEdited indicates if the given message has been edited
     * @param isHistory indicates if this is a message coming from history
     * @param isSimpleTheme indicates if the simple or the advance theme should
     * be used
     * @return the newly constructed message tag
     */
    public static String createMessageTag( String messageID,
                                            String contactName,
                                            String message,
                                            String contentType,
                                            Date date,
                                            boolean isEdited,
                                            boolean isHistory,
                                            boolean isSimpleTheme)
    {
        if (isSimpleTheme)
            return createSimpleMessageTag(  messageID,
                                            contactName,
                                            message,
                                            contentType,
                                            date,
                                            isEdited,
                                            isHistory);
        else
            return createAdvancedMessageTag(messageID,
                                            contactName,
                                            message,
                                            contentType,
                                            date,
                                            isEdited,
                                            isHistory);
    }

    /**
     * Creates an incoming message tag.
     *
     * @param messageID the identifier
     * @param contactName the name of the contact sending the message
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param avatarPath the path to the avatar file
     * @param date the date, when the message was sent
     * @param message the message content
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @return the created incoming message tag
     */
    private static String createSimpleIncomingMessageTag(
        String messageID,
        String contactName,
        String contactDisplayName,
        String avatarPath,
        Date date,
        String message,
        String contentType,
        boolean isHistory)
    {
        StringBuffer headerBuffer = new StringBuffer();


        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        headerBuffer.append("<h2 id=\"" + MESSAGE_HEADER_ID + "\" ");
        headerBuffer.append(DATE_ATTRIBUTE + "='"
            + sdf.format(date) + "'" + ">");
        headerBuffer.append("<a style=\"color:");
        headerBuffer.append(MSG_IN_NAME_FOREGROUND).append(";");
        headerBuffer.append("font-weight:bold;");
        headerBuffer.append("text-decoration:none;\" ");
        headerBuffer.append("href=\"" + contactName + "\">");
        headerBuffer.append(
            contactDisplayName + createEditedAtTag(messageID, -1));
        headerBuffer.append("</a>");
        headerBuffer.append("</h2>");

        StringBuffer messageBuff = new StringBuffer();

        messageBuff.append("<table width=\"100%\" ");
        messageBuff.append(NAME_ATTRIBUTE + "=\""
                            +  Tag.TABLE.toString() + "\" ");
        messageBuff.append("style=\"background-color:");
        messageBuff.append(MSG_NAME_BACKGROUND).append(";\">");
        messageBuff.append("<tr>");
        messageBuff.append("<td align=\"left\" >");
        messageBuff.append(headerBuffer.toString());
        messageBuff.append("</td>");
        messageBuff.append("<td align=\"right\"><h2>");
        messageBuff.append(getDateString(date) + GuiUtils.formatTime(date));
        messageBuff.append("</h2></td>");
        messageBuff.append("</tr>");
        messageBuff.append("</table>");
        messageBuff.append(
            createSimpleMessageTag( messageID,
                                    contactName,
                                    message,
                                    contentType,
                                    date,
                                    false,
                                    isHistory));

        return messageBuff.toString();
    }

    /**
     * Create an outgoing message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the account sending the message
     * @param contactDisplayName the display name of the account sending the
     * message
     * @param avatarPath the path to the avatar image
     * @param date the date, when the message was sent
     * @param message the content of the message
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @return the created outgoing message tag
     */
    private static String createSimpleOutgoingMessageTag( String messageID,
                                                    String contactName,
                                                    String contactDisplayName,
                                                    String avatarPath,
                                                    Date date,
                                                    String message,
                                                    String contentType,
                                                    boolean isHistory)
    {
        StringBuffer headerBuffer = new StringBuffer();


        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        headerBuffer.append("<h3 id=\"" + MESSAGE_HEADER_ID + "\" ");
        headerBuffer.append(DATE_ATTRIBUTE + "='"
            + sdf.format(date) + "'" + ">");
        headerBuffer.append("<a style=\"color:#535353;");
        headerBuffer.append("font-weight:bold;");
        headerBuffer.append("text-decoration:none;\" ");
        headerBuffer.append("href=\"" + contactName + "\">");
        headerBuffer.append(contactDisplayName
                + createEditedAtTag(messageID, -1));
        headerBuffer.append("</a>");
        headerBuffer.append("</h3>");

        StringBuffer messageBuff = new StringBuffer();

        messageBuff.append("<table width=\"100%\" ");
        messageBuff.append(NAME_ATTRIBUTE + "=\""
                            +  Tag.TABLE.toString() + "\" ");
        messageBuff.append("style=\"background-color:");
        messageBuff.append(MSG_NAME_BACKGROUND).append(";\">");
        messageBuff.append("<tr>");
        messageBuff.append("<td align=\"left\" >");
        messageBuff.append(headerBuffer.toString());
        messageBuff.append("</td>");
        messageBuff.append("<td align=\"right\"><h3>");
        messageBuff.append(getDateString(date) + GuiUtils.formatTime(date));
        messageBuff.append("</h3></td>");
        messageBuff.append("</tr>");
        messageBuff.append("</table>");
        messageBuff.append(
            createSimpleMessageTag( messageID,
                                    contactName,
                                    message,
                                    contentType,
                                    date,
                                    false,
                                    isHistory));

        return messageBuff.toString();
    }

    /**
     * Creates an incoming message tag.
     *
     * @param messageID the identifier
     * @param contactName the name of the contact sending the message
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param avatarPath the path to the avatar file
     * @param date the date, when the message was sent
     * @param message the message content
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @return the created incoming message tag
     */
    private static String createAdvancedIncomingMessageTag(
        String messageID,
        String contactName,
        String contactDisplayName,
        String avatarPath,
        Date date,
        String message,
        String contentType,
        boolean isHistory)
    {
        StringBuffer headerBuffer = new StringBuffer();

        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        headerBuffer.append("<h2 id=\"" + MESSAGE_HEADER_ID + "\" ");
        headerBuffer.append(DATE_ATTRIBUTE + "='" + sdf.format(date) + "' ");
        headerBuffer.append(IncomingMessageStyle.createHeaderStyle() + ">");
        headerBuffer.append("<a style=\"color:");
        headerBuffer.append(MSG_IN_NAME_FOREGROUND).append(";");
        headerBuffer.append("font-weight:bold;");
        headerBuffer.append("text-decoration:none;\" ");
        headerBuffer.append("href=\"" + contactName + "\">");
        headerBuffer.append(
            contactDisplayName + createEditedAtTag(messageID, -1));
        headerBuffer.append("</a></h2>");

        StringBuffer messageBuff = new StringBuffer();

        messageBuff.append("<table width=\"100%\" ");
        messageBuff.append(NAME_ATTRIBUTE + "=\""
            +  Tag.TABLE.toString() + "\">");
        messageBuff.append("<tr>");
        messageBuff.append("<td valign=\"top\">");
        messageBuff.append(
            "<table " + IncomingMessageStyle.createTableBubbleStyle()
            + " cellspacing=\"0px\" cellpadding=\"0px\">");
        messageBuff.append("<tr>");
        messageBuff.append("<td style=\"width:26px;\"></td>");
        messageBuff.append("<td style=\"width:9px;\"></td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleTlStyle() + ">");
        messageBuff.append(
            createAdvancedMessageHeaderTag(headerBuffer.toString(), date));
        messageBuff.append("</td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleTrStyle() + "></td>");
        messageBuff.append("</tr>");

        // Third row.
        messageBuff.append("<tr>");
        messageBuff.append("<td><img src=\"" + avatarPath
            + "\" width=\"26px\" height=\"26px\"/> </td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createIndicatorStyle() +"></td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleMessageStyle() + ">");

        messageBuff.append(
            createAdvancedMessageTag(   messageID,
                                        contactName,
                                        message,
                                        contentType,
                                        date,
                                        false,
                                        isHistory));

        messageBuff.append("</td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleMessageRightStyle()
            + "></td>");
        messageBuff.append("</tr>");

        //Forth row.
        messageBuff.append("<tr>");
        messageBuff.append("<td style=\"width:26px;\"></td>");
        messageBuff.append("<td style=\"width:9px;\"></td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleBlStyle() + "></td>");
        messageBuff.append("<td "
            + IncomingMessageStyle.createTableBubbleBrStyle() + "></td>");
        messageBuff.append("</tr>");

        messageBuff.append("</table>");
        messageBuff.append("</td>");
        messageBuff.append("</tr>");
        messageBuff.append("</table>");

        return messageBuff.toString();
    }

    /**
     * Create an outgoing message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the account sending the message
     * @param contactDisplayName the display name of the account sending the
     * message
     * @param avatarPath the path to the avatar image
     * @param date the date, when the message was sent
     * @param message the content of the message
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param isHistory indicates if this is a message coming from history
     * @return the created outgoing message tag
     */
    private static String createAdvancedOutgoingMessageTag( String messageID,
                                                    String contactName,
                                                    String contactDisplayName,
                                                    String avatarPath,
                                                    Date date,
                                                    String message,
                                                    String contentType,
                                                    boolean isHistory)
    {
        StringBuffer headerBuffer = new StringBuffer();

        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        headerBuffer.append("<h3 id=\"" + MESSAGE_HEADER_ID + "\" ");
        headerBuffer.append(DATE_ATTRIBUTE + "='" + sdf.format(date) + "' ");
        headerBuffer.append(IncomingMessageStyle.createHeaderStyle() + ">");
        headerBuffer.append("<a style=\"color:#6a6868;");
        headerBuffer.append("font-weight:bold;");
        headerBuffer.append("float:left;");
        headerBuffer.append("text-decoration:none;\" ");
        headerBuffer.append("href=\"" + contactName + "\">");
        headerBuffer.append(contactDisplayName
                + createEditedAtTag(messageID, -1));
        headerBuffer.append("</a></h3>");

        StringBuffer messageBuff = new StringBuffer();

        // Construct the message.
        messageBuff.append("<table width=\"100%\" ");
        messageBuff.append(NAME_ATTRIBUTE + "=\""
                            +  Tag.TABLE.toString() + "\">");
        messageBuff.append("<tr>");
        messageBuff.append("<td valign=\"top\">");
        messageBuff.append(
            "<table " + OutgoingMessageStyle.createTableBubbleStyle()
            + " cellspacing=\"0px\" cellpadding=\"0px\">");

        // First row.
        messageBuff.append("<tr>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleTlStyle() + ">");
        messageBuff.append(
            createAdvancedMessageHeaderTag(headerBuffer.toString(), date));
        messageBuff.append("</td>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleTrStyle() + "></td>");
        messageBuff.append("<td style=\"width:9px;\"></td>");
        messageBuff.append("<td style=\"width:26px;\"></td>");
        messageBuff.append("</tr>");

        // Third row.
        messageBuff.append("<tr>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleMessageStyle() + ">");

        messageBuff.append(
            createAdvancedMessageTag(   messageID,
                                        contactName,
                                        message,
                                        contentType,
                                        date,
                                        false,
                                        isHistory));

        messageBuff.append("</td>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleMessageRightStyle()
            + "></td>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createIndicatorStyle() +"></td>");
        messageBuff.append("<td><div width=\"26px\" height=\"26px\"><img src=\""
                + avatarPath
                + "\" width=\"26px\" height=\"26px\"/></div></td>");
        messageBuff.append("</tr>");

        // Forth row.
        messageBuff.append("<tr>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleBlStyle() + "></td>");
        messageBuff.append("<td "
            + OutgoingMessageStyle.createTableBubbleBrStyle() + "></td>");
        messageBuff.append("<td style=\"width:9px;\"></td>");
        messageBuff.append("<td style=\"width:26px;\"></td>");
        messageBuff.append("</tr>");
        messageBuff.append("</table>");
        messageBuff.append("</td>");
        messageBuff.append("</tr>");
        messageBuff.append("</table>");

        return messageBuff.toString();
    }

    /**
     * Creates a message table tag, representing the message header.
     *
     * @param nameHeader the name of the header.
     * @param date the date, when the message was sent or received
     * @return the message header tag
     */
    private static String createAdvancedMessageHeaderTag(String nameHeader,
                                                        Date date)
    {
        StringBuffer messageHeader = new StringBuffer();

        messageHeader.append("<table width=\"100%\">");
        messageHeader.append("<tr>");
        messageHeader.append("<td nowrap=\"nowrap\">");
        messageHeader.append(nameHeader);
        messageHeader.append("</td>");
        messageHeader.append("<td nowrap=\"nowrap\" "
                + OutgoingMessageStyle.createDateStyle() + ">");
        messageHeader.append(getDateString(date));
        messageHeader.append(GuiUtils.formatTime(date));
        messageHeader.append("</td>");
        messageHeader.append("</tr>");
        messageHeader.append("</table>");

        return messageHeader.toString();
    }

    /**
     * Creates the start tag, which indicates that the next text would be plain
     * text.
     *
     * @param contentType the current content type
     * @return the start plaintext tag
     */
    public static String createStartPlainTextTag(String contentType)
    {
        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            return "";
        }
        else
        {
            return START_PLAINTEXT_TAG;
        }
    }

    /**
     * Creates the end tag, which indicates that the next text would be plain
     * text.
     *
     * @param contentType the current content type
     * @return the end plaintext tag
     */
    public static String createEndPlainTextTag(String contentType)
    {
        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            return "";
        }
        else
        {
            return END_PLAINTEXT_TAG;
        }
    }

    /**
     * Creates a tag that shows the last edit time of a message, in the format
     *  (Edited at ...).
     * If <tt>date < 0</tt>, returns an empty tag that serves as a placeholder
     * for future corrections of this message.
     *
     * @param messageUID The ID of the edited message.
     * @param date The date when the message was last edited, or -1 to generate
     * an empty tag.
     * @return The string representation of the tag.
     */
    public static String createEditedAtTag(String messageUID, long date)
    {
        StringBuilder res = new StringBuilder();
        // Use a <cite /> tag here as most of the other inline tags (e.g. h1-7,
        // b, i) cause different problems when used in setOuterHTML.
        res.append("<cite id='");
        res.append(messageUID);
        res.append("-editedAt'> ");
        if (date > 0)
        {
            res.append("&nbsp;");
            String contents = GuiActivator.getResources().getI18NString(
                    "service.gui.EDITED_AT",
                    new String[] { GuiUtils.formatTime(date) }
            );
            res.append(contents);
        }
        res.append("</cite>");
        return res.toString();
    }

    /**
     * Creates the message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the sender
     * @param message the message content
     * @param contentType the content type (html or plain text)
     * @param date the date on which the message was sent
     * @param isEdited indicates if the given message has been edited
     * @param isHistory indicates if this is a message coming from history
     * @return the newly constructed message tag
     */
    private static String createSimpleMessageTag(String messageID,
                                                String contactName,
                                                String message,
                                                String contentType,
                                                Date date,
                                                boolean isEdited,
                                                boolean isHistory)
    {
        StringBuilder messageTag = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        messageTag.append(String.format("<div id='%s' %s = '%s' ",
                MESSAGE_TEXT_ID + messageID, NAME_ATTRIBUTE,
                contactName));
        messageTag.append(DATE_ATTRIBUTE + "=\"" + sdf.format(date) + "\" ");
        messageTag.append(String.format("%s = '%s' ",
                ORIGINAL_MESSAGE_ATTRIBUTE, GuiUtils.escapeHTMLChars(message)));
        messageTag.append(IncomingMessageStyle
            .createSingleMessageStyle(isHistory, isEdited, true));
        messageTag.append(">");
        messageTag.append(createStartPlainTextTag(contentType));
        messageTag.append(message);
        if (isEdited)
            messageTag.append("    ");
        messageTag.append(createEndPlainTextTag(contentType));
        if (isEdited)
            messageTag.append(createEditedAt(date));
        messageTag.append("</div>");

        return messageTag.toString();
    }

    /**
     * Creates the message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the sender
     * @param message the message content
     * @param contentType the content type (html or plain text)
     * @param date the date on which the message was sent
     * @param isEdited indicates if the given message has been edited
     * @param isHistory indicates if this is a message coming from history
     * @return the newly constructed message tag
     */
    private static String createAdvancedMessageTag( String messageID,
                                                    String contactName,
                                                    String message,
                                                    String contentType,
                                                    Date date,
                                                    boolean isEdited,
                                                    boolean isHistory)
    {
        StringBuilder messageTag = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        messageTag.append(String.format("<div id='%s' %s = '%s' ",
                MESSAGE_TEXT_ID + messageID, NAME_ATTRIBUTE,
                contactName));
        messageTag.append(DATE_ATTRIBUTE + "=\"" + sdf.format(date) + "\" ");
        messageTag.append(String.format("%s = '%s' ",
                ORIGINAL_MESSAGE_ATTRIBUTE, GuiUtils.escapeHTMLChars(message)));
        messageTag.append(IncomingMessageStyle
            .createSingleMessageStyle(isHistory, isEdited, false));
        messageTag.append(">");
        messageTag.append(createStartPlainTextTag(contentType));
        messageTag.append(message);
        if (isEdited)
            messageTag.append("    ");
        messageTag.append(createEndPlainTextTag(contentType));
        if (isEdited)
            messageTag.append(createEditedAt(date));
        messageTag.append("</div>");

        return messageTag.toString();
    }

    /**
     * Returns the date string to show for the given date.
     *
     * @param date the date to format
     * @return the date string to show for the given date
     */
    public static String getDateString(Date date)
    {
        if (GuiUtils.compareDatesOnly(date, new Date()) <= 0)
        {
            StringBuffer dateStrBuf = new StringBuffer();

            GuiUtils.formatDate(date, dateStrBuf);
            dateStrBuf.append(" ");
            return dateStrBuf.toString();
        }

        return "";
    }

    /**
     * Creates the edited at string.
     *
     * @param date the date of the re-edition
     * @return the newly constructed string
     */
    private static String createEditedAt(Date date)
    {
        return "<font color=\"#b7b7b7\">(" + GuiActivator.getResources()
                    .getI18NString( "service.gui.EDITED_AT",
                                    new String[]{GuiUtils.formatTime(date)})
                + ")</font>";
    }
}
