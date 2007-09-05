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

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>JoinChatRoomDialog</tt> is the dialog containing the form for joining
 * a chat room.
 * 
 * @author Yana Stamcheva
 */
public class JoinChatRoomDialog
    extends SIPCommDialog
    implements ActionListener
{
    private Logger logger = Logger.getLogger(JoinChatRoomDialog.class.getName());
    
    private JoinChatRoomPanel namePanel = new JoinChatRoomPanel();
    
    private SearchChatRoomPanel searchPanel;
    
    private I18NString joinString = Messages.getI18NString("join");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton joinButton = new JButton(joinString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
        .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private JList chatRoomsList = new JList();
    
    private OperationSetMultiUserChat multiUserChatOpSet;
    
    private MainFrame mainFrame;
    
    private ProtocolProviderService protocolProvider;
    
    private JScrollPane chatRoomsScrollPane = new JScrollPane();
    
    /**
     * Creates an instance of <tt>JoinChatRoomDialog</tt>.
     * 
     * @param mainFrame the <tt>MainFrame</tt> parent window
     * @param pps the <tt>ProtocolProviderService</tt>, which will be the chat
     * server for the newly created chat room
     */
    public JoinChatRoomDialog(MainFrame mainFrame,
            ProtocolProviderService pps)
    {
        super(mainFrame, false);
        
        this.mainFrame = mainFrame;
        this.protocolProvider = pps;
        this.multiUserChatOpSet
            = (OperationSetMultiUserChat) protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        
        searchPanel = new SearchChatRoomPanel(this);
        
        this.setTitle(Messages.getI18NString("joinChatRoom").getText());
        
        this.namePanel.setPreferredSize(new Dimension(520, 100));
        this.searchPanel.setPreferredSize(new Dimension(520, 110));
        
        this.getRootPane().setDefaultButton(joinButton);
        this.joinButton.setName("join");
        this.cancelButton.setName("cancel");
        
        this.joinButton.setMnemonic(joinString.getMnemonic());
        
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
    
        this.joinButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(joinButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(namePanel);
        this.mainPanel.add(searchPanel);
        
        this.chatRoomsScrollPane.setBorder(BorderFactory
            .createTitledBorder(Messages.getI18NString("chatRooms").getText()));
        
        this.getContentPane().add(iconLabel, BorderLayout.WEST);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
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
        
        if (name.equals("join"))
        {           
            new Thread()
            {
                public void run()
                {
                    ChatRoom chatRoom = null;
                    
                    String chatRoomName = namePanel.getChatRoomName();
                    
                    try
                    {
                        chatRoom = multiUserChatOpSet
                            .findRoom(chatRoomName);
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error("Failed to find chat room with name:"
                            + chatRoomName, e1);
                    }
                    catch (OperationNotSupportedException e1)
                    {                        
                        logger.error("Failed to find chat room with name:"
                            + chatRoomName, e1);
                    }

                    if(chatRoom == null)
                    {
                        new ErrorDialog(mainFrame,
                            Messages.getI18NString("chatRoomNotExist",
                                new String[]{chatRoomName,
                                protocolProvider.getAccountID().getService()})
                                .getText(),
                            Messages.getI18NString("error").getText())
                                .showDialog();
                    }
                    else
                    {  
                        mainFrame.getMultiUserChatManager()
                            .joinChatRoom(chatRoom);
                    }
                }
            }.start();
        }
        this.dispose();
    }

    /**
     * When escape is pressed clicks the cancel button programatically.
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Loads the list of existing server chat rooms.
     */
    public void loadChatRoomsList()
    {        
        OperationSetMultiUserChat multiUserChat
            = (OperationSetMultiUserChat) protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        
        List list = null;
        try
        {
            list = multiUserChat.getExistingChatRooms();
        }
        catch (OperationFailedException e)
        {
            logger.error("Failed to obtain existing chat rooms for server: "
                + protocolProvider.getAccountID().getService(), e);
        }
        catch (OperationNotSupportedException e)
        {
            logger.error("Failed to obtain existing chat rooms for server: "
                + protocolProvider.getAccountID().getService(), e);
        }

        if(list != null)
        {
            chatRoomsList.setListData(new Vector(list));
            chatRoomsScrollPane.setPreferredSize(new Dimension(500, 120));
            
            chatRoomsScrollPane.getViewport().add(chatRoomsList);
            
            this.mainPanel.add(chatRoomsScrollPane);
            
            this.pack();
        }
    }
}