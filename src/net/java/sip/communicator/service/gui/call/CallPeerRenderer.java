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
package net.java.sip.communicator.service.gui.call;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>CallPeerRenderer</tt> interface is meant to be implemented by
 * different renderers of <tt>CallPeer</tt>s. Through this interface they would
 * could be updated in order to reflect the current state of the CallPeer.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface CallPeerRenderer
{
    /**
     * Releases the resources (which require explicit disposal) acquired by this
     * <tt>CallPeerRenderer</tt> throughout its lifetime and prepares it for
     * garbage collection.
     */
    public void dispose();

    /**
     * Returns the parent call renderer.
     *
     * @return the parent call renderer
     */
    public CallRenderer getCallRenderer();

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible();

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     *
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar);

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param securityStartedEvent
     *            the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityStartedEvent);

    /**
     * Indicates that the security is turned off.
     * @param evt Details about the event that caused this message.
     */
    public void securityOff(CallPeerSecurityOffEvent evt);

    /**
     * Indicates that the security is turned on.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt);

    /**
     * Indicates that the security status is pending confirmation.
     */
    public void securityPending();

    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt);

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     *
     * @param reason the reason of the error to set
     */
    public void setErrorReason(String reason);

    /**
     * Shows/hides the local video component.
     *
     * @param visible <tt>true</tt> to show the local video or <tt>false</tt> to
     * hide it
     */
    public void setLocalVideoVisible(boolean visible);

    /**
     * Sets the mute property value.
     *
     * @param mute <tt>true</tt> to mute the <tt>CallPeer</tt> depicted by this
     * instance; <tt>false</tt>, otherwise
     */
    public void setMute(boolean mute);

    /**
     * Sets the "on hold" property value.
     *
     * @param onHold <tt>true</tt> to put the <tt>CallPeer</tt> depicted by this
     * instance on hold; <tt>false</tt>, otherwise
     */
    public void setOnHold(boolean onHold);

    /**
     * Sets the <tt>image</tt> of the peer.
     *
     * @param image the image to set
     */
    public void setPeerImage(byte[] image);

    /**
     * Sets the name of the peer.
     *
     * @param name the name of the peer
     */
    public void setPeerName(String name);

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param oldState the previous state of the peer
     * @param newState the new state of the peer
     * @param stateString the state of the contained call peer
     */
    public void setPeerState(
            CallPeerState oldState,
            CallPeerState newState,
            String stateString);

    /**
     * Shows/hides the security panel.
     *
     * @param visible <tt>true</tt> to show the security panel or <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean visible);
}
