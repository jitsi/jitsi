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
import java.beans.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A dialog for the chat room automatically open configuration.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomAutoOpenConfigDialog
    extends SIPCommDialog
    implements  ActionListener
{

    /**
     * The serial version ID. 
     */
    private static final long serialVersionUID = -7741709128413173168L;

    /**
     * The current value.
     */
    private String value = null;
    
    /**
     * The protocol provider service associated with the chat room.
     */
    private ProtocolProviderService pps;
    
    /**
     * The chat room id of the chat room.
     */
    private String chatRoomId;
    
    /**
     * Open on activity radio button.
     */
    private final JRadioButton openOnActivity
        = new JRadioButton(
            GuiActivator.getResources()
                    .getI18NString("service.gui.OPEN_ON_ACTIVITY"));

    /**
     * Open on message radio button.
     */
    private final JRadioButton openOnMessage
        = new JRadioButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.OPEN_ON_MESSAGE"));
    
    /**
     * Open on important message radio button.
     */
    private final JRadioButton openOnImportantMessage
        = new JRadioButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.OPEN_ON_IMPORTANT_MESSAGE"));
    
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
     * The property change listener for the message service.
     */
    private PropertyChangeListener propertyListener = new PropertyChangeListener()
    {
        
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            updateView();
        }
    };
    
    /**
     * The <tt>ChatRoomAutoOpenConfigDialog</tt> instance.
     */
    private static ChatRoomAutoOpenConfigDialog dialog = null;
    
    /**
     * Creates if necessary a new  <tt>ChatRoomAutoOpenConfigDialog</tt> 
     * instance and displays it.
     * @param chatRoomId the chat room id of the chat room associated with the 
     * dialog 
     * @param pps the protocol provider service of the chat room
     */
    public static void showChatRoomAutoOpenConfigDialog(
        ProtocolProviderService pps, 
        String chatRoomId)
    {
        if(dialog == null)
        {
            dialog = new ChatRoomAutoOpenConfigDialog(pps, chatRoomId);
        }
        else
        {
            dialog.clearListeners();
            dialog.setProvider(pps);
            dialog.setChatRoomId(chatRoomId);
            dialog.refreshValue();
            if(dialog.isVisible())
            {
                dialog.toFront();
            }
            else
            {
                dialog.setVisible(true);
            }
            dialog.pack();
        }
    }
    
    /**
     * Constructs new <tt>ChatRoomAutoOpenConfigDialog</tt> instance.
     * @param chatRoomId the chat room id of the chat room associated with the 
     * dialog 
     * @param pps the protocol provider service of the chat room
     */
    private ChatRoomAutoOpenConfigDialog(ProtocolProviderService pps, 
        final String chatRoomId)
    {
        
        this.pps = pps;
        this.chatRoomId = chatRoomId;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        refreshValue();
        
        JPanel choicePanel = new TransparentPanel();
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
        choicePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        openOnActivity.addActionListener( this );
        openOnMessage.addActionListener( this );
        openOnImportantMessage.addActionListener(this);

        setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.OPEN_AUTOMATICALLY"));

        openOnActivity.setOpaque(false);
        openOnMessage.setOpaque(false);
        openOnImportantMessage.setOpaque(false);
        
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(openOnActivity);
        buttonGroup.add(openOnMessage);
        buttonGroup.add(openOnImportantMessage);
        choicePanel.add(openOnActivity);
        choicePanel.add(openOnMessage);
        choicePanel.add(openOnImportantMessage);
        
        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        updateView();
    
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(choicePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setVisible(true);
        setPreferredSize(new Dimension(320, 150));
        pack();
    }
    
    /**
     * Sets new <tt>ProtocolProviderService</tt> instance.
     * 
     * @param pps the <tt>ProtocolProviderService</tt> to be set.
     */
    private void setProvider(ProtocolProviderService pps)
    {
        this.pps = pps;
    }
    
    /**
     * Sets new chat room id.
     * @param chatRoomId the chat room id to be set
     */
    private void setChatRoomId(String chatRoomId)
    {
        this.chatRoomId = chatRoomId;
    }
    
    /**
     * Refreshes the selected value if the chat room is changed.
     */
    private void refreshValue()
    {
        value = MUCService.getChatRoomAutoOpenOption(pps, chatRoomId);
        
        GuiActivator.getConfigurationService().addPropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED, 
            propertyListener);
        
        GuiActivator.getConfigurationService().addPropertyChangeListener(
            MessageHistoryService
                .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX + "."
                    + chatRoomId, 
                propertyListener);
        
        if(value == null)
            value = MUCService.DEFAULT_AUTO_OPEN_BEHAVIOUR;
        
        if(value.equals(MUCService.OPEN_ON_ACTIVITY))
        {
            openOnActivity.setSelected(true);
        }
        else if(value.equals(MUCService.OPEN_ON_IMPORTANT_MESSAGE))
        {
            openOnImportantMessage.setSelected(true);
        }
        else
        {
            openOnMessage.setSelected(true);
        }
    }
    /**
     * Sets enable/disable state of the buttons.
     */
    private void updateView()
    {
        MessageHistoryService mhs 
            = GuiActivator.getMessageHistoryService();
        if(!mhs.isHistoryLoggingEnabled() 
            || !mhs.isHistoryLoggingEnabled(chatRoomId))
        {
            openOnImportantMessage.setEnabled(false);
            openOnMessage.setEnabled(false);
            openOnActivity.setSelected(true);
        }
        else
        {
            openOnImportantMessage.setEnabled(true);
            openOnMessage.setEnabled(true);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        

        if (source instanceof JButton)
        {
            if(source.equals(okButton))
            {
                MUCService.setChatRoomAutoOpenOption(
                    pps,
                    chatRoomId, value);
            }
            this.dispose();
            
        }
        else if(source instanceof JRadioButton)
        {
            if(source.equals(openOnActivity))
            {
                value = MUCService.OPEN_ON_ACTIVITY;
            }
            else if(source.equals(openOnMessage))
            {
                value = MUCService.OPEN_ON_MESSAGE;
            }
            else
            {
                value = MUCService.OPEN_ON_IMPORTANT_MESSAGE;
            }
        }
        
    }

    /**
     * Removes the added listeners.
     */
    private void clearListeners()
    {
        GuiActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            propertyListener);
        GuiActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService
                .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX + "."
                    + chatRoomId,
            propertyListener);
    }
    
    @Override
    public void dispose()
    {
        clearListeners();
        super.dispose();
    }
    
    @Override
    protected void close(boolean escaped)
    {
        super.close(escaped);
        dispose();
    }
}
