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

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import java.text.*;

/**
 * The <tt>CallPanel</tt> is the panel that contains the "Call" and "Hangup"
 * buttons, as well as the field, where user could enter the phone number or
 * the contact name of the person, to which he would like to call.
 *
 * @author Yana Stamcheva
 */

public class CallManager
    extends JPanel
    implements ActionListener
{

    private JComboBox phoneNumberCombo = new JComboBox();

    private JPanel comboPanel = new JPanel(new BorderLayout());

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
            10, 0));

    private SIPCommButton callButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_BUTTON_BG), ImageLoader
            .getImage(ImageLoader.CALL_ROLLOVER_BUTTON_BG));

    private SIPCommButton hangupButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.HANGUP_BUTTON_BG), ImageLoader
            .getImage(ImageLoader.HANGUP_ROLLOVER_BUTTON_BG));

    private SIPCommButton minimizeButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_PANEL_MINIMIZE_BUTTON), ImageLoader
            .getImage(ImageLoader.CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON));

    private SIPCommButton restoreButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_PANEL_RESTORE_BUTTON), ImageLoader
            .getImage(ImageLoader.CALL_PANEL_RESTORE_ROLLOVER_BUTTON));

    private JPanel minimizeButtonPanel = new JPanel(new FlowLayout(
            FlowLayout.RIGHT));

    private MainFrame mainFrame;

    private Hashtable activeCalls = new Hashtable();

    private boolean isShown;

    /**
     * Creates an instance of <tt>CallPanel</tt>.
     * @param parentWindow The main application window.
     */
    public CallManager(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        this.buttonsPanel
                .setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        this.comboPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));

        this.init();
    }

    /**
     * Initializes and constructs this panel.
     */
    private void init()
    {
        this.phoneNumberCombo.setEditable(true);

        this.comboPanel.add(phoneNumberCombo, BorderLayout.CENTER);
        // this.add(comboPanel, BorderLayout.NORTH);

        this.callButton.setName("call");
        this.hangupButton.setName("hangup");
        this.minimizeButton.setName("minimize");
        this.restoreButton.setName("restore");

        this.callButton.addActionListener(this);
        this.hangupButton.addActionListener(this);
        this.minimizeButton.addActionListener(this);
        this.restoreButton.addActionListener(this);

        this.buttonsPanel.add(callButton);
        this.buttonsPanel.add(hangupButton);

        this.add(minimizeButtonPanel, BorderLayout.SOUTH);
    }

    /**
     * Returns the combo box, where user enters the phone number to call to.
     * @return the combo box, where user enters the phone number to call to.
     */
    public JComboBox getPhoneNumberCombo()
    {
        return phoneNumberCombo;
    }

    /**
     * Handles the <tt>ActionEvent</tt> generated when user presses one of the
     * buttons in this panel.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equalsIgnoreCase("call")) {
            OperationSetBasicTelephony telephony;

            Object o = mainFrame.getContactListPanel()
                .getContactList().getSelectedValue();

            if(o != null && o instanceof MetaContact) {
                MetaContact metaContact
                    = (MetaContact)o;

                Contact contact
                    = getTelephonyContact(metaContact);

                if(contact != null) {
                    telephony
                        = mainFrame.getTelephony(contact.getProtocolProvider());

                    Call createdCall = null;
                    try
                    {
                        createdCall = telephony.createCall(contact);
                    }
                    catch (OperationFailedException ex1)
                    {
                        /** !!!!!!!!!!!! @todo implement !!!!!!!!!!!!!!!!!! */
                    }
                    CallPanel callPanel = new CallPanel(createdCall);
                    mainFrame.addCallPanel(callPanel);

                    activeCalls.put(createdCall, callPanel);
                }
                else {
                    //Message to user which says "This contact could not be called!"
                }
            }
            else if(phoneNumberCombo.getSelectedItem() != null) {
                ProtocolProviderService pps
                    = getDefaultTelephonyProvider();

                if(pps != null) {
                    telephony = mainFrame.getTelephony(pps);
                    try
                    {
                        telephony.createCall(
                            phoneNumberCombo.getSelectedItem().toString());
                    }
                    catch (ParseException ex)
                    {
                        /** !!!!!!!!!!!! @todo implement !!!!!!!!!!!!!!!!!! */
                    }
                    catch (OperationFailedException ex)
                    {
                        /** !!!!!!!!!!!! @todo implement !!!!!!!!!!!!!!!!!! */
                    }
                }
            }
            else {
                //Message to user which says "You must select a contact to call or enter a phone number"
            }
        }
        else if (buttonName.equalsIgnoreCase("hangup")) {

        }
        else if (buttonName.equalsIgnoreCase("minimize")) {

            this.remove(comboPanel);
            this.remove(buttonsPanel);

            this.minimizeButtonPanel.removeAll();
            this.minimizeButtonPanel.add(restoreButton);
            this.isShown = false;

            this.mainFrame.getContactListPanel()
                .getContactList().requestFocus();

            this.mainFrame.validate();
        }
        else if (buttonName.equalsIgnoreCase("restore")) {

            this.add(comboPanel, BorderLayout.NORTH);
            this.add(buttonsPanel, BorderLayout.CENTER);

            this.minimizeButtonPanel.removeAll();
            this.minimizeButtonPanel.add(minimizeButton);
            this.isShown = true;

            this.mainFrame.validate();
        }
    }

    /**
     * Returns TRUE if this panel is visible, FALSE otherwise.
     * @return TRUE if this panel is visible, FALSE otherwise
     */
    public boolean isShown()
    {
        return this.isShown;
    }

    /**
     * When TRUE shows this panel, when FALSE hides it.
     * @param isShown
     */
    public void setShown(boolean isShown)
    {
        this.isShown = isShown;

        if(isShown) {
            this.add(comboPanel, BorderLayout.NORTH);
            this.add(buttonsPanel, BorderLayout.CENTER);

            this.minimizeButtonPanel.add(minimizeButton);
        }
        else {
            this.minimizeButtonPanel.add(restoreButton);
        }
    }

    /**
     * For the given MetaContact returns the protocol contact that supports
     * a basic telephony operation.
     * @param metaContact the MetaContac we are trying to call
     * @return returns the protocol contact that supports a basic telephony
     * operation
     */
    private Contact getTelephonyContact(
            MetaContact metaContact)
    {
        String telephonySet
            = OperationSetBasicTelephony.class.getName();

        Iterator i = metaContact.getContacts();
        while(i.hasNext()) {
            Contact contact = (Contact)i.next();

            if(contact.getProtocolProvider()
                .getSupportedOperationSets()
                    .containsKey(telephonySet)) {

                return contact;
            }
        }
        return null;
    }

    /**
     * From all registered protocol providers returns the first one that
     * supports basic telephony.
     * @return the first protocol provider from all the registered providers
     * that supports basic telephony
     */
    private ProtocolProviderService getDefaultTelephonyProvider() {
        String telephonySet
            = OperationSetBasicTelephony.class.getName();

        Iterator i = mainFrame.getProtocolProviders();
        while(i.hasNext()) {
            ProtocolProviderService pps
                = (ProtocolProviderService) i.next();

            if(pps.getSupportedOperationSets()
                    .containsKey(telephonySet)) {
                return pps;
            }
        }
        return null;
    }
}
