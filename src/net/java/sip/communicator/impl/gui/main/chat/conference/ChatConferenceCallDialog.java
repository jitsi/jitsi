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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The dialog that allows the user to create or join a conference in the chat 
 * room.
 * 
 * @author Hristo Terezov
 */
public class ChatConferenceCallDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    /**
     * Create conference radio button.
     */
    private final JRadioButton createConferenceButton
        = new JRadioButton(
            GuiActivator.getResources()
                    .getI18NString("service.gui.CREATE_VIDEO_CONFERENCE"));

    /**
     * Join conference radio button.
     */
    private final JRadioButton joinConferenceButton
        = new JRadioButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.JOIN_EXISTING_VIDEO_CONFERENCE"));
    
    /**
     * Main panel that holds all elements in the dialog.
     */
    private final JPanel mainPanel
        = new TransparentPanel(new BorderLayout(10,10));
    
    /**
     * A panel with the join conference elements
     */
    private final JPanel joinPanel
        = new TransparentPanel(new BorderLayout(5,5));
    
    /**
     * A panel with the create conference elements
     */
    private final JPanel createPanel
        = new TransparentPanel(new BorderLayout());
    
    /**
     * Field for the name of the conference.
     */
    private final JTextField name = new JTextField();
    
    /**
     * The list with the announced conferences.
     */
    private ChatRoomConferenceCallsListPanel chatConferenceListPanel;
    
    /**
     * OK button.
     */
    private JButton okButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));
    
    /**
     * Cancel button.
     */
    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));
    
    /**
     * A button that ends the created conference by the user.
     */
    private JButton endConference = new JButton(
        GuiActivator.getResources().getI18NString(
            "service.gui.END_CONFERENCE"));
    
    /**
     * The chat panel that created the dialog.
     */
    private ChatPanel chatPanel = null;
    
    /**
     * The chat room associated with the dialog.
     */
    private ChatRoom chatRoom = null;
    
    /**
     * Constructs the <tt>ChatConferenceCallDialog</tt>.
     *
     * @param chatPanel the chat panel that created and showed the dialog.
     */
    public ChatConferenceCallDialog(ChatPanel chatPanel)
    {
        super(GuiActivator.getUIService().getMainFrame());
        
        setTitle(GuiActivator.getResources().getI18NString(
            "service.gui.CREATE_JOIN_VIDEO_CONFERENCE"));
        
        chatConferenceListPanel = new ChatRoomConferenceCallsListPanel(chatPanel);
        
        this.chatPanel = chatPanel;
        Object o = chatPanel.getChatSession().getDescriptor();
        if (o instanceof ChatRoomWrapper)
            chatRoom = ((ChatRoomWrapper)o).getChatRoom();

        initButtons();
        
        initPanels();
        
        add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Creates panels for create and join conference use cases.
     */
    private void initPanels()
    {
        JPanel conferenceNamePanel
            = new TransparentPanel(new BorderLayout(10,10));
        JLabel nameLabel = new JLabel(
            GuiActivator.getResources().getI18NString("service.gui.NAME"));
        conferenceNamePanel.add(nameLabel, BorderLayout.WEST);
        conferenceNamePanel.add(name, BorderLayout.CENTER);
        name.setEditable(true);
        name.setColumns(30);
        if(chatRoom != null)
            name.setText(GuiActivator.getResources()
                    .getI18NString("service.gui.CHAT_CONFERENCE_ITEM_LABEL",
                        new String[]{chatRoom.getUserNickname()}));

        createPanel.add(conferenceNamePanel,BorderLayout.CENTER);
        createPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        JLabel selectConferenceLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.SELECT_VIDEO_CONFERENCE"));
        joinPanel.add(selectConferenceLabel, BorderLayout.NORTH);
        joinPanel.add(chatConferenceListPanel,BorderLayout.CENTER);
        joinPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
    }

    /**
     * Creates the radio buttons that user chooses to create or join a 
     * conference. Creates OK and Cancel buttons. Creates "End Conference" 
     * button.
     */
    private void initButtons()
    {
        JPanel createOrJoinChoicePanel = new TransparentPanel(
            new BorderLayout(10,10));
        createConferenceButton.addActionListener( this );
        joinConferenceButton.addActionListener( this );


        createConferenceButton.setOpaque(false);
        joinConferenceButton.setOpaque(false);
        
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(createConferenceButton);
        buttonGroup.add(joinConferenceButton);
        createOrJoinChoicePanel.add(createConferenceButton, BorderLayout.NORTH);
        createOrJoinChoicePanel.add(joinConferenceButton, BorderLayout.CENTER);
        createOrJoinChoicePanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 0, 10));
        mainPanel.add(createOrJoinChoicePanel, BorderLayout.NORTH);
        
        JPanel buttonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
    
        endConference.setEnabled(false);
        endConference.addActionListener(this);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        
        buttonPanel.add(endConference);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * {@inheritDoc}
     *  Selects the create radio button if there are no announced conferences or
     *  the join radio button if there are announced conferences.
     */
    @Override 
    public void setVisible(boolean isVisible)
    {
        if(isVisible)
            setSelectedRadioButton(
                ((chatConferenceListPanel.getListSize() == 0 
                    && createConferenceButton.isEnabled())? true : false));
        super.setVisible(isVisible);
    }
    
    /**
     * Selects a radio button and shows the correct panel related to the 
     * selected radio button.
     * 
     * @param isCreateSelected if <tt>true</tt> the create radio button will be 
     * selected. If <tt>false</tt> the join radio button will be selected.
     */
    public void setSelectedRadioButton(boolean isCreateSelected)
    {
        if(isCreateSelected)
        {
            createConferenceButton.setSelected(true);
        }
        else
        {
            joinConferenceButton.setSelected(true);
        }
        updateView();
    }
    
    /**
     * Enables/Disables the create radio button. If the create radio button is 
     * disabled selects the join radio button and shows the join panel.
     * 
     * @param enabled whether the panel should be enabled or disabled.
     */
    public void setCreatePanelEnabled(boolean enabled)
    {
        if(!enabled)
        {
            setSelectedRadioButton(false);
            createConferenceButton.setEnabled(false);
        }
        else
        {
            createConferenceButton.setEnabled(true);
        }
    }
    
    /**
     * Enables (or disables) the "End Conference" button.
     * 
     * @param enabled whether the button should be enabled or disabled.
     */
    public void setEndConferenceButtonEnabled(boolean enabled)
    {
        endConference.setEnabled(enabled);
    }
    
    /**
     * Initializes the list of the conferences that are already announced. The 
     * list is displayed in the join panel.
     */
    public void initConferences()
    {
        chatConferenceListPanel.initConferences();
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     * @param e the event.
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        

        if (source instanceof JButton)
        {
            if(source.equals(okButton))
            {
                if(createConferenceButton.isSelected())
                {
                    createConference();
                }
                else
                {
                    joinConference();
                }
            }
            else if(source.equals(endConference))
            {
                chatRoom.publishConference(null, null);
            }
            this.setVisible(false);

        }
        else if(source instanceof JRadioButton)
        {
            if (source.equals(createConferenceButton) ||
                source.equals(joinConferenceButton)) 
            {
                updateView();
            }
        }
        
    }
    
    /**
     * Checks whether the create or join panel should be visible and shows it.
     */
    private void updateView()
    {
        
        mainPanel.remove(
            (createConferenceButton.isSelected()? joinPanel : 
                createPanel));
        
        mainPanel.add(
            (createConferenceButton.isSelected()? createPanel : 
                joinPanel), 
            BorderLayout.CENTER);
        if(joinConferenceButton.isSelected())
            this.chatConferenceListPanel.setSelectedIndex(0);
        pack();
    }
    
    /**
     * Creates a chat room conference call.
     */
    private void createConference()
    {
        if(chatRoom == null)
            return;
        
        String conferenceName = name.getText();
        
        OperationSetTelephonyConferencing telephonyConferencing
                = chatRoom.getParentProvider().getOperationSet(
                    OperationSetTelephonyConferencing.class);

        ConferenceDescription cd = null;
        if (telephonyConferencing != null)
        {
            cd = telephonyConferencing.setupConference(chatRoom);
        }

        if (cd != null)
        {
            chatRoom.publishConference(cd, conferenceName);
        }
    }
    
    /**
     * Joins an existing chat room conference call.
     */
    private void joinConference()
    {
        
        ConferenceDescription chatConference
            = chatConferenceListPanel.getSelectedValue();

        if (chatConference != null)
            CallManager.call(chatPanel.getChatSession()
                .getCurrentChatTransport()
                    .getProtocolProvider(), chatConference, chatRoom);
    }

    /**
     * Reloads icon label.
     */
    public void loadSkin()
    {
        chatConferenceListPanel.loadSkin();
    }


    /**
     * Adds a <tt>ConferenceDescription</tt> to the list of conferences.
     *
     * @param conferenceDescription the <tt>ConferenceDescription</tt> to add
     */
    public void addConference(ConferenceDescription conferenceDescription)
    {
        chatConferenceListPanel.addConference(conferenceDescription);
    }


    /**
     * Removes the given <tt>ConferenceDescription</tt> from the list of 
     * conferences.
     *
     * @param conferenceDescription the <tt>ConferenceDescription</tt> to remove
     */
    public void removeConference(ConferenceDescription conferenceDescription)
    {
        chatConferenceListPanel.removeConference(conferenceDescription);
    }
}
