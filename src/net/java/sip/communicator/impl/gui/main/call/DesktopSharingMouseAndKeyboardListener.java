/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import java.awt.*;
import java.awt.event.*;

/**
 * This class listens to mouse and keyboard event for desktop sharing at the
 * client side, in order to send moves, clicks and key strokes to the server.
 *
 * @author Vincent Lucas
 */
public class DesktopSharingMouseAndKeyboardListener
    implements RemoteControlListener,
               KeyListener,
               MouseListener,
               MouseMotionListener
{
    /**
     * The remote controlled call peer to which the events must be sent.
     */
    private final CallPeer callPeer;

    /**
     * The operation set which received the granted/revoked desktop sharing
     * rights and to which, we are sending the mouse and key events.
     */
    private final OperationSetDesktopSharingClient opSetDesktopSharingClient;

    /**
     * The video component displaying the remote desktop.
     */
    private Component videoComponent = null;

    /**
     * An object get mutual exclusion access to the videoComponent.
     */
    private final Object videoComponentMutex = new Object();

    /**
     * Creates a new listener of mouse and key event for a the
     * video displaying the streamed remote desktop. The video component is null
     * until calling the setVideoComponent function.
     *
     * @param callPeer The remote controlled call peer to which the events
     * must be sent.
     */
    public DesktopSharingMouseAndKeyboardListener(CallPeer callPeer)
    {
        this.callPeer = callPeer;

        opSetDesktopSharingClient
            = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingClient.class);
    }

    /**
     * Returns the remote-controlled CallPeer.
     *
     * @return The remote-controlled CallPeer.
     */
    public CallPeer getCallPeer()
    {
        return callPeer;
    }

    /**
     * Invoked when a key has been pressed.
     *
     * @param e The keyboard event.
     */
    public void keyPressed(KeyEvent e)
    {
    }

    /**
     * Invoked when a key has been released.
     *
     * @param e The keyboard event.
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * Invoked when a key has been typed.
     *
     * @param e The keyboard event.
     */
    public void keyTyped(KeyEvent e)
    {
        opSetDesktopSharingClient.sendKeyboardEvent(callPeer, e);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed and released) on
     * a component.
     *
     * @param e The mouse event.
     */
    public void mouseClicked(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                callPeer,
                e,
                videoComponent.getSize());
    }

    /**
     * Invoked when a mouse button is pressed on a
     * component and then dragged.
     *
     * @param e The mouse dragged event.
     */
    public void mouseDragged(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                callPeer,
                e,
                videoComponent.getSize());
    }


    /**
     * Invoked when the mouse enters a component.
     *
     * @param e The mouse event.
     */
    public void mouseEntered(MouseEvent e)
    {
    }

    /**
     * Invoked when the mouse exits a component.
     *
     * @param e The mouse event.
     */
    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * Invoked when the mouse cursor has been moved
     * onto a component but no buttons have been pushed.
     *
     * @param e The mouse moved event.
     */
    public void mouseMoved(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                callPeer,
                e,
                videoComponent.getSize());
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     *
     * @param e The mouse event.
     */
    public void mousePressed(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                callPeer,
                e,
                videoComponent.getSize());
    }

    /**
     * Invoked when a mouse button has been released on a component.
     *
     * @param e The mouse event.
     */
    public void mouseReleased(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                callPeer,
                e,
                videoComponent.getSize());
    }

    /**
     * This method is called when remote control has been granted.
     *
     * @param event The event which grants us the control of the remote call
     * peer.
     */
    public void remoteControlGranted(RemoteControlGrantedEvent event)
    {
        synchronized(videoComponentMutex)
        {
            if(videoComponent != null)
            {
                videoComponent.addKeyListener(this);
                videoComponent.addMouseListener(this);
                videoComponent.addMouseMotionListener(this);
            }
        }
    }

    /**
     * This method is called when remote control has been revoked.
     *
     * @param event The event which revokes us the control of the remote call
     * peer.
     */
    public void remoteControlRevoked(RemoteControlRevokedEvent event)
    {
        synchronized(videoComponentMutex)
        {
            if(videoComponent != null)
            {
                videoComponent.removeKeyListener(this);
                videoComponent.removeMouseListener(this);
                videoComponent.removeMouseMotionListener(this);
            }
        }
    }

    /**
     * Sets the video displaying component for the streamed remote desktop.
     *
     * @param videoComponenet The video component displaying the remote desktop.
     */
    public void setVideoComponent(Component videoComponent)
    {
        synchronized(videoComponentMutex)
        {
            if(this.videoComponent == null)
            {
                // If there was no old video component and a new one is set,
                // registers to the operation set.
                if (videoComponent != null)
                    opSetDesktopSharingClient.addRemoteControlListener(this);
            }
            else
            {
                // If there was an old video component and no new one is set,
                // unregisters from the operation set.
                if (videoComponent == null)
                {
                    // The remove remote control listener will also be called
                    // directly by the operation when the peer state change.
                    opSetDesktopSharingClient.removeRemoteControlListener(this);
                }
            }

            this.videoComponent = videoComponent;
        }
    }
}
