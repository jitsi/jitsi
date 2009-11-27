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
    private final Hashtable<CallPeer, ConferencePeerPanel> callPeerPanels
        = new Hashtable<CallPeer, ConferencePeerPanel>();

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
        final ConferencePeerPanel localPeerPanel
            = new ConferencePeerPanel(
                    callDialog, call.getProtocolProvider());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add(localPeerPanel, constraints);

        call.addLocalUserSoundLevelListener(new SoundLevelListener()
        {
            public void soundLevelChanged(SoundLevelChangeEvent evt)
            {
                localPeerPanel.fireLocalUserSoundLevelChanged(evt.getLevel());
            }
        });
    }

    /**
     * Creates and adds a <tt>CallPeerRenderer</tt> for the given <tt>peer</tt>.
     *
     * @param peer the peer for which to create a renderer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        ConferencePeerPanel confPeerPanel
            = new ConferencePeerPanel(callDialog, peer);

        peer.addCallPeerConferenceListener(confPeerPanel);

        peer.addConferenceMembersSoundLevelListener(
            confPeerPanel.getConferenceMembersSoundLevelListener());
        peer.addStreamSoundLevelListener(
            confPeerPanel.getStreamSoundLevelListener());

        // Map the call peer to its renderer.
        callPeerPanels.put(peer, confPeerPanel);

        // Depending on call peer count enables or disables the 
        if (call.getCallPeerCount() > 1)
            setSingleConferenceFocusUI(false);
        else
            setSingleConferenceFocusUI(true);

        // Add the renderer component to this container.
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = mainPanel.getComponentCount() + 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add(confPeerPanel, constraints);

        // Create an adapter which would manage all common call peer listeners.
        CallPeerAdapter callPeerAdapter
            = new CallPeerAdapter(peer, confPeerPanel);

        confPeerPanel.setCallPeerAdapter(callPeerAdapter);

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
        ConferencePeerPanel confPeerPanel = callPeerPanels.get(peer);

        // Remove the corresponding renderer.
        callPeerPanels.remove(peer);

        if (call.getCallPeerCount() > 1)
            setSingleConferenceFocusUI(false);
        else
            setSingleConferenceFocusUI(true);

        // Remove the renderer component.
        mainPanel.remove(confPeerPanel);

        peer.removeCallPeerConferenceListener(confPeerPanel);
        peer.removeConferenceMembersSoundLevelListener(
            confPeerPanel.getConferenceMembersSoundLevelListener());
        peer.removeStreamSoundLevelListener(
            confPeerPanel.getStreamSoundLevelListener());

        // Remove all common listeners.
        CallPeerAdapter adapter = confPeerPanel.getCallPeerAdapter();

        peer.removeCallPeerListener(adapter);
        peer.removePropertyChangeListener(adapter);
        peer.removeCallPeerSecurityListener(adapter);
    }

    /**
     * Sets the single conference focus interface.
     * @param isSingleConferenceFocusUI indicates if the single conference
     * focus interface should be enabled or disabled
     */
    private void setSingleConferenceFocusUI(boolean isSingleConferenceFocusUI)
    {
        Enumeration<CallPeer> callPeers = callPeerPanels.keys();

        while (callPeers.hasMoreElements())
        {
            CallPeer callPeer = callPeers.nextElement();

            if (callPeer.isConferenceFocus())
            {
                callPeerPanels.get(callPeer)
                    .setSingleFocusUI(isSingleConferenceFocusUI);
            }
        }
    }
}
