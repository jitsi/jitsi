/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.whiteboard.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.whiteboard.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The dialog that pops up when a chat room invitation is received.
 *
 * @author Yana Stamcheva
 */
public class InvitationReceivedDialog
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JTextArea infoTextArea = new JTextArea();

    private JTextArea invitationReasonTextArea = new JTextArea();

    private JPanel reasonPanel = new TransparentPanel(new BorderLayout());

    private JLabel reasonLabel = new JLabel(
        Resources.getString("service.gui.REASON") + ": ");

    private JTextField reasonField = new JTextField();

    private JPanel dataPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JButton acceptButton
        = new JButton(Resources.getString("service.gui.ACCEPT"));

    private JButton rejectButton
        = new JButton(Resources.getString("service.gui.REJECT"));

    private JButton ignoreButton
        = new JButton(Resources.getString("service.gui.IGNORE"));

    private JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel northPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JLabel iconLabel = new JLabel(Resources.getImage("inviteIcon"));

    private String title
        = Resources.getString("service.gui.INVITATION_RECEIVED");

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
            Resources.getString("service.gui.INVITATION_RECEIVED_MSG",
                new String[] {  invitation.getInviter(),
                                invitation.getTargetWhiteboard()
                                    .getWhiteboardID()}));

        if(invitation.getReason() != null
                && invitation.getReason().length() != 0)
        {
            invitationReasonTextArea.setText(invitation.getReason());
            invitationReasonTextArea.setBorder(
                BorderFactory.createTitledBorder(
                    Resources.getString("service.gui.INVITATION")));

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
        this.rejectButton.setMnemonic(
            Resources.getMnemonic("service.gui.REJECT"));
        this.ignoreButton.setMnemonic(
            Resources.getMnemonic("service.gui.IGNORE"));

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

    @Override
    protected void close(boolean isEscaped)
    {
        rejectButton.doClick();
    }
}
