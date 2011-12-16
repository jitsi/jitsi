/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.beans.*;

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

        if (newState == CallPeerState.CONNECTED)
        {
            if (!CallPeerState.isOnHold(oldState))
            {
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
     * Does nothing.
     * @param event the event we received
     */
    public void securityMessageRecieved(CallPeerSecurityMessageEvent event)
    {
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
