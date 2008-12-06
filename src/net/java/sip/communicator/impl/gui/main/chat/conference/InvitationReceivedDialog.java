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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;

/**
 * The dialog that pops up when a chat room invitation is received.
 *  
 * @author Yana Stamcheva
 */
public class InvitationReceivedDialog
    extends SIPCommDialog
    implements ActionListener
{    
    private JTextArea infoTextArea = new JTextArea();
    
    private JTextArea invitationReasonTextArea = new JTextArea();
    
    private JPanel reasonPanel = new JPanel(new BorderLayout());
    
    private JLabel reasonLabel = new JLabel(
        Messages.getI18NString("reason").getText() + ": ");
    
    private JTextField reasonField = new JTextField();

    private JPanel dataPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private I18NString acceptString = Messages.getI18NString("accept");
    
    private I18NString rejectString = Messages.getI18NString("reject");
    
    private I18NString ignoreString = Messages.getI18NString("ignore");
    
    private JButton acceptButton = new JButton(acceptString.getText());
    
    private JButton rejectButton = new JButton(rejectString.getText());
    
    private JButton ignoreButton = new JButton(ignoreString.getText());
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel northPanel = new JPanel(new BorderLayout(10, 10));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.INVITE_DIALOG_ICON)));
    
    private String title
        = Messages.getI18NString("invitationReceived").getText();
    
    /**
     * The <tt>ChatRoomInvitation</tt> for which this dialog is.
     */
    private ChatRoomInvitation invitation;
    
    /**
     * The <tt>MultiUserChatManager</tt> is the one that deals with invitation
     * events.
     */
    private ConferenceChatManager multiUserChatManager;
    
    /**
     * The operation set that would handle the rejection if the user choose to
     * reject the invitation.
     */
    private OperationSetMultiUserChat multiUserChatOpSet;
    
    /**
     * Constructs the <tt>ChatInviteDialog</tt>.
     * 
     * @param multiUserChatManager the <tt>MultiUserChatManager</tt> is the one
     * that deals with invitation events
     * @param multiUserChatOpSet the operation set that would handle the
     * rejection if the user choose to reject the invitation
     * @param invitation the invitation that this dialog represents
     */
    public InvitationReceivedDialog (ConferenceChatManager multiUserChatManager,
            OperationSetMultiUserChat multiUserChatOpSet,
            ChatRoomInvitation invitation)
    {
        super(GuiActivator.getUIService().getMainFrame());

        this.multiUserChatManager = multiUserChatManager;

        this.multiUserChatOpSet = multiUserChatOpSet;

        this.invitation = invitation;

        this.setModal(false);

        this.setTitle(title);

        this.mainPanel.setPreferredSize(new Dimension(400, 230));

        infoTextArea.setText(
            Messages.getI18NString("invitationReceivedFormInfo",
                new String[] {  invitation.getInviter(),
                                invitation.getTargetChatRoom().getName()})
                                .getText());

        if(invitation.getReason() != null && invitation.getReason() != "")
        {
            invitationReasonTextArea.setText(invitation.getReason());
            invitationReasonTextArea.setBorder(
                BorderFactory.createTitledBorder(
                    Messages.getI18NString("invitation").getText()));

            this.dataPanel.add(invitationReasonTextArea, BorderLayout.CENTER);
        }
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setEditable(false);
        
        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(infoTextArea, BorderLayout.CENTER);
        
        this.reasonPanel.add(reasonLabel, BorderLayout.WEST);
        this.reasonPanel.add(reasonField, BorderLayout.CENTER);
        
        this.dataPanel.add(reasonPanel, BorderLayout.SOUTH);
        
        this.acceptButton.addActionListener(this);
        this.rejectButton.addActionListener(this);
        this.ignoreButton.addActionListener(this);
        
        this.buttonsPanel.add(acceptButton);
        this.buttonsPanel.add(rejectButton);
        this.buttonsPanel.add(ignoreButton);
        
        this.getRootPane().setDefaultButton(acceptButton);
        this.acceptButton.setMnemonic(acceptString.getMnemonic());
        this.rejectButton.setMnemonic(rejectString.getMnemonic());
        this.ignoreButton.setMnemonic(ignoreString.getMnemonic());
        
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
        
        if (button.equals(acceptButton))
        {
            multiUserChatManager.acceptInvitation(invitation);
        }
        else if (button.equals(rejectButton))
        {
            multiUserChatManager.rejectInvitation(multiUserChatOpSet,
                invitation, reasonField.getText());
        }
        
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {}
}
