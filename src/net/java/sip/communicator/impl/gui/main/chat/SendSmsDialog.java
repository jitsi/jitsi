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

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog, shown when user click on the chat "Send" button, while the
 * "Send as SMS" option is selected. This dialog allow the user to choose or
 * enter its new own phone number to which the SMS would be sent.
 * 
 * @author Yana Stamcheva
 */
public class SendSmsDialog
    extends SIPCommDialog
{
    private Logger logger = Logger.getLogger(SendSmsDialog.class);
    
    private String title
        = GuiActivator.getResources().getI18NString("service.gui.SEND_SMS");
    
    private JLabel phoneNumberLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.ENTER_PHONE_NUMBER"));
    
    private JTextField phoneNumberBox
        = new JTextField();
    
    private JTextArea detailsArea = new JTextArea(
        GuiActivator.getResources().getI18NString("service.gui.SEND_SMS_DETAILS"));
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JButton sendButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.SEND"));
    
    private JPanel buttonPanel = new JPanel(
        new FlowLayout(FlowLayout.RIGHT));
    
    private ChatPanel chatPanel;
    
    private String smsMessage;
    
    private ChatTransport chatTransport;
    
    /**
     * Creates and constructs the SendSmsDialog, by specifying its parent chat,
     * the message that will be send at the end and the
     * <tt>OperationSetSmsMessaging</tt> to be used for sending the message.
     * 
     * @param chatPanel the chat sending the message
     * @param message the SMS message
     * @param opSet the <tt>OperationSetSmsMessaging</tt> that will be used to
     * send the message
     */
    public SendSmsDialog(   ChatPanel chatPanel,
                            ChatTransport chatTransport,
                            String message)
    {
        super(chatPanel.getChatWindow());
        
        this.chatPanel = chatPanel;
        
        this.chatTransport = chatTransport;
        
        this.smsMessage = message;
        
        this.setTitle(title);
        
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20));
        this.mainPanel.add(phoneNumberLabel, BorderLayout.WEST);
        this.mainPanel.add(phoneNumberBox, BorderLayout.CENTER);
        this.mainPanel.add(detailsArea, BorderLayout.SOUTH);
        
        String defaultSmsNumber
            = chatTransport.getParentChatSession().getDefaultSmsNumber();
        
        phoneNumberBox.setText(defaultSmsNumber);
        
        this.detailsArea.setOpaque(false);
        this.detailsArea.setLineWrap(true);
        this.detailsArea.setWrapStyleWord(true);
        this.detailsArea.setEditable(false);
        
        this.buttonPanel.add(sendButton);
        
        this.sendButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                sendSmsMessage( phoneNumberBox.getText(),
                                smsMessage);
            }
        });
    }

    /**
     * Sends the given message to the given phoneNumber, using the current
     * SMS operation set.
     * 
     * @param phoneNumber the phone number to which the message should be sent.
     * @param message the message to send.
     */
    private void sendSmsMessage(String phoneNumber, String message)
    {
        chatTransport.getParentChatSession().setDefaultSmsNumber(phoneNumber);

        try
        {
            chatTransport.sendSmsMessage(phoneNumber, message);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.refreshWriteArea();

            chatPanel.addMessage(
                phoneNumber,
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                message,
                "text/plain");

            chatPanel.addErrorMessage(
                phoneNumber,
                GuiActivator.getResources()
                    .getI18NString("service.gui.SMS_SEND_CONNECTION_PROBLEM"));
        }
        catch (Exception ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.refreshWriteArea();

            chatPanel.addMessage(
                phoneNumber,
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                message,
                "text/plain");

            chatPanel.addErrorMessage(
                phoneNumber,
                GuiActivator.getResources()
                    .getI18NString("service.gui.MSG_DELIVERY_UNKNOWN_ERROR",
                    new String[]{ex.getMessage()}));
        }
        
        this.dispose();
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
