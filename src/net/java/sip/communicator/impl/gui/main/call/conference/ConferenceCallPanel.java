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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI for a conference
 *
 * @author Dilshan Amadoru
 */
public class ConferenceCallPanel
    extends JScrollPane
{
    /**
     * The conference call
     */
    private final Call call;

    /**
     * The panel which contains ConferencePeerPanels
     */
    private final TransparentPanel mainPanel;

    /**
     * The constraints to create the GridBagLayout
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * The CallDialog which contains this panel
     */
    private final CallDialog callDialog;

    /**
     * Constructor
     *
     * @param callDialog The dialog which contains this panel
     * @param c The conference call object
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
    }

    /**
     * Adds the local call peer panel to this conference call.
     */
    private void addLocalCallPeer()
    {
        ConferenceCallPeerPanel localPeerPanel
            = new ConferenceCallPeerPanel(
                    callDialog, call.getProtocolProvider());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add(localPeerPanel, constraints);
    }

    /**
     * Add a ConferencePeerPanel for a given Peer
     *
     * @param peer New Peer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        ConferenceCallPeerPanel remotePeerPanel =
                new ConferenceCallPeerPanel(callDialog, peer);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = mainPanel.getComponentCount() + 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 10);

        mainPanel.add(remotePeerPanel, constraints);

        for (ConferenceMember member : peer.getConferenceMembers())
        {
            remotePeerPanel.addConferenceMemberPanel(member);
        }

        this.revalidate();
        this.repaint();

        peer.addCallPeerListener(remotePeerPanel);
        peer.addPropertyChangeListener(remotePeerPanel);
        peer.addCallPeerSecurityListener(remotePeerPanel);
    }

    /**
     * Removes the <tt>ConferenceCallPeerPanel</tt> corresponding to the given
     * <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt>, which panel to remove
     */
    public void removeCallPeerPanel(CallPeer peer)
    {
        ConferenceCallPeerPanel callPeerPanel = null;

        for (int i=0; i<mainPanel.getComponentCount(); i++)
        {
            Component c = mainPanel.getComponent(i);

            if (c instanceof ConferenceCallPeerPanel
                && ((ConferenceCallPeerPanel) c)
                .getPeerId().equals(peer.getPeerID()))
            {
                callPeerPanel = (ConferenceCallPeerPanel) c;

                mainPanel.remove(callPeerPanel);

                peer.removeCallPeerListener(callPeerPanel);
                peer.removePropertyChangeListener(callPeerPanel);
                peer.removeCallPeerSecurityListener(callPeerPanel);

                break;
            }
        }

        if (callPeerPanel != null)
        {
            for (ConferenceMember member : peer.getConferenceMembers())
            {
                callPeerPanel.addConferenceMemberPanel(member);
            }
        }

        this.revalidate();
        this.repaint();
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
}
