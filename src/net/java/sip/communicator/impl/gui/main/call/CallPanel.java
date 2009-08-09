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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It shows
 * information about call participants, call duration, etc.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class CallPanel
    extends TransparentPanel
    implements  CallChangeListener,
                CallPeerListener,
                PropertyChangeListener,
                CallPeerSecurityListener
{
    private final TransparentPanel mainPanel = new TransparentPanel();

    private final Hashtable<CallPeer, CallParticipantPanel>
        participantsPanels =
            new Hashtable<CallPeer, CallParticipantPanel>();

    private String title;

    private Call call;
    
    private final CallDialog callDialog;

    /**
     * Creates a call panel for the corresponding call, by specifying the 
     * call type (incoming or outgoing) and the parent dialog.
     * 
     * @param callDialog    the dialog containing this panel
     * @param call          the call corresponding to this panel
     * @param callType      the type of the call
     */
    public CallPanel(CallDialog callDialog, Call call, String callType)
    {
        super(new BorderLayout());

        this.callDialog = callDialog;

        this.mainPanel.setBorder(BorderFactory
            .createEmptyBorder(5, 5, 5, 5));

        int contactsCount = call.getCallPeerCount();

        mainPanel.setLayout(new GridLayout(0, (contactsCount < 2) ? 1 : 2));

        if (contactsCount > 0)
        {
            CallPeer participant =
                call.getCallPeers().next();

            this.title = participant.getDisplayName();
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
        CallPeer participant, String callType)
    {
        CallParticipantPanel participantPanel =
            getParticipantPanel(participant);

        if (participantPanel == null)
        {
            participantPanel
                = new CallParticipantPanel(callDialog, participant);

            this.mainPanel.add(participantPanel);

            participantPanel.setCallType(callType);

            this.participantsPanels.put(participant, participantPanel);
        }

        if (participantsPanels.size() > 1)
        {
            SCScrollPane scrollPane = new SCScrollPane();
            scrollPane.setViewportView(mainPanel);
            this.add(scrollPane);
        }
        else
        {
            this.add(mainPanel);
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
    public void callPeerAdded(CallPeerEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            this.addCallParticipant(evt.getSourceCallPeer(), null);

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the CallChangeListener.callParticipantRemoved method. When a
     * call participant is removed from our call remove it from the call panel.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            CallPeer participant = evt.getSourceCallPeer();

            CallParticipantPanel participantPanel =
                getParticipantPanel(participant);

            if (participantPanel != null)
            {
                CallPeerState state = participant.getState();

                participantPanel.setState(state.getStateString(), null);

                participantPanel.stopCallTimer();

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
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourceParticipant = evt.getSourceCallPeer();

        if (sourceParticipant.getCall() != call)
            return;

        CallParticipantPanel participantPanel =
            getParticipantPanel(sourceParticipant);

        Object newState = evt.getNewValue();

        String newStateString = sourceParticipant.getState().getStateString();
        Icon newStateIcon = null;

        if (newState == CallPeerState.ALERTING_REMOTE_SIDE)
        {
            NotificationManager
                .fireNotification(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallPeerState.BUSY)
        {
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            NotificationManager.fireNotification(NotificationManager.BUSY_CALL);
        }
        else if (newState == CallPeerState.CONNECTED)
        {
            if (!CallPeerState.isOnHold((CallPeerState) evt
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
        else if (newState == CallPeerState.DISCONNECTED)
        {
            // The call participant should be already removed from the call
            // see callParticipantRemoved
        }
        else if (newState == CallPeerState.FAILED)
        {
            // The call participant should be already removed from the call
            // see callParticipantRemoved
        }
        else if (CallPeerState.isOnHold((CallPeerState) newState))
        {
            newStateIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON));

            // If we have clicked the hold button in a full screen mode
            // we need to update the state of the call dialog hold button.
            if ((newState.equals(CallPeerState.ON_HOLD_LOCALLY)
                || newState.equals(CallPeerState.ON_HOLD_MUTUALLY))
                && !callDialog.isHoldButtonSelected())
            {
                callDialog.setHoldButtonSelected(true);
            }
        }

        participantPanel.setState(newStateString, newStateIcon);
    }

    public void participantDisplayNameChanged(CallPeerChangeEvent evt)
    {
    }

    public void participantAddressChanged(CallPeerChangeEvent evt)
    {
    }

    public void participantImageChanged(CallPeerChangeEvent evt)
    {
    }

    public void securityOn(CallPeerSecurityOnEvent securityEvent)
    {
        CallPeer participant =
            (CallPeer) securityEvent.getSource();
        CallParticipantPanel participantPanel =
            getParticipantPanel(participant);

        participantPanel.setSecured(true);

        participantPanel.setEncryptionCipher(securityEvent.getCipher());

        switch (securityEvent.getSessionType()) {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            participantPanel.setAudioSecurityOn(true);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            participantPanel.setVideoSecurityOn(true);
            break;
        }

        participantPanel.createSecurityPanel(securityEvent);

        NotificationManager.fireNotification(
            NotificationManager.CALL_SECURITY_ON);
    }

    public void securityOff(CallPeerSecurityOffEvent securityEvent)
    {
        CallPeer participant =
            (CallPeer) securityEvent.getSource();
        CallParticipantPanel participantPanel =
            getParticipantPanel(participant);

        participantPanel.setSecured(false);

        switch (securityEvent.getSessionType())
        {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            participantPanel.setAudioSecurityOn(false);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            participantPanel.setVideoSecurityOn(false);
            break;
        }
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

        Iterator<CallPeer> participants = call.getCallPeers();

        while (participants.hasNext())
        {
            CallPeer participant = participants.next();

            participant.addCallPeerListener(this);
            participant.addCallParticipantSecurityListener(this);
            participant.addPropertyChangeListener(this);

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
        CallPeerChangeEvent evt)
    {
        /** @todo implement participantTransportAddressChanged() */
    }

    /**
     * Removes the given CallParticipant panel from this CallPanel.
     */
    private class RemoveParticipantPanelListener
        implements ActionListener
    {
        private CallPeer participant;

        public RemoveParticipantPanelListener(CallPeer participant)
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
    public Iterator<CallParticipantPanel> getParticipantPanels()
    {
        return participantsPanels.values().iterator();
    }

    /**
     * Returns the number of participants for this call.
     * 
     * @return the number of participants for this call.
     */
    public int getParticipantCount()
    {
        return participantsPanels.size();
    }

    /**
     * Returns the <tt>CallParticipantPanel</tt>, which correspond to the given
     * participant.
     * 
     * @param participant the <tt>CallParticipant</tt> we're looking for
     * @return the <tt>CallParticipantPanel</tt>, which correspond to the given
     *         participant
     */
    public CallParticipantPanel getParticipantPanel(CallPeer participant)
    {
        for (Map.Entry<CallPeer, CallParticipantPanel> participantEntry :
                participantsPanels.entrySet())
        {
            CallPeer entryParticipant = participantEntry.getKey();

            if ((entryParticipant != null)
                && entryParticipant.equals(participant))
            {
                return participantEntry.getValue();
            }
        }
        return null;
    }

    public void securityMessageRecieved(
        CallPeerSecurityMessageEvent event)
    {
        int severity = event.getEventSeverity();

        String messageTitle = null;

        switch (severity)
        {
            // Don't play alert sound for Info or warning.
            case CallPeerSecurityMessageEvent.INFORMATION:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_INFO");
                break;
            }
            case CallPeerSecurityMessageEvent.WARNING:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_WARNING");
                break;
            }
            // Alert sound indicates: security cannot established
            case CallPeerSecurityMessageEvent.SEVERE:
            case CallPeerSecurityMessageEvent.ERROR:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_ERROR");
                NotificationManager.fireNotification(
                    NotificationManager.CALL_SECURITY_ERROR);
            }
        }

        NotificationManager.fireNotification(
            NotificationManager.SECURITY_MESSAGE,
            messageTitle,
            event.getI18nMessage());
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(CallPeer.MUTE_PROPERTY_NAME))
        {
            boolean isMute = (Boolean) evt.getNewValue();

            CallPeer sourceParticipant
                = (CallPeer) evt.getSource();

            if (sourceParticipant.getCall() != call)
                return;

            CallParticipantPanel participantPanel =
                getParticipantPanel(sourceParticipant);

            if (isMute)
            {
                // If we have clicked the mute button in a full screen mode
                // we need to update the state of the call dialog mute button.
                if (!callDialog.isMuteButtonSelected())
                {
                    callDialog.setMuteButtonSelected(true);
                }
            }

            participantPanel.setMute(isMute);
        }
    }
}
