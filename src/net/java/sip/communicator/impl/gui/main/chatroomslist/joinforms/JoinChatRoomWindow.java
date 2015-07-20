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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * This window allows to choose an account for joining a chat room.
 *
 * @author Valentin Martinet
 * @author Adam Netocny
 */
public class JoinChatRoomWindow
    extends SIPCommFrame
    implements Skinnable
{
    /**
     * An Eclipse generated serial version ID.
     */
    private static final long serialVersionUID = -5377426011460074319L;

    /**
     * The list of providers who support chat rooms.
     */
    private Vector<ChatRoomProviderWrapper> chatRoomProviders
        = new Vector<ChatRoomProviderWrapper>();

    /**
     * A JComboBox which will allow to select an account for joining a room.
     */
    private JComboBox jcb_chatRoomProviders;

    /**
     * An editable JComboBox which will allow to set a room name, and gives
     * suggestions regarding to its content.
     */
    private JComboBox jcb_roomName = new JComboBox();

    /**
     * Text editor for the room name combo box.
     */
    private JTextField editor;

    /**
     * Stores the provider icons.
     */
    private Vector<ImageIcon> providerIcons = new Vector<ImageIcon>();

    /**
     * Stores the provider names (plus AccountID).
     */
    private Vector<String> providerNames = new Vector<String>();

    /**
     * Rooms of the currently selected provider.
     */
    private List<String> serverRooms = null;

    /**
     * Search state value.
     */
    private String searchStateValue = GuiActivator.getResources().getI18NString(
        "service.gui.LOADING_ROOMS");

    /**
     * Search state label.
     */
    private JLabel jl_searchState = new JLabel(searchStateValue, JLabel.LEFT);

    /**
     * Builds the window.
     */
    public JoinChatRoomWindow()
    {
        this(null);
    }

    /**
     * Builds the window.
     * @param chatRoomProvider the provider to join the room to.
     */
    public JoinChatRoomWindow(ChatRoomProviderWrapper chatRoomProvider)
    {
        super();

        Iterator<ChatRoomProviderWrapper> providers =
            GuiActivator.getMUCService().getChatRoomProviders();

        while(providers.hasNext())
        {
            chatRoomProviders.add(providers.next());
            ChatRoomProviderWrapper provider =
                chatRoomProviders.get(chatRoomProviders.size()-1);

            if(provider.getProtocolProvider().getRegistrationState()
                    == RegistrationState.REGISTERED)
            {
                providerNames.add(provider.getProtocolProvider()
                    .getAccountID().getAccountAddress());
                providerIcons.add(new ImageIcon(provider.getProtocolProvider()
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
        }

        buildGUI();
        setVisible(true);
        loadProviderRooms();

        if(chatRoomProvider != null)
        {
            jcb_chatRoomProviders.setSelectedItem(
                chatRoomProvider.getProtocolProvider()
                    .getAccountID().getAccountAddress());
        }
    }

    /**
     * Builds the GUI of this window.
     */
    private void buildGUI()
    {
        String title = GuiActivator.getResources().getI18NString(
            "service.gui.JOIN_CHAT_ROOM_TITLE");
        this.setLayout(new BorderLayout());
        this.setTitle(title);

        jcb_chatRoomProviders = new JComboBox(providerNames);
        jcb_chatRoomProviders.setRenderer(new ComboBoxRenderer());
        jcb_roomName.setEditable(true);
        jcb_roomName.setPreferredSize(jcb_chatRoomProviders.getPreferredSize());

        // Initialization of the south panel, contains an 'Ok' button and
        // 'Undo' button:
        JPanel jp_buttons =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton jb_join = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.JOIN"));
        JButton jb_back = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

        jp_buttons.add(jb_back);
        jp_buttons.add(jb_join);

        jb_back.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            { dispose(); }
        });

        jb_join.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                GuiActivator.getMUCService().joinChatRoom(
                    editor.getText(), getSelectedProvider());
                dispose();
            }
        });

        editor = ((JTextField)jcb_roomName.getEditor().getEditorComponent());
        editor.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                handleChange();
            }
        });

        jcb_chatRoomProviders.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange() == ItemEvent.SELECTED)

                    new Thread(){
                        @Override
                        public void run()
                        {
                            jb_join.setEnabled(false);
                            jcb_roomName.setEnabled(false);
                            jl_searchState.setVisible(true);
                            loadProviderRooms();
                            jl_searchState.setVisible(false);
                            jcb_roomName.setEnabled(true);
                            jb_join.setEnabled(true);
                        }
                    }.start();
            }
        });

        // Initialization of the main panel which contains the account field and
        // the room name field:
        JPanel jp_formFields = new TransparentPanel();
        JPanel jp_accountField =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel jp_roomField =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel jp_indication =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        jp_formFields.setLayout(new BoxLayout(jp_formFields, BoxLayout.Y_AXIS));
        jp_accountField.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        jp_roomField.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7));

        jp_accountField.add(new JLabel(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT")
                + ": "));
        jp_accountField.add(jcb_chatRoomProviders);

        jp_roomField.add(new JLabel(GuiActivator.getResources().getI18NString(
            "service.gui.CHAT_ROOM_NAME") + ": "));
        jp_roomField.add(jcb_roomName);

        jl_searchState.setBorder(BorderFactory.createEmptyBorder(3, 7, 0, 0));
        jl_searchState.setFont(
           jl_searchState.getFont().deriveFont(Font.ITALIC, 11));
        jl_searchState.setForeground(Color.DARK_GRAY);
        jl_searchState.setVisible(false);

        JLabel jl_indication = new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.PRESS_ENTER_FOR_SUGGESTIONS"));
        jl_indication.setFont(jl_searchState.getFont());
        jl_indication.setForeground(jl_searchState.getForeground());
        jl_indication.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 8));
        jp_indication.add(jl_indication);

        jp_formFields.add(jl_searchState);
        jp_formFields.add(jp_accountField);
        jp_formFields.add(jp_roomField);
        jp_formFields.add(jp_indication);
        this.add(jp_formFields, BorderLayout.CENTER);
        this.add(jp_buttons, BorderLayout.SOUTH);

        // Size and position:
        this.setSize(new Dimension(415, 190));
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    /**
     * Updates the chat rooms list when a key change is performed in the search
     * field. The new chat rooms list will contain all the chat rooms whose name
     * start with search fields text value.
     * @param match search for.
     * @return the found rooms.
     */
    public Vector<String> getChatRoomList(String match)
    {
        Vector<String> rooms = new Vector<String>();

        if(serverRooms != null)
            for(String room : serverRooms)
                if(room.startsWith(match))
                   rooms.add(room);

        Collections.sort(rooms);
        return rooms;
    }

    /**
     * Indicates the window is about to be closed.
     *
     * @param escaped indicates if the window has been closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean escaped)
    {
        dispose();
    }

    /**
     * Returns the selected provider in the providers combo box.
     *
     * @return the selected provider
     */
    public ChatRoomProviderWrapper getSelectedProvider()
    {
        for(ChatRoomProviderWrapper crp : chatRoomProviders)
        {
            if(crp.getProtocolProvider().getAccountID()
                .getAccountAddress().equals(
                    (jcb_chatRoomProviders.getSelectedItem())))
            {
                return crp;
            }
        }

        return null;
    }

    /**
     * Loads the rooms hosted on the selected provider.
     */
    public void loadProviderRooms()
    {
        serverRooms = GuiActivator.getMUCService().getExistingChatRooms(
           getSelectedProvider());
    }

    /**
     * Cell renderer for the providers combo box: displays the protocol name
     * with its associated icon.
     */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer
    {
        /**
         * The renderer.
         */
        public ComboBoxRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        /**
         * Returns the cell renderer component for the given <tt>list</tt> and
         * <tt>value</tt>.
         *
         * @param list the parent list
         * @param value the value to render
         * @param index the index of the rendered cell in the list
         * @param isSelected indicates if the cell is currently selected
         * @param cellHasFocus indicates that the cell has the focus
         * @return the rendering component
         */
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            String label = (String)value;

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            int idx = providerNames.indexOf(label);
            ImageIcon icon = providerIcons.get(idx);

            setText(label);

            if (icon != null) setIcon(icon);

            return this;
        }
    }

    /**
     * Performs changes in the room name combo box when its editor content has
     * changed.
     */
    public void handleChange()
    {
        final String match = editor.getText();

        SwingUtilities.invokeLater(new Runnable(){
            public void run()
            {
                for(int i=0; i< jcb_roomName.getItemCount(); i++)
                    jcb_roomName.removeItemAt(i);

                for(String room : getChatRoomList(match))
                    jcb_roomName.addItem(room);

                editor.setText(match);
                jcb_roomName.showPopup();
            }
        });
    }

    /**
     * Reloads provider icons.
     */
    public void loadSkin()
    {
        providerIcons.clear();

        for(ChatRoomProviderWrapper provider : chatRoomProviders)
        {
            if(provider.getProtocolProvider().getRegistrationState()
                    == RegistrationState.REGISTERED)
            {
                providerIcons.add(new ImageIcon(provider.getProtocolProvider()
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
        }
    }
}
