/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The parent component for all <tt>CallPeer</tt> panels.
 * @author Yana Stamcheva
 */
public abstract class ParentCallPeerPanel
    extends TransparentPanel
    implements  CallPeerListener,
                PropertyChangeListener,
                CallPeerSecurityListener
{
    /**
     * The parent dialog containing this panel.
     */
    protected final CallDialog callDialog;

    /**
     * The peer who own this UI
     */
    protected final CallPeer callPeer;

    /**
     * The component showing the name of the underlying call peer.
     */
    protected final JLabel nameLabel = new JLabel("", JLabel.CENTER);

    /**
     * The component showing the status of the underlying call peer.
     */
    protected final JLabel callStatusLabel = new JLabel();

    /**
     * Indicates the state of the audio security (on or off).
     */
    private boolean isAudioSecurityOn = false;

    /**
     * Indicates the state of the video security (on or off).
     */
    private boolean isVideoSecurityOn = false;

    /**
     * The label showing whether the voice has been set to mute
     */
    protected final JLabel muteStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    protected SecurityStatusLabel securityStatusLabel;

    /**
     * The encryption cipher.
     */
    private String encryptionCipher;

    /**
     * Panel showing information about security
     */
    private SecurityPanel securityPanel = null;

    /**
     * The image of the peer
     */
    protected ImageIcon peerImage;

    /**
     * Creates a <tt>DefaultCallPeerRenderer</tt> by specifying the
     * corresponding <tt>callPeer</tt>.
     * @param callPeer the peer that would be rendered
     */
    public ParentCallPeerPanel(CallDialog callDialog, CallPeer callPeer)
    {
        this.callDialog = callDialog;

        this.callPeer = callPeer;

        this.peerImage = new ImageIcon
                (ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)
                .getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        this.securityStatusLabel = new SecurityStatusLabel(
            this,
            new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF)),
            JLabel.CENTER);
    }

    /**
     * Sets the audio security on or off.
     *
     * @param isAudioSecurityOn indicates if the audio security is turned on or
     * off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        this.isAudioSecurityOn = isAudioSecurityOn;
    }

    /**
     * Sets the video security on or off.
     *
     * @param isVideoSecurityOn indicates if the video security is turned on or
     * off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        this.isVideoSecurityOn = isVideoSecurityOn;
    }

    /**
     * Indicates if the audio security is turned on or off.
     *
     * @return <code>true</code> if the audio security is on, otherwise -
     * <code>false</code>.
     */
    public boolean isAudioSecurityOn()
    {
        return isAudioSecurityOn;
    }

    /**
     * Indicates if the video security is turned on or off.
     *
     * @return <code>true</code> if the video security is on, otherwise -
     * <code>false</code>.
     */
    public boolean isVideoSecurityOn()
    {
        return isVideoSecurityOn;
    }

    /**
     * Returns the cipher used for the encryption of the current call.
     *
     * @return the cipher used for the encryption of the current call.
     */
    public String getEncryptionCipher()
    {
        return encryptionCipher;
    }

    /**
     * Sets the cipher used for the encryption of the current call.
     *
     * @param encryptionCipher the cipher used for the encryption of the
     * current call.
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        this.encryptionCipher = encryptionCipher;
    }

    /**
     * Notifies this listener about a change in the characteristic of being a
     * conference focus of a specific <code>CallPeer</code>.
     *
     * @param conferenceEvent
     *            a <code>CallPeerConferenceEvent</code> with ID
     *            <code>CallPeerConferenceEvent#CONFERENCE_FOCUS_CHANGED</code>
     *            and no associated <code>ConferenceMember</code>
     */
    public void conferenceFocusChanged(
        CallPeerConferenceEvent conferenceEvent)
    {
        CallPeer sourcePeer = conferenceEvent.getSourceCallPeer();

        if (!sourcePeer.equals(callPeer))
            return;

        this.setBackground(Color.LIGHT_GRAY);
    }
    
    /**
     * Fired when peer's state is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (!sourcePeer.equals(callPeer))
            return;

        Object newState = evt.getNewValue();

        String newStateString = sourcePeer.getState().getStateString();
        Icon newStateIcon = null;

        if (newState == CallPeerState.ALERTING_REMOTE_SIDE)
        {
            NotificationManager
                .fireNotification(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallPeerState.BUSY)
        {
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            NotificationManager.fireNotification(NotificationManager.BUSY_CALL);
        }
        else if (newState == CallPeerState.CONNECTED)
        {
            if (!CallPeerState.isOnHold((CallPeerState) evt
                .getOldValue()))
            {
                NotificationManager
                    .stopSound(NotificationManager.OUTGOING_CALL);
                NotificationManager
                    .stopSound(NotificationManager.INCOMING_CALL);

                if (!callDialog.isCallTimerStarted())
                    callDialog.startCallTimer();
            }
        }
        else if (newState == CallPeerState.DISCONNECTED)
        {
            // The call peer should be already removed from the call
            // see CallPeerRemoved
        }
        else if (newState == CallPeerState.FAILED)
        {
            // The call peer should be already removed from the call
            // see CallPeerRemoved
        }
        else if (CallPeerState.isOnHold((CallPeerState) newState))
        {
            newStateIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON));

            // If we have clicked the hold button in a full screen mode
            // we need to update the state of the call dialog hold button.
            if ((newState.equals(CallPeerState.ON_HOLD_LOCALLY)
                || newState.equals(CallPeerState.ON_HOLD_MUTUALLY))
                && !callDialog.isHoldButtonSelected())
            {
                callDialog.setHoldButtonSelected(true);
            }
        }

        this.setState(newStateString, newStateIcon);
    }

    /**
     * Fired when peer's adress is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {

    }

    /**
     * Fired when peer's display name is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (sourcePeer.equals(callPeer))
        {
            nameLabel.setText((String) evt.getNewValue());
        }
    }

    /**
     * Fired when peer's image is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (!sourcePeer.equals(callPeer))
            return;

        this.setPeerImage(new ImageIcon( (byte[]) evt.getNewValue()));
    }

    /**
     * Fired when peer's transport address is changed
     *
     * @param evt fired CallPeerEvent
     */
    public void peerTransportAddressChanged(CallPeerChangeEvent evt)
    {
    }

    /**
     * Set the image of the peer
     *
     * @param peerImage new image
     */
    public void setPeerImage(ImageIcon peerImage)
    {
        this.peerImage = peerImage;
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name and icon.
     *
     * @param state the state of the contained call peer
     * @param icon the icon of the state
     */
    public void setState(String state, Icon icon)
    {
        this.callStatusLabel.setText(state);
        this.callStatusLabel.setIcon(icon);
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
    }

    /**
     * Sets the secured status icon to the status panel.
     *
     * @param isSecured indicates if the call with this peer is
     * secured
     */
    public void setSecured(boolean isSecured)
    {
        if (isSecured)
            securityStatusLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.SECURE_BUTTON_ON)));
        else
            securityStatusLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.SECURE_BUTTON_OFF)));
    }

    /**
     * Fired when a change in property happened.
     *
     * @param evt fired PropertyChangeEvent
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(CallPeer.MUTE_PROPERTY_NAME))
        {
            boolean isMute = (Boolean) evt.getNewValue();

            if (isMute)
            {
                // If we have clicked the mute button in a full screen mode
                // we need to update the state of the call dialog mute button.
                if (!callDialog.isMuteButtonSelected())
                {
                    callDialog.setMuteButtonSelected(true);
                }
            }

            this.setMute(isMute);
        }
    }

    /**
     * Processes the received security message and passes it to the
     * <tt>NotificationManager</tt> responsible for popup notifications.
     * @param event the event we received
     */
    public void securityMessageRecieved(CallPeerSecurityMessageEvent event)
    {
        int severity = event.getEventSeverity();

        String messageTitle = null;

        switch (severity)
        {
            // Don't play alert sound for Info or warning.
            case CallPeerSecurityMessageEvent.INFORMATION:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_INFO");
                break;
            }
            case CallPeerSecurityMessageEvent.WARNING:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_WARNING");
                break;
            }
            // Alert sound indicates: security cannot established
            case CallPeerSecurityMessageEvent.SEVERE:
            case CallPeerSecurityMessageEvent.ERROR:
            {
                messageTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.SECURITY_ERROR");
                NotificationManager.fireNotification(
                    NotificationManager.CALL_SECURITY_ERROR);
            }
        }

        NotificationManager.fireNotification(
            NotificationManager.SECURITY_MESSAGE,
            messageTitle,
            event.getI18nMessage());
    }

    /**
     * Creates the security panel, when a <tt>securityOnEvent</tt> is received.
     * @param securityOnEvent the event we received
     */
    public void securityOn(CallPeerSecurityOnEvent securityOnEvent)
    {
        CallPeer peer = (CallPeer) securityOnEvent.getSource();

        if (!peer.equals(callPeer))
            return;

        this.setSecured(true);

        this.setEncryptionCipher(securityOnEvent.getCipher());

        switch (securityOnEvent.getSessionType()) {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            this.setAudioSecurityOn(true);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            this.setVideoSecurityOn(true);
            break;
        }

        this.createSecurityPanel(securityOnEvent);

        NotificationManager.fireNotification(
            NotificationManager.CALL_SECURITY_ON);
    }

    /**
     * Indicates the new state through the security indicator components.
     * @param securityOffEvent the event we received
     */
    public void securityOff(CallPeerSecurityOffEvent securityOffEvent)
    {
        CallPeer peer = (CallPeer) securityOffEvent.getSource();

        if (!peer.equals(callPeer))
            return;

        this.setSecured(false);

        switch (securityOffEvent.getSessionType())
        {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            this.setAudioSecurityOn(false);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            this.setVideoSecurityOn(false);
            break;
        }
    }

    /**
     * Returns the underlying call peer.
     *
     * @return the underlying call peer
     */
    public CallPeer getCallPeer()
    {
        return callPeer;
    }

    /**
     * Create a security panel
     *
     * @param event fired CallPeerSecurityOnEvent
     */
    private void createSecurityPanel(CallPeerSecurityOnEvent event)
    {
        if (callPeer != null)
        {
            OperationSetSecureTelephony secure
                = callPeer
                    .getProtocolProvider().getOperationSet(
                            OperationSetSecureTelephony.class);

            if (secure != null)
            {
                if (securityPanel == null)
                {
                    securityPanel = new SecurityPanel(callPeer);

                    GridBagConstraints constraints = new GridBagConstraints();

                    constraints.fill = GridBagConstraints.NONE;
                    constraints.gridx = 0;
                    constraints.gridy = 2;
                    constraints.weightx = 0;
                    constraints.weighty = 0;
                    constraints.insets = new Insets(5, 0, 0, 0);

                    this.add(securityPanel, constraints);
                }

                securityPanel.refreshStates(event);

                this.revalidate();
            }
        }
    }

    /**
     * Creates a new <code>Component</code> representing a UI means to transfer
     * the <code>Call</code> of the associated <code>callPeer</code> or
     * <tt>null</tt> if call-transfer is unsupported.
     *
     * @return a new <code>Component</code> representing the UI means to
     *         transfer the <code>Call</code> of <code>callPeer</code> or
     *         <tt>null</tt> if call-transfer is unsupported
     */
    protected Component createTransferCallButton()
    {
        if (callPeer != null)
        {
            OperationSetAdvancedTelephony telephony =
                callPeer.getProtocolProvider()
                    .getOperationSet(OperationSetAdvancedTelephony.class);

            if (telephony != null)
                return new TransferCallButton(callPeer);
        }
        return null;
    }
}
