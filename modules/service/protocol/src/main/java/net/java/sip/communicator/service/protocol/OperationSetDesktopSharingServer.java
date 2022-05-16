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

    /**
     * Tells if the peer provided can be remotely controlled by this peer:
     * - The server is able to grant/revoke remote access to its desktop.
     * - The client (the call peer) is able to send mouse and keyboard events.
     *
     * @param callPeer The call peer which may remotely control the shared
     * desktop.
     *
     * @return True if the server and the client are able to respectively grant
     *  remote access and send mouse/keyboard events. False, if one of the call
     *  participant (server or client) is not able to deal with remote controls.
     */
    public boolean isRemoteControlAvailable(CallPeer callPeer);
}
