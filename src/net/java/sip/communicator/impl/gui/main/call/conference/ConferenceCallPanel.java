/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI for conference calls. This panel contains all conference peers and
 * members.
 *
 * @author Dilshan Amadoru
 * @author Yana Stamcheva
 */
public class ConferenceCallPanel
    extends JScrollPane
    implements CallPeerConferenceListener
{
    /**
     * The conference call.
     */
    private final Call call;

    /**
     * The panel which contains ConferencePeerPanels.
     */
    private final TransparentPanel mainPanel;

    /**
     * The constraints to create the GridBagLayout.
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Maps a <tt>CallPeer</tt> to its renderer.
     */
    private final Hashtable<CallPeer, CallPeerRenderer> callPeerPanels
        = new Hashtable<CallPeer, CallPeerRenderer>();

    /**
     * The CallDialog which contains this panel.
     */
    private final CallDialog callDialog;

    /**
     * The implementation of the routine which scrolls this scroll pane to its
     * bottom.
     */
    private final Runnable scrollToBottomRunnable = new Runnable()
    {
        /**
         * Implements Runnable#run().
         * @see Runnable#run()
         */
        public void run()
        {
            JScrollBar verticalScrollBar = getVerticalScrollBar();

            if (verticalScrollBar != null)
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
    };

    /**
     * Creates an instance of <tt>ConferenceCallPanel</tt>.
     *
     * @param callDialog the dialog which contains this panel
     * @param c the conference call object
     */
    public ConferenceCallPanel(CallDialog callDialog, Call c)
    {
        this.callDialog = callDialog;
        this.call = c;

        mainPanel = new TransparentPanel();

        mainPanel.setLayout(new GridBagLayout());

        this.addLocalCallPeer();

        Iterator<? extends CallPeer> iterator = this.call.getCallPeers();
        while (iterator.hasNext())
        {
            this.addCallPeerPanel(iterator.next());
        }

        this.setBorder(BorderFactory
            .createEmptyBorder(10, 10, 10, 10));

        this.setOpaque(false);
        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getViewport().setOpaque(false);
        this.getViewport().add(mainPanel);

        mainPanel.setTransferHandler(new CallTransferHandler(call));
    }

    /**
     * Adds the local call peer panel to this conference call.
     */
    private void addLocalCallPeer()
    {
        ConferencePeerPanel localPeerPanel
            = new ConferencePeerPanel(
                    callDialog, call.getProtocolProvider());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add(localPeerPanel, constraints);

        OperationSetBasicTelephony telephonyOpSet = call.getProtocolProvider()
            .getOperationSet(OperationSetBasicTelephony.class);

        if (telephonyOpSet != null)
            telephonyOpSet.addLocalUserSoundLevelListener(localPeerPanel);
    }

    /**
     * Creates and adds a <tt>CallPeerRenderer</tt> for the given <tt>peer</tt>.
     *
     * @param peer the peer for which to create a renderer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        CallPeerRenderer peerRenderer;

        // Add all specific listeners
        if (peer.isConferenceFocus())
        {
            peerRenderer = new ConferenceFocusPanel(callDialog, peer);
            peer.addConferenceMembersSoundLevelListener(
                (ConferenceFocusPanel) peerRenderer);
            peer.addCallPeerConferenceListener(
                (ConferenceFocusPanel) peerRenderer);
        }
        else
        {
            peerRenderer = new ConferencePeerPanel(callDialog, peer);
            peer.addStreamSoundLevelListener((ConferencePeerPanel) peerRenderer);
        }

        // Map the call peer to its renderer.
        callPeerPanels.put(peer, peerRenderer);

        // Add the renderer component to this container.
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = mainPanel.getComponentCount() + 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add((Component) peerRenderer, constraints);

        // Create an adapter which would manage all common call peer listeners.
        CallPeerAdapter callPeerAdapter
            = new CallPeerAdapter(peer, peerRenderer);

        peerRenderer.setCallPeerAdapter(callPeerAdapter);

        peer.addCallPeerListener(callPeerAdapter);
        peer.addPropertyChangeListener(callPeerAdapter);
        peer.addCallPeerSecurityListener(callPeerAdapter);

        SwingUtilities.invokeLater(scrollToBottomRunnable);
    }

    /**
     * Removes the <tt>CallPeerRenderer</tt> and all related listeners
     * corresponding to the given <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt> to remove
     */
    public void removeCallPeerPanel(CallPeer peer)
    {
        CallPeerRenderer peerRenderer = callPeerPanels.get(peer);

        // Remove the renderer component.
        mainPanel.remove((Component) peerRenderer);

        // Remove all specific listeners first.
        if (peerRenderer instanceof ConferenceFocusPanel)
        {
            peer.removeConferenceMembersSoundLevelListener(
                (ConferenceFocusPanel) peerRenderer);
            peer.removeCallPeerConferenceListener(
                (ConferenceFocusPanel) peerRenderer);
        }
        else if (peerRenderer instanceof ConferencePeerPanel)
        {
            peer.removeStreamSoundLevelListener(
                (ConferencePeerPanel) peerRenderer);
        }

        // Remove all common listeners.
        CallPeerAdapter adapter = peerRenderer.getCallPeerAdapter();

        peer.removeCallPeerListener(adapter);
        peer.removePropertyChangeListener(adapter);
        peer.removeCallPeerSecurityListener(adapter);
    }

    /**
     * Updates <tt>CallPeer</tt> related components to fit the new focus state.
     * @param conferenceEvent the event that notified us of the change
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {
        CallPeer callPeer = conferenceEvent.getSourceCallPeer();

        // Removes the previously used panel for the peer.
        this.removeCallPeerPanel(callPeer);

        // Create a new panel which should fit the new state of the peer.
        this.addCallPeerPanel(callPeer);
    }

    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent) {}

    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent) {}
}
