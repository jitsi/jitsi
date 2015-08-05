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

package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>JoinChatRoomWizardPage2</tt> is the second page of the
 * "Join chat room" wizard. Contains the <tt>SearchChatRoomPanel</tt>,
 * where the user should specify the chat room to join.
 *
 * @author Yana Stamcheva
 */
public class JoinChatRoomWizardPage2
        implements WizardPage
{
    public static final String IDENTIFIER = "SEARCH_CHAT_ROOM_PANEL";

    private SearchChatRoomPanel searchChatRoomPanel;

    private WizardContainer wizard;

    private NewChatRoom joinChatRoom;

    /**
     * Creates an instance of <tt>JoinChatRoomWizardPage2</tt>.
     *
     * @param wizard the parent wizard container
     * @param joinChatRoom the object that collects all information for the
     * chat room, collected throughout the wizard
     */
    public JoinChatRoomWizardPage2( WizardContainer wizard,
                                    NewChatRoom joinChatRoom)
    {
        this.wizard = wizard;

        this.joinChatRoom = joinChatRoom;

        searchChatRoomPanel = new SearchChatRoomPanel(wizard);

        searchChatRoomPanel.addChatRoomNameListener(
            new ChatRoomDocumentListener());
    }

    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     *
     * @return the identifier of the next wizard page
     */
    public Object getNextPageIdentifier()
    {
        return WizardPage.FINISH_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     *
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return JoinChatRoomWizardPage1.IDENTIFIER;
    }

    /**
     * Before finishing the wizard sets the identifier entered by the user
     * to the <tt>NewChatRoom</tt> object.
     */
    public void pageHiding()
    {
        joinChatRoom.setChatRoomName(searchChatRoomPanel.getChatRoomName());
    }

    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of this page.
     *
     * @return the identifier of this page
     */
    public Object getIdentifier()
    {
        return IDENTIFIER;
    }

    /**
     * Returns the form contained in this wizard page. In this case it's the
     * <tt>ChatRoomNamePanel</tt>.
     *
     * @return the form contained in this wizard page
     */
    public Object getWizardForm()
    {
        return searchChatRoomPanel;
    }

    /**
     * Pre-configures some properties when showing the page.
     */
    public void pageShown()
    {
        // Disable the finish button before there's anything in the chat room
        // field.
        wizard.setNextFinishButtonEnabled(false);

        searchChatRoomPanel.requestFocusInField();
        searchChatRoomPanel.setChatRoomProvider(
            joinChatRoom.getChatRoomProvider());
    }

    public void pageShowing()
    {
    }

    public void commitPage()
    {
    }

    public void pageBack()
    {
    }

    /**
     * Creates a <tt>DocumentListener</tt>, which would listen for events
     * coming from the <tt>SearchChatRoomPanel</tt>, when the user enters the
     * name of the chat room to join.
     */
    private class ChatRoomDocumentListener implements DocumentListener
    {
        /**
         * Called when the user enters new content in the chat room name field.
         */
        public void insertUpdate(DocumentEvent e)
        {
            if (e.getDocument().getLength() > 0)
                wizard.setNextFinishButtonEnabled(true);
            else
                wizard.setNextFinishButtonEnabled(false);
        }

        /**
         * Called when the user removes content from the chat room name field.
         */
        public void removeUpdate(DocumentEvent e)
        {
            if (e.getDocument().getLength() > 0)
                wizard.setNextFinishButtonEnabled(true);
            else
                wizard.setNextFinishButtonEnabled(false);
        }

        public void changedUpdate(DocumentEvent e)
        {}
    }
}
