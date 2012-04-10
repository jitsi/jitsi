/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>CallPeerRenderer</tt> interface is meant to be implemented by
 * different renderers of <tt>CallPeer</tt>s. Through this interface they would
 * could be updated in order to reflect the current state of the CallPeer.
 *
 * @author Yana Stamcheva
 */
public interface CallPeerRenderer
{
    /**
     * Sets the name of the peer.
     *
     * @param name the name of the peer
     */
    public void setPeerName(String name);

    /**
     * Sets the <tt>image</tt> of the peer.
     *
     * @param image the image to set
     */
    public void setPeerImage(byte[] image);

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param oldState the previous state of the peer
     * @param newState the new state of the peer
     * @param stateString the state of the contained call peer
     */
    public void setPeerState(   CallPeerState oldState,
                                CallPeerState newState,
                                String stateString);

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     *
     * @param reason the reason of the error to set
     */
    public void setErrorReason(String reason);

    /**
     * Sets the mute property value.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute);

    /**
     * Sets the "on hold" property value.
     *
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold);

    /**
     * Indicates that the security is turned on.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt);

    /**
     * Indicates that the security is turned off.
     * @param evt Details about the event that caused this message.
     */
    public void securityOff(CallPeerSecurityOffEvent evt);

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
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param securityStartedEvent
     *            the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityStartedEvent);

    /**
     * Sets the call peer adapter that manages all related listeners.
     *
     * @param adapter the call peer adapter
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter);

    /**
     * Returns the call peer adapter that manages all related listeners.
     *
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter();

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     *
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar);

    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     *
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel();

    /**
     * Returns the parent call renderer.
     *
     * @return the parent call renderer
     */
    public CallRenderer getCallRenderer();

    /**
     * Returns the component associated with this renderer.
     *
     * @return the component associated with this renderer
     */
    public Component getComponent();

    /**
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible);

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible();

    /**
     * Returns the video handler associated with this call peer renderer.
     *
     * @return the video handler associated with this call peer renderer
     */
    public UIVideoHandler getVideoHandler();

    /**
     * Shows/hides the security panel.
     *
     * @param isVisible <tt>true</tt> to show the security panel, <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean isVisible);
}
