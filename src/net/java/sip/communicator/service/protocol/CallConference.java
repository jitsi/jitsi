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

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.util.event.*;

/**
 * Represents the telephony conference-related state of a <tt>Call</tt>.
 * Multiple <tt>Call</tt> instances share a single <tt>CallConference</tt>
 * instance when the former are into a telephony conference i.e. the local
 * peer/user is the conference focus. <tt>CallConference</tt> is
 * protocol-agnostic and thus enables cross-protocol conferences. Since a
 * non-conference <tt>Call</tt> may be converted into a conference <tt>Call</tt>
 * at any time, every <tt>Call</tt> instance maintains a <tt>CallConference</tt>
 * instance regardless of whether the <tt>Call</tt> in question is participating
 * in a telephony conference.
 *
 * @author Lyubomir Marinov
 */
public class CallConference
    extends PropertyChangeNotifier
{
    /**
     * The name of the <tt>CallConference</tt> property which specifies the list
     * of <tt>Call</tt>s participating in a telephony conference. A change in
     * the value of the property is delivered in the form of a
     * <tt>PropertyChangeEvent</tt> which has its <tt>oldValue</tt> or
     * <tt>newValue</tt> set to the <tt>Call</tt> which has been removed or
     * added to the list of <tt>Call</tt>s participating in the telephony
     * conference.
     */
    public static final String CALLS = "calls";

    /**
     * Gets the number of <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in the telephony conference-related state of a specific
     * <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> for which the number of <tt>CallPeer</tt>s
     * associated with the <tt>Call</tt>s participating in its associated
     * telephony conference-related state
     * @return the number of <tt>CallPeer</tt>s associated with the
     * <tt>Call</tt>s participating in the telephony conference-related state
     * of the specified <tt>Call</tt>
     */
    public static int getCallPeerCount(Call call)
    {
        CallConference conference = call.getConference();

        /*
         * A Call instance is supposed to always maintain a CallConference
         * instance. Anyway, if it turns out that it is not the case, we will
         * consider the Call as a representation of a telephony conference.
         */
        return
            (conference == null)
                ? call.getCallPeerCount()
                : conference.getCallPeerCount();
    }

    /**
     * Gets a list of the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in the telephony conference in which a specific
     * <tt>Call</tt> is participating.
     *
     * @param call the <tt>Call</tt> which specifies the telephony conference
     * the <tt>CallPeer</tt>s of which are to be retrieved
     * @return a list of the <tt>CallPeer</tt>s associated with the
     * <tt>Call</tt>s participating in the telephony conference in which the
     * specified <tt>call</tt> is participating
     */
    public static List<CallPeer> getCallPeers(Call call)
    {
        CallConference conference = call.getConference();
        List<CallPeer> callPeers = new ArrayList<CallPeer>();

        if (conference == null)
        {
            Iterator<? extends CallPeer> callPeerIt = call.getCallPeers();

            while (callPeerIt.hasNext())
                callPeers.add(callPeerIt.next());
        }
        else
            conference.getCallPeers(callPeers);
        return callPeers;
    }

    /**
     * Gets the list of <tt>Call</tt>s participating in the telephony conference
     * in which a specific <tt>Call</tt> is participating.
     *
     * @param call the <tt>Call</tt> which participates in the telephony
     * conference the list of participating <tt>Call</tt>s of which is to be
     * returned
     * @return the list of <tt>Call</tt>s participating in the telephony
     * conference in which the specified <tt>call</tt> is participating
     */
    public static List<Call> getCalls(Call call)
    {
        CallConference conference = call.getConference();
        List<Call> calls;

        if (conference == null)
            calls = Collections.emptyList();
        else
            calls = conference.getCalls();
        return calls;
    }

    /**
     * Determines whether a <tt>CallConference</tt> is to report the local
     * peer/user as a conference focus judging by a specific list of
     * <tt>Call</tt>s.
     *
     * @param calls the list of <tt>Call</tt> which are to be judged whether
     * the local peer/user that they represent is to be considered as a
     * conference focus
     * @return <tt>true</tt> if the local peer/user represented by the specified
     * <tt>calls</tt> is judged to be a conference focus; otherwise,
     * <tt>false</tt>
     */
    private static boolean isConferenceFocus(List<Call> calls)
    {
        int callCount = calls.size();
        boolean conferenceFocus;

        if (callCount < 1)
            conferenceFocus = false;
        else if (callCount > 1)
            conferenceFocus = true;
        else
            conferenceFocus = (calls.get(0).getCallPeerCount() > 1);
        return conferenceFocus;
    }

    /**
     * The <tt>CallChangeListener</tt> which listens to changes in the
     * <tt>Call</tt>s participating in this telephony conference.
     */
    private final CallChangeListener callChangeListener
        = new CallChangeListener()
        {
            @Override
            public void callPeerAdded(CallPeerEvent ev)
            {
                CallConference.this.onCallPeerEvent(ev);
            }

            @Override
            public void callPeerRemoved(CallPeerEvent ev)
            {
                CallConference.this.onCallPeerEvent(ev);
            }

            @Override
            public void callStateChanged(CallChangeEvent ev)
            {
                CallConference.this.callStateChanged(ev);
            }
        };

    /**
     * The list of <tt>CallChangeListener</tt>s added to the <tt>Call</tt>s
     * participating in this telephony conference via
     * {@link #addCallChangeListener(CallChangeListener)}.
     */
    private final List<CallChangeListener> callChangeListeners
        = new LinkedList<CallChangeListener>();

    /**
     * The <tt>CallPeerConferenceListener</tt> which listens to the
     * <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating in
     * this telephony conference.
     */
    private final CallPeerConferenceListener callPeerConferenceListener
        = new CallPeerConferenceAdapter()
        {
            /**
             * {@inheritDoc}
             *
             * Invokes
             * {@link CallConference#onCallPeerConferenceEvent(
             * CallPeerConferenceEvent)}.
             */
            @Override
            protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
            {
                CallConference.this.onCallPeerConferenceEvent(ev);
            }
            
            /**
             * {@inheritDoc}
             *
             * Invokes
             * {@link CallConference#onCallPeerConferenceEvent(
             * CallPeerConferenceEvent)}.
             */
            @Override
            public void conferenceMemberErrorReceived(
                CallPeerConferenceEvent ev)
            {
                CallConference.this.onCallPeerConferenceEvent(ev);
            }
        };

    /**
     * The list of <tt>CallPeerConferenceListener</tt>s added to the
     * <tt>CallPeer</tt>s associated with the <tt>CallPeer</tt>s participating
     * in this telephony conference via
     * {@link #addCallPeerConferenceListener}.
     */
    private final List<CallPeerConferenceListener> callPeerConferenceListeners
        = new LinkedList<CallPeerConferenceListener>();

    /**
     * The synchronization root/<tt>Object</tt> which protects the access to
     * {@link #immutableCalls} and {@link #mutableCalls}.
     */
    private final Object callsSyncRoot = new Object();

    /**
     * The indicator which determines whether the local peer represented by this
     * instance and the <tt>Call</tt>s participating in it is acting as a
     * conference focus. The SIP protocol, for example, will add the
     * &quot;isfocus&quot; parameter to the Contact headers of its outgoing
     * signaling if <tt>true</tt>.
     */
    private boolean conferenceFocus = false;

    /**
     * The list of <tt>Call</tt>s participating in this telephony conference as
     * an immutable <tt>List</tt> which can be exposed out of this instance
     * without the need to make a copy. In other words, it is an unmodifiable
     * view of {@link #mutableCalls}.
     */
    private List<Call> immutableCalls;

    /**
     * The indicator which determines whether the telephony conference
     * represented by this instance is utilizing the Jitsi Videobridge
     * server-side telephony conferencing technology.
     */
    private final boolean jitsiVideobridge;

    /**
     * The list of <tt>Call</tt>s participating in this telephony conference as
     * a mutable <tt>List</tt> which should not be exposed out of this instance.
     */
    private List<Call> mutableCalls;

    /**
     * Initializes a new <tt>CallConference</tt> instance.
     */
    public CallConference()
    {
        this(false);
    }

    /**
     * Initializes a new <tt>CallConference</tt> instance which is to optionally
     * utilize the Jitsi Videobridge server-side telephony conferencing
     * technology.
     *
     * @param jitsiVideobridge <tt>true</tt> if the telephony conference
     * represented by the new instance is to utilize the Jitsi Videobridge
     * server-side telephony conferencing technology; otherwise, <tt>false</tt>
     */
    public CallConference(boolean jitsiVideobridge)
    {
        this.jitsiVideobridge = jitsiVideobridge;

        mutableCalls = new ArrayList<Call>();
        immutableCalls = Collections.unmodifiableList(mutableCalls);
    }

    /**
     * Adds a specific <tt>Call</tt> to the list of <tt>Call</tt>s participating
     * in this telephony conference.
     *
     * @param call the <tt>Call</tt> to add to the list of <tt>Call</tt>s
     * participating in this telephony conference
     * @return <tt>true</tt> if the list of <tt>Call</tt>s participating in this
     * telephony conference changed as a result of the method call; otherwise,
     * <tt>false</tt>
     * @throws NullPointerException if <tt>call</tt> is <tt>null</tt>
     */
    boolean addCall(Call call)
    {
        if (call == null)
            throw new NullPointerException("call");

        synchronized (callsSyncRoot)
        {
            if (mutableCalls.contains(call))
                return false;

            /*
             * Implement the List of Calls participating in this telephony
             * conference as a copy-on-write storage in order to optimize the
             * getCalls method which is likely to be executed much more often
             * than the addCall and removeCall methods.
             */
            List<Call> newMutableCalls = new ArrayList<Call>(mutableCalls);

            if (newMutableCalls.add(call))
            {
                mutableCalls = newMutableCalls;
                immutableCalls = Collections.unmodifiableList(mutableCalls);
            }
            else
                return false;
        }

        callAdded(call);
        return true;
    }

    /**
     * Adds a <tt>CallChangeListener</tt> to the <tt>Call</tt>s participating in
     * this telephony conference. The method is a convenience that takes on the
     * responsibility of tracking the <tt>Call</tt>s that get added/removed
     * to/from this telephony conference.
     *
     * @param listener the <tt>CallChangeListner</tt> to be added to the
     * <tt>Call</tt>s participating in this telephony conference
     * @throws NullPointerException if <tt>listener</tt> is <tt>null</tt>
     */
    public void addCallChangeListener(CallChangeListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        else
        {
            synchronized (callChangeListeners)
            {
                if (!callChangeListeners.contains(listener))
                    callChangeListeners.add(listener);
            }
        }
    }

    /**
     * Adds {@link #callPeerConferenceListener} to the <tt>CallPeer</tt>s
     * associated with a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to whose associated <tt>CallPeer</tt>s
     * <tt>callPeerConferenceListener</tt> is to be added
     */
    private void addCallPeerConferenceListener(Call call)
    {
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
        {
            callPeerIter.next().addCallPeerConferenceListener(
                    callPeerConferenceListener);
        }
    }

    /**
     * Adds a <tt>CallPeerConferenceListener</tt> to the <tt>CallPeer</tt>s
     * associated with the <tt>Call</tt>s participating in this telephony
     * conference. The method is a convenience that takes on the responsibility
     * of tracking the <tt>Call</tt>s that get added/removed to/from this
     * telephony conference and the <tt>CallPeer</tt> that get added/removed
     * to/from these <tt>Call</tt>s.
     *
     * @param listener the <tt>CallPeerConferenceListener</tt> to be added to
     * the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating
     * in this telephony conference
     * @throws NullPointerException if <tt>listener</tt> is <tt>null</tt>
     */
    public void addCallPeerConferenceListener(
            CallPeerConferenceListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        else
        {
            synchronized (callPeerConferenceListeners)
            {
                if (!callPeerConferenceListeners.contains(listener))
                    callPeerConferenceListeners.add(listener);
            }
        }
    }

    /**
     * Notifies this <tt>CallConference</tt> that a specific <tt>Call</tt> has
     * been added to the list of <tt>Call</tt>s participating in this telephony
     * conference.
     *
     * @param call the <tt>Call</tt> which has been added to the list of
     * <tt>Call</tt>s participating in this telephony conference
     */
    protected void callAdded(Call call)
    {
        call.addCallChangeListener(callChangeListener);
        addCallPeerConferenceListener(call);

        /*
         * Update the conferenceFocus state. Because the public
         * setConferenceFocus method allows forcing a specific value on the
         * state in question and because it does not sound right to have the
         * adding of a Call set conferenceFocus to false, only update it if the
         * new conferenceFocus value is true,
         */
        boolean conferenceFocus = isConferenceFocus(getCalls());

        if (conferenceFocus)
            setConferenceFocus(conferenceFocus);

        firePropertyChange(CALLS, null, call);
    }

    /**
     * Notifies this <tt>CallConference</tt> that a specific <tt>Call</tt> has
     * been removed from the list of <tt>Call</tt>s participating in this
     * telephony conference.
     *
     * @param call the <tt>Call</tt> which has been removed from the list of
     * <tt>Call</tt>s participating in this telephony conference
     */
    protected void callRemoved(Call call)
    {
        call.removeCallChangeListener(callChangeListener);
        removeCallPeerConferenceListener(call);

        /*
         * Update the conferenceFocus state. Following the line of thinking
         * expressed in the callAdded method, only update it if the new
         * conferenceFocus value is false.
         */
        boolean conferenceFocus = isConferenceFocus(getCalls());

        if (!conferenceFocus)
            setConferenceFocus(conferenceFocus);

        firePropertyChange(CALLS, call, null);
    }

    /**
     * Notifies this telephony conference that the <tt>CallState</tt> of a
     * <tt>Call</tt> has changed.
     *
     * @param ev a <tt>CallChangeEvent</tt> which specifies the <tt>Call</tt>
     * which had its <tt>CallState</tt> changed and the old and new
     * <tt>CallState</tt>s of that <tt>Call</tt>
     */
    private void callStateChanged(CallChangeEvent ev)
    {
        Call call = ev.getSourceCall();

        if (containsCall(call))
        {
            try
            {
                // Forward the CallChangeEvent to the callChangeListeners.
                for (CallChangeListener l : getCallChangeListeners())
                    l.callStateChanged(ev);
            }
            finally
            {
                if (CallChangeEvent.CALL_STATE_CHANGE
                        .equals(ev.getPropertyName())
                    && CallState.CALL_ENDED.equals(ev.getNewValue()))
                {
                    /*
                     * Should not be vital because Call will remove itself.
                     * Anyway, do it for the sake of completeness.
                     */
                    removeCall(call);
                }
            }
        }
    }

    /**
     * Notifies this <tt>CallConference</tt> that the value of its
     * <tt>conferenceFocus</tt> property has changed from a specific old value
     * to a specific new value.
     *
     * @param oldValue the value of the <tt>conferenceFocus</tt> property of
     * this instance before the change
     * @param newValue the value of the <tt>conferenceFocus</tt> property of
     * this instance after the change
     */
    protected void conferenceFocusChanged(boolean oldValue, boolean newValue)
    {
        firePropertyChange(Call.CONFERENCE_FOCUS, oldValue, newValue);
    }

    /**
     * Determines whether a specific <tt>Call</tt> is participating in this
     * telephony conference.
     *
     * @param call the <tt>Call</tt> which is to be checked whether it is
     * participating in this telephony conference
     * @return <tt>true</tt> if the specified <tt>call</tt> is participating in
     * this telephony conference
     */
    public boolean containsCall(Call call)
    {
        synchronized (callsSyncRoot)
        {
            return mutableCalls.contains(call);
        }
    }

    /**
     * Gets the list of <tt>CallChangeListener</tt>s added to the <tt>Call</tt>s
     * participating in this telephony conference via
     * {@link #addCallChangeListener(CallChangeListener)}.
     *
     * @return the list of <tt>CallChangeListener</tt>s added to the
     * <tt>Call</tt>s participating in this telephony conference via
     * {@link #addCallChangeListener(CallChangeListener)}
     */
    private CallChangeListener[] getCallChangeListeners()
    {
        synchronized (callChangeListeners)
        {
            return
                callChangeListeners.toArray(
                        new CallChangeListener[callChangeListeners.size()]);
        }
    }

    /**
     * Gets the number of <tt>Call</tt>s that are participating in this
     * telephony conference.
     *
     * @return the number of <tt>Call</tt>s that are participating in this
     * telephony conference
     */
    public int getCallCount()
    {
        synchronized (callsSyncRoot)
        {
            return mutableCalls.size();
        }
    }

    /**
     * Gets the list of <tt>CallPeerConferenceListener</tt>s added to the
     * <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating in
     * this telephony conference via
     * {@link #addCallPeerConferenceListener(CallPeerConferenceListener)}.
     *
     * @return the list of <tt>CallPeerConferenceListener</tt>s added to the
     * <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating in
     * this telephony conference via
     * {@link #addCallPeerConferenceListener(CallPeerConferenceListener)}
     */
    private CallPeerConferenceListener[] getCallPeerConferenceListeners()
    {
        synchronized (callPeerConferenceListeners)
        {
            return
                callPeerConferenceListeners.toArray(
                        new CallPeerConferenceListener[
                                callPeerConferenceListeners.size()]);
        }
    }

    /**
     * Gets the number of <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in this telephony conference.
     *
     * @return the number of <tt>CallPeer</tt>s associated with the
     * <tt>Call</tt>s participating in this telephony conference
     */
    public int getCallPeerCount()
    {
        int callPeerCount = 0;

        for (Call call : getCalls())
            callPeerCount += call.getCallPeerCount();
        return callPeerCount;
    }

    /**
     * Gets a list of the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in this telephony conference.
     *
     * @return a list of the <tt>CallPeer</tt>s associated with the
     * <tt>Call</tt>s participating in this telephony conference
     */
    public List<CallPeer> getCallPeers()
    {
        List<CallPeer> callPeers = new ArrayList<CallPeer>();

        getCallPeers(callPeers);
        return callPeers;
    }

    /**
     * Adds the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in this telephony conference into a specific <tt>List</tt>.
     *
     * @param callPeers a <tt>List</tt> into which the <tt>CallPeer</tt>s
     * associated with the <tt>Call</tt>s participating in this telephony
     * conference are to be added
     */
    protected void getCallPeers(List<CallPeer> callPeers)
    {
        for (Call call : getCalls())
        {
            Iterator<? extends CallPeer> callPeerIt = call.getCallPeers();

            while (callPeerIt.hasNext())
                callPeers.add(callPeerIt.next());
        }
    }

    /**
     * Gets the list of <tt>Call</tt> participating in this telephony
     * conference.
     *
     * @return the list of <tt>Call</tt>s participating in this telephony
     * conference. An empty array of <tt>Call</tt> element type is returned if
     * there are no <tt>Call</tt>s in this telephony conference-related state.
     */
    public List<Call> getCalls()
    {
        synchronized (callsSyncRoot)
        {
            return immutableCalls;
        }
    }

    /**
     * Determines whether the local peer/user associated with this instance and
     * represented by the <tt>Call</tt>s participating into it is acting as a
     * conference focus.
     *
     * @return <tt>true</tt> if the local peer/user associated by this instance
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    public boolean isConferenceFocus()
    {
        return conferenceFocus;
    }

    /**
     * Determines whether the current state of this instance suggests that the
     * telephony conference it represents has ended. Iterates over the
     * <tt>Call</tt>s participating in this telephony conference and looks for a
     * <tt>Call</tt> which is not in the {@link CallState#CALL_ENDED} state.
     *
     * @return <tt>true</tt> if the current state of this instance suggests that
     * the telephony conference it represents has ended; otherwise,
     * <tt>false</tt>
     */
    public boolean isEnded()
    {
        for (Call call : getCalls())
        {
            if (!CallState.CALL_ENDED.equals(call.getCallState()))
                return false;
        }
        return true;
    }

    /**
     * Determines whether the telephony conference represented by this instance
     * is utilizing the Jitsi Videobridge server-side telephony conferencing
     * technology.
     *
     * @return <tt>true</tt> if the telephony conference represented by this
     * instance is utilizing the Jitsi Videobridge server-side telephony
     * conferencing technology
     */
    public boolean isJitsiVideobridge()
    {
        return jitsiVideobridge;
    }

    /**
     * Notifies this telephony conference that a
     * <tt>CallPeerConferenceEvent</tt> was fired by a <tt>CallPeer</tt>
     * associated with a <tt>Call</tt> participating in this telephony
     * conference. Forwards the specified <tt>CallPeerConferenceEvent</tt> to
     * {@link #callPeerConferenceListeners}.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> which was fired
     */
    private void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
    {
        int eventID = ev.getEventID();

        for (CallPeerConferenceListener l : getCallPeerConferenceListeners())
        {
            switch (eventID)
            {
            case CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED:
                l.conferenceFocusChanged(ev);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
                l.conferenceMemberAdded(ev);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
                l.conferenceMemberRemoved(ev);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_ERROR_RECEIVED:
                l.conferenceMemberErrorReceived(ev);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported CallPeerConferenceEvent eventID.");
            }
        }
    }

    /**
     * Notifies this telephony conference about a specific
     * <tt>CallPeerEvent</tt> i.e. that a <tt>CallPeer</tt> was either added to
     * or removed from a <tt>Call</tt>.
     *
     * @param ev a <tt>CallPeerEvent</tt> which specifies the <tt>CallPeer</tt>
     * which was added or removed and the <tt>Call</tt> to which it was added or
     * from which is was removed
     */
    private void onCallPeerEvent(CallPeerEvent ev)
    {
        Call call = ev.getSourceCall();

        if (containsCall(call))
        {
            /*
             * Update the conferenceFocus state. Following the line of thinking
             * expressed in the callAdded and callRemoved methods, only update
             * it if the new conferenceFocus value is in accord with the
             * expectations.
             */
            int eventID = ev.getEventID();
            boolean conferenceFocus = isConferenceFocus(getCalls());

            switch (eventID)
            {
            case CallPeerEvent.CALL_PEER_ADDED:
                if (conferenceFocus)
                    setConferenceFocus(conferenceFocus);
                break;
            case CallPeerEvent.CALL_PEER_REMOVED:
                if (!conferenceFocus)
                    setConferenceFocus(conferenceFocus);
                break;
            default:
                /*
                 * We're interested in the adding and removing of CallPeers
                 * only.
                 */
                break;
            }

            try
            {
                // Forward the CallPeerEvent to the callChangeListeners.
                for (CallChangeListener l : getCallChangeListeners())
                {
                    switch (eventID)
                    {
                    case CallPeerEvent.CALL_PEER_ADDED:
                        l.callPeerAdded(ev);
                        break;
                    case CallPeerEvent.CALL_PEER_REMOVED:
                        l.callPeerRemoved(ev);
                        break;
                    default:
                        break;
                    }
                }
            }
            finally
            {
                /*
                 * Add/remove the callPeerConferenceListener to/from the source
                 * CallPeer (for the purposes of the
                 * addCallPeerConferenceListener method of this CallConference).
                 */
                CallPeer callPeer = ev.getSourceCallPeer();

                switch (eventID)
                {
                case CallPeerEvent.CALL_PEER_ADDED:
                    callPeer.addCallPeerConferenceListener(
                            callPeerConferenceListener);
                    break;
                case CallPeerEvent.CALL_PEER_REMOVED:
                    callPeer.removeCallPeerConferenceListener(
                            callPeerConferenceListener);
                    break;
                default:
                    break;
                }
            }
        }
    }

    /**
     * Removes a specific <tt>Call</tt> from the list of <tt>Call</tt>s
     * participating in this telephony conference.
     *
     * @param call the <tt>Call</tt> to remove from the list of <tt>Call</tt>s
     * participating in this telephony conference
     * @return <tt>true</tt> if the list of <tt>Call</tt>s participating in this
     * telephony conference changed as a result of the method call; otherwise,
     * <tt>false</tt>
     */
    boolean removeCall(Call call)
    {
        if (call == null)
            return false;

        synchronized (callsSyncRoot)
        {
            if (!mutableCalls.contains(call))
                return false;

            /*
             * Implement the List of Calls participating in this telephony
             * conference as a copy-on-write storage in order to optimize the
             * getCalls method which is likely to be executed much more often
             * than the addCall and removeCall methods.
             */
            List<Call> newMutableCalls = new ArrayList<Call>(mutableCalls);

            if (newMutableCalls.remove(call))
            {
                mutableCalls = newMutableCalls;
                immutableCalls = Collections.unmodifiableList(mutableCalls);
            }
            else
                return false;
        }

        callRemoved(call);
        return true;
    }

    /**
     * Removes a <tt>CallChangeListener</tt> from the <tt>Call</tt>s
     * participating in this telephony conference.
     *
     * @param listener the <tt>CallChangeListener</tt> to be removed from the
     * <tt>Call</tt>s participating in this telephony conference
     * @see #addCallChangeListener(CallChangeListener)
     */
    public void removeCallChangeListener(CallChangeListener listener)
    {
        if (listener != null)
        {
            synchronized (callChangeListeners)
            {
                callChangeListeners.remove(listener);
            }
        }
    }

    /**
     * Removes {@link #callPeerConferenceListener} from the <tt>CallPeer</tt>s
     * associated with a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> from whose associated <tt>CallPeer</tt>s
     * <tt>callPeerConferenceListener</tt> is to be removed
     */
    private void removeCallPeerConferenceListener(Call call)
    {
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
        {
            callPeerIter.next().removeCallPeerConferenceListener(
                    callPeerConferenceListener);
        }
    }

    /**
     * Removes a <tt>CallPeerConferenceListener</tt> from the <tt>CallPeer</tt>s
     * associated with the <tt>Call</tt>s participating in this telephony
     * conference.
     *
     * @param listener the <tt>CallPeerConferenceListener</tt> to be removed
     * from the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in this telephony conference
     * @see #addCallPeerConferenceListener(CallPeerConferenceListener)
     */
    public void removeCallPeerConferenceListener(
            CallPeerConferenceListener listener)
    {
        if (listener != null)
        {
            synchronized (callPeerConferenceListeners)
            {
                callPeerConferenceListeners.remove(listener);
            }
        }
    }

    /**
     * Sets the indicator which determines whether the local peer represented by
     * this instance and the <tt>Call</tt>s participating in it is acting as a
     * conference focus (and thus may, for example, need to send the
     * corresponding parameters in its outgoing signaling).
     *
     * @param conferenceFocus <tt>true</tt> if the local peer represented by
     * this instance and the <tt>Call</tt>s participating in it is to act as a
     * conference focus; otherwise, <tt>false</tt>
     */
    public void setConferenceFocus(boolean conferenceFocus)
    {
        if (this.conferenceFocus != conferenceFocus)
        {
            boolean oldValue = isConferenceFocus();

            this.conferenceFocus = conferenceFocus;

            boolean newValue = isConferenceFocus();

            if (oldValue != newValue)
                conferenceFocusChanged(oldValue, newValue);
        }
    }
}
