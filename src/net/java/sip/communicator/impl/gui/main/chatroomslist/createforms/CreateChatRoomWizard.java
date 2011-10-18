/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.main.*;

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

        super.addWizardListener(this);

        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.CREATE_CHAT_ROOM_WIZARD"));

        this.setFinishButtonText(
            GuiActivator.getResources().getI18NString("service.gui.CREATE"));

        page1 = new CreateChatRoomWizardPage1(this, newChatRoom,
                    mainFrame.getProtocolProviders());

        this.registerWizardPage(CreateChatRoomWizardPage1.IDENTIFIER, page1);

        page2 = new CreateChatRoomWizardPage2(this, newChatRoom);

        this.registerWizardPage(CreateChatRoomWizardPage2.IDENTIFIER, page2);

        this.setCurrentPage(CreateChatRoomWizardPage1.IDENTIFIER);
    }

    /**
     * Implements the Wizard.wizardFinished method.
     */
    public void wizardFinished(WizardEvent e)
    {
        if(e.getEventCode() == WizardEvent.SUCCESS)
        {
            GuiActivator.getUIService().getConferenceChatManager()
                .createChatRoom(newChatRoom.getChatRoomName(),
                                newChatRoom.getProtocolProvider(),
                                null,
                                "");
        }
    }
}
