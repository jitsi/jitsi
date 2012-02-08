/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * This class represents group of several calls. It is used to make
 * cross-protocol conference calls.
 *
 * @author Sebastien Vincent
 */
public class CallGroup
    implements CallChangeListener
{
    /**
     * List of <tt>Call</tt>s.
     */
    private List<Call> calls = new ArrayList<Call>();

    /**
     * Synchronization object.
     */
    private final Object syncRoot = new Object();

    /**
     * Returns list of existing <tt>Call</tt>s in this <tt>CallGroup</tt>.
     *
     * @return list of existing <tt>Call</tt>s in this <tt>CallGroup</tt>.
     */
    public List<Call> getCalls()
    {
        return calls;
    }

    /**
     * Fires <tt>CallGroupEvent</tt>.
     *
     * @param call source <tt>Call</tt>
     * @param eventID event ID
     */
    public void fireCallGroupEvent(Call call, int eventID)
    {
        CallGroupEvent evt = new CallGroupEvent(call, eventID);

        for(Call c : calls)
        {
            if(c == call)
                continue;

            switch(eventID)
            {
            case CallGroupEvent.CALLGROUP_CALL_ADDED:
                c.callAdded(evt);
                call.callAdded(new CallGroupEvent(c, eventID));
                break;
            case CallGroupEvent.CALLGROUP_CALL_REMOVED:
                c.callRemoved(evt);
                call.callRemoved(new CallGroupEvent(c, eventID));
                break;
            default:
                break;
            }
        }
    }

    /**
     * Adds a call.
     *
     * @param call call to add
     */
    public void addCall(Call call)
    {
        synchronized(syncRoot)
        {
            if(!calls.contains(call))
            {
                call.addCallChangeListener(this);
                call.setCallGroup(this);
                calls.add(call);
            }
        }
    }

    /**
     * Removes a call.
     *
     * @param call call to remove
     */
    public void removeCall(Call call)
    {
        synchronized(syncRoot)
        {
            if(calls.contains(call))
            {
                call.removeCallChangeListener(this);
                calls.remove(call);
                call.setCallGroup(null);

            }
        }
    }

    /**
     * Indicates that a new call peer has joined the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerAdded(CallPeerEvent evt)
    {
        /* not used */
    }

    /**
     * Indicates that a call peer has left the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        /* not used */
    }

    /**
     * Indicates that a change has occurred in the state of the source call.
     *
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        Call call = evt.getSourceCall();
        if(evt.getEventType() == CallChangeEvent.CALL_STATE_CHANGE)
        {
            CallState state = (CallState)evt.getNewValue();

            if(evt.getNewValue() == evt.getOldValue())
                return;

            if(state == CallState.CALL_ENDED)
            {
                fireCallGroupEvent(call, CallGroupEvent.CALLGROUP_CALL_REMOVED);
            }
            else if(state == CallState.CALL_IN_PROGRESS)
            {
                fireCallGroupEvent(call, CallGroupEvent.CALLGROUP_CALL_ADDED);
            }
        }
    }
}
