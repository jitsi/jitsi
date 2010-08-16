/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.Dimension;
import java.awt.event.*;

import net.java.sip.communicator.service.protocol.event.RemoteControlListener;

/**
 * Represents an <tt>OperationSet</tt> giving access to desktop sharing
 * client-side specific functionality.
 *
 * @author Sebastien Vincent
 */
public interface OperationSetDesktopSharingClient extends OperationSet
{
    /**
     * Send a keyboard notification.
     *
     * @param event <tt>KeyEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendKeyboardEvent(KeyEvent event);

    /**
     * Send a mouse notification.
     *
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendMouseEvent(MouseEvent event);

    /**
     * Send a mouse notification for specific "moved" <tt>MouseEvent</tt>. As
     * controller computer could have smaller desktop that controlled ones, we
     * should take care to send the percentage of point x and point y regarding
     * to the video panel.
     *
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     * @param videoPanelSize size of the panel that contains video
     */
    public void sendMouseEvent(MouseEvent event, Dimension videoPanelSize);

    /**
     * Add a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept/revoke to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to add
     */
    public void addRemoteControlListener(RemoteControlListener listener);

    /**
     * Remove a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept/revoke to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to remove
     */
    public void removeRemoteControlListener(RemoteControlListener listener);
}
