/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CallPeerAdapter</tt> is an adapter that implements all common
 * <tt>CallPeer</tt> related listeners in order to facilitate the task of
 * different <tt>CallPeerRenderer</tt>s when implementing peer functionalities.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class CallPeerAdapter
    extends net.java.sip.communicator.service.protocol.event.CallPeerAdapter
    implements PropertyChangeListener,
               CallPeerSecurityListener
{
    /**
     * The renderer of the underlying <tt>CallPeer</tt>.
     */
    private final CallPeerRenderer renderer;

    /**
     * The underlying call peer.
     */
    private final CallPeer callPeer;

    /**
     * Creates a <tt>CallPeerAdapter</tt> by specifying the
     * corresponding <tt>callPeer</tt> and corresponding <tt>renderer</tt>.
     *
     * @param callPeer the underlying peer
     * @param renderer the component we're using to render the given
     * <tt>callPeer</tt>
     */
    public CallPeerAdapter(CallPeer callPeer, CallPeerRenderer renderer)
    {
        this.callPeer = callPeer;
        this.renderer = renderer;
    }

    /**
     * Fired when peer's state is changed
     *
     * @param evt fired CallPeerEvent
     */
    @Override
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (!sourcePeer.equals(callPeer))
            return;

        CallPeerState newState = (CallPeerState) evt.getNewValue();
        CallPeerState oldState = (CallPeerState) evt.getOldValue();

        String newStateString = sourcePeer.getState().getStateString();

        // Play the dialing audio when in connecting and initiating call state.
        // Stop the dialing audio when we enter any other state.
        if (newState == CallPeerState.INITIATING_CALL
            || newState == CallPeerState.CONNECTING)
        {
            NotificationManager
                .fireNotification(NotificationManager.DIALING);
        }
        else
        {
            NotificationManager.stopSound(NotificationManager.DIALING);
        }

        if (newState == CallPeerState.ALERTING_REMOTE_SIDE)
        {
            NotificationManager
                .fireNotification(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallPeerState.BUSY)
        {
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            // We start the busy sound only if we're in a simple call.
            if (!renderer.getCallDialog().isConference())
                NotificationManager.fireNotification(
                    NotificationManager.BUSY_CALL);
        }
        else if (newState == CallPeerState.CONNECTING_INCOMING_CALL ||
            newState == CallPeerState.CONNECTING_INCOMING_CALL_WITH_MEDIA)
        {
            if (!CallPeerState.isOnHold(oldState))
            {
                NotificationManager
                    .stopSound(NotificationManager.OUTGOING_CALL);
                NotificationManager
                    .stopSound(NotificationManager.INCOMING_CALL);
            }
        }
        else if (newState == CallPeerState.CONNECTED)
        {
            if (!CallPeerState.isOnHold(oldState))
            {
                NotificationManager
                    .stopSound(NotificationManager.OUTGOING_CALL);
                NotificationManager
                    .stopSound(NotificationManager.INCOMING_CALL);

                if (!renderer.getCallDialog().isCallTimerStarted())
                    renderer.getCallDialog().startCallTimer();
            }
            else
            {
                renderer.setOnHold(false);
                renderer.getCallDialog().updateHoldButtonState();
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
        else if (CallPeerState.isOnHold(newState))
        {
            renderer.setOnHold(true);
            renderer.getCallDialog().updateHoldButtonState();
        }

        renderer.setPeerState(newStateString);
    }

    /**
     * Fired when peer's display name is changed
     *
     * @param evt fired CallPeerEvent
     */
    @Override
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
        CallPeer sourcePeer = evt.getSourceCallPeer();

        if (sourcePeer.equals(callPeer))
            renderer.setPeerName((String) evt.getNewValue());
    }

    /**
     * Fired when peer's image is changed
     *
     * @param evt fired CallPeerEvent
     */
    @Override
    public void peerImageChanged(CallPeerChangeEvent evt)
    {
        if (callPeer.equals(evt.getSourceCallPeer()))
            renderer.setPeerImage(new ImageIcon( (byte[]) evt.getNewValue()));
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

            renderer.setMute(isMute);

            // Update the state of the mute button.
            renderer.getCallDialog().updateMuteButtonState();
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

        OperationSetSecureTelephony secure
            = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetSecureTelephony.class);

        if (secure != null)
        {
            renderer.securityOn(securityOnEvent.getSecurityString(),
                                securityOnEvent.isSecurityVerified());

            renderer.setEncryptionCipher(securityOnEvent.getCipher());

            switch (securityOnEvent.getSessionType()) {
            case CallPeerSecurityOnEvent.AUDIO_SESSION:
                renderer.setAudioSecurityOn(true);
                break;
            case CallPeerSecurityOnEvent.VIDEO_SESSION:
                renderer.setVideoSecurityOn(true);
                break;
            }

            NotificationManager.fireNotification(
                NotificationManager.CALL_SECURITY_ON);
        }
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

        renderer.securityOff();

        switch (securityOffEvent.getSessionType())
        {
        case CallPeerSecurityOnEvent.AUDIO_SESSION:
            renderer.setAudioSecurityOn(false);
            break;
        case CallPeerSecurityOnEvent.VIDEO_SESSION:
            renderer.setVideoSecurityOn(false);
            break;
        }
    }
}
