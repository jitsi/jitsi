/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.whiteboard.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.whiteboard.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The dialog that pops up when a chat room invitation is received.
 *  
 * @author Yana Stamcheva
 */
public class InvitationReceivedDialog
    extends JDialog
    implements ActionListener
{
    private JTextArea infoTextArea = new JTextArea();

    private JTextArea invitationReasonTextArea = new JTextArea();

    private JPanel reasonPanel = new JPanel(new BorderLayout());

    private JLabel reasonLabel = new JLabel(
        Resources.getString("reason") + ": ");

    private JTextField reasonField = new JTextField();

    private JPanel dataPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JButton acceptButton = new JButton(Resources.getString("accept"));
    
    private JButton rejectButton = new JButton(Resources.getString("reject"));
    
    private JButton ignoreButton = new JButton(Resources.getString("ignore"));
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel northPanel = new JPanel(new BorderLayout(10, 10));
    
    private JLabel iconLabel = new JLabel(Resources.getImage("inviteIcon"));
    
    private String title
        = Resources.getString("invitationReceived");
    
    /**
     * The <tt>ChatRoomInvitation</tt> for which this dialog is.
     */
    private WhiteboardInvitation invitation;

    /**
     * The <tt>MultiUserChatManager</tt> is the one that deals with invitation
     * events.
     */
    private WhiteboardSessionManager whiteboardManager;

    /**
     * The operation set that would handle the rejection if the user choose to
     * reject the invitation.
     */
    private OperationSetWhiteboarding whiteboardOpSet;
    
    /**
     * Constructs the <tt>ChatInviteDialog</tt>.
     * 
     * @param whiteboardManager the <tt>WhiteboardSessionManager</tt> is the one
     * that deals with invitation events
     * @param whiteboardOpSet the operation set that would handle the
     * rejection if the user choose to reject the invitation
     * @param invitation the invitation that this dialog represents
     */
    public InvitationReceivedDialog (WhiteboardSessionManager whiteboardManager,
            OperationSetWhiteboarding whiteboardOpSet,
            WhiteboardInvitation invitation)
    {
        this.whiteboardManager = whiteboardManager;

        this.whiteboardOpSet = whiteboardOpSet;

        this.invitation = invitation;

        this.setModal(false);

        this.setTitle(title);

        this.mainPanel.setPreferredSize(new Dimension(400, 230));

        infoTextArea.setText(
            Resources.getString("invitationReceivedFormInfo",
                new String[] {  invitation.getInviter(),
                                invitation.getTargetWhiteboard()
                                    .getWhiteboardID()}));

        if(invitation.getReason() != null && invitation.getReason() != "")
        {
            invitationReasonTextArea.setText(invitation.getReason());
            invitationReasonTextArea.setBorder(
                BorderFactory.createTitledBorder(
                    Resources.getString("invitation")));

            this.dataPanel.add(invitationReasonTextArea, BorderLayout.CENTER);
        }
        
        this.infoTextArea.setFont(
            infoTextArea.getFont().deriveFont(Font.BOLD, 12f));
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
        this.acceptButton.setMnemonic(Resources.getMnemonic("accept"));
        this.rejectButton.setMnemonic(Resources.getMnemonic("reject"));
        this.ignoreButton.setMnemonic(Resources.getMnemonic("ignore"));
        
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
            whiteboardManager.acceptInvitation(invitation);
        }
        else if (button.equals(rejectButton))
        {
            whiteboardManager.rejectInvitation(whiteboardOpSet,
                invitation, reasonField.getText());
        }
        
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {}
}