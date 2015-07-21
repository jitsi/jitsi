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
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;

/**
 * The <tt>CreateChatRoomDialog</tt> is the dialog containing the form for
 * adding a chat room. It is different from the "Create chat room" wizard. The
 * <tt>CreateChatRoomDialog</tt> is used when a new chat room
 * is added to an already existing server in the list.
 *
 * @author Yana Stamcheva
 */
public class CreateChatRoomDialog
    extends SIPCommDialog
    implements ActionListener
{
    private ChatRoomNamePanel chatRoomPanel = new ChatRoomNamePanel();

    private JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CREATE"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    private ChatRoomProviderWrapper chatRoomProvider;

    /**
     * Creates an instance of <tt>CreateChatRoomDialog</tt> that represents a
     * dialog that adds a new chat room to an already existing server.
     *
     * @param provider The <tt>ChatRoomProviderWrapper</tt>.
     */
    public CreateChatRoomDialog(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;

        this.init();
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.CREATE_CHAT_ROOM"));

        this.setSize(620, 450);
        this.setPreferredSize(new Dimension(620, 450));

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("create");
        this.cancelButton.setName("cancel");

        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CREATE"));

        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(chatRoomPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     *
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("create"))
        {
            String chatRoomName = chatRoomPanel.getChatRoomName();

            GuiActivator.getMUCService()
                .createChatRoom(chatRoomName,
                                chatRoomProvider.getProtocolProvider(),
                                null,
                                "",
                                true);
        }
        this.dispose();
    }

    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
