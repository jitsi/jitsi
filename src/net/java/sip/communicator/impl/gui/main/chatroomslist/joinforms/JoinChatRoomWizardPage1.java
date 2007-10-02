/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.util.*;

import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;

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
                    CellEditorListener
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
     * @param protocolProvidersList The list of available installed 
     * <tt>ProtocolProviderServices</tt>, from which the user could select.
     */
    public JoinChatRoomWizardPage1( WizardContainer wizard,
                                    NewChatRoom joinChatRoom,
                                    Iterator protocolProvidersList)
    {
        this.wizard = wizard;

        selectAccountPanel
            = new SelectAccountPanel(joinChatRoom, protocolProvidersList);

        selectAccountPanel.addCheckBoxCellListener(this);
    }
    
    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a check box is already selected or disables it if 
     * nothing is selected.
     */
    public void pageShowing()
    {
        setNextButtonAccordingToCheckBox();
    }

    /**
     * Enables the next button when the user makes a choice and disables it 
     * if nothing is selected.
     */
    private void setNextButtonAccordingToCheckBox()
    {
        if (selectAccountPanel.isRadioSelected())
            this.wizard.setNextFinishButtonEnabled(true);
        else
            this.wizard.setNextFinishButtonEnabled(false);
    }

    /**
     * When user canceled editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingCanceled(ChangeEvent e)
    {
        setNextButtonAccordingToCheckBox();
    }

    /**
     * When user stopped editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingStopped(ChangeEvent e)
    {
        setNextButtonAccordingToCheckBox();
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
    public void pageNext()
    {
        selectAccountPanel.setSelectedAccount();
    }

    public void pageBack()
    {
    }
}