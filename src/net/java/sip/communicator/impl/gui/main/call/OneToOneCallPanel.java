/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It shows
 * information about call peers, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class OneToOneCallPanel
    extends TransparentPanel
{
    private OneToOneCallPeerPanel peerPanel;

    private final CallDialog callDialog;

    /**
     * Creates a call panel for the corresponding call, by specifying the
     * call type (incoming or outgoing) and the parent dialog.
     *
     * @param callDialog    the dialog containing this panel
     * @param call          the call corresponding to this panel
     * @param callPeer      the remote participant in the call
     */
    public OneToOneCallPanel(   CallDialog callDialog,
                                Call call,
                                CallPeer callPeer)
    {
        super(new BorderLayout());

        this.callDialog = callDialog;

        this.setBorder(BorderFactory
            .createEmptyBorder(5, 5, 5, 5));

        this.setTransferHandler(new CallTransferHandler(call));

        this.addCallPeerPanel(callPeer);

        this.setPreferredSize(new Dimension(400, 400));
    }

    /**
     * Creates and adds a panel for a call peer.
     *
     * @param peer the call peer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        if (peerPanel == null)
        {
            peerPanel = new OneToOneCallPeerPanel(callDialog, peer);

            this.add(peerPanel);

            // Create an adapter which would manage all common call peer
            // listeners.
            CallPeerAdapter callPeerAdapter
                = new CallPeerAdapter(peer, peerPanel);

            peerPanel.setCallPeerAdapter(callPeerAdapter);

            peer.addCallPeerListener(callPeerAdapter);
            peer.addPropertyChangeListener(callPeerAdapter);
            peer.addCallPeerSecurityListener(callPeerAdapter);

            // Refresh the call panel if it's already visible.
            if (isVisible())
            {
                this.revalidate();
                this.repaint();
            }
        }
    }
}
