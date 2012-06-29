/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;

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

        super.addWizardListener(this);

        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.JOIN_CHAT_ROOM_WIZARD"));

        this.setFinishButtonText(GuiActivator.getResources()
            .getI18NString("service.gui.JOIN"));

        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = GuiActivator.getUIService().getConferenceChatManager()
                .getChatRoomList().getChatRoomProviders();

        page1 = new JoinChatRoomWizardPage1(this,
                                            newChatRoom,
                                            chatRoomProviders);

        this.registerWizardPage(JoinChatRoomWizardPage1.IDENTIFIER, page1);

        page2 = new JoinChatRoomWizardPage2(this, newChatRoom);

        this.registerWizardPage(JoinChatRoomWizardPage2.IDENTIFIER, page2);

        this.setCurrentPage(JoinChatRoomWizardPage1.IDENTIFIER);
    }

    /**
     * Implements the Wizard.wizardFinished method.
     */
    public void wizardFinished(WizardEvent e)
    {
        if(e.getEventCode() == WizardEvent.SUCCESS)
        {
            GuiActivator.getUIService().getConferenceChatManager()
                .joinChatRoom(  newChatRoom.getChatRoomName(),
                                newChatRoom.getChatRoomProvider());
        }
    }
}
