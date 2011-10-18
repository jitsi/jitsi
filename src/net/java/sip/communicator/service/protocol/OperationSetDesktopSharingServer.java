/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.event.*;

/**
 * Represents an <tt>OperationSet</tt> giving access to desktop sharing
 * server-side specific functionalities.
 *
 * @author Sebastien Vincent
 */
public interface OperationSetDesktopSharingServer
    extends OperationSetDesktopStreaming
{
    /**
     * Enables desktop remote control. Local desktop can now regenerates
     * keyboard and mouse events received from peer.
     *
     * @param callPeer call peer that will take control on local computer
     */
    public void enableRemoteControl(CallPeer callPeer);

    /**
     * Disables desktop remote control. Local desktop stop regenerates
     * keyboard and mouse events received from peer.
     *
     * @param callPeer call peer that will stop controlling local computer
     */
    public void disableRemoteControl(CallPeer callPeer);

    /**
     * Process keyboard notification received from remote peer.
     *
     * @param event <tt>KeyEvent</tt> that will be regenerated on local
     * computer
     */
    public void processKeyboardEvent(KeyEvent event);

    /**
     * Process mouse notification received from remote peer.
     *
     * @param event <tt>MouseEvent</tt> that will be regenerated on local
     * computer
     */
    public void processMouseEvent(MouseEvent event);
}
