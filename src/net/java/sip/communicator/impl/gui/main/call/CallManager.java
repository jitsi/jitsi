/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also the
 * "Call" and "Hangup" buttons panel. Here are handles incoming and outgoing
 * calls from and to the call operation set.
 *
 * @author Yana Stamcheva
 */

public class CallManager
    extends JPanel
    implements  ActionListener,
                CallListener,
                ListSelectionListener,
                ChangeListener
{
    private Logger logger = Logger.getLogger(CallManager.class.getName());

    private CallComboBox phoneNumberCombo;

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
    
    private boolean isCallMetaContact;

    /**
     * Creates an instance of <tt>CallManager</tt>.
     * @param mainFrame The main application window.
     */
    public CallManager(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        phoneNumberCombo = new CallComboBox(this);
        
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
        
        this.callButton.setEnabled(false);
        
        this.hangupButton.setEnabled(false);

        this.add(minimizeButtonPanel, BorderLayout.SOUTH);
    }

    
    /**
     * Handles the <tt>ActionEvent</tt> generated when user presses one of the
     * buttons in this panel.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("call")) {
            OperationSetBasicTelephony telephony;
            
            Object o = mainFrame.getContactListPanel().getContactList()
                .getSelectedValue();
            
            //call button is pressed over an already open call panel
            if(mainFrame.getSelectedCallPanel() != null 
                    && mainFrame.getSelectedCallPanel().getCallType() 
                        == CallPanel.INCOMING_CALL
                    && mainFrame.getSelectedCallPanel().getCall().getCallState()
                        == CallState.CALL_INITIALIZATION) {
            
                CallPanel callPanel = mainFrame.getSelectedCallPanel();
                
                Call call = callPanel.getCall();
                
                ProtocolProviderService pps
                    = call.getProtocolProvider();
                
                Iterator participants = call.getCallParticipants();
                
                while(participants.hasNext()) {
                    CallParticipant participant
                        = (CallParticipant)participants.next();
                    answerCall(pps, participant);
                }
            }
            //call button is pressed when a meta contact is selected
            else if(isCallMetaContact && o != null && o instanceof MetaContact) {
                
                MetaContact metaContact
                    = (MetaContact)o;

                Contact contact = getTelephonyContact(metaContact);
                
                if(contact != null) {
                    telephony
                        = mainFrame.getTelephony(contact.getProtocolProvider());
                
                    Call createdCall;
                    try {
                        createdCall = telephony.createCall(contact);
                        
                        CallPanel callPanel = new CallPanel(
                                this, createdCall, CallPanel.OUTGOING_CALL);
                        
                        mainFrame.addCallPanel(callPanel);
                        
                        activeCalls.put(createdCall, callPanel);
                    }
                    catch (OperationFailedException e) {                        
                        logger.error("The call could not be created: " + e);
                    }
                }
                else {
                    JOptionPane.showMessageDialog(this.mainFrame,
                            Messages.getString("contactNotSupportingTelephony"),
                            Messages.getString("warning"),
                            JOptionPane.WARNING_MESSAGE);
                }
            }
            //if no contact is selected checks if the user has chosen or has
            //writen something in the phone combo box
            else if(!phoneNumberCombo.isComboFieldEmpty()) {
                
                ProtocolProviderService pps
                    = getDefaultTelephonyProvider();
                
                if(pps != null) {
                    telephony = mainFrame.getTelephony(pps);

                    Call createdCall;
                    try {
                        createdCall = telephony.createCall(
                                phoneNumberCombo.getSelectedItem().toString());
                        
                        CallPanel callPanel = new CallPanel(
                                this, createdCall, CallPanel.OUTGOING_CALL);
                        
                        mainFrame.addCallPanel(callPanel);
                        
                        activeCalls.put(createdCall, callPanel);
                    }
                    catch (OperationFailedException e) {
                        logger.error("The call could not be created: " + e);
                    }
                    catch (ParseException e) {
                        logger.error("The call could not be created: " + e);
                    }
                }
            }
        }
        else if (buttonName.equalsIgnoreCase("hangup")) {
            CallPanel selectedCallPanel = this.mainFrame.getSelectedCallPanel();
            
            if(selectedCallPanel != null) {
                Call call = selectedCallPanel.getCall();
                
                ProtocolProviderService pps
                    = call.getProtocolProvider();
                
                OperationSetBasicTelephony telephony
                    = mainFrame.getTelephony(pps);
                
                Iterator participants = call.getCallParticipants();
                
                while(participants.hasNext()) {
                    try {
                        //now we hang up the first call participant in the call
                        telephony.hangupCallParticipant(
                            (CallParticipant)participants.next());
                    }
                    catch (OperationFailedException e) {
                        logger.error("Hang up was not successful: " + e);
                    }
                }
                
                activeCalls.remove(call);
                mainFrame.removeCallPanel(selectedCallPanel);
                updateButtonsStateAccordingToSelectedPanel();
            }
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
     * For the given MetaContact returns the protocol contact that supports
     * a basic telephony operation.
     * @param metaContact the MetaContac we are trying to call
     * @return returns the protocol contact that supports a basic telephony
     * operation
     */
    private Contact getTelephonyContact(
            MetaContact metaContact)
    {
        Iterator i = metaContact.getContacts();
        while(i.hasNext()) {
            Contact contact = (Contact)i.next();

            OperationSetBasicTelephony telephony
                = mainFrame.getTelephony(contact.getProtocolProvider());
            
            if(telephony != null)
                return contact;
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
    
    /**
     * Implements CallListener.incomingCallReceived. When a call is received
     * creates a call panel and adds it to the main tabbed pane and plays the
     * ring phone sound to the user.
     */
    public void incomingCallReceived(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();
                    
        CallPanel callPanel = new CallPanel(
                this, sourceCall, CallPanel.INCOMING_CALL);
        
        mainFrame.addCallPanel(callPanel);
        
        this.callButton.setEnabled(true);
        this.hangupButton.setEnabled(true);
        
        SoundLoader.playInLoop(Constants.getDefaultIncomingCallAudio(), 2000);
        
        activeCalls.put(sourceCall, callPanel);        
    }

    /**
     * Implements CallListener.callEnded. Stops sounds that are playing at the
     * moment if there're any. Removes the call panel and disables the hangup
     * button.
     */
    public void callEnded(CallEvent event)
    {
        SoundLoader.getSound(SoundLoader.BUSY).stop();
        SoundLoader.stop(Constants.getDefaultIncomingCallAudio());
        
        Call sourceCall = event.getSourceCall();
        
        if(activeCalls.get(sourceCall) != null) {
            CallPanel callPanel = (CallPanel)activeCalls.get(sourceCall);
            this.removeCallPanel(callPanel);
        }
    }

    public void outgoingCallCreated(CallEvent event)
    {}
    
    /**
     * Removes the given call panel tab.
     * @param callPanel the CallPanel to remove
     */
    public void removeCallPanel(CallPanel callPanel)
    {        
        Timer timer = new Timer(5000, new RemoveCallPanelListener(callPanel));
        
        timer.setRepeats(false);
        timer.start();
    }
    
    
    /**
     * Removes the given CallPanel from the main tabbed pane.
     */
    private class RemoveCallPanelListener implements ActionListener
    {
        private CallPanel callPanel;
        public RemoveCallPanelListener(CallPanel callPanel)
        {
            this.callPanel = callPanel;
        }
        
        public void actionPerformed(ActionEvent e)
        {
            activeCalls.remove(callPanel.getCall());
            mainFrame.removeCallPanel(callPanel);
            updateButtonsStateAccordingToSelectedPanel();
        }        
    }
 
    /**
     * Implements ListSelectionListener.valueChanged. Enables or disables
     * call and hangup buttons depending on the selection in the contactlist.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        Object o = mainFrame.getContactListPanel()
            .getContactList().getSelectedValue();
        
        if((e.getFirstIndex() != -1 || e.getLastIndex() != -1)
                && (o instanceof MetaContact)) {
            setCallMetaContact(true);
            callButton.setEnabled(true);
        }
        else if(phoneNumberCombo.isComboFieldEmpty()) {
                callButton.setEnabled(false);
        }
    }    

    /**
     * Implements ChangeListener.stateChanged. Enables the hangup button if
     * ones selects a tab in the main tabbed pane that contains a call panel.
     */
    public void stateChanged(ChangeEvent e)
    {
        this.updateButtonsStateAccordingToSelectedPanel();
    }
    
    private void updateButtonsStateAccordingToSelectedPanel()
    {
        if(mainFrame.getSelectedCallPanel() != null) {            
            this.hangupButton.setEnabled(true);
        }
        else {
            this.hangupButton.setEnabled(false);
        }
    }
    
    /**
     * Returns the call button.
     * @return the call button
     */
    public SIPCommButton getCallButton()
    {
        return callButton;
    }

    /**
     * Returns the hangup button.
     * @return the hangup button
     */
    public SIPCommButton getHangupButton()
    {
        return hangupButton;
    }

    /**
     * Returns the main application frame. Meant to be used from the contained
     * components that do not have direct access to the MainFrame.
     * @return the main application frame
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
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
     * Answers the given callParticipant.
     * @param pps the protocol provider to use
     * @param callParticipant the call participant to answer to
     */
    public void answerCall(ProtocolProviderService pps,
            CallParticipant callParticipant)
    {
        OperationSetBasicTelephony telephony = mainFrame.getTelephony(pps);

        try {
            telephony.answerCallParticipant(callParticipant);
        }
        catch (OperationFailedException e) {
            logger.error("Could not answer to : " + callParticipant
                    + " caused by the following exception: " + e);
        }
    }

    public boolean isCallMetaContact()
    {
        return isCallMetaContact;
    }

    public void setCallMetaContact(boolean isCallMetaContact)
    {   
        this.isCallMetaContact = isCallMetaContact;
    }

}
