/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.menu.ChatRightButtonMenu;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.BrowserLauncher;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.utils.SIPCommHTMLEditorKit;
import net.java.sip.communicator.impl.gui.utils.Smiley;
import net.java.sip.communicator.impl.gui.utils.StringUtils;
import net.java.sip.communicator.util.Logger;

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

    private ChatPanel chatPanel;
    
    private ChatRightButtonMenu rightButtonMenu;
    
    private String currentHref;
    
    private JMenuItem copyLinkItem 
        = new JMenuItem(Messages.getString("copyLink"));
    
    private JMenuItem openLinkItem 
        = new JMenuItem(Messages.getString("openInBrowser"));
    
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
    public ChatConversationPanel(ChatPanel chatPanel) {

        super();

        this.chatPanel = chatPanel;

        this.rightButtonMenu 
            = new ChatRightButtonMenu(chatPanel.getChatWindow());
        
        this.document = (HTMLDocument) editorKit.createDefaultDocument();

        this.chatEditorPane.setContentType("text/html");

        this.chatEditorPane.setEditable(false);

        this.chatEditorPane.setEditorKitForContentType("text/html", editorKit);
        this.chatEditorPane.setEditorKit(editorKit);

        this.chatEditorPane.setDocument(document);

        Constants.loadSimpleStyle(document.getStyleSheet());

        this.initEditor();

        this.chatEditorPane.addHyperlinkListener(this);
        this.chatEditorPane.addMouseListener(this);

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.setWheelScrollingEnabled(true);

        this.getViewport().add(chatEditorPane);

        this.getVerticalScrollBar().setUnitIncrement(30);

        this.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
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
                BrowserLauncher.openURL(currentHref);
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
     */
    public void processMessage(String contactName, Date date,
            String messageType, String message) {

        String chatString;
        String endHeaderTag;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (messageType.equals(Constants.INCOMING_MESSAGE)) {
            this.lastIncomingMsgTimestamp = new Date();
            chatString = "<h2>";
            endHeaderTag = "</h2>";
        } else {
            chatString = "<h3>";
            endHeaderTag = "</h3>";
        }

        chatString += contactName + " at "
                + processTime(calendar.get(Calendar.HOUR_OF_DAY)) + ":"
                + processTime(calendar.get(Calendar.MINUTE)) + ":"
                + processTime(calendar.get(Calendar.SECOND)) + endHeaderTag
                + "<DIV>"
                + processSmilies(processNewLines(processLinks(message)))
                + "</DIV>";

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
     * Formats all links in the given message.
     * 
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
    public static String processLinks(String message) {

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
                replacement = "<A href=\"" + "http://" + matchGroup + "\">"
                        + matchGroup + "</A>";
            } else {
                replacement = "<A href=\"" + matchGroup + "\">" + matchGroup
                        + "</A>";
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

        return message.replaceAll("\n", "<BR>");
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

            String replacement = "<IMG SRC='"
                    + ImageLoader.getSmiley(matchGroup).getImagePath()
                    + "' ALT='" + matchGroup + "'></IMG>";

            m.appendReplacement(msgBuffer, replacement);
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
            BrowserLauncher.openURL(url.toString());
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            String href = e.getDescription();
            
            this.chatPanel.setChatStatusMessage(href);
            
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
            
            this.chatPanel.setChatStatusMessage("");
            this.currentHref = "";
            /*
             * Tooltip on hyperlinks - JDK1.5+
             * linkPopup.setVisible(false);
             */
        }
    }

    /**
     * Overrides the javax.swing.JComponent.paint()
     * in order to provide a new round border for the panel.
     * @param g The Graphics object.
     */
    public void paint(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.BLUE_GRAY_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));

        g2.drawRoundRect(3, 3, this.getWidth() - 7, this.getHeight() - 5, 8, 8);

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
}
