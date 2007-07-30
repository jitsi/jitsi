/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AddChatRoomDialog</tt> is the dialog containing the form for adding
 * a chat room into a chat server. It is different from the "Create chat room"
 * wizard opened from the main file menu. The <tt>AddChatRoomDialog</tt> is used
 * when user want to add a chat room directly by clicking on the server in the
 * <tt>ChatRoomsList</tt>.
 * 
 * @author Yana Stamcheva
 */
public class AddChatRoomDialog
    extends SIPCommDialog
    implements ActionListener {

    private Logger logger = Logger.getLogger(AddChatRoomDialog.class.getName());
    
    private ChatRoomNamePanel namePanel = new ChatRoomNamePanel();
    
    private I18NString addString = Messages.getI18NString("add");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton addButton = new JButton(addString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private OperationSetMultiUserChat multiUserChatOpSet;
    
    private MainFrame mainFrame;
    
    private ProtocolProviderService protocolProvider;
    
    /**
     * Creates an instance of <tt>AddChatRoomDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     * 
     * @param mainFrame the <tt>MainFrame</tt> parent window
     * @param pps the <tt>ProtocolProviderService</tt>, which will be the chat
     * server for the newly created chat room
     */
    public AddChatRoomDialog(MainFrame mainFrame,
            ProtocolProviderService pps)
    {
        super(mainFrame);
        
        this.mainFrame = mainFrame;
        this.protocolProvider = pps;
        this.multiUserChatOpSet
            = (OperationSetMultiUserChat) protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        
        this.setTitle(Messages.getI18NString("createChatRoom").getText());
        
        this.setSize(520, 250);
        
        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("add");
        this.cancelButton.setName("cancel");
        
        this.addButton.setMnemonic(addString.getMnemonic());
        
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(namePanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
        
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice creates
     * the desired chat room in a separate thread.
     * <br>
     * Note: No specific properties are set right now!
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("add"))
        {   
            new Thread()
            {
                public void run()
                {
                    ChatRoom newChatRoom = null;
                    
                    String chatRoomName = namePanel.getChatRoomName();
                    try
                    {
                        newChatRoom = multiUserChatOpSet
                            .createChatRoom(chatRoomName,
                                new Hashtable());
                        
                        mainFrame.getChatRoomsListPanel().getChatRoomsList()
                            .addChatRoom(new ChatRoomWrapper(newChatRoom));
                    }
                    catch (OperationFailedException e)
                    {
                        new ErrorDialog(mainFrame,
                            Messages.getI18NString("failedToCreateChatRoom",
                                new String[]{chatRoomName}).getText(),
                            Messages.getI18NString("error").getText())
                                .showDialog();
                        
                        logger.error("Failed to create chat room with name: "
                            + namePanel.getChatRoomName(), e);
                    }
                    catch (OperationNotSupportedException e)
                    {
                        new ErrorDialog(mainFrame,
                            Messages.getI18NString("createChatRoomNotSupported",
                                new String[]{chatRoomName,
                                protocolProvider.getAccountID().getService()})
                                .getText(),
                            Messages.getI18NString("error").getText())
                                .showDialog();
                        
                        logger.error("Failed to create chat room with name: "
                            + namePanel.getChatRoomName(), e);
                    }
                    catch (Exception e)
                    {
                        new ErrorDialog(mainFrame,
                            Messages.getI18NString("failedToCreateChatRoom",
                                new String[]{chatRoomName}).getText(),
                            Messages.getI18NString("error").getText())
                                .showDialog();

                        logger.error("Failed to create chat room with name: "
                            + namePanel.getChatRoomName(), e);
                    }                    
                }
            }.start();                
        }
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();        
    }    
}
