/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.beans.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

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
               CallPeerSecurityListener,
               CallPeerConferenceListener
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

        String newStateString = sourcePeer.getState().getLocalizedStateString();

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

        if (newState == CallPeerState.ALERTING_REMOTE_SIDE
            //if we were already in state CONNECTING_WITH_EARLY_MEDIA the server
            //is already taking care of playing the notifications so we don't
            //need to fire a notification here.
            && oldState != CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
        {
            //
            NotificationManager
                .fireNotification(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallPeerState.BUSY)
        {
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            // We start the busy sound only if we're in a simple call.
            if (!renderer.getCallPanel().isConference())
            {
                NotificationManager.fireNotification(
                    NotificationManager.BUSY_CALL);
            }
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
        else if (newState == CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
        {
            //this means a call with early media. make sure that we are not
            //playing local notifications any more.
            NotificationManager
                .stopSound(NotificationManager.OUTGOING_CALL);
        }
        else if (newState == CallPeerState.CONNECTED)
        {
            if (!CallPeerState.isOnHold(oldState))
            {
                NotificationManager
                    .stopSound(NotificationManager.OUTGOING_CALL);
                NotificationManager
                    .stopSound(NotificationManager.INCOMING_CALL);

                if (!renderer.getCallPanel().isCallTimerStarted())
                    renderer.getCallPanel().startCallTimer();

                // Enabling all buttons when the call is connected.
                renderer.getCallPanel().enableButtons(true);
            }
            else
            {
                renderer.setOnHold(false);
                renderer.getCallPanel().updateHoldButtonState();
                // Enabling all buttons when the call get back from hold
                renderer.getCallPanel().enableButtonsWhileOnHold(false);
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
            renderer.getCallPanel().enableButtonsWhileOnHold(true);
            renderer.getCallPanel().updateHoldButtonState();
        }

        renderer.setPeerState(newStateString);

        String reasonString = evt.getReasonString();
        if (reasonString != null)
            renderer.setErrorReason(reasonString);
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
            renderer.setPeerImage((byte[]) evt.getNewValue());
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
     * @param evt the event we received
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        CallPeer peer = (CallPeer) evt.getSource();

        if (!peer.equals(callPeer))
            return;

        renderer.securityOn(evt);

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

        renderer.securityOff(securityOffEvent);
    }

    /**
     * Adds a <tt>ConferenceMemberPanel</tt> to this container when a
     * <tt>ConferenceMember</tt> has been added to the corresponding conference.
     * @param conferenceEvent the <tt>CallPeerConferenceEvent</tt> that has been
     * triggered
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        renderer.getCallRenderer().conferenceMemberAdded(
            callPeer, conferenceEvent.getConferenceMember());
    }

    /**
     * Removes the corresponding <tt>ConferenceMemberPanel</tt> from this
     * container when a <tt>ConferenceMember</tt> has been removed from the
     * corresponding conference.
     * @param conferenceEvent the <tt>CallPeerConferenceEvent</tt> that has been
     * triggered
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {
        renderer.getCallRenderer().conferenceMemberRemoved(
            callPeer, conferenceEvent.getConferenceMember());
    }

    /**
     * Enables or disables the conference focus UI depending on the change.
     *
     * When a peer changes its status from focus to not focus or the reverse.
     * we must change its listeners.
     * If the peer is focus we use conference member lister, cause we will
     * receive its status and the statuses of its conference members.
     * And if it is not a focus we must listen with stream
     * sound level listener
     *
     * @param conferenceEvent the conference event
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {}
}
