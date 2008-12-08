/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CreateChatRoomDialog</tt> is the dialog containing the form for adding
 * a chat room. It is different from the "Create chat room" wizard. The
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
    
    private I18NString addString = Messages.getI18NString("create");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton addButton = new JButton(addString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel mainPanel = new TransparentPanel(new BorderLayout());
    
    private ChatRoomProviderWrapper chatRoomProvider;
    
    /**
     * Creates an instance of <tt>CreateChatRoomDialog</tt> that represents a dialog
     * that adds a new chat room to an already existing server.
     * 
     * @param mainFrame The <tt>MainFrame</tt> parent window.
     * @param pps The <tt>ProtocolProviderService</tt>.
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
        this.setTitle(Messages.getI18NString("createChatRoom").getText());
        
        this.setSize(620, 450);
        this.setPreferredSize(new Dimension(620, 450));
        
        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("create");
        this.cancelButton.setName("cancel");
        
        this.addButton.setMnemonic(addString.getMnemonic());
        
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
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

            GuiActivator.getUIService().getConferenceChatManager()
                .createChatRoom(chatRoomName,
                                chatRoomProvider.getProtocolProvider());
        }
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
