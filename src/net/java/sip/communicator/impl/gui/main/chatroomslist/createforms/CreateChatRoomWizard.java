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
            GuiActivator.getMUCService()
                .createChatRoom(newChatRoom.getChatRoomName(),
                                newChatRoom.getProtocolProvider(),
                                null,
                                "",
                                true);
        }
    }
}
