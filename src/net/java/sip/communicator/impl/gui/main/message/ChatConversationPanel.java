/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.java.sip.communicator.impl.gui.main.Account;
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

	private ChatWindow msgWindow;

	private AntialiasedEditorPane chatEditorPane = new AntialiasedEditorPane();

	private ChatBuffer chatBuffer;

	public ChatConversationPanel(ChatWindow msgWindow) {

		super();

		this.msgWindow = msgWindow;

		this.chatBuffer = new ChatBuffer();

		this.chatEditorPane.setContentType("text/html");

		this.chatEditorPane.setEditable(false);

		this.chatEditorPane.setEditorKit(new MyHTMLEditorKit());

		this.chatEditorPane.addHyperlinkListener(this);

		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		this
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		this.setWheelScrollingEnabled(true);

		this.getViewport().add(chatEditorPane, BorderLayout.CENTER);
	}

	public void paint(Graphics g) {

		AntialiasingManager.activateAntialiasing(g);

		super.paint(g);

		Graphics2D g2 = (Graphics2D) g;

		g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
		g2.setStroke(new BasicStroke(1.5f));

		g2.drawRoundRect(3, 3, this.getWidth() - 7, this.getHeight() - 5, 8, 8);

	}

	public void processReceivedMessage() {

	}

	public void processSentMessage( Account account, 
	                                Calendar calendar,
	                                String message) {

		this.registerMessage(account.getUin(), calendar, message);

		String chatString = "<HTML><DIV style=\"background-color:"
				+ Constants.FONT_CHAT_HEADER_COLOR
				+ ";text-align:center;font-weight:bold;font-size:"
				+ Constants.FONT_SIZE + "\">"
				+ calendar.get(Calendar.DAY_OF_MONTH) + "/"
				+ calendar.get(Calendar.MONTH) + 1 + "/"
				+ calendar.get(Calendar.YEAR) + " " + "</DIV>";

		for (int i = 0; i < this.chatBuffer.size(); i++) {

			ChatMessage chatMessage = (ChatMessage) this.chatBuffer.get(i);

			chatString += "<DIV style=\"font-family:"
					+ Constants.FONT_NAME
					+ ";font-size:"
					+ Constants.FONT_SIZE
					+ ";color:"
					+ Constants.FONT_OUT_MSG_COLOR
					+ ";font-weight:bold;\">"
					+ chatMessage.getSenderName()
					+ " at "
					+ chatMessage.getCalendar().get(Calendar.HOUR_OF_DAY)
					+ ":"
					+ proccessMinutes(chatMessage.getCalendar().get(Calendar.MINUTE))
					+ "</DIV>"
					+ "<DIV style=\"font-family:"
					+ Constants.FONT_NAME
					+ ";font-size:"
					+ Constants.FONT_SIZE
					+ "\">"
					+ processSmilies(processNewLines(processLinks(chatMessage
							.getMessage()))) + "</DIV>";
		}

		chatString += "</HTML>";

		this.chatEditorPane.setText(chatString);

		this.repaint();
		this.validate();
	}

	private void registerMessage(String senderName, Calendar calendar,
			String message) {

		ChatMessage chatMessage = new ChatMessage(senderName, calendar, message);

		this.chatBuffer.add(chatMessage);
	}

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

	private String processNewLines(String message) {

		return message.replaceAll("\n", "<BR>");
	}

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
							"<IMG src='" + smily.getImagePath() + "'></img>");
				}
			}
		}

		return msgString;
	}
	
	private String proccessMinutes(int minutes){		
		
		String minutesString = new Integer(minutes).toString();
		
		String resultString = minutesString;
		
		if(minutesString.length() < 2)
			resultString.concat("0").concat(minutesString);
		
		return resultString;
	}

	public void hyperlinkUpdate(HyperlinkEvent e) {

		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

			URL url = e.getURL();

			BrowserLauncher.openURL(url.toString());
		}
	}
}
