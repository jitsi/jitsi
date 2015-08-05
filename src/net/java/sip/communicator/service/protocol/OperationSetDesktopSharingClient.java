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

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents an <tt>OperationSet</tt> giving access to desktop sharing
 * client-side specific functionalities.
 *
 * @author Sebastien Vincent
 */
public interface OperationSetDesktopSharingClient extends OperationSet
{
    /**
     * Send a keyboard notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>KeyEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendKeyboardEvent(CallPeer callPeer, KeyEvent event);

    /**
     * Send a mouse notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event);

    /**
     * Send a mouse notification for specific "moved" <tt>MouseEvent</tt>. As
     * controller computer could have smaller desktop that controlled ones, we
     * should take care to send the percentage of point x and point y regarding
     * to the video panel.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     * @param videoPanelSize size of the panel that contains video
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event,
            Dimension videoPanelSize);

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
