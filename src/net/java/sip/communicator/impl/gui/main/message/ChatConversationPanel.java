/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.message.menu.*;
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

    private JMenuItem copyLinkItem
        = new JMenuItem(Messages.getString("copyLink"),
                new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem openLinkItem
        = new JMenuItem(Messages.getString("openInBrowser"),
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

    /**
     * Creates an instance of <tt>ChatConversationPanel</tt>.
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatConversationPanel(ChatConversationContainer chatContainer)
    {
        super();

        this.chatContainer = chatContainer;

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
                CrossPlatformBrowserLauncher.openURL(currentHref);
            }
        });
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

        Calendar calendar = Calendar.getInstance();
        String chatHeader = "<h1>"
                + this.processMonth(calendar.get(Calendar.MONTH) + 1) + " " 
                + this.processTime(calendar.get(Calendar.DAY_OF_MONTH)) + ", "                
                + this.processTime(calendar.get(Calendar.YEAR)) + " " + "</h1>";

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
            String messageType, String message)
    {
        String chatString = "";
        String endHeaderTag = "";
        String timeString = "";

        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if((calendar.get(Calendar.DAY_OF_MONTH)
                < calendar1.get(Calendar.DAY_OF_MONTH))
            && (calendar.get(Calendar.MONTH)
                <= calendar1.get(Calendar.MONTH))
            && (calendar.get(Calendar.YEAR)
                <= calendar1.get(Calendar.YEAR))) {
            timeString = this.processMonth(calendar.get(Calendar.MONTH) + 1)
                + " " + this.processTime(calendar.get(Calendar.DAY_OF_MONTH))
                + " ";
        }

        if (messageType.equals(Constants.INCOMING_MESSAGE)) {
            this.lastIncomingMsgTimestamp = new Date();
            chatString = "<h2>";
            endHeaderTag = "</h2>";

            chatString += timeString + contactName + " at "
                + processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
                + processTime(calendar.get(Calendar.MINUTE)) + ":"
                + processTime(calendar.get(Calendar.SECOND)) + endHeaderTag
                + "<DIV>" + "<PLAINTEXT>"
                + processSmilies(processNewLines(processLinks(message)))
                + "</PLAINTEXT>" + "</DIV>";
        }
        else if (messageType.equals(Constants.OUTGOING_MESSAGE)){
            chatString = "<h3>";
            endHeaderTag = "</h3>";

            chatString += timeString + Messages.getString("me") + " at "
                + processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
                + processTime(calendar.get(Calendar.MINUTE)) + ":"
                + processTime(calendar.get(Calendar.SECOND)) + endHeaderTag
                + "<DIV>" + "<PLAINTEXT>"
                + processSmilies(processNewLines(processLinks(message)))
                + "</PLAINTEXT>" + "</DIV>";
        }
        else if (messageType.equals(Constants.SYSTEM_MESSAGE)) {
            chatString = "<h4>";
            endHeaderTag = "</h4>";

            chatString += processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
            + processTime(calendar.get(Calendar.MINUTE)) + ":"
            + processTime(calendar.get(Calendar.SECOND)) + " "
            + contactName + " " + message + endHeaderTag;
        }
        else if (messageType.equals(Constants.HISTORY_INCOMING_MESSAGE)) {
            chatString = "<h2>";
            endHeaderTag = "</h2>";

            chatString += timeString + contactName + " at "
                + processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
                + processTime(calendar.get(Calendar.MINUTE)) + ":"
                + processTime(calendar.get(Calendar.SECOND)) + endHeaderTag
                + "<DIV style=\"color:#707070;\">" + "<PLAINTEXT>"
                + processSmilies(processNewLines(processLinks(message)))
                + "</PLAINTEXT>" + "</DIV>";
        }
        else if (messageType.equals(Constants.HISTORY_OUTGOING_MESSAGE)) {
            chatString = "<h3>";
            endHeaderTag = "</h3>";

            chatString += timeString + Messages.getString("me") + " at "
                + processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
                + processTime(calendar.get(Calendar.MINUTE)) + ":"
                + processTime(calendar.get(Calendar.SECOND)) + endHeaderTag
                + "<DIV style=\"color:#707070;\">" + "<PLAINTEXT>"
                + processSmilies(processNewLines(processLinks(message)))
                + "</PLAINTEXT>" + "</DIV>";
        }
        return chatString;
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
        //Scroll to the last inserted text in the document.
        this.setCarretToEnd();
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
            String messageType, String message, String keyword)
    {
        String formattedMessage = message;

        if(keyword != null && keyword != "") {
            formattedMessage = processKeyword(message, keyword);
        }
        return this.processMessage(contactName, date,
                    messageType, formattedMessage);
    }

    /**
     * Highlights keywords searched in the history.
     * @param message the source message
     * @param keyword the searched keyword
     * @return the formatted message
     */
    private String processKeyword(String message, String keyword)
    {
        Pattern p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(message);

        StringBuffer msgBuffer = new StringBuffer();

        boolean matchSuccessfull = false;

        while (m.find()) {
            if (!matchSuccessfull)
                matchSuccessfull = true;

            String matchGroup = m.group().trim();

            String replacement = "</PLAINTEXT><B>" + matchGroup
                                + "</B><PLAINTEXT>";

            m.appendReplacement(msgBuffer,
                    StringUtils.replaceSpecialRegExpChars(replacement));
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }

    /**
     * Formats all links in the given message.
     *
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
    private String processLinks(String message)
    {
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
                replacement = "</PLAINTEXT><A href=\"" + "http://" + matchGroup + "\">"
                        + matchGroup + "</A><PLAINTEXT>";
            } else {
                replacement = "</PLAINTEXT><A href=\"" + matchGroup + "\">" + matchGroup
                        + "</A><PLAINTEXT>";
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
    private String processNewLines(String message)
    {
        return message.replaceAll("\n", "</PLAINTEXT><BR><PLAINTEXT>");
    }

    /**
     * Formats message smilies.
     *
     * @param message The source message string.
     * @return The message string with properly formated smilies.
     */
    private String processSmilies(String message)
    {
        ArrayList smiliesList = ImageLoader.getDefaultSmiliesPack();

        String regexp = "";

        for (int i = 0; i < smiliesList.size(); i++) {

            Smiley smiley = (Smiley) smiliesList.get(i);

            String[] smileyStrings = smiley.getSmileyStrings();

            for (int j = 0; j < smileyStrings.length; j++) {
                regexp += StringUtils
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

            String replacement = "</PLAINTEXT><IMG SRC='"
                    + ImageLoader.getSmiley(matchGroup).getImagePath()
                    + "' ALT='" + matchGroup + "'></IMG><PLAINTEXT>";

            m.appendReplacement(msgBuffer,
                    StringUtils.replaceSpecialRegExpChars(replacement));
        }
        m.appendTail(msgBuffer);

        return msgBuffer.toString();
    }

    /**
     * Adds a 0 in the beginning of one digit numbers.
     *
     * @param time The time parameter could be hours, minutes or seconds.
     * @return The formatted minutes string.
     */
    private String processTime(int time)
    {
        String timeString = new Integer(time).toString();

        String resultString = "";
        if (timeString.length() < 2)
            resultString = resultString.concat("0").concat(timeString);
        else
            resultString = timeString;

        return resultString;
    }
    
    /**
     * Replaces the month with its abbreviation.
     * @param month Value from 1 to 12, which indicates the month.
     * @return the corresponding month abbreviation
     */
    private String processMonth(int month)
    {
        String monthString = "";
        if(month == 1)
            monthString = Messages.getString("january");
        else if(month == 2)
            monthString = Messages.getString("february");
        else if(month == 3)
            monthString = Messages.getString("march");
        else if(month == 4)
            monthString = Messages.getString("april");
        else if(month == 5)
            monthString = Messages.getString("may");
        else if(month == 6)
            monthString = Messages.getString("june");
        else if(month == 7)
            monthString = Messages.getString("july");
        else if(month == 8)
            monthString = Messages.getString("august");
        else if(month == 9)
            monthString = Messages.getString("september");
        else if(month == 10)
            monthString = Messages.getString("october");
        else if(month == 11)
            monthString = Messages.getString("november");
        else if(month == 12)
            monthString = Messages.getString("december");
        
        return monthString;
    }   

    /**
     * Opens a link in the default browser when clicked and
     * shows link url in a popup on mouseover.
     * @param e The HyperlinkEvent.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL url = e.getURL();
            CrossPlatformBrowserLauncher.openURL(url.toString());
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
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

        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {

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
                || (e.isControlDown() && !e.isMetaDown())) {
            openContextMenu(p);
        }
    }

    /**
     * Opens this panel context menu at the given point.
     * @param p the point where to position the left-top cornet of the
     * context menu
     */
    private void openContextMenu(Point p)
    {
        if (currentHref != null) {
            if (currentHref != "") {
                rightButtonMenu.insert(openLinkItem, 0);
                rightButtonMenu.insert(copyLinkItem, 1);
                rightButtonMenu.insert(copyLinkSeparator, 2);
            }
            else {
                rightButtonMenu.remove(openLinkItem);
                rightButtonMenu.remove(copyLinkItem);
                rightButtonMenu.remove(copyLinkSeparator);
            }
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
}
