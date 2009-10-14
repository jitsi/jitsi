/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatConversationPanel</tt> is the panel, where all sent and received
 * messages appear. All data is stored in an HTML document. An external CSS file
 * is applied to the document to provide the look&feel. All smileys and link
 * strings are processed and finally replaced by corresponding images and HTML
 * links.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ChatConversationPanel
    extends SCScrollPane
    implements  HyperlinkListener,
                MouseListener,
                ClipboardOwner
{
    private static final Logger logger
        = Logger.getLogger(ChatConversationPanel.class);

    /**
     * The closing tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String END_PLAINTEXT_TAG = "</PLAINTEXT>";

    /**
     * The opening tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String START_PLAINTEXT_TAG = "<PLAINTEXT>";

    /**
     * The regular expression (in the form of compiled <tt>Pattern</tt>) which
     * matches URLs for the purposed of turning them into links.
     */
    private static final Pattern URL_PATTERN
        = Pattern
            .compile(
                "("
                    + "(\\bwww\\.[^\\s<>\"]+\\.[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // wwwURL
                    + "|"
                    + "(\\b\\w+://[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // protocolURL
                    + ")");

    /**
     * The compiled <tt>Pattern</tt> which matches {@link #smileyStrings}. 
     */
    private static Pattern smileyPattern;

    /**
     * The <tt>List</tt> of smiley strings which are matched by
     * {@link #smileyPattern}.
     */
    private static final java.util.List<String> smileyStrings
        = new ArrayList<String>();

    private final JTextPane chatTextPane = new MyTextPane();

    private final HTMLEditorKit editorKit;

    private HTMLDocument document;

    private final ChatConversationContainer chatContainer;

    private final ChatRightButtonMenu rightButtonMenu;

    private String currentHref;

    private final JMenuItem copyLinkItem;

    private final JMenuItem openLinkItem;

    private final JSeparator copyLinkSeparator = new JSeparator();

    private long lastIncomingMsgTimestamp;

    private final boolean isHistory;

    public static final String HTML_CONTENT_TYPE = "text/html";

    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * Creates an instance of <tt>ChatConversationPanel</tt>.
     *
     * @param chatContainer The parent <tt>ChatConversationContainer</tt>.
     */
    public ChatConversationPanel(ChatConversationContainer chatContainer)
    {
        editorKit = new SIPCommHTMLEditorKit(this);

        this.chatContainer = chatContainer;

        isHistory = (chatContainer instanceof HistoryWindow);

        this.rightButtonMenu = new ChatRightButtonMenu(this);

        this.document = (HTMLDocument) editorKit.createDefaultDocument();

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

        this.setWheelScrollingEnabled(true);

        this.setViewportView(chatTextPane);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        ToolTipManager.sharedInstance().registerComponent(chatTextPane);

        String copyLinkString
            = GuiActivator.getResources().getI18NString("service.gui.COPY_LINK");

        copyLinkItem =
            new JMenuItem(copyLinkString,
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
            }
        });

        openLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.OPEN_IN_BROWSER"));

        copyLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.COPY_LINK"));
    }

    /**
     * Initializes the editor by adding a header containing the date.
     * TODO: remove if not used anymore
     */
