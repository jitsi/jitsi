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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatSendPanel</tt> is the panel in the bottom of the chat. It
 * contains the send button, the status panel, where typing notifications are
 * shown and the selector box, where the protocol specific contact is chosen.
 *
 * @author Yana Stamcheva
 */
public class ChatSendPanel
    extends TransparentPanel
    implements ActionListener
{
    private final JButton sendButton;

    private final StatusPanel statusPanel = new StatusPanel();

    private final TransparentPanel sendPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private final JLabel statusLabel = new JLabel();

    private final ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatSendPanel</tt>.
     *
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatSendPanel(ChatPanel chatPanel)
    {
        super(new BorderLayout(5, 0));

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        sendButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.SEND"));

        this.chatPanel = chatPanel;

        this.statusPanel.add(statusLabel, BorderLayout.WEST);

        this.sendPanel.add(sendButton);

        this.add(statusPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.EAST);

        this.sendButton.addActionListener(this);
        this.sendButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.SEND"));
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

        final int dot3 = GuiUtils.getStringWidth(statusLabel, "... ");

        // first, we avoid to loop if it is useless.
        final int statusPanelWidth = statusPanel.getWidth();
        if (dot3 >= statusPanelWidth)
        {
            if (stringWidth > dot3)
                statusMessage = "...";
        }
        else
        {
            while ((stringWidth > (statusPanelWidth - dot3))
                    && !statusMessage.equals("..."))
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

    private class StatusPanel extends TransparentPanel
    {
        public StatusPanel()
        {
            super(new BorderLayout());

            this.setBorder(BorderFactory.createCompoundBorder(
                SIPCommBorders.getRoundBorder(),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        }
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
