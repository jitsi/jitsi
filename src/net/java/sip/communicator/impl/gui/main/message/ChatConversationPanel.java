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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedEditorPane;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.BrowserLauncher;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.main.utils.MyHTMLEditorKit;
import net.java.sip.communicator.impl.gui.main.utils.Smily;
import net.java.sip.communicator.impl.gui.main.utils.StringUtils;

public class ChatConversationPanel extends JScrollPane 
    implements HyperlinkListener {

	private AntialiasedEditorPane chatEditorPane = new AntialiasedEditorPane();

    private HTMLEditorKit editorKit = new MyHTMLEditorKit(); 
    
	private HTMLDocument document;
    
	public ChatConversationPanel(ChatPanel chatPanel) {

		super();

        this.chatEditorPane.setContentType("text/html");

		this.chatEditorPane.setEditable(false);
        
        //this.initStyle();
        
		this.chatEditorPane.setEditorKit(editorKit);

        this.document = (HTMLDocument)this.chatEditorPane.getDocument();
        
        Constants.loadStyle(document.getStyleSheet());
        
        this.initEditor();
        
		this.chatEditorPane.addHyperlinkListener(this);

		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		this.setWheelScrollingEnabled(true);
        
		this.getViewport().add(chatEditorPane);        
	}
    
    private void initEditor(){
        Element root = this.document.getDefaultRootElement();
        
        Calendar calendar = Calendar.getInstance();
        String chatHeader = "<h1>"
        + calendar.get(Calendar.DAY_OF_MONTH) + "/"
        + calendar.get(Calendar.MONTH) + 1 + "/"
        + calendar.get(Calendar.YEAR) + " " + "</h1>";

        try {            
            this.document.insertAfterStart(root, chatHeader);
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Process the message given by the parameters.
     * 
     * @param contactName The name of the contact sending the message.
     * @param calendar The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE 
     * or INCOMING_MESSAGE. 
     * @param message The message text.
     */
    public void processMessage(String contactName,
                                                Calendar calendar,
                                                String messageType, 
                                                String message){
           
        String chatString;
        String endHeaderTag;
        if(messageType.equals(ChatMessage.INCOMING_MESSAGE)){
            chatString = "<h2>";
            endHeaderTag = "</h2>";
        }
        else{
            chatString = "<h3>";
            endHeaderTag = "</h3>";
        }
        
        chatString += contactName
        + " at "
        + calendar.get(Calendar.HOUR_OF_DAY)
        + ":"
        + proccessMinutes(calendar.get(Calendar.MINUTE))
        + endHeaderTag
        + "<DIV>"
        + processSmilies(processNewLines(processLinks(message))) + "</DIV>";
        
        Element root = this.document.getDefaultRootElement();
        
        try {
            this.document.insertAfterEnd(root.getElement(root.getElementCount() - 1), chatString);            
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Scroll to the last inserted text in the document.
        this.chatEditorPane.setCaretPosition(this.document.getLength());
    } 
   
    /**
     * Format message containing links.
     * 
     * @param message The source message string.
     * @return The message string with properly formatted links.
     */
	private String processLinks(String message) {

		String msgString = message;

		Pattern p = Pattern
				.compile("(\\bwww\\.\\w+\\.\\S+\\b)|(\\b\\w+://\\S+\\b)");

		Matcher m = p.matcher(message);

		while (m.find()) {

			msgString = msgString.replaceAll(m.group().trim(), "<A href='"
					+ "http://" + m.group() + "'>" + m.group() + "</A>");
		}

		return msgString;
	}

    /**
     * Format message new lines.
     * 
     * @param message The source message string.
     * @return The message string with properly formatted new lines.
     */
	private String processNewLines(String message) {

		return message.replaceAll("\n", "<BR>");
	}

    /**
     * Format message smilies.
     * 
     * @param message The source message string.
     * @return The message string with properly formated smilies.
     */
	private String processSmilies(String message) {

		ArrayList smiliesList = ImageLoader.getDefaultSmiliesPack();

		String msgString = message;

		for (int i = 0; i < smiliesList.size(); i++) {

			Smily smily = (Smily) smiliesList.get(i);

			String[] smilyStrings = smily.getSmilyStrings();

			for (int j = 0; j < smilyStrings.length; j++) {

				if (message.indexOf(smilyStrings[j]) != -1) {

					msgString = msgString.replaceAll(StringUtils
							.replaceSpecialRegExpChars(smilyStrings[j]),
							"<img src='" + smily.getImagePath() + "'></img>");
				}
			}
		}

		return msgString;
	}
	
    /**
     * Format time string.
     * 
     * @param minutes The minutes int.
     * @return The formatted minutes string.
     */
	private String proccessMinutes(int minutes){		
		
		String minutesString = new Integer(minutes).toString();
		
		String resultString = minutesString;
		
		if(minutesString.length() < 2)
			resultString.concat("0").concat(minutesString);
		
		return resultString;
	}

    /**
     * Opens a link in the default browser when clicked.
     */
	public void hyperlinkUpdate(HyperlinkEvent e) {

		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

			URL url = e.getURL();

			BrowserLauncher.openURL(url.toString());
		}
	}
       
    public void paint(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));

        g2.drawRoundRect(3, 3, this.getWidth() - 7, this.getHeight() - 5, 8, 8);

    }    
}
