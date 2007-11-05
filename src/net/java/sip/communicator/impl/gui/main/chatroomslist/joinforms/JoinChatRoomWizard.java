/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>JoinChatRoomWizard</tt> is the wizard through which the user could
 * join a chat room. 
 * 
 * @author Yana Stamcheva
 */
public class JoinChatRoomWizard
    extends Wizard
    implements  WizardListener
{
    private Logger logger
        = Logger.getLogger(JoinChatRoomWizard.class.getName());
    
    private MainFrame mainFrame;
    
    private NewChatRoom newChatRoom = new NewChatRoom();
    
    private JoinChatRoomWizardPage1 page1;
    
    private JoinChatRoomWizardPage2 page2;
    
    /**
     * Creates an instance of <tt>CreateChatRoomWizard</tt>.
     *  
     * @param mainFrame the main application window
     */
    public JoinChatRoomWizard(MainFrame mainFrame)
    {
        super(mainFrame);
        
        this.mainFrame = mainFrame;
     
        super.addWizardListener(this);
        
        this.setTitle(Messages.getI18NString("joinChatRoomWizard").getText());

        page1 = new JoinChatRoomWizardPage1(this, newChatRoom,
                    mainFrame.getProtocolProviders());

        this.registerWizardPage(JoinChatRoomWizardPage1.IDENTIFIER, page1);

        page2 = new JoinChatRoomWizardPage2(this, newChatRoom);

        this.registerWizardPage(JoinChatRoomWizardPage2.IDENTIFIER, page2);

        this.setCurrentPage(JoinChatRoomWizardPage1.IDENTIFIER);
    }

    /**
     * Joins a chat room in a separate thread.
     */
    private class JoinChatRoom extends Thread
    {
        NewChatRoom newChatRoom;

        JoinChatRoom(NewChatRoom newChatRoom)
        {
            this.newChatRoom = newChatRoom;
        }
        
        public void run()
        {
            ChatRoom chatRoom = null;

            String chatRoomName = newChatRoom.getChatRoomName();

            OperationSetMultiUserChat multiUserChatOpSet
                = (OperationSetMultiUserChat) newChatRoom.getProtocolProvider()
                .getOperationSet(OperationSetMultiUserChat.class);

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
                        newChatRoom.getProtocolProvider().getAccountID()
                        .getService()}).getText())
                        .showDialog();
            }
            else
            {
                mainFrame.getMultiUserChatManager()
                    .joinChatRoom(chatRoom);
            }
        }
    }

    /**
     * Implements the Wizard.wizardFinished method.
     */
    public void wizardFinished(WizardEvent e)
    {
        if(e.getEventCode() == WizardEvent.SUCCESS)
            new JoinChatRoom(newChatRoom).start();
    }
}