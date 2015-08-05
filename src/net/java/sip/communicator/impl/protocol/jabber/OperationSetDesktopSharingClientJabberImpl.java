/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;
// disambiguation

/**
 * Implements all desktop sharing client-side related functions for Jabber
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingClientJabberImpl
    extends AbstractOperationSetDesktopSharingClient
                <ProtocolProviderServiceJabberImpl>
    implements RegistrationStateChangeListener,
                PacketListener,
                PacketFilter
{
    /**
     * Initializes a new <tt>OperationSetDesktopSharingClientJabberImpl</tt>.
     *
     * @param parentProvider the Jabber <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide desktop sharing.
     */
    public OperationSetDesktopSharingClientJabberImpl(
            ProtocolProviderServiceJabberImpl parentProvider)
    {
        super(parentProvider);
        parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Send a keyboard notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>KeyEvent</tt> received and that will be send to remote
     * peer
     */
    public void sendKeyboardEvent(CallPeer callPeer, KeyEvent event)
    {
        RemoteControlExtension payload = new RemoteControlExtension(event);
        this.sendRemoteControlExtension(callPeer, payload);
    }

    /**
     * Send a mouse notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to remote
     * peer
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event)
    {
        RemoteControlExtension payload = new RemoteControlExtension(event);
        this.sendRemoteControlExtension(callPeer, payload);
    }

    /**
     * Send a mouse notification for specific "moved" <tt>MouseEvent</tt>. As
     * controller computer could have smaller desktop that controlled ones, we
     * should take care to send the percentage of point x and point y.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to remote
     * peer
     * @param videoPanelSize size of the panel that contains video
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event,
            Dimension videoPanelSize)
    {
        RemoteControlExtension payload
            = new RemoteControlExtension(event, videoPanelSize);
        this.sendRemoteControlExtension(callPeer, payload);
    }

    /**
     * Send a mouse/keyboard/videoPanelSize notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param payload  The packet payload containing the
     * key/mouse/videoPanelSize event to send to remote peer
     */
    private void sendRemoteControlExtension(
            CallPeer callPeer,
            RemoteControlExtension payload)
    {
        DiscoverInfo discoverInfo
            = ((CallPeerJabberImpl) callPeer).getDiscoveryInfo();
        if(this.parentProvider.getDiscoveryManager()
                .includesFeature(InputEvtIQ.NAMESPACE_CLIENT)
                && discoverInfo != null
                && discoverInfo.containsFeature(InputEvtIQ.NAMESPACE_SERVER))
        {
            InputEvtIQ inputIQ = new InputEvtIQ();

            inputIQ.setAction(InputEvtAction.NOTIFY);
            inputIQ.setType(IQ.Type.SET);
            inputIQ.setFrom(parentProvider.getOurJID());
            inputIQ.setTo(callPeer.getAddress());
            inputIQ.addRemoteControl(payload);

            parentProvider.getConnection().sendPacket(inputIQ);
        }
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>InputEvtManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        OperationSetDesktopSharingServerJabberImpl.registrationStateChanged(
                    evt,
                    this,
                    this,
                    this.parentProvider.getConnection());
    }

    /**
     * Handles incoming inputevt packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        InputEvtIQ inputIQ = (InputEvtIQ)packet;

        //first ack all "set" requests.
        if(inputIQ.getType() == IQ.Type.SET
                && inputIQ.getAction() != InputEvtAction.NOTIFY)
        {
            IQ ack = IQ.createResultIQ(inputIQ);
            parentProvider.getConnection().sendPacket(ack);

            String callPeerID = inputIQ.getFrom();
            if(callPeerID != null)
            {
                CallPeer callPeer = getListenerCallPeer(callPeerID);
                if(callPeer != null)
                {
                    if(inputIQ.getAction() == InputEvtAction.START)
                    {
                        fireRemoteControlGranted(callPeer);
                    }
                    else if(inputIQ.getAction() == InputEvtAction.STOP)
                    {
                        fireRemoteControlRevoked(callPeer);
                    }
                }
            }
        }
    }

    /**
     * Tests whether or not the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery
     * and it would only accept <tt>InputEvtIQ</tt>s.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet)
    {
        //we only handle InputEvtIQ-s
        return (packet instanceof InputEvtIQ);
    }

    /**
     * Returns the callPeer corresponding to the given callPeerAddress given in
     * parameter, if this callPeer exists in the listener list.
     *
     * @param callPeerAddress The XMPP address of the call peer to seek.
     *
     * @return The callPeer corresponding to the given callPeerAddress given in
     * parameter, if this callPeer exists in the listener list. null otherwise.
     */
    protected CallPeer getListenerCallPeer(String callPeerAddress)
    {
        CallPeerJabberImpl callPeer;
        List<RemoteControlListener> listeners = getListeners();
        for(int i = 0; i < listeners.size(); ++i)
        {
            callPeer = (CallPeerJabberImpl) listeners.get(i).getCallPeer();
            if(callPeer.getAddress().equals(callPeerAddress))
            {
                return callPeer;
            }
        }
        // If no peers corresponds, then return NULL.
        return null;
    }
}
