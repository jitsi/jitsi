/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import java.util.Map.*;
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
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.SwingWorker;

/**
 * The <tt>ChatConversationPanel</tt> is the panel, where all sent and received
 * messages appear. All data is stored in an HTML document. An external CSS file
 * is applied to the document to provide the look&feel. All smileys and link
 * strings are processed and finally replaced by corresponding images and HTML
 * links.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ChatConversationPanel
    extends SCScrollPane
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
        = Pattern.compile(
            "("
            + "(\\bwww\\.[^\\s<>\"]+\\.[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // wwwURL
            + "|"
            + "(\\b\\w+://[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // protocolURL
            + ")");

    /**
     * The component rendering chat conversation panel text.
     */
    private final JTextPane chatTextPane = new MyTextPane();

    /**
     * The editor kit used by the text component.
     */
    private final HTMLEditorKit editorKit;

    /**
     * The document used by the text component.
     */
    private HTMLDocument document;

    /**
     * The parent container.
     */
    private final ChatConversationContainer chatContainer;

    /**
     * The menu shown on right button mouse click.
     */
    private final ChatRightButtonMenu rightButtonMenu;

    /**
     * The currently shown href.
     */
    private String currentHref;

    /**
     * The copy link item, contained in the right mouse click menu.
     */
    private final JMenuItem copyLinkItem;

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
    private long lastIncomingMsgTimestamp;

    /**
     * Indicates if this component is rendering a history conversation.
     */
    private final boolean isHistory;

    /**
     * The html text content type.
     */
    public static final String HTML_CONTENT_TYPE = "text/html";

    /**
     * The plain text content type.
     */
    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * The indicator which determines whether an automatic scroll to the bottom
     * of {@link #chatTextPane} is to be performed.
     */
    private boolean scrollToBottomIsPending = false;

    /**
     * The implementation of the routine which scrolls {@link #chatTextPane} to its
     * bottom.
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
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
    };

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
            }
        });

        openLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.OPEN_IN_BROWSER"));

        copyLinkItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.COPY_LINK"));

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
     * @param chatMessage the message
     * @param keyword a substring of <tt>chatMessage</tt> to be highlighted upon
     * display of <tt>chatMessage</tt> in the UI
     * @return the processed message
     */
    public String processMessage(ChatMessage chatMessage, String keyword)
    {
        String contactName = chatMessage.getContactName();
        String contactDisplayName = chatMessage.getContactDisplayName();
        if (contactDisplayName == null
                || contactDisplayName.trim().length() <= 0)
            contactDisplayName = contactName;

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

        if (HTML_CONTENT_TYPE.equals(contentType))
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

            chatString      = "<h2 identifier=\"" + msgHeaderID + "\""
                                    + " date=\"" + date + "\">"
                                    + "<a style=\"color:#ef7b1e;"
                                    + "font-weight:bold;"
                                    + "text-decoration:none;\" "
                                    + "href=\"" + contactName + "\">";

            endHeaderTag = "</a></h2>";

            chatString
                += dateString + contactDisplayName + " at "
                    + GuiUtils.formatTime(date)
                    + endHeaderTag + startDivTag + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.SMS_MESSAGE))
        {
            chatString      = "<h2 identifier=\""
                            + msgHeaderID
                            + "\" date=\""
                            + date + "\">";

            endHeaderTag = "</h2>";

            chatString
                += "SMS: " + dateString + contactName + " at "
                    + GuiUtils.formatTime(date) + endHeaderTag + startDivTag
                    + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.OUTGOING_MESSAGE))
        {
            chatString      = "<h3 identifier=\"" + msgHeaderID + "\""
                                    + " date=\"" + date + "\">"
                                    + "<a style=\"color:#2e538b;"
                                    + "font-weight:bold;"
                                    + "text-decoration:none;\" "
                                    + "href=\"" + contactName + "\">";

            endHeaderTag = "</a></h3>";

            chatString
                += dateString + contactDisplayName + " at "
                    + GuiUtils.formatTime(date) + endHeaderTag
                    + startDivTag + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.STATUS_MESSAGE))
        {
            chatString =    "<h4 identifier=\"statusMessage\" date=\""
                            + date + "\">";
            endHeaderTag = "</h4>";

            chatString
                += GuiUtils.formatTime(date) + " " + contactName + " " + message
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
            chatString
                += startSystemDivTag + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
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
            chatString      = "<h2 identifier=\"" + msgHeaderID + "\""
                                    + " date=\"" + date + "\">"
                                + "<a style=\"color:#ef7b1e;"
                                + "font-weight:bold;"
                                + "text-decoration:none;\" "
                                + "href=\"" + contactName + "\">";

            endHeaderTag = "</a></h2>";

            chatString
                += dateString + contactDisplayName
                    + " at " + GuiUtils.formatTime(date)
                    + endHeaderTag + startHistoryDivTag + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
        }
        else if (messageType.equals(Chat.HISTORY_OUTGOING_MESSAGE))
        {
            chatString      = "<h3 identifier=\"" + msgHeaderID + "\""
                                    + " date=\"" + date + "\">"
                                + "<a style=\"color:#2e538b;"
                                + "font-weight:bold;"
                                + "text-decoration:none;\" "
                                + "href=\"" + contactName + "\">";

            endHeaderTag = "</h3>";

            chatString
                += dateString
                    + contactDisplayName
                    + " at " + GuiUtils.formatTime(date) + endHeaderTag
                    + startHistoryDivTag + startPlainTextTag
                    + formatMessage(message, contentType, keyword)
                    + endPlainTextTag + endDivTag;
        }

        return chatString;
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message.
     * @return the formatted message
     */
    public String processMessage(ChatMessage chatMessage)
    {
        return processMessage(chatMessage, null);
    }

    /**
     * Appends the given string at the end of the contained in this panel
     * document.
     *
     * @param chatString the string to append
     */
    public void appendMessageToEnd(String chatString, String contentType)
    {
        synchronized (scrollToBottomRunnable)
        {
            Element root = document.getDefaultRootElement();

//          Need to call explicitly scrollToBottom, because for some
//          reason the componentResized event isn't fired every time we
//          add text.
//          Replaced by the code on line: 573.
//
//            scrollToBottomIsPending = true;

            try
            {
                document
                    .insertAfterEnd(
                        root.getElement(root.getElementCount() - 1),
                        chatString);

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
            if (!isHistory)
                ensureDocumentSize();

            // Process replacements.
            final Element elem;
            /*
             * Check to make sure element isn't the first element in the HTML
             * document.
             */
            if (!(root.getElementCount() < 2))
            {
                elem = root.getElement(root.getElementCount() - 2);
            }
            else
                elem = root.getElement(1);

            /*
             * Replacements will be processed only if it is enabled in the
             * property
             */
            if (GuiActivator.getConfigurationService().getBoolean(
                ReplacementProperty.REPLACEMENT_ENABLE, true)
                || GuiActivator.getConfigurationService().getBoolean(
                    ReplacementProperty.getPropertyName("SMILEY"), true))
            {
                processReplacement(elem, chatString, contentType);
            }
        }
    }

    /**
    * Formats the given message. Processes the messages and replaces links to
    * video/image sources with their previews or any other substitution. Spawns
    * a separate thread for replacement.
    * 
    * @param elem the element in the HTML Document.
    * @param chatString the message.
    */
    private void processReplacement(final Element elem,
                                    final String chatString,
                                    final String contentType)
    {
       final String chatFinal = chatString;

       SwingWorker worker = new SwingWorker()
       {
           public Object construct() throws Exception
           {
               String temp = "", msgStore = chatFinal;

               boolean isEnabled
                   = GuiActivator.getConfigurationService().getBoolean(
                       ReplacementProperty.REPLACEMENT_ENABLE, true);

               Map<String, ReplacementService> listSources
                   = GuiActivator.getReplacementSources();

               Iterator<Entry<String, ReplacementService>> entrySetIter
                   = listSources.entrySet().iterator();

               for (int i = 0; i < listSources.size(); i++)
               {
                   Map.Entry<String, ReplacementService> entry
                       = entrySetIter.next();

                   ReplacementService source = entry.getValue();

                   boolean isSmiley
                       = source instanceof SmiliesReplacementService;

                   if (!(GuiActivator.getConfigurationService().getBoolean(
                       ReplacementProperty.getPropertyName(source
                       .getSourceName()), true) && (isEnabled || isSmiley)))
                       continue;

                   String sourcePattern = source.getPattern();
                   Pattern p = Pattern.compile(sourcePattern,
                                   Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

                   Matcher m = p.matcher(msgStore);

                   String startPlainTextTag = "";
                   String endPlainTextTag = "";

                   if (!HTML_CONTENT_TYPE.equals(contentType))
                   {
                       startPlainTextTag = START_PLAINTEXT_TAG;
                       endPlainTextTag = END_PLAINTEXT_TAG;
                   }

                   int count = 0, startPos = 0;
                   StringBuffer msgBuff = new StringBuffer();

                   while (m.find())
                   {
                       count++;
                       msgBuff.append(msgStore.substring(startPos, m.start()));
                       startPos = m.end();

                       temp = source.getReplacement(m.group());

                       if(!temp.equals(m.group(0)) || source.getSourceName()
                               .equals("DIRECTIMAGE"))
                       {
                           if(isSmiley)
                           {
                               msgBuff.append(endPlainTextTag);
                               msgBuff.append("<IMG SRC=\"");
                           }
                           else
                           {
                               msgBuff.append(
                                   "<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
                           }

                           msgBuff.append(temp);
                           msgBuff.append("\" BORDER=\"0\" ALT=\"");
                           msgBuff.append(m.group(0));
                           msgBuff.append("\"></IMG>");

                           if(isSmiley)
                               msgBuff.append(startPlainTextTag);
                       }
                       else
                       {
                           msgBuff.append(
                               msgStore.substring(m.start(), m.end()));
                       }
                   }

                   msgBuff.append(msgStore.substring(startPos));

                   /*
                    * replace the msgStore variable with the current replaced
                    * message before next iteration
                    */
                   if (!msgBuff.toString().equals(msgStore))
                   {
                       msgStore = msgBuff.toString();
                   }
               }

               if (!msgStore.equals(chatFinal))
               {
                   synchronized (scrollToBottomRunnable)
                   {
                       scrollToBottomIsPending = true;
                       document.setOuterHTML(elem, msgStore.toString()
                           .substring(msgStore.indexOf("<DIV")));
                   }
               }
               return "";
           }
       };
       worker.start();
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

        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = START_PLAINTEXT_TAG;
            endPlainTextTag = END_PLAINTEXT_TAG;
        }

        Matcher m
            = Pattern
                .compile(keyword, Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer msgBuffer = new StringBuffer();
        int prevEnd = 0;

        while (m.find())
        {
            msgBuffer.append(message.substring(prevEnd, m.start()));
            prevEnd = m.end();

            String keywordMatch = m.group().trim();

            msgBuffer.append(endPlainTextTag);
            msgBuffer.append("<b>");
            msgBuffer.append(keywordMatch);
            msgBuffer.append("</b>");
            msgBuffer.append(startPlainTextTag);
        }

        /*
         * If the keyword didn't match, let the outside world be able to
         * discover it.
         */
        if (prevEnd == 0)
            return message;

        msgBuffer.append(message.substring(prevEnd));
        return msgBuffer.toString();
    }

    /**
     * Formats the given message. Processes all smiley chars, new lines and
     * links.
     *
     * @param message the message to be formatted
     * @param contentType the content type of the message to be formatted
     * @param keyword the word to be highlighted
     * @return the formatted message
     */
    private String formatMessage(String message,
                                 String contentType,
                                 String keyword)
    {
        // If the message content type is HTML we won't process links and
        // new lines, but only the smileys.
        if (!HTML_CONTENT_TYPE.equals(contentType))
        {

            /*
             * We disallow HTML in plain-text messages. But processKeyword
             * introduces HTML. So we'll allow HTML if processKeyword has
             * introduced it in order to not break highlighting.
             */
            boolean processHTMLChars;

            if ((keyword != null) && (keyword.length() != 0))
            {
                String messageWithProcessedKeyword
                    = processKeyword(message, contentType, keyword);

                /*
                 * The same String instance will be returned if there was no
                 * keyword match. Calling #equals() is expensive so == is
                 * intentional.
                 */
                processHTMLChars = (messageWithProcessedKeyword == message);
                message = messageWithProcessedKeyword;
            }
            else
                processHTMLChars = true;

            message
                = processNewLines(
                    processLinksAndHTMLChars(message, processHTMLChars));
        }
        // If the message content is HTML, we process br and img tags.
        else
        {
            if ((keyword != null) && (keyword.length() != 0))
                message = processKeyword(message, contentType, keyword);
            message = processImgTags(processBrTags(message));
        }

        return message;
    }

    /**
     * Formats all links in a given message and optionally escapes special HTML
     * characters such as &lt;, &gt;, &amp; and &quot; in order to prevent HTML
     * injection in plain-text messages such as writing
     * <code>&lt;/PLAINTEXT&gt;</code>, HTML which is going to be rendered as
     * such and <code>&lt;PLAINTEXT&gt;</code>. The two procedures are carried
     * out in one call in order to not break URLs which contain special HTML
     * characters such as &amp;.
     * 
     * @param message The source message string.
     * @param processHTMLChars  <tt>true</tt> to escape the special HTML chars;
     * otherwise, <tt>false</tt>
     * @return The message string with properly formatted links.
     */
    private String processLinksAndHTMLChars(String message,
                                            boolean processHTMLChars)
    {
        Matcher m = URL_PATTERN.matcher(message);
        StringBuffer msgBuffer = new StringBuffer();
        int prevEnd = 0;

        while (m.find())
        {
            String fromPrevEndToStart = message.substring(prevEnd, m.start());

            if (processHTMLChars)
                fromPrevEndToStart = processHTMLChars(fromPrevEndToStart);
            msgBuffer.append(fromPrevEndToStart);
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

        String fromPrevEndToEnd = message.substring(prevEnd);

        if (processHTMLChars)
            fromPrevEndToEnd = processHTMLChars(fromPrevEndToEnd);
        msgBuffer.append(fromPrevEndToEnd);

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
                    END_PLAINTEXT_TAG + "<BR/>&#10;" + START_PLAINTEXT_TAG);
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
     * @param document the document to set
     */
    public void setContent(HTMLDocument document)
    {
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

            style
                .addAttribute(StyleConstants.ComponentAttribute, wrapPanel);
            style.addAttribute("identifier", "messageHeader");
            style.addAttribute("date", component.getDate().getTime());

            scrollToBottomIsPending = true;

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
     * Returns the date string to show for the given date.
     * 
     * @param date the date to format
     * @return the date string to show for the given date
     */
    public static String getDateString(long date)
    {
        if (GuiUtils.compareDatesOnly(date, System.currentTimeMillis()) < 0)
        {
            StringBuffer dateStrBuf = new StringBuffer();

            GuiUtils.formatDate(date, dateStrBuf);
            dateStrBuf.append(" ");
            return dateStrBuf.toString();
        }

        return "";
    }

    /**
     * Reloads images.
     */
    public void loadSkin()
    {
        openLinkItem.setIcon(
                new ImageIcon(ImageLoader.getImage(ImageLoader.BROWSER_ICON)));
        copyLinkItem.setIcon(
                new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

        getRightButtonMenu().loadSkin();
    }

    /**
     * Highlights the string in multi user chat.
     * 
     * @param message the message to process
     * @param contentType the content type of the message
     * @param keyWord the keyword to highlight
     * @return the message string with the keyword highlighted
     */
    public String processChatRoomHighlight(String message, String contentType,
        String keyWord)
    {
        return processKeyword(message, contentType, keyWord);
    }
    
    public String processMeCommand(ChatMessage chatMessage)
    {
        String contentType = chatMessage.getContentType();
        String message = chatMessage.getMessage();

        String msgID = "message";
        String chatString = "";
        String endHeaderTag = "";

        String startDivTag = "<DIV identifier=\"" + msgID + "\">";
        String endDivTag = "</DIV>";

        String startPlainTextTag;
        String endPlainTextTag;

        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = START_PLAINTEXT_TAG;
            endPlainTextTag = END_PLAINTEXT_TAG;
        }

        if (message.length() > 4 && message.substring(0, 4).equals("/me "))
        {
            chatString = startDivTag + "<B><I>";

            endHeaderTag = "</I></B>" + endDivTag;

            chatString +=

                processHTMLChars("*** " + chatMessage.getContactName() + " "
                    + message.substring(4))
                    + endHeaderTag;

            Map<String, ReplacementService> listSources =
                GuiActivator.getReplacementSources();

            Iterator<Entry<String, ReplacementService>> entrySetIter =
                listSources.entrySet().iterator();
            StringBuffer msgStore = new StringBuffer(chatString);

            for (int i = 0; i < listSources.size(); i++)
            {
                Map.Entry<String, ReplacementService> entry =
                    entrySetIter.next();

                ReplacementService source = entry.getValue();

                boolean isSmiley = source instanceof SmiliesReplacementService;
                if (isSmiley)
                {
                    String sourcePattern = source.getPattern();
                    Pattern p =
                        Pattern.compile(sourcePattern, Pattern.CASE_INSENSITIVE
                            | Pattern.DOTALL);
                    Matcher m = p.matcher(msgStore);

                    StringBuffer msgTemp = new StringBuffer(chatString);

                    while (m.find())
                    {
                        msgTemp.insert(m.start(), startPlainTextTag);
                        msgTemp.insert(m.end() + startPlainTextTag.length(),
                            endPlainTextTag);

                    }
                    if (msgTemp.length() != msgStore.length())
                        msgStore = msgTemp;
                }
            }

            return msgStore.toString();
        }
        else
            return "";
    }
}
