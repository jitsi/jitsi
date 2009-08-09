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
 * information about call peers, call duration, etc.
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

    private final Hashtable<CallPeer, CallPeerPanel>
        peersPanels = new Hashtable<CallPeer, CallPeerPanel>();

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
            CallPeer peer = call.getCallPeers().next();

            this.title = peer.getDisplayName();
        }

        this.setCall(call, callType);
    }

    /**
     * Creates and adds a panel for a call peer.
     *
     * @param peer the call peer
     * @param callType the type of call - INCOMING of OUTGOING
     */
    private CallPeerPanel addCallPeer(
        CallPeer peer, String callType)
    {
        CallPeerPanel peerPanel = getPeerPanel(peer);

        if (peerPanel == null)
        {
            peerPanel
                = new CallPeerPanel(callDialog, peer);

            this.mainPanel.add(peerPanel);

            peerPanel.setCallType(callType);

            this.peersPanels.put(peer, peerPanel);
        }

        if (peersPanels.size() > 1)
        {
            SCScrollPane scrollPane = new SCScrollPane();
            scrollPane.setViewportView(mainPanel);
            this.add(scrollPane);
        }
        else
        {
            this.add(mainPanel);
        }

        return peerPanel;
    }

    /**
     * Returns the title of this call panel. The title is now the name of the
     * first peer in the list of the call peers. Should be
     * improved in the future.
     *
     * @return the title of this call panel
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Implements the CallChangeListener.callPeerAdded method. When a new
     * peer is added to our call add it to the call panel.
     */
    public void callPeerAdded(CallPeerEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            this.addCallPeer(evt.getSourceCallPeer(), null);

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the CallChangeListener.callPeerRemoved method. When a
     * call peer is removed from our call remove it from the call panel.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        if (evt.getSourceCall() == call)
        {
            CallPeer peer = evt.getSourceCallPeer();

            CallPeerPanel peerPanel =
                getPeerPanel(peer);

            if (peerPanel != null)
            {
                CallPeerState state = peer.getState();

                peerPanel.setState(state.getStateString(), null);

                peerPanel.stopCallTimer();

                if (peersPanels.size() != 0)
                {
                    Timer timer =
                        new Timer(5000, new RemovePeerPanelListener(
                            peer));

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
     * Implements the CallParicipantChangeListener.peerStateChanged
     * method.
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (sourcePeer.getCall() != call)
            return;

        CallPeerPanel peerPanel =
            getPeerPanel(sourcePeer);

        Object newState = evt.getNewValue();

        String newStateString = sourcePeer.getState().getStateString();
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

                peerPanel.startCallTimer();
            }
        }
        else if (newState == CallPeerState.DISCONNECTED)
        {
            // The call peer should be already removed from the call
            // see callPeerRemoved
        }
        else if (newState == CallPeerState.FAILED)
        {
            // The call peer should be already removed from the call
            // see callPeerRemoved
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

        peerPanel.setState(newStateString, newStateIcon);
    }

    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
    }

    public void peerAddressChanged(CallPeerChangeEvent evt)
    {
    }

    public void peerImageChanged(CallPeerChangeEvent evt)
    {
    }

    public void securityOn(CallPeerSecurityOnEvent securityEvent)
    {
        CallPeer peer = (CallPeer) securityEvent.getSource();
        CallPeerPanel peerPanel = getPeerPanel(peer);

        peerPanel.setSecured(true);

        peerPanel.setEncryptionCipher(securityEvent.getCipher());

        switch (securityEvent.getSessionType()) {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            peerPanel.setAudioSecurityOn(true);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            peerPanel.setVideoSecurityOn(true);
            break;
        }

        peerPanel.createSecurityPanel(securityEvent);

        NotificationManager.fireNotification(
            NotificationManager.CALL_SECURITY_ON);
    }

    public void securityOff(CallPeerSecurityOffEvent securityEvent)
    {
        CallPeer peer = (CallPeer) securityEvent.getSource();
        CallPeerPanel peerPanel = getPeerPanel(peer);

        peerPanel.setSecured(false);

        switch (securityEvent.getSessionType())
        {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            peerPanel.setAudioSecurityOn(false);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            peerPanel.setVideoSecurityOn(false);
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

        // Remove all previously added peer panels, because they do not
        // correspond to real peers.
        this.mainPanel.removeAll();
        this.peersPanels.clear();

        Iterator<CallPeer> peers = call.getCallPeers();

        while (peers.hasNext())
        {
            CallPeer peer = peers.next();

            peer.addCallPeerListener(this);
            peer.addCallPeerSecurityListener(this);
            peer.addPropertyChangeListener(this);

            this.addCallPeer(peer, callType);
        }
    }

    /**
     * Indicates that a change has occurred in the transport address that we use
     * to communicate with the peer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new transport
     *            address.
     */
    public void peerTransportAddressChanged(
        CallPeerChangeEvent evt)
    {
        /** @todo implement peerTransportAddressChanged() */
    }

    /**
     * Removes the given CallPeer panel from this CallPanel.
     */
    private class RemovePeerPanelListener
        implements ActionListener
    {
        private CallPeer peer;

        public RemovePeerPanelListener(CallPeer peer)
        {
            this.peer = peer;
        }

        public void actionPerformed(ActionEvent e)
        {
            CallPeerPanel peerPanel =
                peersPanels.get(peer);

            mainPanel.remove(peerPanel);

            // remove the peer panel from the list of panels
            peersPanels.remove(peer);
        }
    }

    /**
     * Returns all peer panels contained in this call panel.
     *
     * @return an <tt>Iterator</tt> over a list of all peer panels
     *         contained in this call panel
     */
    public Iterator<CallPeerPanel> getPeerPanels()
    {
        return peersPanels.values().iterator();
    }

    /**
     * Returns the number of peers for this call.
     *
     * @return the number of peers for this call.
     */
    public int getPeerCount()
    {
        return peersPanels.size();
    }

    /**
     * Returns the <tt>CallPeerPanel</tt>, which correspond to the given
     * peer.
     *
     * @param peer the <tt>CallPeer</tt> we're looking for
     * @return the <tt>CallPeerPanel</tt>, which correspond to the given
     *         peer
     */
    public CallPeerPanel getPeerPanel(CallPeer peer)
    {
        for (Map.Entry<CallPeer, CallPeerPanel> peerEntry :
                peersPanels.entrySet())
        {
            CallPeer entryPeer = peerEntry.getKey();

            if ((entryPeer != null)
                && entryPeer.equals(peer))
            {
                return peerEntry.getValue();
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

            CallPeer sourcePeer
                = (CallPeer) evt.getSource();

            if (sourcePeer.getCall() != call)
                return;

            CallPeerPanel peerPanel = getPeerPanel(sourcePeer);

            if (isMute)
            {
                // If we have clicked the mute button in a full screen mode
                // we need to update the state of the call dialog mute button.
                if (!callDialog.isMuteButtonSelected())
                {
                    callDialog.setMuteButtonSelected(true);
                }
            }

            peerPanel.setMute(isMute);
        }
    }
}
