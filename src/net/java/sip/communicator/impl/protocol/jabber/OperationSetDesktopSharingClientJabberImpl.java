/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List; // disambiguation

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements all desktop sharing client-side related functions for Jabber
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingClientJabberImpl
    implements OperationSetDesktopSharingClient
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetDesktopSharingClientJabberImpl.class);

    /**
     * The Jabber <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    private final ProtocolProviderServiceJabberImpl parentProvider;

    /**
     * List of listeners to be notified when a change occurred in remote control
     * access.
     */
    private List<RemoteControlListener> listeners =
        new ArrayList<RemoteControlListener>();

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
        this.parentProvider = parentProvider;
    }

    /**
     * Fire a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlGranted()
    {
        RemoteControlGrantedEvent event = new RemoteControlGrantedEvent(this);

        for (RemoteControlListener l : listeners)
        {
            l.remoteControlGranted(event);
        }
    }

    /**
     * Fire a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlRevoked()
    {
        RemoteControlRevokedEvent event = new RemoteControlRevokedEvent(this);

        for (RemoteControlListener l : listeners)
        {
            l.remoteControlRevoked(event);
        }
    }

    /**
     * Add a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to add
     */
    public void addRemoteControlListener(RemoteControlListener listener)
    {
        if (logger.isInfoEnabled())
            logger.info("Enable remote control");

        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /**
     * Remove a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept/revoke to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to remove
     */
    public void removeRemoteControlListener(RemoteControlListener listener)
    {
        if (logger.isInfoEnabled())
            logger.info("Disable remote control");

        if (listeners.contains(listener))
        {
            listeners.remove(listener);
        }
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