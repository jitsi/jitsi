/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CreateChatRoomWizard</tt> is the wizard through which the user could
 * create its own chat room. 
 * 
 * @author Yana Stamcheva
 */
public class CreateChatRoomWizard
    extends Wizard
    implements  WizardListener
{
    private Logger logger
        = Logger.getLogger(CreateChatRoomWizard.class.getName());
    
    private MainFrame mainFrame;
    
    private NewChatRoom newChatRoom = new NewChatRoom();
    
    private CreateChatRoomWizardPage1 page1;
    
    private CreateChatRoomWizardPage2 page2;
    
    /**
     * Creates an instance of <tt>CreateChatRoomWizard</tt>.
     *  
     * @param mainFrame the main application window
     */
    public CreateChatRoomWizard(MainFrame mainFrame)
    {
        super(mainFrame);
        
        this.mainFrame = mainFrame;
     
        super.addWizardListener(this);
        
        this.setTitle(Messages.getI18NString("createChatRoomWizard").getText());

        page1 = new CreateChatRoomWizardPage1(this, newChatRoom,
                    mainFrame.getProtocolProviders());

        this.registerWizardPage(CreateChatRoomWizardPage1.IDENTIFIER, page1);

        page2 = new CreateChatRoomWizardPage2(this, newChatRoom);

        this.registerWizardPage(CreateChatRoomWizardPage2.IDENTIFIER, page2);

        this.setCurrentPage(CreateChatRoomWizardPage1.IDENTIFIER);
    }

    /**
     * Creates a new chat room in a separate thread.
     */
    private class CreateChatRoom extends Thread
    {
        NewChatRoom newChatRoom;
        
        CreateChatRoom(NewChatRoom newChatRoom)
        {
            this.newChatRoom = newChatRoom;
        }
        
        public void run()
        {
            ChatRoom chatRoom = null;
            try
            {
                chatRoom = mainFrame.getMultiUserChatOpSet(
                    newChatRoom.getProtocolProvider()).createChatRoom(
                        newChatRoom.getChatRoomName(), null);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to create chat room.", ex);
                
                new ErrorDialog(mainFrame,
                    Messages.getI18NString(
                        "createChatRoomError",
                        new String[]{newChatRoom.getChatRoomName()}).getText(),
                        ex,
                    Messages.getI18NString(
                        "error").getText())
                        .showDialog();
            }
            catch (OperationNotSupportedException ex)
            {
                logger.error("Failed to create chat room.", ex);
                
                new ErrorDialog(mainFrame,
                    Messages.getI18NString(
                        "createChatRoomError",
                        new String[]{newChatRoom.getChatRoomName()}).getText(),
                        ex,
                    Messages.getI18NString(
                        "error").getText())
                        .showDialog();
            }
            
            if(chatRoom != null)
                mainFrame.getChatRoomsListPanel().getChatRoomsList()
                    .addChatRoom(new ChatRoomWrapper(chatRoom));
        }
    }

    /**
     * Implements the Wizard.wizardFinished method.
     */
    public void wizardFinished(WizardEvent e)
    {
        if(e.getEventCode() == WizardEvent.SUCCESS)
            new CreateChatRoom(newChatRoom).start();
    }
}
