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
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A base implementation of a user interface <tt>Component</tt> which depicts a
 * <tt>CallConference</tt> and is contained in a <tt>CallPanel</tt>.
 *
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public abstract class BasicConferenceCallPanel
    extends JPanel
    implements SwingCallRenderer
{
    /**
     * The <tt>CallPanel</tt> which has created this instance and uses it to
     * depict {@link #callConference}.
     */
    protected final CallPanel callPanel;

    /**
     * The <tt>CallConference</tt> which is depicted by this
     * <tt>BasicConferenceCallPanel</tt> i.e. the model of this view.
     */
    protected final CallConference callConference;

    /**
     * The listener which listens to the <tt>CallConference</tt> depicted by
     * this instance, the <tt>Call</tt>s participating in it, and the
     * <tt>CallPeer</tt>s associated with them.
     */
    private final CallConferenceListener callConferenceListener
        = new CallConferenceListener();

    /**
     * The <tt>ConferenceCallPeerRenderer</tt>s which depict/render
     * <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating in
     * the telephony conference depicted by this instance.
     */
    private final Map<CallPeer, ConferenceCallPeerRenderer> callPeerPanels
        = new HashMap<CallPeer, ConferenceCallPeerRenderer>();

    /**
     * List of call peers that should be removed with delay.
     */
    private Map<CallPeer, Timer> delayedCallPeers
        = new HashMap<CallPeer, Timer>();

    /**
     * The indicator which determines whether {@link #dispose()} has already
     * been invoked on this instance. If <tt>true</tt>, this instance is
     * considered non-functional and is to be left to the garbage collector.
     */
    private boolean disposed = false;

    /**
     * List of conference peer panel listeners that will be notified for adding
     * or removing peer panels.
     */
    private List<ConferencePeerViewListener> peerViewListeners
        = new ArrayList<ConferencePeerViewListener>();

    /**
     * The <tt>Runnable</tt> which is scheduled by
     * {@link #updateViewFromModel()} for execution in the AWT event dispatching
     * thread in order to invoke
     * {@link #updateViewFromModelInEventDispatchThread()}.
     */
    private final Runnable updateViewFromModelInEventDispatchThread
        = new Runnable()
        {
            public void run()
            {
                /*
                 * We receive events/notifications from various threads and we
                 * respond to them in the AWT event dispatching thread. It is
                 * possible to first schedule an event to be brought to the AWT
                 * event dispatching thread, then to have #dispose() invoked on
                 * this instance and, finally, to receive the scheduled event in
                 * the AWT event dispatching thread. In such a case, this
                 * disposed instance should not respond to the event.
                 */
                if (!disposed)
                    updateViewFromModelInEventDispatchThread();
            }
        };

    /**
     * Initializes a new <tt>BasicConferenceCallPanel</tt> instance which is to
     * be used by a specific <tt>CallPanel</tt> to depict a specific
     * <tt>CallConference</tt>.
     *
     * @param callPanel the <tt>CallPanel</tt> which will use the new instance
     * to depict the specified <tt>CallConference</tt>.
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     */
    protected BasicConferenceCallPanel(
            CallPanel callPanel,
            CallConference callConference)
    {
        super(new BorderLayout());

        this.callPanel = callPanel;
        this.callConference = callConference;
    }

    /**
     * Creates a timer for the call peer and adds the timer and the call peer to
     * <tt>delayedCallPeers</tt> list.
     *
     * @param peer the peer to be added.
     */
    public void addDelayedCallPeer(final CallPeer peer)
    {
        Timer timer
            = new Timer(
                5000,
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        removeDelayedCallPeer(peer, false);
                        updateViewFromModel();
                    }
                });

        synchronized (delayedCallPeers)
        {
            delayedCallPeers.put(peer, timer);
        }

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Adds new <tt>ConferencePeerViewListener</tt> listener if the listener
     * is not already added.
     *
     * @param listener the listener to be added.
     */
    public void addPeerViewlListener(ConferencePeerViewListener listener)
    {
        if(!peerViewListeners.contains(listener))
            peerViewListeners.add(listener);
    }

    /**
     * Notifies this instance that a <tt>CallPeer</tt> was added to a
     * <tt>Call</tt> participating in the telephony conference depicted by this
     * instance.
     *
     * @param ev a <tt>CallPeerEvent</tt> which specifies the <tt>CallPeer</tt>
     * which was added and the <tt>Call</tt> to which it was added
     */
    protected void callPeerAdded(CallPeerEvent ev)
    {
        updateViewFromModel();
    }

    /**
     * Notifies this instance that a <tt>CallPeer</tt> was removed from a
     * <tt>Call</tt> participating in the telephony conference depicted by this
     * instance.
     *
     * @param ev a <tt>CallPeerEvent</tt> which specifies the <tt>CallPeer</tt>
     * which was removed and the <tt>Call</tt> from which it was removed
     */
    protected void callPeerRemoved(CallPeerEvent ev)
    {
        final CallPeer peer = ev.getSourceCallPeer();
        if(ev.isDelayed())
        {
            addDelayedCallPeer(peer);
        }
        else
        {
            if(delayedCallPeers.containsKey(peer))
            {
                removeDelayedCallPeer(peer, false);
            }
            updateViewFromModel();
        }

    }

    /**
     * Notifies this instance that there was a change in the <tt>CallState</tt>
     * of a <tt>Call</tt> participating in the telephony conference depicted by
     * this instance.
     *
     * @param ev a <tt>CallChangeEvent</tt> which specifies the <tt>Call</tt>
     * whose <tt>CallState</tt> was changed and the old and new
     * <tt>CallState</tt>s
     */
    protected void callStateChanged(CallChangeEvent ev)
    {
        updateViewFromModel();
    }

    /**
     * Notifies this instance that a <tt>CallPeer</tt> associated with a
     * <tt>Call</tt> participating in the telephony conference depicted by this
     * instance changed its <tt>conferenceFocus</tt> state/property.
     *
     * @param ev a <tt>CallPeerConferenceEvent</tt> which specifies the
     * <tt>CallPeer</tt> which changed its <tt>conferenceFocus</tt>
     * state/property
     */
    protected void conferenceFocusChanged(CallPeerConferenceEvent ev)
    {
        updateViewFromModel();
    }

    /**
     * Notifies this instance that a <tt>CallPeer</tt> associated with a
     * <tt>Call</tt> participating in the telephony conference depicted by this
     * instance added a <tt>ConferenceMember</tt> (to its list).
     *
     * @param ev a <tt>CallPeerConferenceEvent</tt> which specifies the
     * <tt>ConferenceMember</tt> which was added and the <tt>CallPeer</tt> which
     * added that <tt>ConferenceMember</tt> (to its list)
     */
    protected void conferenceMemberAdded(CallPeerConferenceEvent ev)
    {
        updateViewFromModel();
    }
    
    /**
    * Notifies this instance that a <tt>CallPeer</tt> associated with a
    * <tt>Call</tt> participating in the telephony conference received a error 
    * packet.
    *
    * @param ev a <tt>CallPeerConferenceEvent</tt> which specifies the
    * <tt>CallPeer</tt> which sent the error packet and an error message.
    */
    protected void conferenceMemberErrorReceived(CallPeerConferenceEvent ev)
    {
        CallPeer callPeer = ev.getSourceCallPeer();
        
        callPeerPanels.get(callPeer).setErrorReason(
            GuiActivator.getResources().getI18NString(
                "service.gui.PROBLEMS_ENCOUNTERED"));
        
        GuiActivator.getAlertUIService().showAlertPopup(
            GuiActivator.getResources().getI18NString(
                "service.gui.ERROR_RECEIVED_FROM",
                new String[]{callPeer.getDisplayName()}), 
            ev.getErrorString());
    }

    /**
     * Notifies this instance that a <tt>CallPeer</tt> associated with a
     * <tt>Call</tt> participating in the telephony conference depicted by this
     * instance removed a <tt>ConferenceMember</tt> (from its list).
     *
     * @param ev a <tt>CallPeerConferenceEvent</tt> which specifies the
     * <tt>ConferenceMember</tt> which was removed and the <tt>CallPeer</tt>
     * which removed that <tt>ConferenceMember</tt> (from its list)
     */
    protected void conferenceMemberRemoved(CallPeerConferenceEvent ev)
    {
        updateViewFromModel();
    }

    /**
     * Releases the resources acquired by this instance which require explicit
     * disposal (e.g. any listeners added to the depicted
     * <tt>CallConference</tt>, the participating <tt>Call</tt>s, and their
     * associated <tt>CallPeer</tt>s). Invoked by <tt>CallPanel</tt> when it
     * determines that this <tt>BasicConferenceCallPanel</tt> is no longer
     * necessary.
     */
    public void dispose()
    {
        disposed = true;

        callConference.removeCallChangeListener(callConferenceListener);
        callConference.removeCallPeerConferenceListener(callConferenceListener);

        for (ConferenceCallPeerRenderer callPeerPanel
                : callPeerPanels.values())
            callPeerPanel.dispose();
    }

    /**
     * Creates and fires <tt>ConferencePeerViewEvent</tt> event. The method
     * notifies all listeners added by {@link #addPeerViewlListener} method.
     *
     * @param eventID the ID of this event which may be
     * {@link ConferencePeerViewEvent#CONFERENCE_PEER_VIEW_ADDED} or
     * {@link ConferencePeerViewEvent#CONFERENCE_PEER_VIEW_REMOVED}
     * @param callPeer the call peer associated with the event.
     * @param callPeerView the peer view associated with the event.
     */
    public void fireConferencePeerViewEvent(int eventID, CallPeer callPeer,
        ConferenceCallPeerRenderer callPeerView)
    {
        for(ConferencePeerViewListener listener :
            peerViewListeners)
        {
            listener.peerViewRemoved(
                new ConferencePeerViewEvent(eventID, callPeer, callPeerView));
        }

    }

    /**
     * {@inheritDoc}
     *
     * Implements {@link SwingCallRenderer#getCallContainer()}.
     */
    public CallPanel getCallContainer()
    {
        return callPanel;
    }

    /**
     * {@inheritDoc}
     *
     * Implements {@link SwingCallRenderer#getCallPeerRenderer(CallPeer)}.
     */
    public SwingCallPeerRenderer getCallPeerRenderer(CallPeer callPeer)
    {
        return callPeerPanels.get(callPeer);
    }

    /**
     * Check if the list with the delayed call peers is empty.
     *
     * @return <tt>true</tt> if the list is not empty and <tt>false</tt> if the
     * list is empty.
     */
    public boolean hasDelayedCallPeers()
    {
        return !delayedCallPeers.isEmpty();
    }

    /**
     * Notifies this instance that it has been fully initialized and the view
     * that it implements is ready to be updated from its model. Allows
     * extenders to provide additional initialization in their constructors
     * before <tt>BasicConferenceCallPanel</tt> invokes
     * {@link #updateViewFromModel()}.
     */
    protected void initializeComplete()
    {
        callConference.addCallChangeListener(callConferenceListener);
        callConference.addCallPeerConferenceListener(callConferenceListener);

        updateViewFromModel();
    }

    /**
     * Returns <tt>true</tt> if {@link #dispose()} has already been invoked on
     * this instance; otherwise, <tt>false</tt>.
     *
     * @return <tt>true</tt> if <tt>dispose()</tt> has already been invoked on
     * this instance; otherwise, <tt>false</tt>
     */
    protected final boolean isDisposed()
    {
        return disposed;
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerConferenceEvent</tt>
     * fired in the telephony conference depicted by this instance.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> this instance is notified
     * about
     */
    protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
    {
        switch (ev.getEventID())
        {
        case CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED:
            conferenceFocusChanged(ev);
            break;
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
            conferenceMemberAdded(ev);
            break;
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
            conferenceMemberRemoved(ev);
            break;
        default:
            throw new IllegalArgumentException(
                    "CallPeerConferenceEvent.getEventID");
        }
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerEvent</tt> fired in
     * the telephony conference depicted by this instance. Depending on the
     * <tt>eventID</tt> of <tt>ev</tt>, calls
     * {@link #callPeerAdded(CallPeerEvent)} or
     * {@link #callPeerRemoved(CallPeerEvent)}.
     *
     * @param ev the <tt>CallPeerEvent</tt> this instance is notified about
     */
    protected void onCallPeerEvent(CallPeerEvent ev)
    {
        switch (ev.getEventID())
        {
        case CallPeerEvent.CALL_PEER_ADDED:
            callPeerAdded(ev);
            break;
        case CallPeerEvent.CALL_PEER_REMOVED:
            callPeerRemoved(ev);
            break;
        default:
            throw new IllegalArgumentException("CallPeerEvent.getEventID");
        }
    }

    /**
     * Removes a call peer from <tt>delayedCallPeers</tt> list.
     *
     * @param peer a call peer to be removed.
     * @param stopTimer if <tt>true</tt> the timer for the peer will be stopped
     * before the removal.
     */
    public void removeDelayedCallPeer(CallPeer peer, boolean stopTimer)
    {
        if(stopTimer)
        {
            Timer timer = delayedCallPeers.get(peer);
            if(timer != null)
                timer.stop();
        }

        synchronized (delayedCallPeers)
        {
            delayedCallPeers.remove(peer);
        }
    }

    /**
     * Removes <tt>ConferencePeerViewListener</tt> listener.
     *
     * @param listener the listener to be removed.
     */
    public void removePeerViewListener(ConferencePeerViewListener listener)
    {
        peerViewListeners.remove(listener);
    }

    /**
     * Updates this view i.e. <tt>BasicConferenceCallPanel</tt> so that it
     * depicts the current state of its model i.e. <tt>callConference</tt>.
     */
    protected void updateViewFromModel()
    {
        /*
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event.
         */
        if (!disposed)
        {
            if (SwingUtilities.isEventDispatchThread())
                updateViewFromModelInEventDispatchThread();
            else
            {
                SwingUtilities.invokeLater(
                        updateViewFromModelInEventDispatchThread);
            }
        }
    }

    /**
     * Updates the <tt>ConferenceCallPeerRenderer</tt> which is to depict a
     * specific <tt>CallPeer</tt>. Invoked by
     * {@link #updateViewFromModelInEventDispatchThread()} in the AWT event
     * dispatching thread.
     *
     * @param callPeer the <tt>CallPeer</tt> whose depicting
     * <tt>ConferenceCallPeerPanel</tt> is to be updated. The <tt>null</tt>
     * value is used to indicate the local peer.
     * @see #updateViewFromModel(ConferenceCallPeerRenderer, CallPeer)
     */
    protected void updateViewFromModel(CallPeer callPeer)
    {
        ConferenceCallPeerRenderer oldCallPeerPanel
            = callPeerPanels.get(callPeer);
        ConferenceCallPeerRenderer newCallPeerPanel
            = updateViewFromModel(oldCallPeerPanel, callPeer);

        if (oldCallPeerPanel != newCallPeerPanel)
        {
            if (oldCallPeerPanel != null)
            {
                callPeerPanels.remove(callPeer);
                try
                {
                    viewForModelRemoved(oldCallPeerPanel, callPeer);
                }
                finally
                {
                    oldCallPeerPanel.dispose();
                }
            }
            if (newCallPeerPanel != null)
            {
                callPeerPanels.put(callPeer, newCallPeerPanel);
                viewForModelAdded(newCallPeerPanel, callPeer);
            }
        }
    }

    /**
     * Updates the <tt>ConferenceCallPeerRenderer</tt> which is to depict a
     * specific <tt>CallPeer</tt>. The update is in the sense of making sure
     * that the existing <tt>callPeerPanel</tt> is of the right run-time type to
     * continue depicting the current state of <tt>callPeer</tt> and the
     * telephony conference in which it participates, replacing it with a new
     * <tt>ConferenceCallPeerRenderer</tt> if the existing one is no longer
     * appropriate, or creating a new <tt>ConferenceCallPeerRenderer</tt> if
     * there is no existing one to depict the specified <tt>callPeer</tt>. If
     * the existing <tt>callPeerPanel</tt> is still appropriate for the current
     * state of the specified <tt>callPeer</tt>, the update does not include
     * notifying the existing <tt>callPeerPanel</tt> that it should update its
     * view from its model. <tt>BasicConferenceCallPanel</tt> invokes the method
     * in the AWT event dispatching thread.
     *
     * @param callPeerPanel the <tt>ConferenceCallPeerRenderer</tt>, if any,
     * which currently depicts the specified <tt>CallPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> whose depicting
     * <tt>ConferenceCallPeerPanel</tt> is to be updated. The <tt>null</tt>
     * value is used to indicate the local peer.
     * @return the <tt>ConferenceCallPeerRenderer</tt>, if any, which is to
     * depict the specified <tt>callPeer</tt>. If it is different from
     * <tt>callPeerPanel</tt> (and <tt>callPeerPanel</tt> is non-<tt>null</tt>),
     * <tt>callPeerPanel</tt> will be disposed of with a call to
     * {@link ConferenceCallPeerRenderer#dispose()}.
     */
    protected abstract ConferenceCallPeerRenderer updateViewFromModel(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer);

    /**
     * Updates this view i.e. <tt>BasicConferenceCallPanel</tt> so that it
     * depicts the current state of its model i.e. <tt>callConference</tt>. The
     * update is performed on the AWT event dispatching thread.
     */
    protected void updateViewFromModelInEventDispatchThread()
    {
        /*
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event.
         */
        if (disposed)
            return;

        /* Update the view of the local peer/user. */
        updateViewFromModel(null);

        List<CallPeer> callPeers = callConference.getCallPeers();

        /*
         * Dispose of the callPeerPanels whose CallPeers are no longer in the
         * telephony conference depicted by this instance.
         */
        for (Iterator<Map.Entry<CallPeer, ConferenceCallPeerRenderer>> entryIter
                    = callPeerPanels.entrySet().iterator();
                entryIter.hasNext();)
        {
            Map.Entry<CallPeer, ConferenceCallPeerRenderer> entry
                = entryIter.next();
            CallPeer callPeer = entry.getKey();

            if ((callPeer != null) && !callPeers.contains(callPeer)
                    && !delayedCallPeers.containsKey(callPeer))
            {
                ConferenceCallPeerRenderer callPeerPanel = entry.getValue();

                entryIter.remove();

                fireConferencePeerViewEvent(
                    ConferencePeerViewEvent.CONFERENCE_PEER_VIEW_REMOVED,
                    callPeer, callPeerPanel);

                try
                {
                    viewForModelRemoved(callPeerPanel, callPeer);
                }
                finally
                {
                    callPeerPanel.dispose();
                }
            }
        }

        /*
         * Update the callPeerPanels whose CallPeers are still in the telephony
         * conference depicted by this instance. The update procedure includes
         * adding callPeerPanels for new CallPeers and replacing callPeerPanels
         * for existing CallPeers who require different callPeerPanels.
         */
        for (CallPeer callPeer : callPeers)
            updateViewFromModel(callPeer);
    }

    /**
     * Notifies this instance that a <tt>ConferenceCallPeerRenderer</tt> was
     * added to depict a specific <tt>CallPeer</tt>. Implementers are expected
     * to add the AWT <tt>Component</tt> of the specified <tt>callPeerPanel</tt>
     * to their user interface hierarchy.
     *
     * @param callPeerPanel the <tt>ConferenceCallPeerRenderer</tt> which was
     * added to depict the specified <tt>callPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> which is depicted by the specified
     * <tt>callPeerPanel</tt>
     */
    protected abstract void viewForModelAdded(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer);

    /**
     * Notifies this instance that a <tt>ConferenceCallPeerRenderer</tt> was
     * removed to no longer depict a specific <tt>CallPeer</tt>. Implementers
     * are expected to remove the AWT <tt>Component</tt> of the specified
     * <tt>callPeerPanel</tt> from their user interface hierarchy.
     *
     * @param callPeerPanel the <tt>ConferenceCallPeerRenderer</tt> which was
     * removed to no longer depict the specified <tt>callPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> which is depicted by the specified
     * <tt>callPeerPanel</tt>
     */
    protected abstract void viewForModelRemoved(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer);

    /**
     * Implements the listeners which get notified about events related to the
     * telephony conference depicted by this <tt>BasicConferenceCallPanel</tt>
     * and which may cause a need to update this view from its model.
     */
    private class CallConferenceListener
        extends CallPeerConferenceAdapter
        implements CallChangeListener
    {
        public void callPeerAdded(CallPeerEvent ev)
        {
            BasicConferenceCallPanel.this.onCallPeerEvent(ev);
        }

        public void callPeerRemoved(CallPeerEvent ev)
        {
            BasicConferenceCallPanel.this.onCallPeerEvent(ev);
        }

        public void callStateChanged(CallChangeEvent ev)
        {
            BasicConferenceCallPanel.this.callStateChanged(ev);
        }

        /**
         * Invokes
         * {@link BasicConferenceCallPanel#conferenceMemberErrorReceived(
         * CallPeerConferenceEvent)}.
         */
        public void conferenceMemberErrorReceived(
            CallPeerConferenceEvent ev)
        {
            BasicConferenceCallPanel.this.conferenceMemberErrorReceived(ev);
        }
        
        /**
         * {@inheritDoc}
         *
         * Invokes
         * {@link BasicConferenceCallPanel#onCallPeerConferenceEvent(
         * CallPeerConferenceEvent)}.
         */
        @Override
        protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
        {
            BasicConferenceCallPanel.this.onCallPeerConferenceEvent(ev);
        }
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        callPanel.startCallTimer();
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        callPanel.stopCallTimer();
    }

    /**
     * Returns <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>.
     *
     * @return <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>
     */
    public boolean isCallTimerStarted()
    {
        return callPanel.isCallTimerStarted();
    }

    /**
     * Updates the state of the general hold button. The hold button is selected
     * only if all call peers are locally or mutually on hold at the same time.
     * In all other cases the hold button is unselected.
     */
    public void updateHoldButtonState()
    {
        callPanel.updateHoldButtonState();
    }
}
