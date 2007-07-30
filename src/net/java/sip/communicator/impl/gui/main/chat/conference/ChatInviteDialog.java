/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;

public class ChatInviteDialog
    extends SIPCommDialog
    implements ActionListener
{    
    private JTextArea infoTextArea = new JTextArea();
    
    private JLabel contactAddressLabel = new JLabel(
        Messages.getI18NString("contactAddress").getText() + ": ");
    
    private JTextField contactAddressField = new JTextField();
    
    private JLabel reasonLabel = new JLabel(
        Messages.getI18NString("reason").getText() + ": ");
    
    private JTextField reasonField = new JTextField();
    
    private JPanel dataLeftPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    
    private JPanel dataCenterPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    
    private JPanel dataPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private I18NString inviteString = Messages.getI18NString("invite");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton inviteButton = new JButton(inviteString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel northPanel = new JPanel(new BorderLayout(10, 10));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));
    
    private String title
        = Messages.getI18NString("inviteContactToChat").getText();
    
    private ChatPanel chatPanel;
    /**
     * Constructs the <tt>ChatInviteDialog</tt>.
     * 
     * @param chatPanel the <tt>ChatPanel</tt> corresponding to the
     * <tt>ChatRoom</tt>, where the contact is invited. 
     */
    public ChatInviteDialog (ChatPanel chatPanel)
    {
        super(chatPanel.getChatWindow());
        
        this.chatPanel = chatPanel;
        
        this.setModal(false);
        
        this.setTitle(title);
        
        this.mainPanel.setPreferredSize(new Dimension(400, 230));
        
        infoTextArea.setText(
            Messages.getI18NString("inviteContactFormInfo").getText());
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setEditable(false);

        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(infoTextArea, BorderLayout.CENTER);
        
        this.dataLeftPanel.add(contactAddressLabel);
        this.dataLeftPanel.add(reasonLabel);
        
        this.dataCenterPanel.add(contactAddressField);
        this.dataCenterPanel.add(reasonField);
        
        this.dataPanel.add(dataLeftPanel, BorderLayout.WEST);
        this.dataPanel.add(dataCenterPanel, BorderLayout.CENTER);
        
        this.inviteButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(inviteButton);
        this.buttonsPanel.add(cancelButton);
        
        this.getRootPane().setDefaultButton(inviteButton);
        this.inviteButton.setMnemonic(inviteString.getMnemonic());
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        this.mainPanel.add(northPanel, BorderLayout.NORTH);
        this.mainPanel.add(dataPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);                   
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        
        if(button.equals(inviteButton))
        {
            this.chatPanel.inviteChatContact(
                contactAddressField.getText(), reasonField.getText());
        }
        
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }   
}
