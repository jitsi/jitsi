/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.awt.*;
import java.awt.event.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements all desktop sharing client-side related functions for Jabber
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingClientJabberImpl
    extends AbstractOperationSetDesktopSharingClient
                <ProtocolProviderServiceJabberImpl>
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
        InputEvtIQ inputIQ = new InputEvtIQ();

        inputIQ.setAction(InputEvtAction.NOTIFY);
        inputIQ.setType(IQ.Type.SET);
        inputIQ.setFrom(parentProvider.getOurJID());
        inputIQ.setTo(callPeer.getAddress());
        inputIQ.addRemoteControl(payload);

        parentProvider.getConnection().sendPacket(inputIQ);
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
        InputEvtIQ inputIQ = new InputEvtIQ();

        inputIQ.setAction(InputEvtAction.NOTIFY);
        inputIQ.setType(IQ.Type.SET);
        inputIQ.setFrom(parentProvider.getOurJID());
        inputIQ.setTo(callPeer.getAddress());
        inputIQ.addRemoteControl(payload);
        parentProvider.getConnection().sendPacket(inputIQ);
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
        RemoteControlExtension payload = new RemoteControlExtension(event,
                videoPanelSize);
        InputEvtIQ inputIQ = new InputEvtIQ();

        inputIQ.setAction(InputEvtAction.NOTIFY);
        inputIQ.setType(IQ.Type.SET);
        inputIQ.setFrom(parentProvider.getOurJID());
        inputIQ.setTo(callPeer.getAddress());
        inputIQ.addRemoteControl(payload);
        parentProvider.getConnection().sendPacket(inputIQ);
    }
}
