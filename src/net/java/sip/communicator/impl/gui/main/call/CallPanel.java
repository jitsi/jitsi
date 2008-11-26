/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It shows
 * information about call participants, call duration, etc.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class CallPanel
    extends SCScrollPane
    implements CallChangeListener, CallParticipantListener
{
    private final TransparentPanel mainPanel = new TransparentPanel();

    private final Hashtable<CallParticipant, CallParticipantPanel>
        participantsPanels =
            new Hashtable<CallParticipant, CallParticipantPanel>();

    private String title;

    private Call call;

    public CallPanel(Call call, String callType)
    {
        this.mainPanel.setBorder(BorderFactory
            .createEmptyBorder(5, 5, 5, 5));

        this.mainPanel.setLayout(new BorderLayout());

        this.setViewportView(mainPanel);

        int contactsCount = call.getCallParticipantsCount();

        if (contactsCount > 0)
        {
            CallParticipant participant =
                (CallParticipant) call.getCallParticipants().next();

            this.title = participant.getDisplayName();

            if (contactsCount < 2)
            {
                this.mainPanel.setLayout(new BorderLayout());
            }
            else
            {
                int rows = contactsCount / 2 + contactsCount % 2;
                this.mainPanel.setLayout(new GridLayout(rows, 2));
            }
        }

        this.setCall(call, callType);
    }

    /**
     * Creates and adds a panel for a call participant.
     * 
     * @param participant the call participant
     * @param callType the type of call - INCOMING of OUTGOING
     */
    private CallParticipantPanel addCallParticipant(
        CallParticipant participant, String callType)
    {
        CallParticipantPanel participantPanel =
            getParticipantPanel(participant);

        if (participantPanel == null)
        {
            participantPanel = new CallParticipantPanel(participant);

            this.mainPanel.add(participantPanel);

            participantPanel.setCallType(callType);

            this.participantsPanels.put(participant, participantPanel);
        }

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
     * Implements the CallChangeListener.callParticipantAdded method. When a new
     * participant is added to our call add it to the call panel.
     */
    public void callParticipantAdded(CallParticipantEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            this.addCallParticipant(evt.getSourceCallParticipant(), null);

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the CallChangeListener.callParticipantRemoved method. When a
     * call participant is removed from our call remove it from the call panel.
     */
    public void callParticipantRemoved(CallParticipantEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            CallParticipant participant = evt.getSourceCallParticipant();

            CallParticipantPanel participantPanel =
                getParticipantPanel(participant);

            if (participantPanel != null)
            {
                CallParticipantState state = participant.getState();

                participantPanel.setState(state.getStateString());

                participantPanel.stopCallTimer();

                // Create a call record and add it to the call list.
                GuiCallParticipantRecord participantRecord =
                    new GuiCallParticipantRecord(participantPanel
                        .getParticipantName(), participantPanel.getCallType(),
                        participantPanel.getCallStartTime(), participantPanel
                            .getCallDuration());

                if (participantsPanels.size() != 0)
                {
                    Timer timer =
                        new Timer(5000, new RemoveParticipantPanelListener(
                            participant));

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

        if (sourceParticipant.getCall() != call)
            return;

        CallParticipantPanel participantPanel =
            getParticipantPanel(sourceParticipant);

        participantPanel
            .setState(sourceParticipant.getState().getStateString());

        Object newState = evt.getNewValue();

        if (newState == CallParticipantState.ALERTING_REMOTE_SIDE)
        {
            NotificationManager
                .fireNotification(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallParticipantState.BUSY)
        {
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            NotificationManager.fireNotification(NotificationManager.BUSY_CALL);
        }
        else if (newState == CallParticipantState.CONNECTED)
        {
            if (!CallParticipantState.isOnHold((CallParticipantState) evt
                .getOldValue()))
            {
                // start the timer that takes care of refreshing the time label

                NotificationManager
                    .stopSound(NotificationManager.OUTGOING_CALL);
                NotificationManager
                    .stopSound(NotificationManager.INCOMING_CALL);

                participantPanel.startCallTimer();
            }
        }
        else if (newState == CallParticipantState.CONNECTING)
        {
        }
        else if (newState == CallParticipantState.DISCONNECTED)
        {
            // The call participant should be already removed from the call
            // see callParticipantRemoved
        }
        else if (newState == CallParticipantState.FAILED)
        {
            // The call participant should be already removed from the call
            // see callParticipantRemoved
        }
        else if (newState == CallParticipantState.INCOMING_CALL)
        {
        }
        else if (newState == CallParticipantState.INITIATING_CALL)
        {
        }
        else if (CallParticipantState.isOnHold((CallParticipantState) newState))
        {
        }
        else if (newState == CallParticipantState.UNKNOWN)
        {
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
     * 
     * @return the call for this call panel
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Sets the <tt>Call</tt> corresponding to this <tt>CallPanel</tt>.
     * 
     * @param call the <tt>Call</tt> corresponding to this <tt>CallPanel</tt>
     * @param callType the call type - INCOMING or OUTGOING
     */
    private void setCall(Call call, String callType)
    {
        this.call = call;

        this.call.addCallChangeListener(this);

        // Remove all previously added participant panels, because they do not
        // correspond to real participants.
        this.mainPanel.removeAll();
        this.participantsPanels.clear();

        Iterator<CallParticipant> participants = call.getCallParticipants();

        while (participants.hasNext())
        {
            CallParticipant participant = participants.next();

            participant.addCallParticipantListener(this);

            this.addCallParticipant(participant, callType);
        }
    }

    /**
     * Indicates that a change has occurred in the transport address that we use
     * to communicate with the participant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new transport
     *            address.
     */
    public void participantTransportAddressChanged(
        CallParticipantChangeEvent evt)
    {
        /** @todo implement participantTransportAddressChanged() */
    }

    /**
     * Removes the given CallParticipant panel from this CallPanel.
     */
    private class RemoveParticipantPanelListener
        implements ActionListener
    {
        private CallParticipant participant;

        public RemoveParticipantPanelListener(CallParticipant participant)
        {
            this.participant = participant;
        }

        public void actionPerformed(ActionEvent e)
        {
            CallParticipantPanel participantPanel =
                participantsPanels.get(participant);

            mainPanel.remove(participantPanel);

            // remove the participant panel from the list of panels
            participantsPanels.remove(participant);
        }
    }

    /**
     * Returns all participant panels contained in this call panel.
     * 
     * @return an <tt>Iterator</tt> over a list of all participant panels
     *         contained in this call panel
     */
    public Iterator<CallParticipantPanel> getParticipantsPanels()
    {
        return participantsPanels.values().iterator();
    }

    /**
     * Returns the <tt>CallParticipantPanel</tt>, which correspond to the given
     * participant.
     * 
     * @param participant the <tt>CallParticipant</tt> we're looking for
     * @return the <tt>CallParticipantPanel</tt>, which correspond to the given
     *         participant
     */
    public CallParticipantPanel getParticipantPanel(CallParticipant participant)
    {
        for (Map.Entry<CallParticipant, CallParticipantPanel> participantEntry : participantsPanels
            .entrySet())
        {
            CallParticipant entryParticipant = participantEntry.getKey();

            if ((entryParticipant != null)
                && entryParticipant.equals(participant))
            {
                return participantEntry.getValue();
            }
        }
        return null;
    }
}
