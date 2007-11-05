/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.awt.event.*;

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
    
    private SearchChatRoomPanel searchPanel;
    
    private I18NString joinString = Messages.getI18NString("join");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton joinButton = new JButton(joinString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
        .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private OperationSetMultiUserChat multiUserChatOpSet;
    
    private MainFrame mainFrame;
    
    private ProtocolProviderService protocolProvider;
    
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

        this.searchPanel = new SearchChatRoomPanel(this, protocolProvider);

        this.setTitle(Messages.getI18NString("joinChatRoom").getText());

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

        this.getContentPane().add(iconLabel, BorderLayout.WEST);
        this.getContentPane().add(searchPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice creates
     * the desired chat room in a separate thread.
     * <br>
     * Note: No specific properties are set right now!
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("join"))
        {           
            new Thread()
            {
                public void run()
                {
                    ChatRoom chatRoom = null;

                    String chatRoomName = searchPanel.getChatRoomName();

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
                            Messages.getI18NString("error").getText(),
                            Messages.getI18NString("chatRoomNotExist",
                                new String[]{chatRoomName,
                                protocolProvider.getAccountID().getService()})
                                .getText())
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
     * Shows this dialog. And requests the current focus in the chat room name
     * field.
     */
    public void showDialog()
    {
        this.setVisible(true);

        searchPanel.requestFocusInField();
    }
}