/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * Constructs new <tt>ChatRoomAutoOpenConfigDialog</tt> instance.
     * @param chatRoomId the chat room id of the chat room associated with the 
     * dialog 
     * @param pps the protocol provider service of the chat room
     */
    public ChatRoomAutoOpenConfigDialog(ProtocolProviderService pps, 
        final String chatRoomId)
    {
        
        this.pps = pps;
        this.chatRoomId = chatRoomId;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
            value = MUCService.OPEN_ON_MESSAGE;
        
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
        
        JPanel choicePanel = new TransparentPanel();
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
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
        
        setPreferredSize(new Dimension(300, 140));
        setVisible(true);
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
            else if(source.equals(openOnImportantMessage))
            {
                value = MUCService.OPEN_ON_IMPORTANT_MESSAGE;
            }
            else
            {
                value = MUCService.OPEN_ON_MESSAGE;
            }
        }
        
    }

    @Override
    public void dispose()
    {
        GuiActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            propertyListener);
        GuiActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService
                .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX + "."
                    + chatRoomId,
            propertyListener);
        super.dispose();
    }
    
    @Override
    protected void close(boolean escaped)
    {
        super.close(escaped);
        dispose();
    }
}
