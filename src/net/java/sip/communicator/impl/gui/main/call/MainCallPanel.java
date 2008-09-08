/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The panel containing the call field and button, serving to make calls.
 * 
 * @author Yana Stamcheva
 */
public class MainCallPanel
    extends JPanel
    implements  ActionListener,
                ListSelectionListener,
                RegistrationStateChangeListener
{
    private static final String CALL_BUTTON = "CallButton";

    private static final String DIAL_BUTTON = "HangupButton";

    private MainFrame mainFrame;

    private ProtocolProviderService protocolProvider;

    private SIPCommButton callButton = new SIPCommButton(ImageLoader
        .getImage(ImageLoader.CALL_BUTTON_BG), ImageLoader
        .getImage(ImageLoader.CALL_ROLLOVER_BUTTON_BG), null, ImageLoader
        .getImage(ImageLoader.CALL_BUTTON_PRESSED_BG));

    private CallComboBox phoneNumberCombo = new CallComboBox(this);

    private JPanel comboPanel = new JPanel(new BorderLayout(5, 5));

    private JLabel callViaLabel
        = new JLabel(Messages.getI18NString("callVia").getText() + " ");

    private JPanel callViaPanel
        = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));

    private AccountSelectorBox accountSelectorBox = new AccountSelectorBox(this);

    private SIPCommButton dialButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.DIAL_BUTTON),
        ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

    private DialpadDialog dialpadDialog;

    private boolean isCallMetaContact = false;

    /**
     * Initializes and constructs this panel.
     */
    public MainCallPanel(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        this.dialpadDialog = new DialpadDialog(this);

        comboPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        phoneNumberCombo.setEditable(true);

        callButton.setEnabled(false);

        this.add(comboPanel, BorderLayout.CENTER);

        callViaPanel.add(callViaLabel);
        callViaPanel.add(accountSelectorBox);

        comboPanel.add(dialButton, BorderLayout.WEST);
        comboPanel.add(phoneNumberCombo, BorderLayout.CENTER);
        comboPanel.add(callButton, BorderLayout.EAST);

        callButton.setName(CALL_BUTTON);
        dialButton.setName(DIAL_BUTTON);

        callButton.setToolTipText(
            Messages.getI18NString("call").getText());

        callButton.addActionListener(this);
        dialButton.addActionListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> generated when user presses one of the
     * buttons in this panel.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(CALL_BUTTON))
        {
            // call button is pressed when a meta contact is selected
            if (isCallMetaContact)
            {
                Object[] selectedContacts = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValues();

                Vector telephonyContacts = new Vector();

                for (int i = 0; i < selectedContacts.length; i++)
                {
                    Object o = selectedContacts[i];

                    if (o instanceof MetaContact)
                    {
                        Contact contact = ((MetaContact) o)
                                .getDefaultContact(
                                OperationSetBasicTelephony.class);

                        if (contact != null)
                            telephonyContacts.add(contact);
                        else
                        {
                            new ErrorDialog(
                                this.mainFrame,
                                Messages.getI18NString("warning").getText(),
                                Messages.getI18NString(
                                    "contactNotSupportingTelephony",
                                    new String[]
                                    { ((MetaContact) o).getDisplayName() })
                                        .getText())
                                .showDialog();
                        }
                    }
                }

                if (telephonyContacts.size() > 0)
                    CallManager.createCall(protocolProvider, telephonyContacts);
            }
            else if (!phoneNumberCombo.isComboFieldEmpty())
            {
                // if no contact is selected checks if the user has chosen
                // or has
                // writen something in the phone combo box

                String stringContact = phoneNumberCombo.getEditor()
                    .getItem().toString();

                CallManager.createCall(protocolProvider, stringContact);
            }
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {

            if(!dialpadDialog.isVisible())
            {
                dialpadDialog.setSize(
                    mainFrame.getWidth() - 20,
                    dialpadDialog.getHeight());

                dialpadDialog.setLocation(
                    mainFrame.getX() + 10,
                    dialButton.getLocationOnScreen().y
                        - dialpadDialog.getHeight());

                dialpadDialog.setVisible(true);
            }
            else
            {
                dialpadDialog.setVisible(false);
            }
        }
    }

    /**
     * Returns the main application window, which is the parent of this panel.
     * 
     * @return the main application window, which is the parent of this panel.
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * Sets the protocol provider to be used for making calls.
     * 
     * @param protocolProvider the protocol provider to be used for making calls.
     */
    public ProtocolProviderService getCallProvider()
    {
        return this.protocolProvider;
    }

    /**
     * Sets the protocol provider to be used for making calls.
     * 
     * @param protocolProvider the protocol provider to be used for making calls.
     */
    public void setCallProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Sets the isCallMetaContact variable to TRUE or FALSE. This defines if
     * this call is a call to a given meta contact selected from the contact
     * list or a call to an external contact or phone number.
     * 
     * @param isCallMetaContact TRUE to define this call as a call to an
     *            internal meta contact and FALSE to define it as a call to an
     *            external contact or phone number.
     */
    public void setCallMetaContact(boolean isCallMetaContact)
    {
        this.isCallMetaContact = isCallMetaContact;
    }

    /**
     * Enables or disabled the call button.
     * 
     * @param isEnabled <code>true</code> to enable the call button and
     * <tt>false</tt> otherwise.
     */
    public void setCallButtonEnabled(boolean isEnabled)
    {
        this.callButton.setEnabled(isEnabled);
    }

    /**
     * Sets the given <tt>phoneNumber</tt> to the phone number combo box.
     * 
     * @param phoneNumber the phone number to set.
     */
    public void setPhoneNumberComboText(String phoneNumber)
    {
        this.phoneNumberCombo.getEditor().setItem(phoneNumber);
    }

    /**
     * Returns the content of the phone number combo box.
     * 
     * @return the content of the phone number combo box.
     */
    public String getPhoneNumberComboText()
    {
        return (String) this.phoneNumberCombo.getEditor().getItem();
    }
    
    /**
     * Requests the focus in the phone number combo box.
     */
    public void requestFocusInPhoneCombo()
    {
        this.phoneNumberCombo.requestFocus();
    }

    /**
     * Adds the given call account to the list of call via accounts.
     * 
     * @param pps the protocol provider service corresponding to the account
     */
    public void addCallAccount(ProtocolProviderService pps)
    {
        if (accountSelectorBox.getAccountsNumber() > 0)
        {
            this.comboPanel.add(callViaPanel, BorderLayout.SOUTH);
        }

        accountSelectorBox.addAccount(pps);

        pps.addRegistrationStateChangeListener(this);
    }

    /**
     * Removes the account corresponding to the given protocol provider from the
     * call via selector box.
     * 
     * @param pps the protocol provider service to remove
     */
    public void removeCallAccount(ProtocolProviderService pps)
    {
        this.accountSelectorBox.removeAccount(pps);

        pps.removeRegistrationStateChangeListener(this);

        if (accountSelectorBox.getAccountsNumber() < 2)
        {
            this.comboPanel.remove(callViaPanel);
        }
    }

    /**
     * Returns TRUE if the account corresponding to the given protocol provider
     * is already contained in the call via selector box, otherwise returns
     * FALSE.
     * 
     * @param pps the protocol provider service for the account
     * @return TRUE if the account corresponding to the given protocol provider
     *         is already contained in the call via selector box, otherwise
     *         returns FALSE
     */
    public boolean containsCallAccount(ProtocolProviderService pps)
    {
        return accountSelectorBox.containsAccount(pps);
    }

    /**
     * Updates the call via account status.
     * 
     * @param pps the protocol provider service for the account
     */
    public void updateCallAccountStatus(ProtocolProviderService pps)
    {
        accountSelectorBox.updateAccountStatus(pps);
    }

    /**
     * Returns the account selector box.
     * 
     * @return the account selector box.
     */
    public AccountSelectorBox getAccountSelectorBox()
    {
        return accountSelectorBox;
    }

    /**
     * Implements ListSelectionListener.valueChanged. Enables or disables call
     * and hangup buttons depending on the selection in the contactlist.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        Object o = mainFrame.getContactListPanel().getContactList()
            .getSelectedValue();

        if ((e.getFirstIndex() != -1 || e.getLastIndex() != -1)
            && (o instanceof MetaContact))
        {
            setCallMetaContact(true);

            // Switch automatically to the appropriate pps in account selector
            // box and enable callButton if telephony is supported.
            Contact contact = ((MetaContact) o)
                    .getDefaultContact(OperationSetBasicTelephony.class);

            if (contact != null)
            {
                callButton.setEnabled(true);

                if(contact.getProtocolProvider().isRegistered())
                    getAccountSelectorBox().
                        setSelected(contact.getProtocolProvider());
            }
            else
            {
                callButton.setEnabled(false);
            }
        }
        else if (phoneNumberCombo.isComboFieldEmpty())
        {
            callButton.setEnabled(false);
        }
    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        this.updateCallAccountStatus(protocolProvider);
    }

}
