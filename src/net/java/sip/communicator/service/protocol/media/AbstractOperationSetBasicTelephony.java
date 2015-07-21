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
package net.java.sip.communicator.service.protocol.media;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.recording.*;

/**
 * Represents a default implementation of <tt>OperationSetBasicTelephony</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on implementation-specific details.
 *
 * @param <T> the implementation specific provider class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt>.
 *
 * @author Lyubomir Marinov
 * @author Emil Ivov
 * @author Dmitri Melnikov
 */
public abstract class AbstractOperationSetBasicTelephony
                                        <T extends ProtocolProviderService>
    implements OperationSetBasicTelephony<T>
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetBasicTelephony</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetBasicTelephony.class);

    /**
     * A list of listeners registered for call events.
     */
    private final List<CallListener> callListeners
        = new ArrayList<CallListener>();

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
     * {@inheritDoc}
     *
     * Forwards to
     * {@link OperationSetBasicTelephony#createCall(Contact, CallConference)}
     * with <tt>null</tt> as the <tt>CallConference</tt> argument.
     */
    public Call createCall(Contact callee)
        throws OperationFailedException
    {
        return createCall(callee, null);
    }

    /**
     * {@inheritDoc}
     *
     * Forwards to
     * {@link OperationSetBasicTelephony#createCall(String, CallConference)}
     * with {@link Contact#getAddress()} as the <tt>String</tt> argument.
     */
    public Call createCall(Contact callee, CallConference conference)
        throws OperationFailedException
    {
        try
        {
            return createCall(callee.getAddress(), conference);
        }
        catch (ParseException pe)
        {
            throw new OperationFailedException(
                    pe.getMessage(),
                    OperationFailedException.ILLEGAL_ARGUMENT,
                    pe);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Forwards to
     * {@link OperationSetBasicTelephony#createCall(String, CallConference)}
     * with <tt>null</tt> as the <tt>CallConference</tt> argument.
     */
    public Call createCall(String uri)
        throws OperationFailedException,
               ParseException
    {
        return createCall(uri, null);
    }

    /**
     * {@inheritDoc}
     *
     * Always throws an exception.
     */
    @Override
    public Call createCall(ConferenceDescription cd, ChatRoom chatRoom)
        throws OperationFailedException
    {
        throw new OperationFailedException("Creating a call with a" +
                " ConferenceDescription is not implemented in " + getClass(),
                OperationFailedException.INTERNAL_ERROR);
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
        fireCallEvent(eventID, sourceCall, null);
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred.
     * @param mediaDirections direction map for media types
     */
    public void fireCallEvent(
            int eventID,
            Call sourceCall,
            Map<MediaType, MediaDirection> mediaDirections)
    {
        fireCallEvent(new CallEvent(sourceCall, eventID, mediaDirections));
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param event the event to dispatch
     */
    public void fireCallEvent(CallEvent event)
    {
        List<CallListener> listeners;

        synchronized (callListeners)
        {
            listeners = new ArrayList<CallListener>(callListeners);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Dispatching a CallEvent to "
                        + listeners.size()
                        + " listeners. The event is: "
                        + event);
        }

        for (CallListener listener : listeners)
        {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Dispatching a CallEvent to "
                        + listener.getClass() + " . The event is: "+ event);

            switch (event.getEventID())
            {
            case CallEvent.CALL_INITIATED:
                listener.outgoingCallCreated(event);
                break;
            case CallEvent.CALL_RECEIVED:
                listener.incomingCallReceived(event);
                break;
            case CallEvent.CALL_ENDED:
                listener.callEnded(event);
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
     * Sets the mute state of the <tt>Call</tt>.
     * <p>
     * Muting audio streams sent from the call is implementation specific
     * and one of the possible approaches to it is sending silence.
     * </p>
     *
     * @param call the <tt>Call</tt> whose mute state is to be set
     * @param mute <tt>true</tt> to mute the call streams being sent to
     * <tt>peers</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(Call call, boolean mute)
    {
        if (call instanceof MediaAwareCall)
            ((MediaAwareCall<?,?,?>) call).setMute(mute);
        else
        {
            /*
             * While throwing UnsupportedOperationException may be a possible
             * approach, putOnHold/putOffHold just do nothing when not supported
             * so this implementation takes inspiration from them.
             */
        }
    }

    /**
     * Creates a new <tt>Recorder</tt> which is to record the specified
     * <tt>Call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>).
     * <p>
     * <tt>AbstractOperationSetBasicTelephony</tt> implements the described
     * functionality for <tt>MediaAwareCall</tt> only; otherwise, does nothing
     * and just returns <tt>null</tt>.
     * </p>
     *
     * @param call the <tt>Call</tt> which is to be recorded by the returned
     * <tt>Recorder</tt> when the latter is started
     * @return a new <tt>Recorder</tt> which is to record the specified
     * <tt>call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>)
     * @throws OperationFailedException if anything goes wrong while creating
     * the new <tt>Recorder</tt> for the specified <tt>call</tt>
     * @see OperationSetBasicTelephony#createRecorder(Call)
     */
    public Recorder createRecorder(Call call)
        throws OperationFailedException
    {
        return
            (call instanceof MediaAwareCall<?, ?, ?>)
                ? ((MediaAwareCall<?, ?, ?>) call).createRecorder()
                : null;
    }
}
