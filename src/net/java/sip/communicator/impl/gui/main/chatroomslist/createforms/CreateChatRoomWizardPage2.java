/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>CreateChatRoomWizardPage2</tt> is the second page of the
 * "Create chat room" wizard. Contains the <tt>ChatRoomNamePanel</tt>, where
 * the user should enter the name of the chat room.
 * 
 * @author Yana Stamcheva
 */
public class CreateChatRoomWizardPage2
        implements WizardPage
{
    public static final String IDENTIFIER = "NAME_PANEL";
    
    private ChatRoomNamePanel namePanel;

    private NewChatRoom newChatRoom;
    
    /**
     * Creates an instance of <tt>CreateChatRoomWizardPage2</tt>.
     * @param wizard the parent wizard container
     * @param newChatRoom the object that collects all information for the
     * chat room, collected throughout the wizard
     */
    public CreateChatRoomWizardPage2(WizardContainer wizard,
            NewChatRoom newChatRoom)
    {
        this.newChatRoom = newChatRoom;
        
        namePanel = new ChatRoomNamePanel(wizard);
    }

    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return WizardPage.FINISH_PAGE_IDENTIFIER;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return CreateChatRoomWizardPage1.IDENTIFIER;
    }
    
    /**
     * Before finishing the wizard sets the identifier entered by the user
     * to the <tt>NewChatRoom</tt> object.
     */
    public void pageHiding()
    {
        newChatRoom.setChatRoomName(namePanel.getChatRoomName());
    }

    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of this page.
     */
    public Object getIdentifier()
    {
        return IDENTIFIER;
    }

    /**
     * Returns the form contained in this wizard page. In this case it's the
     * <tt>ChatRoomNamePanel</tt>.
     */
    public Object getWizardForm()
    {
        return namePanel;
    }

    public void pageShown()
    {
        namePanel.requestFocusInField();
    }

    public void pageShowing()
    {
        namePanel.setNextFinishButtonAccordingToUIN();
    }

    public void commitPage()
    {
    }

    public void pageBack()
    {
    }
}
