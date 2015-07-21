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
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;

/**
 * This panel allows to search chat rooms on the considered provider, and to
 * join them.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class SearchChatRoomPanel
    extends TransparentPanel
    implements ActionListener, DocumentListener
{
    private final ChatRoomNamePanel namePanel = new ChatRoomNamePanel();

    private final JPanel mainPanel = new TransparentPanel();

    private final JPanel searchPanel = new TransparentPanel(new GridLayout(0, 1));

    private final JTextArea searchTextArea = new JTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.SEARCH_FOR_CHAT_ROOMS_MSG"));

    private final JButton searchButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.SEARCH"));

    private final JPanel buttonPanel = new TransparentPanel(
        new FlowLayout(FlowLayout.CENTER));

    private final JList chatRoomsList = new JList();

    private final JScrollPane chatRoomsScrollPane = new JScrollPane();

    private final WizardContainer wizardContainer;

    private ChatRoomProviderWrapper chatRoomProvider;

    List<String> serverRooms = null;

    /**
     * Creates a <tt>SearchChatRoomPanel</tt> instance without specifying
     * neither the parent window, nor the protocol provider.
     */
    public SearchChatRoomPanel(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.init();
    }

    /**
     * Creates an instance of <tt>SearchChatRoomPanel</tt>.
     *
     * @param provider the chat room provider wrapper corresponding to the
     * account for which the search panel is created
     */
    public SearchChatRoomPanel(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;
        this.wizardContainer = null;

        this.init();
    }

    private void init()
    {
        this.setLayout(new BorderLayout());

        this.buttonPanel.add(searchButton);

        this.searchTextArea.setOpaque(false);
        this.searchTextArea.setEditable(false);
        this.searchTextArea.setLineWrap(true);
        this.searchTextArea.setWrapStyleWord(true);

        this.namePanel.addChatRoomNameListener(this);

        this.searchPanel.add(searchTextArea);
        this.searchPanel.add(buttonPanel);

        this.namePanel.setPreferredSize(new Dimension(520, 100));
        this.searchPanel.setPreferredSize(new Dimension(520, 150));

        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(namePanel);
        this.mainPanel.add(searchPanel);

        this.searchPanel.setBorder(BorderFactory
            .createTitledBorder(GuiActivator.getResources()
                .getI18NString("service.gui.SEARCH")));

        this.searchButton.addActionListener(this);

        this.add(mainPanel, BorderLayout.NORTH);

        this.chatRoomsScrollPane.setBorder(BorderFactory
            .createTitledBorder(GuiActivator.getResources()
                .getI18NString("service.gui.CHAT_ROOMS")));

        this.chatRoomsList.addListSelectionListener(
            new ChatRoomListSelectionListener());
    }

    /**
     * Invoked when the Search button is clicked.
     */
    public void actionPerformed(ActionEvent e)
    {
        this.loadChatRoomsList();
    }

    /**
     * Loads the list of existing server chat rooms.
     */
    public void loadChatRoomsList()
    {
        serverRooms = GuiActivator.getMUCService()
                .getExistingChatRooms(chatRoomProvider);

        if(serverRooms != null)
        {
            if(serverRooms.size() == 0)
            {
                serverRooms.add(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_AVAILABLE_ROOMS"));
            }

            chatRoomsList.setListData(new Vector<String>(serverRooms));
            chatRoomsList.setBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            chatRoomsScrollPane.setPreferredSize(new Dimension(500, 250));
            chatRoomsScrollPane.setOpaque(false);

            chatRoomsScrollPane.getViewport().add(chatRoomsList);

            this.mainPanel.add(chatRoomsScrollPane);

            if (wizardContainer != null)
                wizardContainer.refresh();

            // When we're finished we replace the "wait cursor"
            // by the default cursor.
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * The <tt>ListSelectionListener</tt> of the chat rooms list. When a chat
     * room is selected in the list, we update the text field containing the
     * name of the chat room to join.
     */
    private class ChatRoomListSelectionListener
        implements ListSelectionListener
    {
        /**
         * When a chat room is selected in the list, we update the text field
         * containing the name of the chat room to join.
         */
        public void valueChanged(ListSelectionEvent e)
        {
            if(e.getValueIsAdjusting()
                || chatRoomsList.getSelectedIndex() == -1)
                return;

            // The listener has to be removed while the textfield he's listening
            // is being changed:
            namePanel.removeChatRoomNameListener(SearchChatRoomPanel.this);
            namePanel.setChatRoomName(
                chatRoomsList.getSelectedValue().toString());
            namePanel.addChatRoomNameListener(SearchChatRoomPanel.this);
        }
    }

    /**
     * Returns the chat room name entered by user.
     * @return the chat room name entered by user
     */
    public String getChatRoomName()
    {
        return namePanel.getChatRoomName();
    }

    /**
     * Sets the given chat room name to the text field, contained in this panel.
     *
     * @param chatRoomName the chat room name to set to the text field
     */
    public void setChatRoomName(String chatRoomName)
    {
        namePanel.setChatRoomName(chatRoomName);
    }

    /**
     * Requests the focus in the name text field.
     */
    public void requestFocusInField()
    {
        namePanel.requestFocusInField();
    }

    /**
     * Sets the protocol provider that have been chosen by user on the first
     * page of the join chat room wizard.
     *
     * @param provider the chat room provider wrapper for which we'd search a
     * chat room
     */
    public void setChatRoomProvider(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;
        serverRooms = GuiActivator.getMUCService().getExistingChatRooms(
            chatRoomProvider);
    }

    /**
     * Adds a <tt>DocumentListener</tt> to the text field containing the chosen
     * chat room.
     *
     * @param l the <tt>DocumentListener</tt> to add
     */
    public void addChatRoomNameListener(DocumentListener l)
    {
        namePanel.addChatRoomNameListener(l);
    }

    /**
     * Updates the chat rooms list when a key change is performed in the search
     * field. The new chat rooms list will contain all the chat rooms whose name
     * start with search field's text value.
     */
    public void updateChatRoomList()
    {
        if(namePanel.getChatRoomName().length() > 0)
        {
            Vector<String> newCRL = new Vector<String>();

            if(serverRooms != null)
                for(String s : serverRooms)
                    if(s.startsWith(namePanel.getChatRoomName())) newCRL.add(s);

            chatRoomsList.setListData(newCRL);
        }
        else
            chatRoomsList.setListData(new Vector<String>(serverRooms));
    }

    public void changedUpdate(DocumentEvent e)
    {
        updateChatRoomList();
    }

    public void insertUpdate(DocumentEvent e)
    {
        updateChatRoomList();
    }

    public void removeUpdate(DocumentEvent e)
    {
        updateChatRoomList();
    }
}
