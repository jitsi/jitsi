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

    /**
     * Creates an instance of CallPanel for the given call and call type.
     * @param call the call
     */
    public CallPanel(CallManager callManager, Call call)
    {
        this.call = call;

        this.callManager = callManager;
        this.call.addCallChangeListener(this);

        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 10, 20, 10));

        int participantsCount = call.getCallParticipantsCount();

        if(participantsCount > 0) {
            CallParticipant participant
                = ((CallParticipant)call.getCallParticipants().next());

            if(participant.getDisplayName() != null)
                this.title = participant.getDisplayName();
            else
                this.title = participant.getAddress();

            if(participantsCount < 2) {
                this.mainPanel.setLayout(new BorderLayout());
            }
            else {
                int rows = participantsCount/2 + participantsCount%2;
                this.mainPanel.setLayout(new GridLayout(rows, 2));
            }
        }

        this.getViewport().add(mainPanel);

        this.init();
    }

    /**
     * Initializes all panels which will contain participants information.
     */
    public void init()
    {
        Iterator i = call.getCallParticipants();

        while(i.hasNext()) {
            CallParticipant participant = (CallParticipant)i.next();

            this.addCallParticipant(participant);
        }
    }

    /**
     * Cteates and adds a panel for a call participant.
     *
     * @param participant the call participant
     */
    private void addCallParticipant(CallParticipant participant)
    {
        if(participantsPanels.get(participant) != null)
            return;

        CallParticipantPanel participantPanel
            = new CallParticipantPanel(participant);

        this.mainPanel.add(participantPanel);
        this.participantsPanels.put(participant, participantPanel);

        participant.addCallParticipantListener(this);
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
            this.addCallParticipant(evt.getSourceCallParticipant());
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


}
