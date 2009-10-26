/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

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
     * @param name the name of the peer
     */
    public void setPeerName(String name);

    /**
     * Sets the <tt>icon</tt> of the peer.
     * @param icon the icon to set
     */
    public void setPeerImage(ImageIcon icon);

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state);

    /**
     * Sets the mute property value.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute);

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold);

    /**
     * Indicates that the security is turned on by specifying the
     * <tt>securityString</tt> and whether it has been already verified.
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified by the underlying <tt>CallPeer</tt>
     */
    public void securityOn( String securityString,
                            boolean isSecurityVerified);

    /**
     * Indicates that the security is turned off.
     */
    public void securityOff();

    /**
     * Enters in full screen view mode.
     */
    public void enterFullScreen();

    /**
     * Exits from the full screen view mode by specifying the full screen window
     * previously created.
     *
     * @param fullScreenWindow the window previously shown in full screen mode
     */
    public void exitFullScreen(Window fullScreenWindow);

    /**
     * Sets the audio security on or off.
     *
     * @param isAudioSecurityOn indicates if the audio security is turned on or
     * off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn);

    /**
     * Sets the video security on or off.
     *
     * @param isVideoSecurityOn indicates if the video security is turned on or
     * off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn);

    /**
     * Sets the cipher used for the encryption of the current call.
     *
     * @param encryptionCipher the cipher used for the encryption of the
     * current call.
     */
    public void setEncryptionCipher(String encryptionCipher);

    /**
     * Sets the call peer adapter that manages all related listeners.
     * @param adapter the call peer adapter
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter);

    /**
     * Returns the call peer adapter that manages all related listeners.
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter();

    /**
     * Returns the parent <tt>CallDialog</tt> containing this renderer.
     * @return the parent <tt>CallDialog</tt> containing this renderer
     */
    public CallDialog getCallDialog();
}
