/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of <tt>OperationSetBasicTelephony</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on implementation-specific details.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetBasicTelephony
    implements OperationSetBasicTelephony
{
    private static final Logger logger =
        Logger.getLogger(AbstractOperationSetBasicTelephony.class);

    /**
     * A list of listeners registered for call events.
     */
    private final List<CallListener> callListeners = new Vector<CallListener>();

    /**
     * Registers <tt>listener</tt> with this provider so that it
     * could be notified when incoming calls are received.
     *
     * @param listener the listener to register with this provider.
     */
    public void addCallListener(CallListener listener)
    {
        synchronized(callListeners)
        {
            if (!callListeners.contains(listener))
                callListeners.add(listener);
        }
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred.
     */
    public void fireCallEvent(int eventID, Call sourceCall)
    {
        CallEvent cEvent = new CallEvent(sourceCall, eventID);
        List<CallListener> listeners;

        synchronized (callListeners)
        {
            listeners = new ArrayList<CallListener>(callListeners);
        }

        logger.debug("Dispatching a CallEvent to " + listeners.size()
            + " listeners. event is: " + cEvent);

        for (Iterator<CallListener> listenerIter = listeners.iterator(); listenerIter.hasNext();)
        {
            CallListener listener = listenerIter.next();

            switch (eventID)
            {
            case CallEvent.CALL_INITIATED:
                listener.outgoingCallCreated(cEvent);
                break;
            case CallEvent.CALL_RECEIVED:
                listener.incomingCallReceived(cEvent);
                break;
            case CallEvent.CALL_ENDED:
                listener.callEnded(cEvent);
                break;
            }
        }
    }

    /**
     * Removes the <tt>listener</tt> from the list of call listeners.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallListener(CallListener listener)
    {
        synchronized(callListeners)
        {
            callListeners.remove(listener);
        }
    }

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallPeer</tt>.
     * <p>
     * The default implementation does nothing.
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>peer</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallPeer peer, boolean mute)
    {

        /*
         * While throwing UnsupportedOperationException may be a possible
         * approach, putOnHold/putOffHold just do nothing when not supported so
         * this implementation takes inspiration from them.
         */
    }
}
