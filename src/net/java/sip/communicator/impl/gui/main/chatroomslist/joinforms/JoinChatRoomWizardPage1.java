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

import java.util.*;

import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;

/**
 * The <tt>CreateChatRoomWizardPage1</tt> is the first page of the
 * "Create chat room" wizard. Contains the <tt>SelectAccountPanel</tt>, where
 * the user should select the account, for which the new chat room will be
 * created.
 *
 * @author Yana Stamcheva
 */
public class JoinChatRoomWizardPage1
        implements  WizardPage,
                    ListSelectionListener
{
    /**
     * The identifier of this wizard page.
     */
    public static final String IDENTIFIER = "SELECT_ACCOUNT_PANEL";

    private SelectAccountPanel selectAccountPanel;

    private WizardContainer wizard;

    /**
     * Creates an instance of <tt>JoinChatRoomWizardPage1</tt>.
     *
     * @param wizard the parent wizard container
     * @param joinChatRoom the object that will collect the information through
     * the wizard
     * @param chatRoomProviders The list of available installed
     * <tt>ChatRoomProviderWrapper</tt>, from which the user could select.
     */
    public JoinChatRoomWizardPage1(
        WizardContainer wizard,
        NewChatRoom joinChatRoom,
        Iterator<ChatRoomProviderWrapper> chatRoomProviders)
    {
        this.wizard = wizard;

        selectAccountPanel
            = new SelectAccountPanel(joinChatRoom, chatRoomProviders);

        selectAccountPanel.addListSelectionListener(this);
    }

    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a check box is already selected or disables it if
     * nothing is selected.
     */
    public void pageShowing()
    {
        setNextButtonAccordingToRowSelection();
    }

    /**
     * Enables the next button when the user makes a choice and disables it
     * if nothing is selected.
     */
    private void setNextButtonAccordingToRowSelection()
    {
        if (selectAccountPanel.isRowSelected())
            this.wizard.setNextFinishButtonEnabled(true);
        else
            this.wizard.setNextFinishButtonEnabled(false);
    }

    /**
     * Listens for selection evens so that we would only enable the next button
     * if an account has actually been chosen.
     *
     * @param e the <tt>ListSelectionEvent</tt> that has just occurred.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        setNextButtonAccordingToRowSelection();
    }

    /**
     * Returns the identifier of this wizard page.
     *
     * @return  the identifier of this wizard page
     */
    public Object getIdentifier()
    {
        return IDENTIFIER;
    }

    /**
     * Returns the identifier of the next wizard page.
     *
     * @return the identifier of the next wizard page
     */
    public Object getNextPageIdentifier()
    {
        return JoinChatRoomWizardPage2.IDENTIFIER;
    }

    /**
     * Returns the identifier of the back wizard page.
     *
     * @return the identifier of the back wizard page
     */
    public Object getBackPageIdentifier()
    {
        return IDENTIFIER;
    }

    /**
     * Returns the form contained in this wizard page.
     *
     * @return the form contained in this wizard page
     */
    public Object getWizardForm()
    {
        return selectAccountPanel;
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    /**
     * Saves the selected account before going to the next page.
     */
    public void commitPage()
    {
        selectAccountPanel.initSelectedAccount();
    }

    public void pageBack()
    {
    }
}
