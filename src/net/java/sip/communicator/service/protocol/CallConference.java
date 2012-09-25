/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * The <tt>CallChangeListener</tt> which listens to changes in the
     * <tt>Call</tt>s participating in this telephony conference.
     */
    private final CallChangeListener callChangeListener
        = new CallChangeListener()
                {
                    public void callPeerAdded(CallPeerEvent ev)
                    {
                        CallConference.this.callPeerAdded(ev);
                    }

                    public void callPeerRemoved(CallPeerEvent ev)
                    {
                        CallConference.this.callPeerRemoved(ev);
                    }

                    public void callStateChanged(CallChangeEvent ev)
                    {
                        CallConference.this.callStateChanged(ev);
                    }
                };

    /**
     * The list of <tt>Call</tt>s participating in this telephony conference.
     */
    private final List<Call> calls = new LinkedList<Call>();

    /**
     * The indicator which determines whether the local peer represented by this
     * instance and the <tt>Call</tt>s participating in it is acting as a
     * conference focus. The SIP protocol, for example, will add the
     * &quot;isfocus&quot; parameter to the Contact headers of its outgoing
     * signaling if <tt>true</tt>.
     */
    private boolean conferenceFocus = false;

    /**
     * Initializes a new <tt>CallConference</tt> instance.
     */
    public CallConference()
    {
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
     */
    boolean addCall(Call call)
    {
        synchronized (calls)
        {
            if (calls.contains(call) || !calls.add(call))
                return false;
        }

        callAdded(call);
        return true;
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
     * Notifies this telephony conference that a <tt>CallPeer</tt> has been
     * added to a <tt>Call</tt>.
     *
     * @param ev a <tt>CallPeerEvent</tt> which specifies the <tt>CallPeer</tt>
     * which has been added and the <tt>Call</tt> to which it has been added
     */
    private void callPeerAdded(CallPeerEvent ev)
    {
        if (containsCall(ev.getSourceCall()))
        {
            /*
             * Update the conferenceFocus state. Following the line of thinking
             * expressed in the callAdded and callRemoved methods, only update
             * it if the new conferenceFocus value is in accord with the
             * expectations.
             */
            boolean conferenceFocus = isConferenceFocus(getCalls());

            switch (ev.getEventID())
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
        }
    }

    /**
     * Notifies this telephony conference that a <tt>CallPeer</tt> has been
     * removed from a <tt>Call</tt>.
     *
     * @param ev a <tt>CallPeerEvent</tt> which specifies the <tt>CallPeer</tt>
     * which has been removed and the <tt>Call</tt> from which it has been
     * removed
     */
    private void callPeerRemoved(CallPeerEvent ev)
    {
        /*
         * The callPeerAdded method will take into account the eventID of the
         * CallPeerEvent.
         */
        callPeerAdded(ev);
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

        if (containsCall(call) && CallState.CALL_ENDED.equals(ev.getNewValue()))
        {
            /*
             * Should not be vital because Call will remove itself. Anyway, do
             * it for the sake of completeness.
             */
            removeCall(call);
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
        synchronized (calls)
        {
            return calls.contains(call);
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
        synchronized (calls)
        {
            return calls.size();
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
     * Gets the number of <tt>CallPeer</tt>s associated with the <tt>Call</tt>s
     * participating in the telephony conference-related state of a specific
     * <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> for which the number of <tt>CallPeer</tt>s
     * associated with the <tt>Call</tt>s participating in its associated
     * telephony conference-related state
     * @return the number of <tt>CallPeer</tt>s associated with the
     * <tt>Call</tt>s participating in the telephony conference-related state
     * of the specified <tt>call</tt>
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
    public Call[] getCalls()
    {
        synchronized (calls)
        {
            return calls.toArray(new Call[calls.size()]);
        }
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
    public static Call[] getCalls(Call call)
    {
        CallConference conference = call.getConference();

        return
            (conference == null) ? new Call[] { call } : conference.getCalls();
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
    private static boolean isConferenceFocus(Call[] calls)
    {
        boolean conferenceFocus;

        if (calls.length < 1)
            conferenceFocus = false;
        else if (calls.length > 1)
            conferenceFocus = true;
        else
            conferenceFocus = (calls[0].getCallPeerCount() > 1);
        return conferenceFocus;
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
        synchronized (calls)
        {
            if (!calls.remove(call))
                return false;
        }

        callRemoved(call);
        return true;
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
