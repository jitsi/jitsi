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
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It
 * shows information about call participants, call duration, etc.
 *
 * @author Yana Stamcheva
 */
public class CallPanel
    extends JScrollPane
    implements  CallChangeListener,
                CallParticipantListener
{
    private JPanel mainPanel = new JPanel();

    private Hashtable participantsPanels = new Hashtable();

    private CallManager callManager;

    private String title;

    private Call call;
    
    private String callType;
    
    public CallPanel(CallManager callManager, Call call, String callType)
    {
        this.callManager = callManager;
        
        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 10, 20, 10));

        int contactsCount = call.getCallParticipantsCount();

        if(contactsCount > 0) {
            CallParticipant participant
                = (CallParticipant) call.getCallParticipants().next();
                        
            this.title = participant.getDisplayName();

            if(contactsCount < 2) {
                this.mainPanel.setLayout(new BorderLayout());
            }
            else {
                int rows = contactsCount/2 + contactsCount%2;
                this.mainPanel.setLayout(new GridLayout(rows, 2));
            }
        }

        this.setCall(call, callType);
        
        this.getViewport().add(mainPanel);
    }
    
    /**
     * Creates an instance of CallPanel for the given call and call type.
     * @param callManager the CallManager that manages this panel
     * @param contacts the list of contacts for this call panel
     */
    public CallPanel(CallManager callManager, Vector contacts)
    {
        this.callManager = callManager;
        
        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 10, 20, 10));

        int contactsCount = contacts.size();

        if(contactsCount > 0) {
            String firstContact = (String) contacts.get(0);
            
            this.title = firstContact;

            if(contactsCount < 2) {
                this.mainPanel.setLayout(new BorderLayout());
            }
            else {
                int rows = contactsCount/2 + contactsCount%2;
                this.mainPanel.setLayout(new GridLayout(rows, 2));
            }
        }

        Iterator i = contacts.iterator();

        while(i.hasNext()) {
            String contact = (String)i.next();
        
            this.addCallParticipant(contact, callType);
        }
        
        this.getViewport().add(mainPanel);
    }

    /**
     * Creates an instance of CallPanel for the given call and call type.
     * @param callManager the CallManager that manages this call panel
     * @param contactString the contact string that we are calling
     */
    public CallPanel(CallManager callManager, String contactString)
    {
        this.callManager = callManager;
        
        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 10, 20, 10));
            
        this.title = contactString;
            
        this.mainPanel.setLayout(new BorderLayout());
        
        this.addCallParticipant(contactString, callType);
        
        this.getViewport().add(mainPanel);
    }

    
    /**
     * Cteates and adds a panel for a call participant.
     *
     * @param participantName the call participant name
     * @param callType the type of call: INCOMING or OUTGOING
     */
    private CallParticipantPanel addCallParticipant(
            String participantName, String callType)
    {
        CallParticipantPanel participantPanel
            = new CallParticipantPanel(participantName);

        this.mainPanel.add(participantPanel);
        
        participantPanel.setCallType(callType);
        
        return participantPanel;
    }
    
    /**
     * Cteates and adds a panel for a call participant.
     *
     * @param participant the call participant
     */
    private CallParticipantPanel addCallParticipant(
            CallParticipant participant, String callType)
    {
        CallParticipantPanel participantPanel
            = new CallParticipantPanel(participant);

        this.mainPanel.add(participantPanel);
        
        participantPanel.setCallType(callType);
        
        return participantPanel;
    }

    /**
     * Returns the title of this call panel. The title is now the name of the
     * first participant in the list of the call participants. Should be
     * improved in the future.
     *
     * @return the title of this call panel
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Implements the CallChangeListener.callParticipantAdded method.
     * When a new participant is added to our call add it to the call panel.
     */
    public void callParticipantAdded(CallParticipantEvent evt)
    {
        if(evt.getSourceCall() == call) {
            this.addCallParticipant(
                    evt.getSourceCallParticipant().getDisplayName(), null);
            
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the CallChangeListener.callParticipantRemoved method.
     * When a call participant is removed from our call remove it from the
     * call panel.
     */
    public void callParticipantRemoved(CallParticipantEvent evt)
    {
        if(evt.getSourceCall() == call) {
            CallParticipant participant = evt.getSourceCallParticipant();

            CallParticipantPanel participantPanel
                = (CallParticipantPanel)participantsPanels.get(participant);

            if(participantPanel != null) {

                CallParticipantState state = participant.getState();

                participantPanel.setState(state.getStateString());

                participantPanel.stopCallTimer();

                //Create a call record and add it to the call list.
                GuiCallParticipantRecord participantRecord
                    = new GuiCallParticipantRecord(
                            participantPanel.getParticipantName(),
                            participantPanel.getCallType(),
                            participantPanel.getStartTime(),
                            participantPanel.getTime());

                callManager.getMainFrame().getCallListManager().addCallRecord(
                        0, participantRecord);

                //remove the participant panel for this participant
                this.participantsPanels.remove(participant);

                if(participantsPanels.size() != 0) {
                    Timer timer = new Timer(5000,
                        new RemoveParticipantPanelListener(participantPanel));

                    timer.setRepeats(false);
                    timer.start();
                }

                this.revalidate();
                this.repaint();
            }
        }
    }

    public void callStateChanged(CallChangeEvent evt)
    {
    }

    /**
     * Implements the CallParicipantChangeListener.participantStateChanged
     * method.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        CallParticipant sourceParticipant = evt.getSourceCallParticipant();

        if(sourceParticipant.getCall() != call)
            return;

        CallParticipantPanel participantPanel
            = (CallParticipantPanel)participantsPanels.get(sourceParticipant);

        participantPanel.setState(
                sourceParticipant.getState().getStateString());

        if(evt.getNewValue() == CallParticipantState.ALERTING_REMOTE_SIDE) {
            SoundLoader.playInLoop(
                    Constants.getDefaultOutgoingCallAudio(), 3000);
        }
        else if(evt.getNewValue() == CallParticipantState.BUSY) {
            SoundLoader.stop(Constants.getDefaultOutgoingCallAudio());
            SoundLoader.getSound(SoundLoader.BUSY).loop();
        }
        else if(evt.getNewValue() == CallParticipantState.CONNECTED) {
            //start the timer that takes care of refreshing the time label
            SoundLoader.stop(Constants.getDefaultOutgoingCallAudio());            
            participantPanel.startCallTimer();
        }
        else if(evt.getNewValue() == CallParticipantState.CONNECTING) {            
        }
        else if(evt.getNewValue() == CallParticipantState.DISCONNECTED) {            
            //The call participant should be already removed from the call
            //see callParticipantRemoved
        }
        else if(evt.getNewValue() == CallParticipantState.FAILED) {            
            //The call participant should be already removed from the call
            //see callParticipantRemoved
        }
        else if(evt.getNewValue() == CallParticipantState.INCOMING_CALL) {            
        }
        else if(evt.getNewValue() == CallParticipantState.INITIATING_CALL) {            
        }
        else if(evt.getNewValue() == CallParticipantState.ON_HOLD) {            
        }
        else if(evt.getNewValue() == CallParticipantState.UNKNOWN) {            
        }
    }

    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {
    }

    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {
    }

    public void participantImageChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Returns the call for this call panel.
     * @return the call for this call panel
     */
    public Call getCall()
    {
        return call;
    }

    public void setCall(Call call, String callType)
    {
        this.call = call;
        this.callType = callType;
        
        this.call.addCallChangeListener(this);
        
        this.mainPanel.removeAll();
        
        Iterator participants = call.getCallParticipants();
        
        while(participants.hasNext()) {
            
            CallParticipant participant = (CallParticipant) participants.next();
            
            if(participantsPanels.contains(participant))
                return;
            
            participant.addCallParticipantListener(this);
            
            CallParticipantPanel participantPanel = this.addCallParticipant(
                    participant, callType);
            
            this.participantsPanels.put(participant, participantPanel);
        }
    }
    
    /**
     * Indicates that a change has occurred in the transport address that we
     * use to communicate with the participant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new transport address.
     */
    public void participantTransportAddressChanged(CallParticipantChangeEvent
        evt)
    {
        /** @todo implement participantTransportAddressChanged() */
    }

    /**
     * Removes the given CallParticipant panel from this CallPanel.
     */
    private class RemoveParticipantPanelListener implements ActionListener
    {
        private JPanel participantPanel;
        public RemoveParticipantPanelListener(JPanel participantPanel)
        {
            this.participantPanel = participantPanel;
        }

        public void actionPerformed(ActionEvent e)
        {
            mainPanel.remove(participantPanel);

        }        
    }

    public Iterator getParticipantsPanels()
    {
        return participantsPanels.values().iterator();
    }
}
