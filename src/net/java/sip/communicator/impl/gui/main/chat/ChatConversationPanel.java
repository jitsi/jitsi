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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.Map;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTML.Attribute;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.replacers.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.directimage.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

import org.apache.commons.lang3.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.StringUtils;
import org.osgi.framework.*;

/**
 * The <tt>ChatConversationPanel</tt> is the panel, where all sent and received
 * messages appear. All data is stored in an HTML document. An external CSS file
 * is applied to the document to provide the look&feel. All smileys and link
 * strings are processed and finally replaced by corresponding images and HTML
 * links.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 * @author Danny van Heumen
 */
public class ChatConversationPanel
    extends SIPCommScrollPane
    implements  HyperlinkListener,
                MouseListener,
                ClipboardOwner,
                Skinnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatConversationPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ChatConversationPanel.class);

    /**
     * The regular expression (in the form of compiled <tt>Pattern</tt>) which
     * matches URLs for the purposed of turning them into links.
     *
     * TODO Current pattern misses tailing '/' (slash) that is sometimes
     * included in URL's. (Danny)
     *
     * TODO Current implementation misses # after ? has been encountered in URL.
     * (Danny)
     */
    private static final Pattern URL_PATTERN
        = Pattern.compile(
            "("
            + "(\\bwww\\.[^\\s<>\"]+\\.[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // wwwURL
            + "|"
            + "(\\bjitsi\\:[^\\s<>\"]+\\.[^\\s<>\"]*\\b)" // internalURL
            + "|"
            + "(\\b\\w+://[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // protocolURL
            + ")");

    /**
     * A regular expression that matches a <div> tag and its contents.
     * The opening tag is group 1, and the tag contents is group 2 when
     * a match is found.
     */
    private static final Pattern DIV_PATTERN =
            Pattern.compile("(<div[^>]*>)(.*)(</div>)", Pattern.DOTALL);

    /**
     * A regular expression for searching all pieces of plain text within a blob
     * of HTML text. <i>This expression assumes that the plain text part is
     * correctly escaped, such that there is no occurrence of the symbols &lt;
     * and &gt;.</i>
     *
     * <pre>
     * In essence this regexp pattern works as follows:
     * 1. Find all the text that isn't the start of a tag. (so all chars != '<')
     *    -> This is your actual result: textual content that is not part of a
     *    tag.
     * 2. Then, if you find a '<', find as much chars as you can until you find
     *    '>' (if it is possible at all to find a closing '>')
     *
     *    In depth explanation of 2.:
     *    The text between tags consists mainly of 2 parts:
     *
     *    A) a piece of text
     *    B) some value "between quotes"
     *
     *    So everything up to the "quote" is part of a piece of text (A). Then
     *    if we encounter a "quote" we consider the rest of the text part of the
     *    value (B) until the value section is closed with a closing "quote".
     *    (We tend to be rather greedy, so we even swallow '>' along the way
     *    looking for the closing "quote".)
     *
     *    This subpattern is allowed any number of times, until eventually the
     *    closing '>' is encountered. (Or not if the pattern is incomplete.)
     *
     * 3. And consider that 2. is optional, since it could also be that we only
     *    find plain text, which would all be captured by 1.
     * </pre>
     *
     * <p>The first group matches any piece of text outside of the &lt; and &gt;
     * brackets that define the start and end of HTML tags.</p>
     */
    static final Pattern TEXT_TO_REPLACE_PATTERN = Pattern.compile(
        "([^<]*+)(?:<(?:[^>\"]*(?:\"[^\"]*+\"?)*)*+>?)?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * List for observing text messages.
     */
    private Set<ChatLinkClickedListener> chatLinkClickedListeners =
        new HashSet<ChatLinkClickedListener>();

    /**
     * The component rendering chat conversation panel text.
     */
    private final JTextPane chatTextPane = new MyTextPane();

    /**
     * The editor kit used by the text component.
     */
    private final ChatConversationEditorKit editorKit;

    /**
     * The document used by the text component.
     */
    HTMLDocument document;

    /**
     * The parent container.
     */
    private final ChatConversationContainer chatContainer;

    /**
     * The menu shown on right button mouse click.
     */
    private ChatRightButtonMenu rightButtonMenu;

    /**
     * The currently shown href.
     */
    private String currentHref;

    /**
     * The currently shown href, is it an img element.
     */
    private boolean isCurrentHrefImg = false;

    /**
     * The copy link item, contained in the right mouse click menu.
     */
    private final JMenuItem copyLinkItem;

    /**
     * The copy link item, contained in the right mouse click menu.
     */
    private final JMenuItem configureReplacementItem;

    /**
     * The configure replacement item separator.
     */
    private final JSeparator configureReplacementSeparator = new JSeparator();

    /**
     * The open link item, contained in the right mouse click menu.
     */
    private final JMenuItem openLinkItem;

    /**
     * The right mouse click menu separator.
     */
    private final JSeparator copyLinkSeparator = new JSeparator();

    /**
     * The timestamp of the last incoming message.
     */
    private Date lastIncomingMsgTimestamp = new Date(0);

    /**
     * The timestamp of the last message.
     */
    private Date lastMessageTimestamp = new Date(0);

    /**
     * Indicates if this component is rendering a history conversation.
     */
    private final boolean isHistory;

    /**
     * The indicator which determines whether an automatic scroll to the bottom
     * of {@link #chatTextPane} is to be performed.
     */
    private boolean scrollToBottomIsPending = false;

    private String lastMessageUID = null;

    private boolean isSimpleTheme = true;

    private ShowPreviewDialog showPreview
        = new ShowPreviewDialog(ChatConversationPanel.this);

    /**
     * The implementation of the routine which scrolls {@link #chatTextPane} to
     * its bottom.
     */
    private final Runnable scrollToBottomRunnable = new Runnable()
    {
        /*
         * Implements Runnable#run().
         */
        public void run()
        {
            JScrollBar verticalScrollBar = getVerticalScrollBar();

            if (verticalScrollBar != null)
            {
                // We need to call both methods in order to be sure to scroll
                // to the bottom of the text even when the user has selected
                // something (changed the caret) or when a new tab has been
                // added or the window has been resized.
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                Document doc = chatTextPane.getDocument();
                if(doc != null)
                {
                    int pos = document.getLength();
                    if (pos >= 0 &&
                        pos <= chatTextPane.getDocument().getLength())
                    {
                        chatTextPane.setCaretPosition(pos);
                    }
                }
            }
        }
    };

    /**
     * Creates an instance of <tt>ChatConversationPanel</tt>.
     *
     * @param chatContainer The parent <tt>ChatConversationContainer</tt>.
     */
    public ChatConversationPanel(ChatConversationContainer chatContainer)
    {
        editorKit = new ChatConversationEditorKit(this);

        this.chatContainer = chatContainer;

        isHistory = (chatContainer instanceof HistoryWindow);

        this.rightButtonMenu = new ChatRightButtonMenu(this);

        this.document = (HTMLDocument) editorKit.createDefaultDocument();

        this.document.addDocumentListener(editorKit);

        this.chatTextPane.setEditorKitForContentType("text/html", editorKit);
        this.chatTextPane.setEditorKit(editorKit);
        this.chatTextPane.setEditable(false);
        this.chatTextPane.setDocument(document);
        this.chatTextPane.setDragEnabled(true);

        chatTextPane.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        Constants.loadSimpleStyle(
            document.getStyleSheet(), chatTextPane.getFont());

        this.chatTextPane.addHyperlinkListener(this);
        this.chatTextPane.addMouseListener(this);
        this.chatTextPane.setCursor(
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        this.addChatLinkClickedListener(showPreview);

        this.setWheelScrollingEnabled(true);

        this.setViewportView(chatTextPane);

        this.setBorder(null);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        ToolTipManager.sharedInstance().registerComponent(chatTextPane);

        String copyLinkString
            = GuiActivator.getResources().getI18NString("service.gui.COPY_LINK");

        copyLinkItem
            = new JMenuItem(copyLinkString,
                new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

        copyLinkItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                StringSelection stringSelection = new StringSelection(
                    currentHref);
                Clipboard clipboard = Toolkit.getDefaultToolkit()
                    .getSystemClipboard();
                clipboard.setContents(stringSelection,
                    ChatConversationPanel.this);
            }
        });

        String openLinkString
            = GuiActivator.getResources().getI18NString(
                "service.gui.OPEN_IN_BROWSER");

        openLinkItem =
            new JMenuItem(
                openLinkString,
                new ImageIcon(ImageLoader.getImage(ImageLoader.BROWSER_ICON)));

        openLinkItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GuiActivator.getBrowserLauncher().openURL(currentHref);

                // after opening the link remove the currentHref to avoid
                // clicking on the window to gain focus to open the link again
                ChatConversationPanel.this.currentHref = "";
            }
        });

        openLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.OPEN_IN_BROWSER"));

        copyLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.COPY_LINK"));

        configureReplacementItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(
                "plugin.chatconfig.replacement.CONFIGURE_REPLACEMENT"),
            GuiActivator.getResources().getImage(
                "service.gui.icons.CONFIGURE_ICON"));

        configureReplacementItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                final ConfigurationContainer configContainer
                    = GuiActivator.getUIService().getConfigurationContainer();

                ConfigurationForm chatConfigForm = getChatConfigForm();

                if (chatConfigForm != null)
                {
                    configContainer.setSelected(chatConfigForm);

                    configContainer.setVisible(true);
                }
            }
        });

        this.isSimpleTheme = ConfigurationUtils.isChatSimpleThemeEnabled();

        /*
         * When we append a new message (regardless of whether it is a string or
         * an UI component), we want to make it visible in the viewport of this
         * JScrollPane so that the user can see it.
         */
        ComponentListener componentListener = new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                synchronized (scrollToBottomRunnable)
                {
                    if (!scrollToBottomIsPending)
                        return;
                    scrollToBottomIsPending = false;

                    /*
                     * Yana Stamcheva, pointed out that Java 5 (on Linux only?)
                     * needs invokeLater for JScrollBar.
                     */
                    SwingUtilities.invokeLater(scrollToBottomRunnable);
                }
            }
        };

        chatTextPane.addComponentListener(componentListener);
        getViewport().addComponentListener(componentListener);
    }

    /**
     * Overrides Component#setBounds(int, int, int, int) in order to determine
     * whether an automatic scroll of #chatTextPane to its bottom will be
     * necessary at a later time in order to keep its vertical scroll bar to its
     * bottom after the realization of the resize if it is at its bottom before
     * the resize.
     */
    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        synchronized (scrollToBottomRunnable)
        {
            JScrollBar verticalScrollBar = getVerticalScrollBar();

            if (verticalScrollBar != null)
            {
                BoundedRangeModel verticalScrollBarModel
                    = verticalScrollBar.getModel();

                if ((verticalScrollBarModel.getValue()
                                + verticalScrollBarModel.getExtent()
                            >= verticalScrollBarModel.getMaximum())
                        || !verticalScrollBar.isVisible())
                    scrollToBottomIsPending = true;
            }
        }

        super.setBounds(x, y, width, height);
    }

    /**
     * Retrieves the contents of the sent message with the given ID.
     *
     * @param messageUID The ID of the message to retrieve.
     * @return The contents of the message, or null if the message is not found.
     */
    public String getMessageContents(String messageUID)
    {
        Element root = document.getDefaultRootElement();
        Element e = document.getElement(
            root,
            Attribute.ID,
            ChatHtmlUtils.MESSAGE_TEXT_ID + messageUID);
        if (e == null)
        {
            logger.warn("Could not find message with ID " + messageUID);
            return null;
        }

        Object original_message = e.getAttributes().getAttribute(
                ChatHtmlUtils.ORIGINAL_MESSAGE_ATTRIBUTE);
        if (original_message == null)
        {
            logger.warn("Message with ID " + messageUID +
                    " does not have original_message attribute");
            return null;
        }

        String res = new String(net.java.sip.communicator.util.Base64
            .decode(original_message.toString()));
        // Remove all newline characters that were inserted to make copying
        // newlines from the conversation panel work.
        // They shouldn't be in the write panel, because otherwise a newline
        // would consist of two chars, one of them invisible (the &#10;), but
        // both of them have to be deleted in order to remove it.
        // On the other hand this means that copying newlines from the write
        // area produces only spaces, but this seems like the better option.
        res = res.replace("&#10;", "");
        return res;
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message
     * @param keyword a substring of <tt>chatMessage</tt> to be highlighted upon
     * display of <tt>chatMessage</tt> in the UI
     * @return the processed message
     */
    public String processMessage(   ChatMessage chatMessage,
                                    String keyword,
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress)
    {
        // If this is a consecutive message don't go through the initiation
        // and just append it.
        if (isConsecutiveMessage(chatMessage))
        {
            appendConsecutiveMessage(chatMessage, keyword);
            return null;
        }

        String contentType = chatMessage.getContentType();

        lastMessageTimestamp = chatMessage.getDate();

        String contactName = chatMessage.getContactName();
        String contactDisplayName = chatMessage.getContactDisplayName();
        if (contactDisplayName == null
                || contactDisplayName.trim().length() <= 0)
            contactDisplayName = contactName;
        else
        {
            // for some reason &apos; is not rendered correctly from our ui,
            // lets use its equivalent. Other similar chars(< > & ") seem ok.
            contactDisplayName
                = contactDisplayName.replaceAll("&apos;", "&#39;");
        }

        Date date = chatMessage.getDate();
        String messageType = chatMessage.getMessageType();
        String messageTitle = chatMessage.getMessageTitle();
        String message = chatMessage.getMessage();
        String chatString = "";
        String endHeaderTag = "";

        lastMessageUID = chatMessage.getMessageUID();

        if (messageType.equals(Chat.INCOMING_MESSAGE))
        {
            this.lastIncomingMsgTimestamp = new Date();

            chatString = ChatHtmlUtils.createIncomingMessageTag(
                lastMessageUID,
                contactName,
                contactDisplayName,
                getContactAvatar(protocolProvider, contactAddress),
                date,
                formatMessageAsHTML(message, contentType, keyword),
                ChatHtmlUtils.HTML_CONTENT_TYPE,
                false,
                isSimpleTheme);
        }
        else if (messageType.equals(Chat.OUTGOING_MESSAGE))
        {
            chatString = ChatHtmlUtils.createOutgoingMessageTag(
                lastMessageUID,
                contactName,
                contactDisplayName,
                getContactAvatar(protocolProvider),
                date,
                formatMessageAsHTML(message, contentType, keyword),
                ChatHtmlUtils.HTML_CONTENT_TYPE,
                false,
                isSimpleTheme);
        }
        else if (messageType.equals(Chat.HISTORY_INCOMING_MESSAGE))
        {
            chatString = ChatHtmlUtils.createIncomingMessageTag(
                lastMessageUID,
                contactName,
                contactDisplayName,
                getContactAvatar(protocolProvider, contactAddress),
                date,
                formatMessageAsHTML(message, contentType, keyword),
                ChatHtmlUtils.HTML_CONTENT_TYPE,
                true,
                isSimpleTheme);
        }
        else if (messageType.equals(Chat.HISTORY_OUTGOING_MESSAGE))
        {
            chatString = ChatHtmlUtils.createOutgoingMessageTag(
                lastMessageUID,
                contactName,
                contactDisplayName,
                getContactAvatar(protocolProvider),
                date,
                formatMessageAsHTML(message, contentType, keyword),
                ChatHtmlUtils.HTML_CONTENT_TYPE,
                true,
                isSimpleTheme);
        }
        else if (messageType.equals(Chat.SMS_MESSAGE))
        {
            chatString = ChatHtmlUtils.createIncomingMessageTag(
                lastMessageUID,
                contactName,
                contactDisplayName,
                getContactAvatar(protocolProvider, contactAddress),
                date,
                ConfigurationUtils.isSmsNotifyTextDisabled() ?
                    formatMessageAsHTML(message, contentType, keyword)
                    : formatMessageAsHTML("SMS: " + message, contentType, keyword),
                ChatHtmlUtils.HTML_CONTENT_TYPE,
                false,
                isSimpleTheme);
        }
        else if (messageType.equals(Chat.STATUS_MESSAGE))
        {
            chatString = "<div id=\"statusMessage\" date=\"" + date + "\""
                + " style=\"color: #8F8F8F; font-size: 8px;\">";
            endHeaderTag = "</div>";

            chatString +=
                GuiUtils.formatTime(date)
                    + " "
                    + StringEscapeUtils.escapeHtml4(contactName) + " "
                    + formatMessageAsHTML(message, contentType, keyword)
                    + endHeaderTag;
        }
        else if (messageType.equals(Chat.ACTION_MESSAGE))
        {
            chatString =    "<p id=\"actionMessage\" date=\""
                            + date + "\">";
            endHeaderTag = "</p>";

            chatString += "* " + GuiUtils.formatTime(date)
                + " " + StringEscapeUtils.escapeHtml4(contactName) + " "
                + formatMessageAsHTML(message, contentType, keyword)
                + endHeaderTag;
        }
        else if (messageType.equals(Chat.SYSTEM_MESSAGE))
        {
            String startSystemDivTag =
                "<DIV id=\"systemMessage\" style=\"color:#627EB7;\">";
            String endDivTag = "</DIV>";

            chatString +=
                startSystemDivTag
                    + formatMessageAsHTML(message, contentType, keyword)
                    + endDivTag;
        }
        else if (messageType.equals(Chat.ERROR_MESSAGE))
        {
            chatString      = "<h6 id=\""
                            + ChatHtmlUtils.MESSAGE_HEADER_ID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h6>";

            String errorIcon = "<IMG SRC=\""
                + ImageLoader.getImageUri(ImageLoader.EXCLAMATION_MARK)
                + "\"></IMG>";

            // If the message title is null do not show it and show the error
            // icon on the same line as the actual error message.
            if (messageTitle != null)
            {
                chatString +=
                    errorIcon + StringEscapeUtils.escapeHtml4(messageTitle)
                        + endHeaderTag + "<h5>"
                        + formatMessageAsHTML(message, contentType, keyword)
                        + "</h5>";
            }
            else
            {
                chatString +=
                    endHeaderTag + "<h5>" + errorIcon + " "
                        + formatMessageAsHTML(message, contentType, keyword)
                        + "</h5>";
            }
        }

        return chatString;
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message.
     * @return the formatted message
     */
    public String processMessage(   ChatMessage chatMessage,
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress)
    {
        return processMessage(  chatMessage,
                                null,
                                protocolProvider,
                                contactAddress);
    }

    /**
     * Appends a consecutive message to the document.
     *
     * @param chatMessage the message to append
     * @param keyword the keywords to highlight
     */
    public void appendConsecutiveMessage(final ChatMessage chatMessage,
        final String keyword)
    {
        String previousMessageUID = lastMessageUID;
        lastMessageUID = chatMessage.getMessageUID();

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    appendConsecutiveMessage(chatMessage, keyword);
                }
            });
            return;
        }

        Element lastMsgElement = document.getElement(
            ChatHtmlUtils.MESSAGE_TEXT_ID + previousMessageUID);

        String contactAddress
            = (String) lastMsgElement.getAttributes()
                .getAttribute(Attribute.NAME);

        boolean isHistory
            = (chatMessage.getMessageType()
                .equals(Chat.HISTORY_INCOMING_MESSAGE)
                || chatMessage.getMessageType()
                .equals(Chat.HISTORY_OUTGOING_MESSAGE))
                ? true : false;

        String newMessage = ChatHtmlUtils.createMessageTag(
                                    chatMessage.getMessageUID(),
                                    contactAddress,
                                    formatMessageAsHTML(
                                        chatMessage.getMessage(),
                                        chatMessage.getContentType(),
                                        keyword),
                                    ChatHtmlUtils.HTML_CONTENT_TYPE,
                                    chatMessage.getDate(),
                                    false,
                                    isHistory,
                                    isSimpleTheme);

        synchronized (scrollToBottomRunnable)
        {
            try
            {
                Element parentElement = lastMsgElement.getParentElement();

                document.insertBeforeEnd(parentElement, newMessage);

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time
                // we add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException ex)
            {
                logger.error("Could not replace chat message", ex);
            }
            catch (IOException ex)
            {
                logger.error("Could not replace chat message", ex);
            }
        }

        finishMessageAdd(newMessage);
    }

    /**
     * Replaces the contents of the message with ID of the corrected message
     * specified in chatMessage, with this message.
     *
     * @param chatMessage A <tt>ChatMessage</tt> that contains all the required
     * information to correct the old message.
     */
    public void correctMessage(final ChatMessage chatMessage)
    {
        lastMessageUID = chatMessage.getMessageUID();

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    correctMessage(chatMessage);
                }
            });
            return;
        }

        String correctedUID = chatMessage.getCorrectedMessageUID();
        Element root = document.getDefaultRootElement();
        Element correctedMsgElement
            = document.getElement(root,
                                  Attribute.ID,
                                  ChatHtmlUtils.MESSAGE_TEXT_ID + correctedUID);

        if (correctedMsgElement == null)
        {
            logger.warn("Could not find message with ID " + correctedUID);
            return;
        }

        String contactAddress
            = (String) correctedMsgElement.getAttributes()
                .getAttribute(Attribute.NAME);

        boolean isHistory
            = (chatMessage.getMessageType()
                .equals(Chat.HISTORY_INCOMING_MESSAGE)
                || chatMessage.getMessageType()
                .equals(Chat.HISTORY_OUTGOING_MESSAGE))
                ? true : false;

        String newMessage = ChatHtmlUtils.createMessageTag(
            chatMessage.getMessageUID(),
            contactAddress,
            formatMessageAsHTML(chatMessage.getMessage(),
                            chatMessage.getContentType(),
                            ""),
            ChatHtmlUtils.HTML_CONTENT_TYPE,
            chatMessage.getDate(),
            true,
            isHistory,
            isSimpleTheme);

        synchronized (scrollToBottomRunnable)
        {
            try
            {
                document.setOuterHTML(correctedMsgElement, newMessage);

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time
                // we add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException ex)
            {
                logger.error("Could not replace chat message", ex);
            }
            catch (IOException ex)
            {
                logger.error("Could not replace chat message", ex);
            }
        }

        finishMessageAdd(newMessage);
    }

    /**
     * Appends the given string at the end of the contained in this panel
     * document.
     *
     * Note: Currently, it looks like appendMessageToEnd is only called for
     * messages that are already converted to HTML. So It is quite possible that
     * we can remove the content type without any issues.
     *
     * @param original the message string to append
     * @param contentType the message's content type
     */
    public void appendMessageToEnd(final String original,
                                   final String contentType)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    appendMessageToEnd(original, contentType);
                }
            });
            return;
        }

        if (original == null)
        {
            return;
        }

        final String message;
        if (ChatHtmlUtils.HTML_CONTENT_TYPE.equalsIgnoreCase(contentType))
        {
            message = original;
        }
        else
        {
            message = StringEscapeUtils.escapeHtml4(original);
        }

        synchronized (scrollToBottomRunnable)
        {
            Element root = document.getDefaultRootElement();

            try
            {
                document.insertBeforeEnd(
                            // the body element
                            root.getElement(root.getElementCount() - 1),
                            // the message to insert
                            message);

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time we
                // add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException e)
            {
                logger.error("Insert in the HTMLDocument failed.", e);
            }
            catch (IOException e)
            {
                logger.error("Insert in the HTMLDocument failed.", e);
            }
        }

        String lastElemContent = getElementContent(lastMessageUID, message);

        if (lastElemContent != null)
        {
            finishMessageAdd(lastElemContent);
        }
    }

    /**
     * Performs all operations needed in order to finish the adding of the
     * message to the document.
     *
     * @param message the message string
     * @param contentType
     */
    private void finishMessageAdd(final String message)
    {
        // If we're not in chat history case we need to be sure the document
        // has not exceeded the required size (number of messages).
        if (!isHistory)
            ensureDocumentSize();

        /*
         * Replacements will be processed only if it is enabled in the
         * property.
         */
        ConfigurationService cfg = GuiActivator.getConfigurationService();

        if (cfg.getBoolean(ReplacementProperty.REPLACEMENT_ENABLE, true)
                ||cfg.getBoolean(ReplacementProperty.REPLACEMENT_PROPOSAL, true)
                || cfg.getBoolean(
                        ReplacementProperty.getPropertyName("SMILEY"),
                        true))
        {
            processReplacement(ChatHtmlUtils.MESSAGE_TEXT_ID + lastMessageUID,
                                message);
        }
    }

    /**
    * Formats the given message. Processes the messages and replaces links to
    * video/image sources with their previews or any other substitution. Spawns
    * a separate thread for replacement.
    *
    * @param messageID the messageID element.
    * @param chatString the message.
    */
    void processReplacement(final String messageID, final String chatString)
    {
        new ReplacementWorker(messageID, chatString).start();
    }

    /**
     * Ensures that the document won't become too big. When the document reaches
     * a certain size the first message in the page is removed.
     */
    private void ensureDocumentSize()
    {
        if (document.getLength() > Chat.CHAT_BUFFER_SIZE)
        {
            String[] ids = new String[]
                                      {ChatHtmlUtils.MESSAGE_TEXT_ID,
                                       "statusMessage",
                                       "systemMessage",
                                       "actionMessage"};

            Element firstMsgElement = findElement(Attribute.ID, ids);

            int startIndex = firstMsgElement.getStartOffset();
            int endIndex = firstMsgElement.getEndOffset();

            try
            {
                // Remove the message.
                this.document.remove(startIndex, endIndex - startIndex);
            }
            catch (BadLocationException e)
            {
                logger.error("Error removing messages from chat: ", e);
            }

            if(firstMsgElement.getName().equals("table"))
            {
                // as we have removed a header for maybe several messages,
                // delete all messages without header
                deleteAllMessagesWithoutHeader();
            }
        }
    }

    /**
     * Deletes all messages "div"s that are missing their header the table tag.
     * The method calls itself recursively.
     */
    private void deleteAllMessagesWithoutHeader()
    {
        String[] ids = new String[]
            {ChatHtmlUtils.MESSAGE_TEXT_ID,
                "statusMessage",
                "systemMessage",
                "actionMessage"};

        Element firstMsgElement = findElement(Attribute.ID, ids);

        if(firstMsgElement == null
            || !firstMsgElement.getName().equals("div"))
        {
            return;
        }

        int startIndex = firstMsgElement.getStartOffset();
        int endIndex = firstMsgElement.getEndOffset();

        try
        {
            // Remove the message.
            if(endIndex - startIndex < document.getLength())
                this.document.remove(startIndex, endIndex - startIndex);
            else
            {
                // currently there is a problem of deleting the last message
                // if it is the last message on the view
                return;
            }
        }
        catch (BadLocationException e)
        {
            logger.error("Error removing messages from chat: ", e);

            return;
        }

        deleteAllMessagesWithoutHeader();
    }

    /**
     * Formats the given message. Processes all smiley chars, new lines and
     * links. This method expects <u>only</u> the message's <u>body</u> to be
     * provided.
     *
     * @param message the message to be formatted
     * @param contentType the content type of the message to be formatted
     * @param keyword the word to be highlighted
     * @return the formatted message
     */
    private String formatMessageAsHTML(final String original,
                                 final String contentType,
                                 final String keyword)
    {
        if (original == null)
        {
            return "";
        }

        // prepare source message
        String source;
        if (ChatHtmlUtils.HTML_CONTENT_TYPE.equals(contentType))
        {
            source = original;
        }
        else
        {
            source = StringEscapeUtils.escapeHtml4(original);
        }

        return processReplacers(source,
            new NewlineReplacer(),
            new URLReplacer(URL_PATTERN),
            new KeywordReplacer(keyword),
            new BrTagReplacer(),
            new ImgTagReplacer());
    }

    /**
     * Process provided replacers one by one sequentially. The output of the
     * first replacer is then fed as input into the second replacer, and so on.
     * <p>
     * {@link Replacer}s that expect HTML content (
     * {@link Replacer#expectsPlainText()}) will typically receive the complete
     * message as an argument. {@linkplain Replacer}s that expect plain text
     * content will typically receive small pieces that are found in between
     * HTML tags. The pieces of plain text content cannot be predicted as
     * results change when they are processed by other replacers.
     * </p>
     *
     * @param content the original content to process
     * @param replacers the replacers to call
     * @return returns the final result message content after it has been
     *         processed by all replacers
     */
    private String processReplacers(final String content,
        final Replacer... replacers)
    {
        StringBuilder source = new StringBuilder(content);
        for (final Replacer replacer : replacers)
        {
            final StringBuilder target = new StringBuilder();
            if (replacer.expectsPlainText())
            {
                int startPos = 0;
                final Matcher plainTextInHtmlMatcher =
                    TEXT_TO_REPLACE_PATTERN.matcher(source);
                while (plainTextInHtmlMatcher.find())
                {
                    final String plainTextAsHtml =
                        plainTextInHtmlMatcher.group(1);
                    final int startMatchPosition =
                        plainTextInHtmlMatcher.start(1);
                    final int endMatchPosition = plainTextInHtmlMatcher.end(1);
                    target.append(source
                        .substring(startPos, startMatchPosition));
                    final String plaintext =
                        StringEscapeUtils.unescapeHtml4(plainTextAsHtml);

                    // Invoke replacer.
                    try
                    {
                        replacer.replace(target, plaintext);
                    }
                    catch (RuntimeException e)
                    {
                        logger.error("An error occurred in replacer: "
                            + replacer.getClass().getName(), e);
                    }

                    startPos = endMatchPosition;
                }
                target.append(source.substring(startPos));
            }
            else
            {
                // Invoke replacer.
                try
                {
                    replacer.replace(target, source.toString());
                }
                catch (RuntimeException e)
                {
                    logger.error("An error occurred in replacer: "
                        + replacer.getClass().getName(), e);
                }
            }
            source = target;
        }
        return source.toString();
    }

    /**
     * Opens a link in the default browser when clicked and shows link url in a
     * popup on mouseover.
     *
     * @param e The HyperlinkEvent.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
        {
            String href = e.getDescription();

            this.isCurrentHrefImg
                = e.getSourceElement().getName().equals("img");
            this.currentHref = href;
        }
        else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
        {
            this.currentHref = "";
            this.isCurrentHrefImg = false;
        }
    }

    /**
     * Returns the text pane of this conversation panel.
     *
     * @return The text pane of this conversation panel.
     */
    public JTextPane getChatTextPane()
    {
        return chatTextPane;
    }

    /**
     * Returns the time of the last received message.
     *
     * @return The time of the last received message.
     */
    public Date getLastIncomingMsgTimestamp()
    {
        return lastIncomingMsgTimestamp;
    }

    /**
     * When a right button click is performed in the editor pane, a popup menu
     * is opened.
     * In case of the Scheme being internal, it won't open the Browser but
     * instead it will trigger the forwarded action.
     *
     * @param e The MouseEvent.
     */
    public void mouseClicked(MouseEvent e)
    {
        Point p = e.getPoint();
        SwingUtilities.convertPointToScreen(p, e.getComponent());

        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            openContextMenu(p);
        }
        else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
            && currentHref != null && currentHref.length() != 0)
        {
            URI uri;
            try
            {
                uri = new URI(currentHref);
            }
            catch (URISyntaxException e1)
            {
                logger.error("Failed to open hyperlink in chat window. " +
                        "Error was: Invalid URL - " + currentHref);
                return;
            }
            if("jitsi".equals(uri.getScheme()))
            {
                for(ChatLinkClickedListener l:chatLinkClickedListeners)
                {
                    l.chatLinkClicked(uri);
                }
            }
            else
                GuiActivator.getBrowserLauncher().openURL(currentHref);

            // after opening the link remove the currentHref to avoid
            // clicking on the window to gain focus to open the link again
            this.currentHref = "";
        }
    }

    /**
     * Opens this panel context menu at the given point.
     *
     * @param p the point where to position the left-top cornet of the context
     *            menu
     */
    private void openContextMenu(Point p)
    {
        if (currentHref != null && currentHref.length() != 0
                && !currentHref.startsWith("jitsi://"))
        {
            rightButtonMenu.insert(openLinkItem, 0);
            rightButtonMenu.insert(copyLinkItem, 1);
            rightButtonMenu.insert(copyLinkSeparator, 2);
            if(isCurrentHrefImg)
            {
                rightButtonMenu.insert(configureReplacementItem, 3);
                rightButtonMenu.insert(configureReplacementSeparator, 4);
            }
        }
        else
        {
            rightButtonMenu.remove(openLinkItem);
            rightButtonMenu.remove(copyLinkItem);
            rightButtonMenu.remove(copyLinkSeparator);
            rightButtonMenu.remove(configureReplacementItem);
            rightButtonMenu.remove(configureReplacementSeparator);
        }

        if (chatTextPane.getSelectedText() != null)
        {
            rightButtonMenu.enableCopy();
        }
        else
        {
            rightButtonMenu.disableCopy();
        }
        rightButtonMenu.setInvoker(chatTextPane);
        rightButtonMenu.setLocation(p.x, p.y);
        rightButtonMenu.setVisible(true);
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void lostOwnership(Clipboard clipboard, Transferable contents) {}

    /**
     * Returns the chat container.
     *
     * @return the chat container
     */
    public ChatConversationContainer getChatContainer()
    {
        return chatContainer;
    }

    /**
     * Copies the selected conversation panel content to the clipboard.
     */
    public void copyConversation()
    {
        this.chatTextPane.copy();
    }

    /**
     * Creates new document and all the messages that will be processed in the
     * future will be appended in it.
     */
    public void clear()
    {
        this.document = (HTMLDocument) editorKit.createDefaultDocument();
        Constants.loadSimpleStyle(
            document.getStyleSheet(), chatTextPane.getFont());
        this.isSimpleTheme = ConfigurationUtils.isChatSimpleThemeEnabled();
    }

    /**
     * Sets the given document to the editor pane in this panel.
     *
     * @param document the document to set
     */
    public void setContent(final HTMLDocument document)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setContent(document);
                }
            });
            return;
        }

        synchronized (scrollToBottomRunnable)
        {
            scrollToBottomIsPending = true;

            this.document = document;
            chatTextPane.setDocument(this.document);
        }
    }

    /**
     * Sets the default document contained in this panel, created on init or
     * when clear is invoked.
     */
    public void setDefaultContent()
    {
        setContent(document);
    }

    /**
     * Returns the document contained in this panel.
     *
     * @return the document contained in this panel
     */
    public HTMLDocument getContent()
    {
        return (HTMLDocument) this.chatTextPane.getDocument();
    }

    /**
     * Returns the right button popup menu.
     *
     * @return the right button popup menu
     */
    public ChatRightButtonMenu getRightButtonMenu()
    {
        return rightButtonMenu;
    }

    /**
     * Returns the date of the first message in the current page.
     *
     * @return the date of the first message in the current page
     */
    public Date getPageFirstMsgTimestamp()
    {
        Element firstHeaderElement
            = document.getElement(ChatHtmlUtils.MESSAGE_HEADER_ID);

        if(firstHeaderElement == null)
            return new Date(Long.MAX_VALUE);

        String dateObject = firstHeaderElement
            .getAttributes().getAttribute(ChatHtmlUtils.DATE_ATTRIBUTE)
                .toString();

        SimpleDateFormat sdf = new SimpleDateFormat(HistoryService.DATE_FORMAT);
        try
        {
            return sdf.parse(dateObject);
        }
        catch (ParseException e)
        {
            return new Date(0);
        }
    }

    /**
     * Returns the date of the last message in the current page.
     *
     * @return the date of the last message in the current page
     */
    public Date getPageLastMsgTimestamp()
    {
        Date timestamp = new Date(0);

        if (lastMessageUID != null)
        {
            Element lastMsgElement
                = document.getElement(
                        ChatHtmlUtils.MESSAGE_TEXT_ID + lastMessageUID);

            if (lastMsgElement != null)
            {
                Object date
                    = lastMsgElement.getAttributes().getAttribute(
                            ChatHtmlUtils.DATE_ATTRIBUTE);

                SimpleDateFormat sdf
                    = new SimpleDateFormat(HistoryService.DATE_FORMAT);
                if (date != null)
                {
                    try
                    {
                        timestamp = sdf.parse(date.toString());
                    }
                    catch (ParseException e)
                    {}
                }
            }
        }

        return timestamp;
    }

    /**
     * Extend Editor pane to add URL tooltips.
     */
    private class MyTextPane
        extends JTextPane
    {
        /**
         * Returns the string to be used as the tooltip for <i>event</i>.
         *
         * @param event the <tt>MouseEvent</tt>
         * @return the string to be used as the tooltip for <i>event</i>.
         */
        @Override
        public String getToolTipText(MouseEvent event)
        {
            return
                ((currentHref != null) && (currentHref.length() != 0))
                    ? currentHref
                    : null;
        }
    }

    /**
     * Adds a custom component at the end of the conversation.
     *
     * @param component the component to add at the end of the conversation.
     */
    public void addComponent(ChatConversationComponent component)
    {
        synchronized (scrollToBottomRunnable)
        {
            StyleSheet styleSheet = document.getStyleSheet();
            Style style
                = styleSheet
                    .addStyle(
                        StyleConstants.ComponentElementName,
                        styleSheet.getStyle("body"));

            // The image must first be wrapped in a style
            style
                .addAttribute(
                    AbstractDocument.ElementNameAttribute,
                    StyleConstants.ComponentElementName);

            TransparentPanel wrapPanel
                = new TransparentPanel(new BorderLayout());

            wrapPanel.add(component, BorderLayout.NORTH);

            style.addAttribute(StyleConstants.ComponentAttribute, wrapPanel);
            style.addAttribute(Attribute.ID, ChatHtmlUtils.MESSAGE_TEXT_ID);
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            style.addAttribute(ChatHtmlUtils.DATE_ATTRIBUTE,
                                sdf.format(component.getDate()));

            scrollToBottomIsPending = true;

            // We need to reinitialize the last message ID, because we don't
            // want components to be taken into account.
            lastMessageUID = null;

            // Insert the component style at the end of the text
            try
            {
                document
                    .insertString(document.getLength(), "ignored text", style);
            }
            catch (BadLocationException e)
            {
                logger.error("Insert in the HTMLDocument failed.", e);
            }
        }
    }

    /**
     * Registers a new link click listener.
     *
     * @param listener the object that should be notified when an internal
     * link was clicked.
     */
    public void addChatLinkClickedListener(ChatLinkClickedListener listener)
    {
        if(!chatLinkClickedListeners.contains(listener))
            chatLinkClickedListeners.add(listener);
    }

    /**
     * Remove a registered link click listener.
     *
     * @param listener a registered click listener to remove
     */
    public void removeChatLinkClickedListener(ChatLinkClickedListener listener)
    {
        chatLinkClickedListeners.remove(listener);
    }

    /**
     * Reloads images.
     */
    @Override
    public void loadSkin()
    {
        openLinkItem.setIcon(
                new ImageIcon(ImageLoader.getImage(ImageLoader.BROWSER_ICON)));
        copyLinkItem.setIcon(
                new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

        getRightButtonMenu().loadSkin();
    }

    /**
     * Processes /me command in group chats.
     *
     * @param chatMessage the chat message
     * @return the newly processed message string
     */
    public String processMeCommand(ChatMessage chatMessage)
    {
        String contentType = chatMessage.getContentType();
        String message = chatMessage.getMessage();
        if (message.length() <= 4 || !message.startsWith("/me "))
        {
            return "";
        }

        String msgID
            = ChatHtmlUtils.MESSAGE_TEXT_ID + chatMessage.getMessageUID();
        String chatString = "<DIV ID='" + msgID + "'><B><I>";
        String endHeaderTag = "</I></B></DIV>";

        chatString +=
            GuiUtils.escapeHTMLChars("*** " + chatMessage.getContactName()
                + " " + message.substring(4))
                + endHeaderTag;

        Map<String, ReplacementService> listSources =
            GuiActivator.getReplacementSources();
        for (ReplacementService source : listSources.values())
        {
            boolean isSmiley = source instanceof SmiliesReplacementService;
            if (!isSmiley)
            {
                continue;
            }
            String sourcePattern = source.getPattern();
            Pattern p =
                Pattern.compile(sourcePattern, Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);
            Matcher m = p.matcher(chatString);
            chatString =
                m.replaceAll(ChatHtmlUtils.HTML_CONTENT_TYPE
                    .equalsIgnoreCase(contentType) ? "$0" : StringEscapeUtils
                    .escapeHtml4("$0"));
        }
        return chatString;
    }

    /**
     * Returns the avatar corresponding to the account of the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider service, which account
     * avatar we're looking for
     * @return the avatar corresponding to the account of the given
     * <tt>protocolProvider</tt>
     */
    private static String getContactAvatar(
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress)
    {
        String avatarPath
            = AvatarCacheUtils.getCachedAvatarPath( protocolProvider,
                                                    contactAddress);

        File avatarFile;
        try
        {
            avatarFile = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(avatarPath, FileCategory.CACHE);
        }
        catch (Exception e)
        {
            return null;
        }

        if(avatarFile.exists() && avatarFile.length() > 0)
            return "file:" + avatarFile.getAbsolutePath();
        else
            return GuiActivator.getResources().getImageURL(
                "service.gui.DEFAULT_USER_PHOTO_SMALL").toString();
    }

   /**
    * Returns the avatar corresponding to the account of the given
    * <tt>protocolProvider</tt>.
    *
    * @param protocolProvider the protocol provider service, which account
    * avatar we're looking for
    * @return the avatar corresponding to the account of the given
    * <tt>protocolProvider</tt>
    */
    private static String getContactAvatar(
                                    ProtocolProviderService protocolProvider)
    {
        String avatarPath
            = AvatarCacheUtils.getCachedAvatarPath(protocolProvider);

        File avatarFile;
        try
        {
            avatarFile = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(avatarPath, FileCategory.CACHE);
        }
        catch (Exception e)
        {
            return null;
        }

        if(avatarFile.exists() && avatarFile.length() > 0)
            return "file:" + avatarFile.getAbsolutePath();
        else
            return GuiActivator.getResources().getImageURL(
                "service.gui.DEFAULT_USER_PHOTO_SMALL").toString();
    }

    /**
     * Indicates if this is a consecutive message.
     *
     * @param chatMessage the message to verify
     * @return <tt>true</tt> if the given message is a consecutive message,
     * <tt>false</tt> - otherwise
     */
    private boolean isConsecutiveMessage(ChatMessage chatMessage)
    {
        if (lastMessageUID == null)
            return false;

        Element lastMsgElement = document.getElement(
            ChatHtmlUtils.MESSAGE_TEXT_ID + lastMessageUID);

        if (lastMsgElement == null)
        {
            // This will happen if the last message is a non-user message, such
            // as a system message. For these messages we *do* update the
            // lastMessageUID, however we do *not* include the new UID in the
            // newly appended message.
            logger.info("Could not find message with ID " + lastMessageUID);
            return false;
        }

        String contactAddress
            = (String) lastMsgElement.getAttributes()
                .getAttribute(Attribute.NAME);

        if (contactAddress != null
                && (chatMessage.getMessageType()
                        .equals(Chat.INCOMING_MESSAGE)
                    || chatMessage.getMessageType()
                        .equals(Chat.OUTGOING_MESSAGE)
                    || chatMessage.getMessageType()
                        .equals(Chat.HISTORY_INCOMING_MESSAGE)
                    || chatMessage.getMessageType()
                        .equals(Chat.HISTORY_OUTGOING_MESSAGE))
                && contactAddress.equals(chatMessage.getContactName())
                // And if the new message is within a minute from the last one.
                && ((chatMessage.getDate().getTime()
                    - lastMessageTimestamp.getTime())
                        < 60000))
        {
            lastMessageTimestamp = chatMessage.getDate();

            return true;
        }

        return false;
    }

    /**
     * Releases the resources allocated by this instance throughout its lifetime
     * and prepares it for garbage collection.
     */
    @Override
    public void dispose()
    {
        if(editorKit != null)
        {
            editorKit.dispose();
        }

        super.dispose();

        if(showPreview != null)
        {
            showPreview.dispose();
            showPreview = null;
        }

        if(rightButtonMenu != null)
        {
            rightButtonMenu.dispose();
            rightButtonMenu = null;
        }

        clear();
    }

    /**
     *
     * @param attribute
     * @param matchStrings
     * @return
     */
    private Element findElement(HTML.Attribute attribute,
                                String[] matchStrings)
    {
        return findFirstElement(document.getDefaultRootElement(),
                                attribute,
                                matchStrings);
    }

    /**
     * Finds the first element with <tt>name</tt>.
     * @param name the name to search for.
     * @return the first element with <tt>name</tt>.
     */
    private Element findFirstElement(String name)
    {
        return findFirstElement(document.getDefaultRootElement(), name);
    }

    /**
     *
     * @param element
     * @param attrName
     * @param matchStrings
     * @return
     */
    private Element findFirstElement(   Element element,
                                        HTML.Attribute attrName,
                                        String[] matchStrings)
    {
        String attr = (String) element.getAttributes().getAttribute(attrName);

        if(attr != null)
            for (String matchString : matchStrings)
                if (attr.startsWith(matchString))
                    return element;

        Element resultElement = null;

        // Count how many messages we have in the document.
        for (int i = 0; i < element.getElementCount(); i++)
        {
            resultElement = findFirstElement(element.getElement(i),
                                        attrName,
                                        matchStrings);
            if (resultElement != null)
                return resultElement;
        }

        return null;
    }

    /**
     * Finds the first element with <tt>name</tt> among the child elements of
     * <tt>element</tt>.
     * @param element the element to searh for.
     * @param name the name to search for.
     * @return the first element with <tt>name</tt>.
     */
    private Element findFirstElement(   Element element,
                                        String name)
    {
        if (element.getName().equalsIgnoreCase(name))
            return element;

        Element resultElement = null;

        // Count how many messages we have in the document.
        for (int i = 0; i < element.getElementCount(); i++)
        {
            resultElement = findFirstElement(element.getElement(i), name);

            if (resultElement != null)
                return resultElement;
        }

        return null;
    }

    /**
     *
     * @param elementId
     * @param message
     * @return
     */
    private String getElementContent(String elementId, String message)
    {
        Pattern p = Pattern.compile(
            ".*(<div.*id=[\\\"']"
            + ChatHtmlUtils.MESSAGE_TEXT_ID
            + elementId
            + "[\\\"'].*?</div>)", Pattern.DOTALL);

        Matcher m = p.matcher(message);

        if (m.find())
        {
            return m.group(1);
        }

        return null;
    }

    /**
     * Returns the first available advanced configuration form.
     *
     * @return the first available advanced configuration form
     */
    public static ConfigurationForm getChatConfigForm()
    {
        // General configuration forms only.
        Collection<ServiceReference<ConfigurationForm>> cfgFormRefs;
        String osgiFilter
            = "(" + ConfigurationForm.FORM_TYPE + "="
                + ConfigurationForm.GENERAL_TYPE + ")";

        try
        {
            cfgFormRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        ConfigurationForm.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            cfgFormRefs = null;
        }

        if ((cfgFormRefs != null) && !cfgFormRefs.isEmpty())
        {
            String chatCfgFormClassName
                = "net.java.sip.communicator.plugin.chatconfig.ChatConfigPanel";

            for (ServiceReference<ConfigurationForm> cfgFormRef : cfgFormRefs)
            {
                ConfigurationForm form
                    = GuiActivator.bundleContext.getService(cfgFormRef);

                if (form instanceof LazyConfigurationForm)
                {
                    LazyConfigurationForm lazyConfigForm
                        = (LazyConfigurationForm) form;

                    if (chatCfgFormClassName.equals(
                            lazyConfigForm.getFormClassName()))
                    {
                        return form;
                    }
                }
                else if (form.getClass().getName().equals(chatCfgFormClassName))
                {
                    return form;
                }
            }
        }

        return null;
    }

    /**
     * Extends SIPCommHTMLEditorKit to keeps track of created ImageView for
     * the gif images in order to flush them whenever they are no longer visible
     */
    private class ChatConversationEditorKit
        extends SIPCommHTMLEditorKit
        implements DocumentListener
    {
        /**
         * List of the image views.
         */
        private java.util.List<ImageView> imageViews =
            new ArrayList<ImageView>();

        /**
         * Constructs.
         * @param container
         */
        public ChatConversationEditorKit(JComponent container)
        {
            super(container);
        }

        /**
         * Clears any left img view and removes any listener was added.
         */
        public void dispose()
        {
            if(document != null)
            {
                document.removeDocumentListener(this);
            }

            for(ImageView iv : imageViews)
            {
                Image img = iv.getImage();
                if(img != null)
                    img.flush();
            }

            imageViews.clear();
        }

        /**
         * Inform view creation.
         * @param view the newly created view.
         */
        protected void viewCreated(ViewFactory factory, View view)
        {
            if(view instanceof ImageView)
            {
                Element e = findFirstElement(view.getElement(), "img");

                if(e == null)
                    return;

                Object src = e.getAttributes().getAttribute(Attribute.SRC);
                if(src != null && src instanceof String
                    && ((String)src).endsWith("gif"))
                {
                    imageViews.add((ImageView)view);
                }
            }
        }

        /**
         * Not used.
         * @param e
         */
        @Override
        public void insertUpdate(DocumentEvent e)
        {}

        /**
         * When something is removed from the current document we will check
         * the stored image views for any element which si no longer visible.
         * @param e the event.
         */
        @Override
        public void removeUpdate(DocumentEvent e)
        {
            // will check if some image view is no longer visible
            // will consider not visible when its length is 0
            Iterator<ImageView> imageViewIterator = imageViews.iterator();
            while(imageViewIterator.hasNext())
            {
                ImageView iv = imageViewIterator.next();

                if((iv.getElement().getEndOffset()
                    - iv.getElement().getStartOffset()) != 0)
                    continue;

                Image img = iv.getImage();
                if(img != null)
                    img.flush();

                imageViewIterator.remove();
            }
        }

        /**
         * Not used.
         * @param e
         */
        @Override
        public void changedUpdate(DocumentEvent e)
        {}

        /**
         * For debugging purposes, prints the content of the document
         * in the console.
         */
        public void debug()
        {
            try {
                write(System.out, document, 0, document.getLength());
            } catch(Throwable t){}
        }
    }

    /**
     * Swing worker used by processReplacement.
     */
    private final class ReplacementWorker
        extends SwingWorker
    {
        /**
         * The messageID element.
         */
        private final String messageID;

        /**
         * The message.
         */
        private final String chatString;

        /**
         * Counts links while processing. Used to generate unique href.
         */
        private int linkCounter = 0;

        /**
         * Is image replacement enabled.
         */
        private final boolean isEnabled;

        /**
         * Is replacement proposal enabled.
         */
        private final boolean isProposalEnabled;

        /**
         * Constructs worker.
         *
         * @param messageID the messageID element.
         * @param chatString the messages.
         */
        private ReplacementWorker(final String messageID,
            final String chatString)
        {
            this.messageID = messageID;
            this.chatString = chatString;

            ConfigurationService cfg = GuiActivator.getConfigurationService();
            isEnabled = cfg.getBoolean(
                ReplacementProperty.REPLACEMENT_ENABLE,
                true);
            isProposalEnabled
                = cfg.getBoolean(
                ReplacementProperty.REPLACEMENT_PROPOSAL,
                true);
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        public void finished()
        {
            ShowPreviewDialog previewDialog = showPreview;
            // There is a race between the replacement worker and the
            // ChatConversationPanel when it is (being) disposed of. Make sure
            // we have an instance before continuing.
            if (previewDialog == null)
            {
                // Abort if dialog has been disposed of.
                return;
            }

            String newMessage = (String) get();

            if (newMessage != null && !newMessage.equals(chatString))
            {
                previewDialog.getMsgIDToChatString().put(
                    messageID, newMessage);
                synchronized (scrollToBottomRunnable)
                {
                    scrollToBottomIsPending = true;

                    try
                    {
                        Element elem = document.getElement(messageID);
                        document.setOuterHTML(elem, newMessage);
                    }
                    catch (BadLocationException ex)
                    {
                        logger.error("Could not replace chat message", ex);
                    }
                    catch (IOException ex)
                    {
                        logger.error("Could not replace chat message", ex);
                    }
                }
            }
        }

        @Override
        public Object construct() throws Exception
        {
            Matcher divMatcher = DIV_PATTERN.matcher(chatString);
            String openingTag = "";
            String msgStore = chatString;
            String closingTag = "";
            if (divMatcher.find())
            {
                openingTag = divMatcher.group(1);
                msgStore = divMatcher.group(2);
                closingTag = divMatcher.group(3);
            }

            StringBuilder msgBuff;
            for (Map.Entry<String, ReplacementService> entry : GuiActivator
                .getReplacementSources().entrySet())
            {
                msgBuff = new StringBuilder();
                processReplacementService(entry.getValue(), msgStore, msgBuff);
                msgStore = msgBuff.toString();
            }

            return openingTag + msgStore + closingTag;
        }

        /**
         * Process message for a ReplacementService.
         *
         * @param service the service.
         * @param msg the message.
         * @param buff current accumulated buffer.
         */
        private void processReplacementService(final ReplacementService service,
            final String msg, final StringBuilder buff)
        {
            String sourcePattern = service.getPattern();
            Pattern pattern =
                Pattern.compile(sourcePattern, Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);

            int startPos = 0;

            Matcher plainTextInHtmlMatcher =
                TEXT_TO_REPLACE_PATTERN.matcher(msg);
            while (plainTextInHtmlMatcher.find())
            {
                String plainTextAsHtml = plainTextInHtmlMatcher.group(1);
                int startMatchPosition = plainTextInHtmlMatcher.start(1);
                int endMatchPosition = plainTextInHtmlMatcher.end(1);

                // don't process nothing
                // or don't process already processed links content
                if (!StringUtils.isNullOrEmpty(plainTextAsHtml))
                {
                    // always add from the end of previous match, to current one
                    // or from the start to the first match
                    buff.append(msg.substring(startPos, startMatchPosition));

                    final String plaintext =
                        StringEscapeUtils.unescapeHtml4(plainTextAsHtml);

                    // Test whether this piece of content (exactly) matches a
                    // URL pattern. We should find at most a full URL text if it
                    // exists, since links have already been processed, so any
                    // URL is already wrapped in A-tags.
                    final boolean isURL =
                        URL_PATTERN.matcher(plaintext).matches();

                    processText(plaintext, buff, pattern, service, isURL);

                    startPos = endMatchPosition;
                }
            }

            // add end from startPos to end
            buff.append(msg.substring(startPos));
        }

        /**
         * Process plain text content.
         *
         * @param plainText the nodes text.
         * @param msgBuff the currently accumulated buffer.
         * @param pattern the pattern for current replacement service, created
         *            earlier so we don't create it for every text we check.
         * @param rService the replacement service.
         * @param isURL whether this content matches the URL pattern
         */
        private void processText(final String plainText,
                                 final StringBuilder msgBuff,
                                 final Pattern pattern,
                                 final ReplacementService rService,
                                 final boolean isURL)
        {
            final ShowPreviewDialog previewDialog = showPreview;
            // There is a race between the replacement worker and the
            // ChatConversationPanel when it is (being) disposed of. Make sure
            // we have an instance before continuing.
            if (previewDialog == null)
            {
                // Abort if dialog has been disposed of.
                return;
            }

            Matcher m = pattern.matcher(plainText);

            ConfigurationService cfg = GuiActivator.getConfigurationService();
            boolean isSmiley
                = rService instanceof SmiliesReplacementService;
            boolean isDirectImage
                = rService instanceof DirectImageReplacementService;
            boolean isEnabledForSource
                = cfg.getBoolean(
                ReplacementProperty.getPropertyName(
                    rService.getSourceName()), true);

            int startPos = 0;
            while (m.find())
            {
                msgBuff.append(StringEscapeUtils.escapeHtml4(plainText
                    .substring(startPos, m.start())));
                startPos = m.end();

                String group = m.group();
                String temp = rService.getReplacement(group);
                String group0 = m.group(0);

                if (!temp.equals(group0) || isDirectImage)
                {
                    if (isSmiley)
                    {
                        if (cfg.getBoolean(ReplacementProperty.
                                getPropertyName("SMILEY"),
                            true) && !isURL)
                        {
                            msgBuff.append("<IMG SRC=\"");
                            msgBuff.append(temp);
                            msgBuff.append("\" BORDER=\"0\" ALT=\"");
                            msgBuff.append(group0);
                            msgBuff.append("\"></IMG>");
                        }
                        else
                        {
                            msgBuff
                                .append(StringEscapeUtils.escapeHtml4(group));
                        }
                    }
                    else if (isProposalEnabled)
                    {
                        msgBuff.append(StringEscapeUtils.escapeHtml4(group));
                        msgBuff.append("</A> <A href=\"jitsi://"
                            + previewDialog.getClass().getName()
                            + "/SHOWPREVIEW?" + messageID
                            + "#"
                            + linkCounter
                            + "\">"
                            + StringEscapeUtils.escapeHtml4(GuiActivator
                                .getResources().getI18NString(
                                    "service.gui.SHOW_PREVIEW")));

                        previewDialog.getMsgIDandPositionToLink()
                            .put(messageID + "#" + linkCounter++, group);
                        previewDialog.getLinkToReplacement()
                            .put(group, temp);
                    }
                    else if (isEnabled && isEnabledForSource)
                    {
                        if (isDirectImage)
                        {
                            DirectImageReplacementService service
                                = (DirectImageReplacementService) rService;
                            if (service.isDirectImage(group)
                                && service.getImageSize(group) != -1)
                            {
                                msgBuff.append(
                                    "<IMG HEIGHT=\"90\" "
                                        + "WIDTH=\"120\" SRC=\"");
                                msgBuff.append(temp);
                                msgBuff.append("\" BORDER=\"0\" ALT=\"");
                                msgBuff.append(group0);
                                msgBuff.append("\"></IMG>");
                            }
                            else
                            {
                                msgBuff.append(StringEscapeUtils
                                    .escapeHtml4(group));
                            }
                        }
                        else
                        {
                            msgBuff.append(
                                "<IMG HEIGHT=\"90\" "
                                    + "WIDTH=\"120\" SRC=\"");
                            msgBuff.append(temp);
                            msgBuff.append("\" BORDER=\"0\" ALT=\"");
                            msgBuff.append(group0);
                            msgBuff.append("\"></IMG>");
                        }
                    }
                    else
                    {
                        msgBuff.append(StringEscapeUtils.escapeHtml4(group));
                    }
                }
                else
                {
                    msgBuff.append(StringEscapeUtils.escapeHtml4(group));
                }
            }

            msgBuff.append(StringEscapeUtils.escapeHtml4(plainText
                .substring(startPos)));
        }
    }
}
