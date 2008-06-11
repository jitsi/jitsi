/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
    
    private String title = Messages.getI18NString("sendSms").getText();
    
    private JLabel phoneNumberLabel = new JLabel(
        Messages.getI18NString("enterPhoneNumber").getText());
    
    private JTextField phoneNumberBox
        = new JTextField();
    
    private JTextArea detailsArea = new JTextArea(
        Messages.getI18NString("sendSmsDetails").getText());
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JButton sendButton = new JButton(
        Messages.getI18NString("send").getText());
    
    private JPanel buttonPanel = new JPanel(
        new FlowLayout(FlowLayout.RIGHT));
    
    private ChatPanel chatPanel;
    
    private OperationSetSmsMessaging smsOpSet;
    
    private Message smsMessage;
    
    private MetaContact metaContact;
    
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
                            Message message,
                            OperationSetSmsMessaging opSet)
    {
        super(chatPanel.getChatWindow());
        
        this.chatPanel = chatPanel;
        
        this.smsOpSet = opSet;
        
        this.smsMessage = message;
        
        this.metaContact = (MetaContact) chatPanel.getChatIdentifier();
        
        this.setTitle(title);
        
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20));
        this.mainPanel.add(phoneNumberLabel, BorderLayout.WEST);
        this.mainPanel.add(phoneNumberBox, BorderLayout.CENTER);
        this.mainPanel.add(detailsArea, BorderLayout.SOUTH);
        
        List detailsList = metaContact.getDetails("mobile");
        
        if (detailsList != null && detailsList.size() > 0)
        {
            String detail = (String) detailsList.iterator().next();
            
            phoneNumberBox.setText(detail);
        }
        
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

    @Override
    protected void close(boolean isEscaped)
    {
    }
    
    /**
     * Sends the given message to the given phoneNumber, using the current
     * SMS operation set.
     * 
     * @param phoneNumber the phone number to which the message should be sent.
     * @param message the message to send.
     */
    private void sendSmsMessage(String phoneNumber, Message message)
    {
        metaContact.addDetail("mobile", phoneNumber);
        
        try
        {
            smsOpSet.sendSmsMessage(phoneNumber, message);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.refreshWriteArea();

            chatPanel.processMessage(
                phoneNumber,
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                message.getContent(),
                message.getContentType());

            chatPanel.processMessage(
                phoneNumber,
                new Date(System.currentTimeMillis()),
                Constants.ERROR_MESSAGE,
                Messages.getI18NString("msgSendConnectionProblem")
                .getText(), "text");
        }
        catch (Exception ex)
        {
            logger.error("Failed to send SMS.", ex);

            chatPanel.refreshWriteArea();

            chatPanel.processMessage(
                phoneNumber,
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                message.getContent(), message.getContentType());

            chatPanel.processMessage(
                phoneNumber,
                new Date(System.currentTimeMillis()),
                Constants.ERROR_MESSAGE,
                Messages.getI18NString("msgDeliveryInternalError")
                .getText(), "text");
        }
        
        this.dispose();
    }
}
