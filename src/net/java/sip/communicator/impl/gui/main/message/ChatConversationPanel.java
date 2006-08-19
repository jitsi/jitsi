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
public class ChatConversationPanel extends JScrollPane implements
        HyperlinkListener, MouseListener, ClipboardOwner {

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
    public ChatConversationPanel(ChatConversationContainer chatContainer) {

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
    private void initEditor() {
        Element root = this.document.getDefaultRootElement();

        Calendar calendar = Calendar.getInstance();
        String chatHeader = "<h1>"
                + this.processTime(calendar.get(Calendar.DAY_OF_MONTH)) + "/"
                + this.processTime(calendar.get(Calendar.MONTH) + 1) + "/"
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
            String messageType, String message) {

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
            timeString = this.processTime(calendar.get(Calendar.DAY_OF_MONTH)) + "/"
                        + this.processTime(calendar.get(Calendar.MONTH) + 1) + " ";
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
            String messageType, String message, String keyword) {

        String formattedMessage = message;

        if(keyword != null) {
            formattedMessage = processKeyWords(message, keyword);
        }
        return this.processMessage(contactName, date,
                    messageType, formattedMessage);
    }

    private String processKeyWords(String message, String keyword) {

        return message.replaceAll(keyword,
                "</PLAINTEXT><B>"
                + keyword + "</B><PLAINTEXT>");
    }

    /**
     * Formats all links in the given message.
     *
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
    private String processLinks(String message) {

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
    private String processNewLines(String message) {

        return message.replaceAll("\n", "</PLAINTEXT><BR><PLAINTEXT>");
    }

    /**
     * Formats message smilies.
     *
     * @param message The source message string.
     * @return The message string with properly formated smilies.
     */
    private String processSmilies(String message) {

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
     * Formats a time string.
     *
     * @param time The time parameter could be hours, minutes or seconds.
     * @return The formatted minutes string.
     */
    private String processTime(int time) {

        String timeString = new Integer(time).toString();

        String resultString = "";
        if (timeString.length() < 2)
            resultString = resultString.concat("0").concat(timeString);
        else
            resultString = timeString;

        return resultString;
    }

    /**
     * Opens a link in the default browser when clicked and
     * shows link url in a popup on mouseover.
     * @param e The HyperlinkEvent.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
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
    public JEditorPane getChatEditorPane() {
        return chatEditorPane;
    }

    /**
     * Returns the time of the last received message.
     *
     * @return The time of the last received message.
     */
    public Date getLastIncomingMsgTimestamp() {
        return lastIncomingMsgTimestamp;
    }

    /**
     * Moves the caret to the end of the editor pane.
     */
    public void setCarretToEnd() {
        this.chatEditorPane.setCaretPosition(this.document.getLength());
    }

    /**
     * When a right button click is performed in the editor pane, a
     * popup menu is opened.
     *
     * @param e The MouseEvent.
     */
    public void mouseClicked(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {

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

            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());

            rightButtonMenu.setInvoker(chatEditorPane);
            rightButtonMenu.setLocation(p.x, p.y);
            rightButtonMenu.setVisible(true);
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

    public ChatConversationContainer getChatContainer() {
        return chatContainer;
    }

    /**
     * Copies the selected conversation panel content to the clipboard.
     */
    public void copyConversation(){
        this.chatEditorPane.copy();
    }
}
