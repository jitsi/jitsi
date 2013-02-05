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
 * This class listens to mouse and keyboard event for dekstop sharing at the
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
    private CallPeer remoteCallPeer;

    /**
     * The video component displqying the remote desktop.
     */
    private Component videoComponent = null;

    /**
     * An object get mutual exclusion access to the videoComponent.
     */
    private Object videoComponentMutex = new Object();

    /**
     * The oeration set which received the granted/revoked desktop sharing
     * rights and to which, we are sending the mouse and key events.
     */
    private OperationSetDesktopSharingClient opSetDesktopSharingClient = null;

    /**
     * Creates a new listener of mouse and key event for a the
     * video diplaying the streamed remote desktop. The video component is null
     * until calling the setVideoComponent function.
     *
     * @param remoteCallPeer The remote controlled call peer to which the events
     * must be sent.
     */
    public DesktopSharingMouseAndKeyboardListener(CallPeer remoteCallPeer)
    {
        this.remoteCallPeer = remoteCallPeer;

        this.opSetDesktopSharingClient
            = remoteCallPeer.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingClient.class);
    }

    /**
     * Sets the video diplaying component for the streamed remote desktop.
     *
     * @param videoComponenet The video component displqying the remote desktop.
     */
    public void setVideoComponent(Component videoComponent)
    {
        synchronized(this.videoComponentMutex)
        {
            // If there was an old video component, and no new one, then
            // unregisters to the operation set. 
            if(this.videoComponent != null
                    && videoComponent == null)
            {
                // The remove remote control listener will also be called
                // directly by the operationst when the peer state change.
                opSetDesktopSharingClient.removeRemoteControlListener(this);
            }
            // If there was no video component, and a new one is set, then
            // registers to the operation set. 
            else if(this.videoComponent == null
                    && videoComponent != null)
            {
                opSetDesktopSharingClient.addRemoteControlListener(this);
            }

            this.videoComponent = videoComponent;
        }
    }

    /**
     * Returns the remote-controlled CallPeer.
     *
     * @return The remote-controlled CallPeer.
     */
    public CallPeer getCallPeer()
    {
        return this.remoteCallPeer;
    }

    /**
     * This method is called when remote control has been granted.
     *
     * @param event The event which grants us the control of the remote call
     * peer.
     */
    public void remoteControlGranted(RemoteControlGrantedEvent event)
    {
        synchronized(this.videoComponentMutex)
        {
            if(this.videoComponent != null)
            {
                this.videoComponent.addKeyListener(this);
                this.videoComponent.addMouseListener(this);
                this.videoComponent.addMouseMotionListener(this);
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
        synchronized(this.videoComponentMutex)
        {
            if(this.videoComponent != null)
            {
                this.videoComponent.removeKeyListener(this);
                this.videoComponent.removeMouseListener(this);
                this.videoComponent.removeMouseMotionListener(this);
            }
        }
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
                remoteCallPeer,
                e,
                videoComponent.getBounds().getSize());
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
                remoteCallPeer,
                e,
                videoComponent.getBounds().getSize());
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
                remoteCallPeer,
                e,
                videoComponent.getBounds().getSize());
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
     * Invoked when a mouse button has been pressed on a component.
     *
     * @param e The mouse event.
     */
    public void mousePressed(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                remoteCallPeer,
                e,
                videoComponent.getBounds().getSize());
    }

    /**
     * Invoked when a mouse button has been released on a component.
     *
     * @param e The mouse event.
     */
    public void mouseReleased(MouseEvent e)
    {
        opSetDesktopSharingClient.sendMouseEvent(
                remoteCallPeer,
                e,
                videoComponent.getBounds().getSize());
    }

    /**
     * Invoked when a key has been pressed.
     *
     * @param e The keyborad event.
     */
    public void keyPressed(KeyEvent e)
    {
    }

    /**
     * Invoked when a key has been released.
     *
     * @param e The keyborad event.
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * Invoked when a key has been typed.
     *
     * @param e The keyborad event.
     */
    public void keyTyped(KeyEvent e)
    {
        opSetDesktopSharingClient.sendKeyboardEvent(
                remoteCallPeer,
                e);
    }
}
