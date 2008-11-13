/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatSendPanel</tt> is the panel in the bottom of the chat. It
 * contains the send button, the status panel, where typing notifications are
 * shown and the selector box, where the protocol specific contact is choosen.
 *
 * @author Yana Stamcheva
 */
public class ChatSendPanel
    extends TransparentPanel
    implements ActionListener
{
    private Logger logger = Logger.getLogger(ChatSendPanel.class);

    private I18NString sendString = Messages.getI18NString("send");

    private JButton sendButton = new JButton(sendString.getText());

    private TransparentPanel statusPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    private TransparentPanel sendPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JLabel statusLabel = new JLabel();

    private ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatSendPanel</tt>.
     *
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatSendPanel(ChatPanel chatPanel)
    {
        super(new BorderLayout(5, 0));

        this.chatPanel = chatPanel;

        this.statusPanel.add(statusLabel);

        this.sendPanel.add(sendButton);

        this.add(statusPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.EAST);

        this.sendButton.addActionListener(this);
        this.sendButton.setMnemonic(sendString.getMnemonic());
    }

    /**
     * Defines actions when send button is pressed.
     *
     * @param evt The <tt>ActionEvent</tt> object.
     */
    public void actionPerformed(ActionEvent evt)
    {   
        if (!chatPanel.isWriteAreaEmpty())
        {
            new Thread()
            {
                public void run()
                {
                    chatPanel.sendMessage();
                }
            }.start();
        }
    }

    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     *
     * @param statusMessage The message text to be displayed.
     */
    public void setStatusMessage(String statusMessage)
    {
        int stringWidth = GuiUtils.getStringWidth(statusLabel, statusMessage);

        int dot3 = GuiUtils.getStringWidth(statusLabel, "... ");

        // first, we avoid to loop if it is useless.
        if (dot3 >= statusPanel.getWidth())
        {
            if (stringWidth > dot3)
                statusMessage = "...";
        }
        else
        {
            while ((stringWidth > (statusPanel.getWidth() - dot3))
                    && (statusMessage != "..."))
            {
                if (statusMessage.endsWith("..."))
                {
                    statusMessage = statusMessage.substring(0,
                        statusMessage.indexOf("...") - 1).concat("...");
                }
                else
                {
                    statusMessage = statusMessage.substring(0,
                        statusMessage.length() - 3).concat("...");
                }
                stringWidth = GuiUtils.getStringWidth(statusLabel, statusMessage);
            }
        }
        statusLabel.setText(statusMessage);
    }

    /**
     * Overrides the <code>javax.swing.JComponent.paint()</code> to provide a
     * new round border for the status panel.
     *
     * @param g The Graphics object.
     */
    public void paint(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.GRADIENT_DARK_COLOR);
        g2.setStroke(new BasicStroke(1f));

        g2.drawRoundRect(3, 4, this.statusPanel.getWidth() - 2,
            this.statusPanel.getHeight() - 2, 8, 8);
    }

    /**
     * Returns the parent <tt>ChatPanel</tt>.
     * @return the parent <tt>ChatPanel</tt>
     */
    public ChatPanel getChatPanel()
    {
        return chatPanel;
    }

    /**
     * Returns the main container. Used by the single user chat panel to add
     * here the "send via" selector box.
     * @return the main container
     */
    public JPanel getSendPanel()
    {
        return sendPanel;
    }

    /**
     * Returns the status panel contained in this panel.
     * @return the status panel contained in this panel
     */
    public JPanel getStatusPanel()
    {
        return statusPanel;
    }
    

    /**
     * Returns the send button.
     *
     * @return The send button.
     */
    public JButton getSendButton()
    {
        return sendButton;
    }

}
