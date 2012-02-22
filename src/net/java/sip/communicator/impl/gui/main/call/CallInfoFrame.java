/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.transparent.*;

import java.util.*;
import javax.swing.*;

/**
 * The frame displaying the statistical information for a call.
 *
 * @author Vincent Lucas
 */
public class CallInfoFrame
    extends SIPCommFrame
    implements CallTitleListener
{
    /**
     * The Call from which are computed the statisics displayed.
     */
    private Call call;

    /**
     * The list of call peer IDs displayed.
     */
    private Vector<CallPeerInfoPanel> callPeerInfoPanels;
    
    /**
     * The panel which contains all the information displayed.
     */
    private TransparentPanel callInfoPanel;

    /**
     * Creates a new frame containing the statistical information for a call.
     *
     * @param call The call from which are computed the statisics displayed.
     */
    public CallInfoFrame(Call call)
    {
        super(false);

        this.call = call;
        this.callPeerInfoPanels =
            new Vector<CallPeerInfoPanel>(call.getCallPeerCount(), 1);

        this.callInfoPanel = new TransparentPanel();
        callInfoPanel.setLayout(new BoxLayout(callInfoPanel, BoxLayout.Y_AXIS));
        this.add(callInfoPanel);

        this.addCallInfo();
        this.updateCallPeerInfo();

        this.pack();
    }

    /**
     * Adds a Panel with all information provided for a Call and its
     * CallStreams.
     */
    private void addCallInfo()
    {
        int callPeerCount = call.getCallPeerCount();
        int crossProtocolCallPeerCount = call.getCrossProtocolCallPeerCount();
        boolean callIsConferenceFocus = call.isConferenceFocus();
        boolean callIsDefaultEncrypted = call.isDefaultEncrypted();

        String callProtocolDisplayName =
            call.getProtocolProvider().getProtocolDisplayName();
        String callProtocolName = call.getProtocolProvider().getProtocolName();
        boolean callProtocolIsSignalingTransportSecure =
            call.getProtocolProvider().isSignalingTransportSecure();

        callInfoPanel.add(new JLabel("Call information: "));
        callInfoPanel.add(new JLabel("Peer count: " + callPeerCount));
        callInfoPanel.add(new JLabel("Cross Protocol peer count: " +
                crossProtocolCallPeerCount));
        callInfoPanel.add(new JLabel("Is conference focus: " +
                callIsConferenceFocus));
        callInfoPanel.add(new JLabel("Is default encrypted: " +
                callIsDefaultEncrypted));
        callInfoPanel.add(new JLabel("Protocol display name: " +
                callProtocolDisplayName));
        callInfoPanel.add(new JLabel("Protocol name: " + callProtocolName));
        callInfoPanel.add(new JLabel("Protocol is signaling transport secure: "
                + callProtocolIsSignalingTransportSecure));
    }

    /**
     * Updates, add and removes respectively current, new or old CallPeers
     * information statistics.
     */
    private void updateCallPeerInfo()
    {
        Vector<CallPeerInfoPanel> leftCallPeers =
            new Vector<CallPeerInfoPanel>(this.callPeerInfoPanels);
        Iterator<? extends CallPeer> callPeers = this.call.getCallPeers();
        while(callPeers.hasNext())
        {
            CallPeer callPeer = (CallPeer) callPeers.next();
            int index = indexOfCallPeerInfoPanel(callPeer);
            // If the CallPeer information is not yet displayed, then we had a
            // corresponding CallPeerInfoPAnel.
            if(index == -1)
            {
                CallPeerInfoPanel callPeerInfoPanel =
                    new CallPeerInfoPanel(callPeer);
                callInfoPanel.add(callPeerInfoPanel);
                callPeerInfoPanels.add(callPeerInfoPanel);
            }
            // Else, we update the CallPeerInfoPanel.
            else
            {
                this.callPeerInfoPanels.get(index).updateInfos();
                // The given CallPeer does not have left the Call, then this
                // CallPeer is deleted from the leftCallPeers list.
                leftCallPeers.remove(this.callPeerInfoPanels.get(index));
            }
        }
        // Removes CallPeerInfoPanels corresponding to CallPeers which have
        // left the Call.
        for(int i = 0; i < leftCallPeers.size(); ++i)
        {
            callInfoPanel.remove(leftCallPeers.get(i));
            this.callPeerInfoPanels.remove(leftCallPeers.get(i));
        }
        // Remove the leftCallPeers from the official CallPeer list displayed.
    }

    /**
     * Returns the index of the CallPeerInfoPanel corresponding to the given
     * CallPeer.
     *
     * @param callPeer The CallPeer corresponding to the CallPeerInfoPanel
     * searched.
     *
     * @return the index of the CallPeerInfoPanel inside the callPeerInfoPanels
     * corresponding to the given CallPeer, if the CallPeerInfoPanel exits.
     * Otherwise returns -1.
     */
    private int indexOfCallPeerInfoPanel(CallPeer callPeer)
    {
        for(int i = 0; i < this.callPeerInfoPanels.size(); ++i)
        {
            if(this.callPeerInfoPanels.get(i).getCallPeerID()
                    .equals(callPeer.getPeerID()))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Called when the title of the given CallPanel changes.
     *
     * @param callContainer the <tt>CallContainer</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callContainer)
    {
        this.updateCallPeerInfo();
        this.pack();
    }
}
