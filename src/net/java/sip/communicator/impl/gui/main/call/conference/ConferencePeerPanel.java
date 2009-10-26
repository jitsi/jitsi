/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>ConferencePeerPanel</tt> renders a single <tt>ConferencePeer</tt>,
 * which is not a conference focus.
 *
 * @author Yana Stamcheva
 */
public class ConferencePeerPanel
    extends BasicConferenceParticipantPanel
    implements  CallPeerRenderer,
                StreamSoundLevelListener
{
    /**
     * The parent dialog containing this panel.
     */
    private final CallDialog callDialog;

    /**
     * The call peer shown in this panel.
     */
    private CallPeer callPeer;

    /**
     * The action menu available for all call peers.
     */
    private CallPeerActionMenuBar actionMenuBar;

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel = new SecurityStatusLabel();

    /**
     * The component showing the security details.
     */
    private SecurityPanel securityPanel;

    private CallPeerAdapter callPeerAdapter;

    /**
     * Creates a <tt>ConferencePeerPanel</tt> by specifying the parent
     * <tt>callDialog</tt>, containing it and the corresponding
     * <tt>protocolProvider</tt>.
     *
     * @param callDialog the call dialog containing this panel
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for the
     * call
     */
    public ConferencePeerPanel( CallDialog callDialog,
                                ProtocolProviderService protocolProvider)
    {
        this.callDialog = callDialog;

        this.setPeerName(protocolProvider.getAccountID().getUserID());

        this.setTitleBackground(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_LOCAL_USER_BACKGROUND")));
    }

    /**
     * Creates a <tt>ConferencePeerPanel</tt>, that would be contained in
     * the given <tt>callDialog</tt> and would correspond to the given
     * <tt>callPeer</tt>.
     *
     * @param callDialog the dialog, in which this panel is shown
     * @param callPeer The peer who own this UI
     */
    public ConferencePeerPanel(CallDialog callDialog, CallPeer callPeer)
    {
        this.callDialog = callDialog;

        this.setPeerName(callPeer.getDisplayName());

        // We initialize the status bar for call peers only.
        this.initStatusBar(callPeer);

        actionMenuBar = new CallPeerActionMenuBar(callPeer);
        this.addToNameBar(actionMenuBar);

        this.setTitleBackground(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_PEER_NAME_BACKGROUND")));
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff()
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF)));
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified by the underlying <tt>CallPeer</tt>
     */
    public void securityOn( String securityString,
                            boolean isSecurityVerified)
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_ON)));

        if (securityPanel == null)
        {
            SecurityPanel securityPanel = new SecurityPanel(callPeer);

            GridBagConstraints constraints = new GridBagConstraints();

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            this.add(securityPanel, constraints);
        }

        securityPanel.refreshStates(securityString, isSecurityVerified);

        this.revalidate();
    }

    /**
     * Sets the mute status icon to the status panel.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute)
    {
        if(isMute)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
        else
            muteStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold)
    {
        if(isOnHold)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));
        else
            holdStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the <tt>icon</tt> of the peer.
     * @param icon the icon to set
     */
    public void setPeerImage(ImageIcon icon)
    {
        this.setParticipantImage(icon);
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        this.setParticipantName(name);
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state)
    {
        this.setParticipantState(state);
    }

    /**
     * Updates all related components to fit the new value.
     * @param isAudioSecurityOn indicates if the audio security is turned on
     * or off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        securityStatusLabel.setAudioSecurityOn(isAudioSecurityOn);
    }

    /**
     * Updates all related components to fit the new value.
     * @param encryptionCipher the encryption cipher to show
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        securityStatusLabel.setEncryptionCipher(encryptionCipher);
    }

    /**
     * Updates all related components to fit the new value.
     * @param isVideoSecurityOn indicates if the video security is turned on
     * or off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        securityStatusLabel.setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * Sets the call peer adapter that manages all related listeners.
     * @param adapter the call peer adapter
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        this.callPeerAdapter = adapter;
    }

    /**
     * Returns the call peer adapter that manages all related listeners.
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter()
    {
        return callPeerAdapter;
    }

    /**
     * Returns the parent <tt>CallDialog</tt> containing this renderer.
     * @return the parent <tt>CallDialog</tt> containing this renderer
     */
    public CallDialog getCallDialog()
    {
        return callDialog;
    }

    /**
     * Enters in full screen view mode.
     */
    public void enterFullScreen()
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Exits from the full screen view mode by specifying the full screen window
     * previously created.
     *
     * @param fullScreenWindow the window previously shown in full screen mode
     */
    public void exitFullScreen(Window fullScreenWindow)
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Initializes the status bar component for the given <tt>callPeer</tt>.
     *
     * @param callPeer the underlying peer, which status would be displayed
     */
    private void initStatusBar(CallPeer callPeer)
    {
        this.setParticipantState(callPeer.getState().getStateString());

        this.addToStatusBar(securityStatusLabel);
        this.addToStatusBar(holdStatusLabel);
        this.addToStatusBar(muteStatusLabel);

        Component[] buttons = new Component[]
        {
            CallPeerRendererUtils.createTransferCallButton(callPeer)
        };

        Component buttonBar
            = CallPeerRendererUtils.createButtonBar(false, buttons);

        this.addToStatusBar(buttonBar);
    }

    /**
     * Updates the sound bar level to fit the new stream sound level.
     * @param event the <tt>StreamSoundLevelEvent</tt> that notifies us of the
     * sound level change
     */
    public void streamSoundLevelChanged(StreamSoundLevelEvent event)
    {
        this.updateSoundBar(event.getLevel());
    }
}
