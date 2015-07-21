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
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>ChatRoomNamePanel</tt> is the form, where we should enter the chat
 * room name.
 *
 * @author Yana Stamcheva
 */
@SuppressWarnings("serial")
public class ChatRoomNamePanel
    extends TransparentPanel
{
    private JLabel chatRoomLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.CHAT_ROOM_NAME"));

    private JTextField textField = new JTextField();

    private JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.JOIN_CHAT_ROOM_NAME"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.JOIN_CHAT_ROOM_TITLE"));

    private JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

    private JPanel rightPanel = new TransparentPanel(new BorderLayout());

    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     */
    public ChatRoomNamePanel()
    {
        super(new BorderLayout());

        this.infoLabel.setEditable(false);

        this.dataPanel.add(chatRoomLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(
            font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * Returns the chat room name entered by user.
     * @return the chat room name entered by user
     */
    public String getChatRoomName()
    {
        return textField.getText();
    }

    /**
     * Sets the given chat room name to the text field, contained in this panel.
     *
     * @param chatRoomName the chat room name to set to the text field
     */
    public void setChatRoomName(String chatRoomName)
    {
        textField.setText(chatRoomName);
    }

    /**
     * Requests the focus in the name text field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }

    /**
     * Adds a <tt>DocumentListener</tt> to the text field containing the chosen
     * chat room.
     *
     * @param l the <tt>DocumentListener</tt> to add
     */
    public void addChatRoomNameListener(DocumentListener l)
    {
        this.textField.getDocument().addDocumentListener(l);
    }

    /**
     * Removess a <tt>DocumentListener</tt> to the text field containing the
     * chosen chat room.
     *
     * @param l the <tt>DocumentListener</tt> to add
     */
    public void removeChatRoomNameListener(DocumentListener l)
    {
        this.textField.getDocument().removeDocumentListener(l);
    }
}