//    private void initEditor()
//    {
//        Element root = this.document.getDefaultRootElement();
//
//        Date date = new Date(System.currentTimeMillis());
//
//        String chatHeader = "<h1>" + GuiUtils.formatDate(date) + " " + "</h1>";
//
//        try
//        {
//            this.document.insertAfterStart(root, chatHeader);
//        }
//        catch (BadLocationException e)
//        {
//            logger.error("Insert in the HTMLDocument failed.", e);
//        }
//        catch (IOException e)
//        {
//            logger.error("Insert in the HTMLDocument failed.", e);
//        }
//    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message.
     * @return the formatted message
     */
    public String processMessage(ChatMessage chatMessage)
    {
        String contactName = chatMessage.getContactName();
        String contentType = chatMessage.getContentType();
        long date = chatMessage.getDate();
        String messageType = chatMessage.getMessageType();
        String messageTitle = chatMessage.getMessageTitle();
        String message = chatMessage.getMessage();

        String msgID = "message";
        String msgHeaderID = "messageHeader";
        String chatString = "";
        String endHeaderTag = "";
        String dateString = getDateString(date);

        String startDivTag = "<DIV identifier=\"" + msgID + "\">";
        String startHistoryDivTag
            = "<DIV identifier=\"" + msgID + "\" style=\"color:#707070;\">";
        String startSystemDivTag
            = "<DIV identifier=\"systemMessage\" style=\"color:#627EB7;\">";
        String endDivTag = "</DIV>";

        String startPlainTextTag;
        String endPlainTextTag;

        if (contentType != null && contentType.equals(HTML_CONTENT_TYPE))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = START_PLAINTEXT_TAG;
            endPlainTextTag = END_PLAINTEXT_TAG;
        }

        if (messageType.equals(Chat.INCOMING_MESSAGE))
        {
            this.lastIncomingMsgTimestamp = System.currentTimeMillis();

            chatString      = "<h2 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h2>";

            chatString += dateString + contactName + " at "
                + GuiUtils.formatTime(date) + endHeaderTag + startDivTag
                + startPlainTextTag + formatMessage(message, contentType)
                + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.SMS_MESSAGE))
        {
            chatString      = "<h2 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h2>";

            chatString += "SMS: " + dateString + contactName + " at "
                + GuiUtils.formatTime(date) + endHeaderTag + startDivTag
                + startPlainTextTag + formatMessage(message, contentType)
                + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.OUTGOING_MESSAGE))
        {
            chatString      = "<h3 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h3>";

            chatString += dateString
                + GuiActivator.getResources()
                    .getI18NString("service.gui.ME")
                + " at " + GuiUtils.formatTime(date) + endHeaderTag
                + startDivTag + startPlainTextTag
                + formatMessage(message, contentType) + endPlainTextTag
                + endDivTag;
        }
        else if (messageType.equals(Chat.STATUS_MESSAGE))
        {
            chatString =    "<h4 identifier=\"statusMessage\" date=\""
                            + date + "\">";
            endHeaderTag = "</h4>";

            chatString += GuiUtils.formatTime(date)
                + " " + contactName + " "
                + message
                + endHeaderTag;
        }
        else if (messageType.equals(Chat.ACTION_MESSAGE))
        {
            chatString =    "<p identifier=\"actionMessage\" date=\""
                            + date + "\">";
            endHeaderTag = "</p>";

            chatString += "* " + GuiUtils.formatTime(date)
                + " " + contactName + " "
                + message
                + endHeaderTag;
        }
        else if (messageType.equals(Chat.SYSTEM_MESSAGE))
        {
            chatString += startSystemDivTag
                + startPlainTextTag
                + formatMessage(message, contentType)
                + endPlainTextTag
                + endDivTag;
        }
        else if (messageType.equals(Chat.ERROR_MESSAGE))
        {
            chatString      = "<h6 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h6>";

            String errorIcon = "<IMG SRC='"
                + ImageLoader.getImageUri(ImageLoader.EXCLAMATION_MARK)
                + "' </IMG>";

            chatString += errorIcon
                + messageTitle
                + endHeaderTag + "<h5>" + message + "</h5>";
        }
        else if (messageType.equals(Chat.HISTORY_INCOMING_MESSAGE))
        {
            chatString      = "<h2 identifier='"
                            + msgHeaderID
                            + "' date=\""
                            + date + "\">";

            endHeaderTag = "</h2>";

            chatString += dateString + contactName + " at "
                + GuiUtils.formatTime(date) + endHeaderTag + startHistoryDivTag
                + startPlainTextTag + formatMessage(message, contentType)
                + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.HISTORY_OUTGOING_MESSAGE))
        {
            chatString      = "<h3 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h3>";

            chatString += dateString
                + GuiActivator.getResources()
                    .getI18NString("service.gui.ME")
                + " at " + GuiUtils.formatTime(date) + endHeaderTag
                + startHistoryDivTag + startPlainTextTag
                + formatMessage(message, contentType) + endPlainTextTag
                + endDivTag;
        }

        return chatString;
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message
     * @param keyword
     * @return the processed message
     */
    public String processMessage(ChatMessage chatMessage, String keyword)
    {
        if (keyword != null && keyword.length() != 0)
        {
            chatMessage
                .setMessage(
                    processKeyword(
                        chatMessage.getMessage(),
                        chatMessage.getContentType(),
                        keyword));
        }

        return this.processMessage(chatMessage);
    }

    /**
     * Appends the given string at the end of the contained in this panel
     * document.
     *
     * @param chatString the string to append
     */
    public void appendMessageToEnd(String chatString)
    {
        Element root = this.document.getDefaultRootElement();

        try
        {
            this.document.insertAfterEnd(root
                .getElement(root.getElementCount() - 1), chatString);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }

        if (!isHistory)
            this.ensureDocumentSize();

        // Scroll to the last inserted text in the document.
        this.scrollToBottom();
    }

    /**
     * Inserts the given string at the beginning of the contained in this panel
     * document.
     *
     * @param chatString the string to insert
     */
    public void insertMessageAfterStart(String chatString)
    {
        Element root = this.document.getDefaultRootElement();

        try
        {
            this.document.insertBeforeStart(root.getElement(0), chatString);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }

        if (!isHistory)
            this.ensureDocumentSize();

        // Scroll to the last inserted text in the document.
        this.scrollToBottom();
    }

    /**
     * Ensures that the document won't become too big. When the document reaches
     * a certain size the first message in the page is removed.
     */
    private void ensureDocumentSize()
    {
        if (document.getLength() > Chat.CHAT_BUFFER_SIZE)
        {
            int msgElementCount = 0;

            Element firstMsgElement = null;

            int firstMsgIndex = 0;

            Element rootElement = this.document.getDefaultRootElement();
            // Count how many messages we have in the document.
            for (int i = 0; i < rootElement.getElementCount(); i++)
            {
                String idAttr = (String) rootElement.getElement(i)
                    .getAttributes().getAttribute("identifier");

                if(idAttr != null
                    && (idAttr.equals("message")
                        || idAttr.equals("statusMessage")
                        || idAttr.equals("systemMessage")))
                {
                    if(firstMsgElement == null)
                    {
                        firstMsgElement = rootElement.getElement(i);
                        firstMsgIndex = i;
                    }

                    msgElementCount++;
                }
            }

            // If we doesn't have any known elements in the document or if we
            // have only one long message we don't want to remove it.
            if(firstMsgElement == null || msgElementCount < 2)
                return;

            try
            {
                // Remove the header of the message if such exists.
                if(firstMsgIndex > 0)
                {
                    Element headerElement = rootElement.getElement(firstMsgIndex - 1);

                    String idAttr = (String) headerElement
                        .getAttributes().getAttribute("identifier");

                    if(idAttr != null && idAttr.equals("messageHeader"))
                    {
                        this.document.remove(headerElement.getStartOffset(),
                            headerElement.getEndOffset()
                                - headerElement.getStartOffset());
                    }
                }

                // Remove the message itself.
                this.document.remove(firstMsgElement.getStartOffset(),
                        firstMsgElement.getEndOffset()
                            - firstMsgElement.getStartOffset());
            }
            catch (BadLocationException e)
            {
                logger.error("Error removing messages from chat: ", e);
            }
        }
    }

    /**
     * Highlights keywords searched in the history.
     *
     * @param message the source message
     * @param contentType the content type
     * @param keyword the searched keyword
     * @return the formatted message
     */
    private String processKeyword(  String message,
                                    String contentType,
                                    String keyword)
    {
        String startPlainTextTag;
        String endPlainTextTag;

        if (contentType != null && contentType.equals(HTML_CONTENT_TYPE))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = START_PLAINTEXT_TAG;
            endPlainTextTag = END_PLAINTEXT_TAG;
        }

        Pattern p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(message);

        StringBuffer msgBuffer = new StringBuffer();

        boolean matchSuccessfull = false;

        while (m.find())
        {
            if (!matchSuccessfull)
                matchSuccessfull = true;

            String matchGroup = m.group().trim();

            String replacement = endPlainTextTag + "<h7>" + matchGroup + "</h7>"
                + startPlainTextTag;

            m.appendReplacement(msgBuffer, GuiUtils
                .replaceSpecialRegExpChars(replacement));
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }

    /**
     * Formats the given message. Processes all smiley chars, new lines and
     * links.
     *
     * @param message the message to be formatted
     * @param contentType the content type of the message to be formatted
     * @return the formatted message
     */
    private String formatMessage(String message, String contentType)
    {
        // If the message content type is HTML we won't process links and
        // new lines, but only the smileys.
        if (!HTML_CONTENT_TYPE.equals(contentType))
        {
            message = processNewLines(processLinksAndHTMLChars(message));
        }
        // If the message content is HTML, we process br and img tags.
        else
        {
            message = processImgTags(processBrTags(message));
        }

        return processSmileys(message, contentType);
    }

    /**
     * Formats all links in the given message and escapes special HTML
     * characters such as &lt;, &gt;, &amp; and &quot; in order to prevent HTML
     * injection in plain-text messages such as writing
     * <code>&lt;/PLAINTEXT&gt;</code>, HTML which is going to be rendered as
     * such and <code>&lt;PLAINTEXT&gt;</code>. The two procedures are carried
     * out in one call in order to not break URLs which contain special HTML
     * characters such as &amp;.
     * 
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
    private String processLinksAndHTMLChars(String message)
    {
        Matcher m = URL_PATTERN.matcher(message);
        StringBuffer msgBuffer = new StringBuffer();
        int prevEnd = 0;

        while (m.find())
        {
            msgBuffer
                .append(
                    processHTMLChars(message.substring(prevEnd, m.start())));
            prevEnd = m.end();

            String url = m.group().trim();

            msgBuffer.append(END_PLAINTEXT_TAG);
            msgBuffer.append("<A href=\"");
            if (url.startsWith("www"))
                msgBuffer.append("http://");
            msgBuffer.append(url);
            msgBuffer.append("\">");
            msgBuffer.append(url);
            msgBuffer.append("</A>");
            msgBuffer.append(START_PLAINTEXT_TAG);
        }
        msgBuffer.append(processHTMLChars(message.substring(prevEnd)));

        return msgBuffer.toString();
    }

    /**
     * Escapes special HTML characters such as &lt;, &gt;, &amp; and &quot; in
     * the specified message.
     *
     * @param message the message to be processed
     * @return the processed message with escaped special HTML characters
     */
    private String processHTMLChars(String message)
    {
        return
            message
                .replace("&", "&amp;")
                    .replace("<", "&lt;")
                        .replace(">", "&gt;")
                            .replace("\"", "&quot;");
    }

    /**
     * Formats message new lines.
     *
     * @param message The source message string.
     * @return The message string with properly formatted new lines.
     */
    private String processNewLines(String message)
    {

        /*
         * <br> tags are needed to visualize a new line in the html format, but
         * when copied to the clipboard they are exported to the plain text
         * format as ' ' and not as '\n'.
         *
         * See bug N4988885:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4988885
         *
         * To fix this we need "&#10;" - the HTML-Code for ASCII-Character No.10
         * (Line feed).
         */
        return
            message
                .replaceAll(
                    "\n",
                    END_PLAINTEXT_TAG + "<BR>&#10;" + START_PLAINTEXT_TAG);
    }

    /**
     * Formats message smileys.
     *
     * @param message the source message string
     * @param contentType the content type
     * @return the message string with properly formated smileys
     */
    private String processSmileys(String message, String contentType)
    {
        String startPlainTextTag;
        String endPlainTextTag;
        if (!HTML_CONTENT_TYPE.equals(contentType))
        {
            startPlainTextTag = START_PLAINTEXT_TAG;
            endPlainTextTag = END_PLAINTEXT_TAG;
        }
        else
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }

        Collection<Smiley> smileys = ImageLoader.getDefaultSmileyPack();
        Matcher m = getSmileyPattern(smileys).matcher(message);
        StringBuffer msgBuffer = new StringBuffer();
        int prevEnd = 0;

        while (m.find())
        {
            msgBuffer.append(message.substring(prevEnd, m.start()));
            prevEnd = m.end();

            String smileyString = m.group().trim();

            msgBuffer.append(endPlainTextTag);
            msgBuffer.append("<IMG SRC=\"");
            msgBuffer
                .append(ImageLoader.getSmiley(smileyString).getImagePath());
            msgBuffer.append("\" ALT=\"");
            msgBuffer.append(smileyString);
            msgBuffer.append("\"></IMG>");
            msgBuffer.append(startPlainTextTag);
        }
        msgBuffer.append(message.substring(prevEnd));

        return msgBuffer.toString();
    }

    /**
     * Gets a compiled <tt>Pattern</tt> which matches the smiley strings of the
     * specified <tt>Collection</tt> of <tt>Smiley</tt>s.
     *
     * @param smileys the <tt>Collection</tt> of <tt>Smiley</tt>s for which to
     * get a compiled <tt>Pattern</tt> which matches its smiley strings
     * @return a compiled <tt>Pattern</tt> which matches the smiley strings of
     * the specified <tt>Collection</tt> of <tt>Smiley</tt>s
     */
    private static Pattern getSmileyPattern(Collection<Smiley> smileys)
    {
        synchronized (smileyStrings)
        {
            boolean smileyStringsIsEqual;

            if (smileyPattern == null)
                smileyStringsIsEqual = false;
            else
            {
                smileyStringsIsEqual = true;

                int smileyStringIndex = 0;
                int smileyStringCount = smileyStrings.size();

                smileyLoop: for (Smiley smiley : smileys)
                    for (String smileyString : smiley.getSmileyStrings())
                        if ((smileyStringIndex < smileyStringCount)
                                && smileyString
                                    .equals(
                                        smileyStrings.get(smileyStringIndex)))
                            smileyStringIndex++;
                        else
                        {
                            smileyStringsIsEqual = false;
                            break smileyLoop;
                        }
                if (smileyStringsIsEqual
                        && (smileyStringIndex != smileyStringCount))
                    smileyStringsIsEqual = false;
            }

            if (!smileyStringsIsEqual)
            {
                smileyStrings.clear();

                StringBuffer regex = new StringBuffer();

                regex.append("(?<!(alt='|alt=\"))(");
                for (Smiley smiley : smileys)
                    for (String smileyString : smiley.getSmileyStrings())
                    {
                        smileyStrings.add(smileyString);

                        regex
                            .append(
                                    GuiUtils
                                        .replaceSpecialRegExpChars(
                                            smileyString))
                                .append("|");
                    }
                regex = regex.deleteCharAt(regex.length() - 1);
                regex.append(')');

                smileyPattern = Pattern.compile(regex.toString());
            }
            return smileyPattern;
        }
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

            this.currentHref = href;
        }
        else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
        {
            this.currentHref = "";
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
    public long getLastIncomingMsgTimestamp()
    {
        return lastIncomingMsgTimestamp;
    }

    /**
     * Moves the caret to the end of the editor pane.
     */
    public void scrollToBottom()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                getVerticalScrollBar()
                    .setValue(getVerticalScrollBar().getMaximum());
            }
        });
    }

    /**
     * When a right button click is performed in the editor pane, a popup menu
     * is opened.
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
            GuiActivator.getBrowserLauncher().openURL(currentHref);
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
        if (currentHref != null && currentHref.length() != 0)
        {
            rightButtonMenu.insert(openLinkItem, 0);
            rightButtonMenu.insert(copyLinkItem, 1);
            rightButtonMenu.insert(copyLinkSeparator, 2);
        }
        else
        {
            rightButtonMenu.remove(openLinkItem);
            rightButtonMenu.remove(copyLinkItem);
            rightButtonMenu.remove(copyLinkSeparator);
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
    }

    /**
     * Sets the given document to the editor pane in this panel.
     *
     * @param doc the document to set
     */
    public void setContent(HTMLDocument doc)
    {
        this.document = doc;
        this.chatTextPane.setDocument(doc);
    }

    /**
     * Sets the default document contained in this panel, created on init or
     * when clear is invoked.
     */
    public void setDefaultContent()
    {
        this.chatTextPane.setDocument(document);
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
        Element rootElement = this.document.getDefaultRootElement();

        Element firstMessageElement = null;

        for(int i = 0; i < rootElement.getElementCount(); i ++)
        {
            String idAttr = (String) rootElement.getElement(i)
                    .getAttributes().getAttribute("identifier");

            if (idAttr != null && idAttr.equals("messageHeader"))
            {
                firstMessageElement = rootElement.getElement(i);
                break;
            }
        }

        if(firstMessageElement == null)
            return new Date(Long.MAX_VALUE);

        String dateObject = firstMessageElement
            .getAttributes().getAttribute("date").toString();

        return new Date(Long.parseLong(dateObject));
    }

    /**
     * Returns the date of the last message in the current page.
     *
     * @return the date of the last message in the current page
     */
    public Date getPageLastMsgTimestamp()
    {
        Element rootElement = this.document.getDefaultRootElement();

        Element lastMessageElement = null;

        for(int i = rootElement.getElementCount() - 1; i >= 0; i --)
        {
            String idAttr = (String) rootElement.getElement(i)
                .getAttributes().getAttribute("identifier");

            if (idAttr != null && idAttr.equals("messageHeader"))
            {
                lastMessageElement = rootElement.getElement(i);
                break;
            }
        }

        if(lastMessageElement == null)
            return new Date(0);

        String dateObject = lastMessageElement
            .getAttributes().getAttribute("date").toString();

        return new Date(Long.parseLong(dateObject));
    }

    /**
     * Formats HTML tags &lt;br/&gt; to &lt;br&gt; or &lt;BR/&gt; to &lt;BR&gt;.
     * The reason of this function is that the ChatPanel does not support
     * &lt;br /&gt; closing tags (XHTML syntax), thus we have to remove every
     * slash from each &lt;br /&gt; tags.
     * @param message The source message string.
     * @return The message string with properly formatted &lt;br&gt; tags.
     */
    private String processBrTags(String message)
    {
        // The resulting message after being processed by this function.
        StringBuffer processedMessage = new StringBuffer();

        // Compile the regex to match something like <br .. /> or <BR .. />.
        // This regex is case sensitive and keeps the style or other
        // attributes of the <br> tag.
        Matcher m
            = Pattern.compile("<\\s*[bB][rR](.*?)(/\\s*>)").matcher(message);
        int start = 0;

        // while we find some <br /> closing tags with a slash inside.
        while(m.find())
        {
            // First, we have to copy all the message preceding the <br> tag.
            processedMessage.append(message.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            int slash_index = m.group().lastIndexOf("/");
            // We copy the <br> tag till the slash exclude.
            processedMessage.append(m.group().substring(0, slash_index));
            // We copy all the end of the tag following the slash exclude.
            processedMessage.append(m.group().substring(slash_index+1));
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <br> tag, or the whole message if there is no <br> tag.
        processedMessage.append(message.substring(start));

        return processedMessage.toString();
    }

    /**
     * Formats HTML tags &lt;img ... /&gt; to &lt; img ... &gt;&lt;/img&gt; or
     * &lt;IMG ... /&gt; to &lt;IMG&gt;&lt;/IMG&gt;.
     * The reason of this function is that the ChatPanel does not support
     * &lt;img /&gt; tags (XHTML syntax).
     * Thus, we remove every slash from each &lt;img /&gt; and close it with a
     * separate closing tag.
     * @param message The source message string.
     * @return The message string with properly formatted &lt;img&gt; tags.
     */
    private String processImgTags(String message)
    {
        // The resulting message after being processed by this function.
        StringBuffer processedMessage = new StringBuffer();

        // Compile the regex to match something like <img ... /> or
        // <IMG ... />. This regex is case sensitive and keeps the style,
        // src or other attributes of the <img> tag.
        Pattern p = Pattern.compile("<\\s*[iI][mM][gG](.*?)(/\\s*>)");
        Matcher m = p.matcher(message);
        int slash_index;
        int start = 0;

        // while we find some <img /> self-closing tags with a slash inside.
        while(m.find()){
            // First, we have to copy all the message preceding the <img> tag.
            processedMessage.append(message.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            slash_index = m.group().lastIndexOf("/");
            // We copy the <img> tag till the slash exclude.
            processedMessage.append(m.group().substring(0, slash_index));
            // We copy all the end of the tag following the slash exclude.
            processedMessage.append(m.group().substring(slash_index+1));
            // We close the tag with a separate closing tag.
            processedMessage.append("</img>");
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <img> tag, or the whole message if there is no <img> tag.
        processedMessage.append(message.substring(start));

        return processedMessage.toString();
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
        public String getToolTipText(MouseEvent event)
        {
            if(currentHref != null && currentHref.length() != 0)
                return currentHref;
            else
                return null;
        }
    }

    /**
     * Adds a custom component at the end of the conversation.
     * 
     * @param component the component to add at the end of the conversation.
     */
    public void addComponent(ChatConversationComponent component)
    {
        Style style = document.getStyleSheet().addStyle(
            StyleConstants.ComponentElementName,
            document.getStyleSheet().getStyle("body"));

        // The image must first be wrapped in a style
        style.addAttribute(
            AbstractDocument.ElementNameAttribute,
            StyleConstants.ComponentElementName);

        TransparentPanel wrapPanel = new TransparentPanel(new BorderLayout());

        wrapPanel.add(component, BorderLayout.NORTH);

        style.addAttribute(
            StyleConstants.ComponentAttribute,
            wrapPanel);

        style.addAttribute(
            "identifier",
            "messageHeader");

        style.addAttribute(
            "date",
            component.getDate().getTime());

        // Insert the component style at the end of the text
        try
        {
            document.insertString(  document.getLength(),
                                    "ignored text", style);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }

        this.scrollToBottom();
    }

    /**
     * Returns the date string to show for the given date.
     * 
     * @param date the date to format
     * @return the date string to show for the given date
     */
    public static String getDateString(long date)
    {
        if (GuiUtils.compareDates(date, System.currentTimeMillis()) < 0)
        {
            return GuiUtils.formatDate(date) + " ";
        }

        return "";
    }
}
