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
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatConversationPanel</tt> is the panel, where all sent and received
 * messages appear. All data is stored in an HTML document. An external CSS file
 * is applied to the document to provide the look&feel. All smilies and link
 * strings are processed and finally replaced by corresponding images and html
 * links.
 *
 * @author Yana Stamcheva
 */
public class ChatConversationPanel
    extends JScrollPane
    implements  HyperlinkListener,
                MouseListener, 
                ClipboardOwner
{

    private static final Logger LOGGER = Logger
            .getLogger(ChatConversationPanel.class.getName());

    private JEditorPane chatEditorPane = new JEditorPane();

    private HTMLEditorKit editorKit = new SIPCommHTMLEditorKit();

    private HTMLDocument document;

    private ChatConversationContainer chatContainer;

    private ChatRightButtonMenu rightButtonMenu;

    private String currentHref;

    private I18NString copyLinkString = Messages.getI18NString("copyLink");
    
    private I18NString openLinkString = Messages.getI18NString("openInBrowser");
    
    private JMenuItem copyLinkItem
        = new JMenuItem(copyLinkString.getText(),
                new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem openLinkItem
        = new JMenuItem(openLinkString.getText(),
                new ImageIcon(ImageLoader.getImage(ImageLoader.BROWSER_ICON)));

    private JSeparator copyLinkSeparator = new JSeparator();
    /*
     * Tooltip on hyperlinks - JDK 1.5+
     *
     * private JPopupMenu linkPopup = new JPopupMenu();
     *
     * private JTextArea hrefItem = new JTextArea();
     *
     * private final int hrefPopupMaxWidth = 300;
     * private final int hrefPopupInitialHeight = 20;
     */

    private Date lastIncomingMsgTimestamp = new Date(0);
    
    private Date pageFirstMsgTimestamp = new Date(Long.MAX_VALUE);
    
    private Date pageLastMsgTimestamp = new Date(0);
    
    private int messagesPerPage = 0;
    
    private boolean isHistory = false;
    
    /**
     * Creates an instance of <tt>ChatConversationPanel</tt>.
     * @param chatContainer The parent <tt>ChatConversationContainer</tt>.
     */
    public ChatConversationPanel(ChatConversationContainer chatContainer)
    {
        super();

        this.chatContainer = chatContainer;

        if(chatContainer instanceof HistoryWindow)
            isHistory = true;
       
        this.rightButtonMenu
            = new ChatRightButtonMenu(this);

        this.document = (HTMLDocument) editorKit.createDefaultDocument();
        
        this.chatEditorPane.setContentType("text/html");

        this.chatEditorPane.setEditable(false);

        this.chatEditorPane.setEditorKitForContentType("text/html", editorKit);
        this.chatEditorPane.setEditorKit(editorKit);

        this.chatEditorPane.setDocument(document);

        Constants.loadSimpleStyle(document.getStyleSheet());

        this.chatEditorPane.addHyperlinkListener(this);
        this.chatEditorPane.addMouseListener(this);

        this.setWheelScrollingEnabled(true);

        this.getViewport().add(chatEditorPane);

        this.getVerticalScrollBar().setUnitIncrement(30);

        this.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));
        
        ToolTipManager.sharedInstance().registerComponent(chatEditorPane);

        copyLinkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection
                    = new StringSelection(currentHref);
                Clipboard clipboard
                    = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection,
                            ChatConversationPanel.this);
            }
        });
        openLinkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GuiActivator.getBrowserLauncher().openURL(currentHref);
            }
        });
        
        openLinkItem.setMnemonic(openLinkString.getMnemonic());
        copyLinkItem.setMnemonic(copyLinkString.getMnemonic());
        
        /*
         * Tooltip on hyperlinks - JDK 1.5+
         *
         * this.hrefItem.setLineWrap(true);
         * this.linkPopup.add(hrefItem);
         * this.hrefItem.setSize(new Dimension(hrefPopupMaxWidth,
         *       hrefPopupInitialHeight));
         */
    }

    /**
     * Initializes the editor by adding a header containing the
     * date.
     */
    private void initEditor()
    {
        Element root = this.document.getDefaultRootElement();

        Date date = new Date(System.currentTimeMillis());
        
        String chatHeader = "<h1>" + GuiUtils.formatDate(date) + " " + "</h1>";

        try {
            this.document.insertAfterStart(root, chatHeader);
        } catch (BadLocationException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        }
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param contactName The name of the contact sending the message.
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE.
     * @param message The message text.
     * @return the formatted message
     */
    public String processMessage(String contactName, Date date,
            String messageType, String message, String contentType)
    {
        if(!isHistory
            && (messageType.equals(Constants.HISTORY_INCOMING_MESSAGE)
                || messageType.equals(Constants.HISTORY_OUTGOING_MESSAGE))
            && (date.compareTo(pageFirstMsgTimestamp) < 0)) {
            
            pageFirstMsgTimestamp = date;
        }
        
        if(!isHistory
            && (messageType.equals(Constants.HISTORY_INCOMING_MESSAGE)
                || messageType.equals(Constants.HISTORY_OUTGOING_MESSAGE))
            && (date.compareTo(pageLastMsgTimestamp) > 0)) {
            pageLastMsgTimestamp = date;            
        }
        
        String dateString = DateFormat
            .getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(date);
                
        String chatString = "";
        String endHeaderTag = "";
        String timeString = "";
        
        String startDivTag = "<DIV id=\"message\">";
        String startHistoryDivTag = "<DIV id=\"message\" style=\"color:#707070;\">";
        String endDivTag = "</DIV>";

        String startPlainTextTag;
        String endPlainTextTag;

        if(contentType.equals("html"))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = "<PLAINTEXT>";
            endPlainTextTag = "</PLAINTEXT>";
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if(GuiUtils.compareDates(
                date, new Date(System.currentTimeMillis())) < 0) {
            timeString = GuiUtils.formatDate(date) + " ";
        }

        if (messageType.equals(Constants.INCOMING_MESSAGE)) {
            this.lastIncomingMsgTimestamp = new Date();
            chatString = "<h2 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h2>";

            chatString += timeString + contactName + " at "
                + GuiUtils.formatTime(date) + endHeaderTag
                + startDivTag
                + startPlainTextTag
                + formatMessage(message, contentType)
                + endPlainTextTag
                + endDivTag;
        }
        else if (messageType.equals(Constants.OUTGOING_MESSAGE)){
            chatString = "<h3 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h3>";

            chatString += timeString + Messages.getI18NString("me").getText()
                + " at "
                + GuiUtils.formatTime(date) + endHeaderTag
                + startDivTag
                + startPlainTextTag
                + formatMessage(message, contentType)
                + endPlainTextTag
                + endDivTag;
        }
        else if (messageType.equals(Constants.SYSTEM_MESSAGE)) {
            chatString = "<h4 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h4>";

            chatString += GuiUtils.formatTime(date) + " "
            + contactName + " " + message + endHeaderTag;
        }
        else if (messageType.equals(Constants.ERROR_MESSAGE)) {
            chatString = "<h6 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h6>";
            
            String errorIcon = "<IMG SRC='"
                + ImageLoader.getImagePath(
                        ImageLoader.getImage(ImageLoader.EXCLAMATION_MARK))
                + "' </IMG>";
            
            chatString  += errorIcon
                + Messages.getI18NString("msgDeliveryFailure").getText()
                + endHeaderTag + "<h5>" + message + "</h5>";
        }
        else if (messageType.equals(Constants.HISTORY_INCOMING_MESSAGE)) {
            chatString = "<h2 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h2>";

            chatString += timeString + contactName + " at "
                + GuiUtils.formatTime(date) + endHeaderTag
                + startHistoryDivTag
                + startPlainTextTag
                + formatMessage(message, contentType)
                + endPlainTextTag
                + endDivTag;
        }
        else if (messageType.equals(Constants.HISTORY_OUTGOING_MESSAGE)) {
            chatString = "<h3 id=\"header\" date=\"" + dateString + "\">";
            endHeaderTag = "</h3>";

            chatString += timeString + Messages.getI18NString("me").getText()
                + " at "
                + GuiUtils.formatTime(date) + endHeaderTag
                + startHistoryDivTag
                + startPlainTextTag
                + formatMessage(message, contentType)
                + endPlainTextTag
                + endDivTag;
        }
        
        messagesPerPage++;
        
        return chatString;
    }
    
    /**
     * Processes the message given by the parameters.
     *
     * @param contactName The name of the contact sending the message.
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE.
     * @param message The message text.
     */
    public String processMessage(String contactName, Date date,
            String messageType, String message, String contentType, String keyword)
    {
        String formattedMessage = message;

        if(keyword != null && keyword != "") {
            formattedMessage = processKeyword(message, contentType, keyword);
        }
        return this.processMessage(contactName, date,
                    messageType, formattedMessage, contentType);
    }
    
    /**
     * Appends the given string at the end of the contained in this panel
     * document.
     * @param chatString the string to append
     */
    public void appendMessageToEnd(String chatString)
    {
        Element root = this.document.getDefaultRootElement();

        try {
            this.document.insertAfterEnd(root
                    .getElement(root.getElementCount() - 1), chatString);            
        } catch (BadLocationException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        }
        
        if(!isHistory)
            this.ensureDocumentSize();
        
        //Scroll to the last inserted text in the document.
        this.setCarretToEnd();
    }
    
    /**
     * Inserts the given string at the beginning of the contained in this panel
     * document.
     * @param chatString the string to insert
     */
    public void insertMessageAfterStart(String chatString)
    {
        Element root = this.document.getDefaultRootElement();
        
        try {
            this.document.insertBeforeStart(root
                    .getElement(0), chatString);            
        } catch (BadLocationException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            LOGGER.error("Insert in the HTMLDocument failed.", e);
        }
        
        if(!isHistory)
            this.ensureDocumentSize();
        
        //Scroll to the last inserted text in the document.
        this.setCarretToEnd();
    }
    

    private void ensureDocumentSize()
    {                
        if(messagesPerPage >= Constants.CHAT_BUFFER_SIZE)
        {
            Element firstHeaderElement = this.document.getElement("header");
            Element firstMessageElement = this.document.getElement("message");
            try {
                if(firstHeaderElement != null)
                    this.document.remove(firstHeaderElement.getStartOffset(),
                            firstHeaderElement.getEndOffset()
                                - firstHeaderElement.getStartOffset());
                
                if(firstMessageElement != null)
                    this.document.remove(firstMessageElement.getStartOffset(),
                            firstMessageElement.getEndOffset()
                                - firstMessageElement.getStartOffset());
            }
            catch (BadLocationException e) {
                LOGGER.error("Error removing messages from chat: ", e);
            }
            
            Element newHeaderElement = this.document.getElement("header");
            
            String dateString = (String)newHeaderElement
                .getAttributes().getAttribute("date");
                 
            
            try {
                Date newFirstMsgTimestamp;
                newFirstMsgTimestamp
                    = DateFormat.getDateTimeInstance(
                        DateFormat.LONG, DateFormat.LONG).parse(dateString);
                
                //Update the start date for the last page. Needed in the main
                //toolbar in order to load differently messages for the last page.
                if(chatContainer instanceof ChatPanel) {
                    
                    ((ChatPanel)chatContainer).setBeginLastPageTimeStamp(
                                pageFirstMsgTimestamp);
                    
                    pageFirstMsgTimestamp = newFirstMsgTimestamp;
                }
            }
            catch (ParseException e) {
                LOGGER.error("Error removing messages from chat: ", e);
            }
        }
    }
    
    /**
     * Highlights keywords searched in the history.
     * @param message the source message
     * @param keyword the searched keyword
     * @return the formatted message
     */
    private String processKeyword(String message,
        String contentType, String keyword)
    {
        String startPlainTextTag;
        String endPlainTextTag;

        if(contentType.equals("html"))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = "<PLAINTEXT>";
            endPlainTextTag = "</PLAINTEXT>";
        }
        
        Pattern p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(message);

        StringBuffer msgBuffer = new StringBuffer();

        boolean matchSuccessfull = false;

        while (m.find()) {
            if (!matchSuccessfull)
                matchSuccessfull = true;

            String matchGroup = m.group().trim();

            String replacement = endPlainTextTag + "<I>" + matchGroup
                                + "</I>" + startPlainTextTag;

            m.appendReplacement(msgBuffer,
                    GuiUtils.replaceSpecialRegExpChars(replacement));
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }

    /**
     * Formats the given message. Processes all smilies chars, new lines and all
     * the links.
     * 
     * @return the formatted message
     */
    private String formatMessage(String message, String contentType)
    {
        String linkProcessedString
            = processLinks(message, contentType);
        
        String newLinesProcessedString
            = processNewLines(linkProcessedString, contentType);
        
        return processSmilies(newLinesProcessedString, contentType);
    }
    
    /**
     * Formats all links in the given message.
     *
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
    private String processLinks(String message, String contentType)
    {
        String startPlainTextTag;
        String endPlainTextTag;

        if(contentType.equals("html"))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = "<PLAINTEXT>";
            endPlainTextTag = "</PLAINTEXT>";
        }

        String wwwURL = "(\\bwww\\.\\S+\\.\\S+/*[?#]*(\\w+[&=;?]\\w+)*\\b)";
        String protocolURL = "(\\b\\w+://\\S+/*[?#]*(\\w+[&=;?]\\w+)*\\b)";
        String url = "(" + wwwURL + "|" + protocolURL + ")";

        Pattern p = Pattern.compile(url);

        Matcher m = p.matcher(message);

        StringBuffer msgBuffer = new StringBuffer();

        boolean matchSuccessfull = false;

        while (m.find()) {
            if (!matchSuccessfull)
                matchSuccessfull = true;

            String matchGroup = m.group().trim();
            String replacement;

            if (matchGroup.startsWith("www")) {
                replacement = endPlainTextTag + "<A href=\"" + "http://"
                        + matchGroup + "\">"
                        + matchGroup + "</A>"
                        + startPlainTextTag;
            } else {
                replacement = endPlainTextTag + "<A href=\"" + matchGroup + "\">"
                        + matchGroup
                        + "</A>"
                        + startPlainTextTag;
            }
            m.appendReplacement(msgBuffer, replacement);
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }

    /**
     * Formats message new lines.
     *
     * @param message The source message string.
     * @return The message string with properly formatted new lines.
     */
    private String processNewLines(String message, String contentType)
    {
        String startPlainTextTag;
        String endPlainTextTag;

        if(contentType.equals("html"))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = "<PLAINTEXT>";
            endPlainTextTag = "</PLAINTEXT>";
        }
        
        /*
         * <br> tags are needed to visualize a new line in the html format, but
         * when copied to the clipboard they are exported to the plain text
         * format as ' ' and not as '\n'.
         * 
         * See bug N4988885:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4988885
         * 
         * To fix this we need "&#10;" - the HTML-Code for ASCII-Character
         * No.10 (Line feed).
         */
        return message.replaceAll("\n",
            endPlainTextTag + "<BR>&#10;" + startPlainTextTag);
    }

    /**
     * Formats message smilies.
     *
     * @param message The source message string.
     * @return The message string with properly formated smilies.
     */
    private String processSmilies(String message, String contentType)
    {
        String startPlainTextTag;
        String endPlainTextTag;

        if(contentType.equals("html"))
        {
            startPlainTextTag = "";
            endPlainTextTag = "";
        }
        else
        {
            startPlainTextTag = "<PLAINTEXT>";
            endPlainTextTag = "</PLAINTEXT>";
        }

        ArrayList smiliesList = ImageLoader.getDefaultSmiliesPack();

        String regexp = "";

        for (int i = 0; i < smiliesList.size(); i++) {

            Smiley smiley = (Smiley) smiliesList.get(i);

            String[] smileyStrings = smiley.getSmileyStrings();

            for (int j = 0; j < smileyStrings.length; j++) {
                regexp += GuiUtils
                        .replaceSpecialRegExpChars(smileyStrings[j])
                        + "|";
            }
        }
        regexp = regexp.substring(0, regexp.length() - 1);

        Pattern p = Pattern.compile(regexp);

        Matcher m = p.matcher(message);

        StringBuffer msgBuffer = new StringBuffer();

        boolean matchSuccessfull = false;

        while (m.find()) {
            if (!matchSuccessfull)
                matchSuccessfull = true;

            String matchGroup = m.group().trim();

            String replacement = endPlainTextTag + "<IMG SRC='"
                    + ImageLoader.getSmiley(matchGroup).getImagePath()
                    + "' ALT='" + matchGroup + "'></IMG>" + startPlainTextTag;

            m.appendReplacement(msgBuffer,
                    GuiUtils.replaceSpecialRegExpChars(replacement));
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }
    
    /**
     * Opens a link in the default browser when clicked and
     * shows link url in a popup on mouseover.
     * @param e The HyperlinkEvent.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {   
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
        {
            String href = e.getDescription();

            this.chatContainer.setStatusMessage(href);

            this.currentHref = href;
            /*
             * Tooltip on hyperlinks - JDK1.5+
             *
             * int stringWidth = StringUtils.getStringWidth(hrefItem, href);
             *
             * hrefItem.setText(href);
             *
             * if (stringWidth < hrefPopupMaxWidth)
             *       hrefItem.setSize(stringWidth, hrefItem.getHeight());
             *  else
             *       hrefItem.setSize(hrefPopupMaxWidth, hrefItem.getHeight());
             *
             * linkPopup.setLocation(MouseInfo.getPointerInfo().getLocation());
             * linkPopup.setVisible(true);
             */

        }
        else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
        {
            this.chatContainer.setStatusMessage("");
            this.currentHref = "";
            /*
             * Tooltip on hyperlinks - JDK1.5+
             * linkPopup.setVisible(false);
             */
        }
    }
    
    /**
     * Returns the editor of this conversation panel.
     * @return The editor of this conversation panel.
     */
    public JEditorPane getChatEditorPane()
    {
        return chatEditorPane;
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
     * Moves the caret to the end of the editor pane.
     */
    public void setCarretToEnd()
    {
        if(this.chatEditorPane.getDocument().getLength()
                == this.document.getLength()) {
            this.chatEditorPane.setCaretPosition(this.document.getLength());
        }
    }
    
    /**
     * When a right button click is performed in the editor pane, a
     * popup menu is opened.
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
        else if((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
            && currentHref != null && currentHref != "")
        {
            GuiActivator.getBrowserLauncher().openURL(currentHref);
        }
    }

    /**
     * Opens this panel context menu at the given point.
     * @param p the point where to position the left-top cornet of the
     * context menu
     */
    private void openContextMenu(Point p)
    {   
        if (currentHref != null && currentHref != "") {
            rightButtonMenu.insert(openLinkItem, 0);
            rightButtonMenu.insert(copyLinkItem, 1);
            rightButtonMenu.insert(copyLinkSeparator, 2);
        }
        else {
            rightButtonMenu.remove(openLinkItem);
            rightButtonMenu.remove(copyLinkItem);
            rightButtonMenu.remove(copyLinkSeparator);
        }

        if(chatEditorPane.getSelectedText() != null) {
            rightButtonMenu.enableCopy();
        }
        else {
            rightButtonMenu.disableCopy();
        }
        rightButtonMenu.setInvoker(chatEditorPane);
        rightButtonMenu.setLocation(p.x, p.y);
        rightButtonMenu.setVisible(true);
    }
    
    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
    }

    /**
     * Returns the chat container.
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
        this.chatEditorPane.copy();
    }
    
    /**
     * Creates new document and all the messages that will be processed in the
     * future will be appended in it.
     */
    public void clear()
    {
        this.document = (HTMLDocument) editorKit.createDefaultDocument();
        Constants.loadSimpleStyle(document.getStyleSheet());
        
        pageFirstMsgTimestamp = new Date(System.currentTimeMillis());
        pageLastMsgTimestamp = new Date(0);
        messagesPerPage = 0;
    }
    
    /**
     * Sets the given document to the editor pane in this panel.
     * @param doc the document to set
     */
    public void setContent(HTMLDocument doc)
    {
        this.document = doc;        
        this.chatEditorPane.setDocument(doc);
    }
    
    /**
     * Sets the default document contained in this panel, created on init or 
     * when clear is invoked.
     */
    public void setDefaultContent()
    {
        this.chatEditorPane.setDocument(document);
    }
    
    /**
     * Returns the document contained in this panel.
     * @return the document contained in this panel
     */
    public HTMLDocument getContent()
    {
        return (HTMLDocument)this.chatEditorPane.getDocument();
    }

    /**
     * Returns the right button popup menu.
     * @return the right button popup menu
     */
    public ChatRightButtonMenu getRightButtonMenu()
    {
        return rightButtonMenu;
    }

    /**
     * Returns the date of the first message in the current page.
     * @return the date of the first message in the current page
     */
    public Date getPageFirstMsgTimestamp()
    {
        return pageFirstMsgTimestamp;
    }

    /**
     * Returns the date of the last message in the current page.
     * @return the date of the last message in the current page
     */
    public Date getPageLastMsgTimestamp()
    {
        return pageLastMsgTimestamp;
    }
}
