/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

public class SearchChatRoomPanel
    extends TransparentPanel
    implements ActionListener
{
    private Logger logger = Logger.getLogger(SearchChatRoomPanel.class);
    
    private ChatRoomNamePanel namePanel = new ChatRoomNamePanel();
    
    private JPanel mainPanel = new TransparentPanel();

    private JPanel searchPanel = new TransparentPanel(new GridLayout(0, 1));
    
    private JTextArea searchTextArea = new JTextArea(
        Messages.getI18NString("searchForChatRoomsText").getText());
    
    private JButton searchButton = new JButton(
        Messages.getI18NString("search").getText());
    
    private JPanel buttonPanel = new TransparentPanel(
        new FlowLayout(FlowLayout.CENTER));
    
    private JList chatRoomsList = new JList();
    
    private JScrollPane chatRoomsScrollPane = new JScrollPane();

    private WizardContainer wizardContainer;

    private ChatRoomProviderWrapper chatRoomProvider;

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
     * @param parentWindow the window containing this panel
     * @param protocolProvider the protocol provider corresponding to the
     * account for which the search panel is created
     */
    public SearchChatRoomPanel(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;

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

        this.searchPanel.add(searchTextArea);
        this.searchPanel.add(buttonPanel);

        this.namePanel.setPreferredSize(new Dimension(520, 100));
        this.searchPanel.setPreferredSize(new Dimension(520, 150));

        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(namePanel);
        this.mainPanel.add(searchPanel);

        this.searchPanel.setBorder(BorderFactory
                .createTitledBorder(Messages.getI18NString("search").getText()));

        this.searchButton.addActionListener(this);

        this.add(mainPanel, BorderLayout.NORTH);

        this.chatRoomsScrollPane.setBorder(BorderFactory
            .createTitledBorder(Messages.getI18NString("chatRooms").getText()));

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
        List list = GuiActivator.getUIService().getConferenceChatManager()
            .getExistingChatRooms(chatRoomProvider);

        if(list != null)
        {
            if(list.size() == 0)
                list.add(Messages.getI18NString("noAvailableRooms").getText());

            chatRoomsList.setListData(new Vector(list));
            chatRoomsScrollPane.setPreferredSize(new Dimension(500, 120));

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
            if(e.getValueIsAdjusting())
                return;

            namePanel.setChatRoomName(
                chatRoomsList.getSelectedValue().toString());
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
     * @param protocolProvider the protocol provider for which we'd search a
     * chat room
     */
    public void setChatRoomProvider(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;
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
}
