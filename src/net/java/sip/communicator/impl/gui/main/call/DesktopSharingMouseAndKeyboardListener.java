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
    private final OperationSetDesktopSharingClient desktopSharingClient;

    /**
     * The video component displaying the remote desktop.
     */
    private Component videoComponent = null;

    /**
     * An object get mutual exclusion access to the videoComponent.
     */
    private final Object videoComponentMutex = new Object();

    /**
     * Initializes a new <tt>DesktopSharingMouseAndKeyboardListener</tt>
     * instance which is to handle mouse and keyboard events for the purposes of
     * desktop sharing with a specific <tt>CallPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> which is controlled remotely and to
     * which the mouse and keyboard events are to be sent
     */
    public DesktopSharingMouseAndKeyboardListener(CallPeer callPeer)
    {
        this(
                callPeer,
                callPeer.getProtocolProvider().getOperationSet(
                        OperationSetDesktopSharingClient.class));
    }

    /**
     * Initializes a new <tt>DesktopSharingMouseAndKeyboardListener</tt>
     * instance which is to handle mouse and keyboard events for the purposes of
     * desktop sharing with a specific <tt>CallPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> which is controlled remotely and to
     * which the mouse and keyboard events are to be sent
     * @param desktopSharingClient the <tt>OperationSetDesktopSharingClient</tt>
     * instance which is to send the mouse and keyboard events to the specified
     * <tt>callPeer</tt>
     */
    public DesktopSharingMouseAndKeyboardListener(
            CallPeer callPeer,
            OperationSetDesktopSharingClient desktopSharingClient)
    {
        this.callPeer = callPeer;
        this.desktopSharingClient = desktopSharingClient;
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
        if (desktopSharingClient != null)
            desktopSharingClient.sendKeyboardEvent(callPeer, e);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed and released) on
     * a component.
     *
     * @param e The mouse event.
     */
    public void mouseClicked(MouseEvent e)
    {
        if (desktopSharingClient != null)
        {
            desktopSharingClient.sendMouseEvent(
                    callPeer,
                    e,
                    videoComponent.getSize());
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
        if (desktopSharingClient != null)
        {
            desktopSharingClient.sendMouseEvent(
                    callPeer,
                    e,
                    videoComponent.getSize());
        }
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
        if (desktopSharingClient != null)
        {
            desktopSharingClient.sendMouseEvent(
                    callPeer,
                    e,
                    videoComponent.getSize());
        }
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     *
     * @param e The mouse event.
     */
    public void mousePressed(MouseEvent e)
    {
        if (desktopSharingClient != null)
        {
            desktopSharingClient.sendMouseEvent(
                    callPeer,
                    e,
                    videoComponent.getSize());
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     *
     * @param e The mouse event.
     */
    public void mouseReleased(MouseEvent e)
    {
        if (desktopSharingClient != null)
        {
            desktopSharingClient.sendMouseEvent(
                    callPeer,
                    e,
                    videoComponent.getSize());
        }
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
        if (desktopSharingClient == null)
            return;

        synchronized(videoComponentMutex)
        {
            if(this.videoComponent == null)
            {
                // If there was no old video component and a new one is set,
                // registers to the operation set.
                if (videoComponent != null)
                    desktopSharingClient.addRemoteControlListener(this);
            }
            else
            {
                // If there was an old video component and no new one is set,
                // unregisters from the operation set.
                if (videoComponent == null)
                {
                    // The remove remote control listener will also be called
                    // directly by the operation when the peer state change.
                    desktopSharingClient.removeRemoteControlListener(this);
                }
            }

            this.videoComponent = videoComponent;
        }
    }
}
