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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Emil Ivov
 */
public class CallPeerEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The call that the source call peer is associated with.
     */
    private final Call sourceCall;

    /**
     * An event id value indicating that this event is about the fact that
     * the source call peer has joined the source call.
     */
    public static final int CALL_PEER_ADDED = 1;

    /**
     * An event id value indicating that this event is about the fact that
     * the source call peer has left the source call.
     */
    public static final int CALL_PEER_REMOVED = 2;

    /**
     * The id indicating the type of this event.
     */
    private final int eventID;

    /**
     * Indicates if adding/removing peer should be delayed or not.
     */
    private final boolean delayed;

    /**
     * Creates a call peer event instance indicating that an event with
     * id <tt>eventID</tt> has happened to <tt>sourceCallPeer</tt> in
     * <tt>sourceCall</tt>
     * @param sourceCallPeer the call peer that this event is
     * about.
     * @param sourceCall the call that the source call peer is associated
     * with.
     * @param eventID one of the CALL_PEER_XXX member ints indicating
     * the type of this event.
     */
    public CallPeerEvent(CallPeer sourceCallPeer,
                         Call     sourceCall,
                         int      eventID)
    {
        this(sourceCallPeer, sourceCall, eventID, false);
    }

    /**
     * Creates a call peer event instance indicating that an event with
     * id <tt>eventID</tt> has happened to <tt>sourceCallPeer</tt> in
     * <tt>sourceCall</tt>
     * @param sourceCallPeer the call peer that this event is
     * about.
     * @param sourceCall the call that the source call peer is associated
     * with.
     * @param eventID one of the CALL_PEER_XXX member ints indicating
     * the type of this event.
     * @param delayed initial value for <tt>delayed</tt> property. If the value is
     * true adding/removing peer from GUI will be delayed.
     */
    public CallPeerEvent(CallPeer sourceCallPeer,
                         Call     sourceCall,
                         int      eventID,
                         boolean  delayed)
    {
        super(sourceCallPeer);
        this.sourceCall = sourceCall;
        this.eventID = eventID;
        this.delayed = delayed;
    }

    /**
     * Checks whether the adding/removing of the peer should be delayed or not.
     * @return true if the adding/removing should be delayed from the GUI and
     * false if not.
     */
    public boolean isDelayed()
    {
        return delayed;
    }

    /**
     * Returns one of the CALL_PEER_XXX member ints indicating
     * the type of this event.
     * @return one of the CALL_PEER_XXX member ints indicating
     * the type of this event.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns the call that the source call peer is associated with.
     *
     * @return a reference to the <tt>Call</tt> that the source call peer
     * is associated with.
     */
    public Call getSourceCall()
    {
        return sourceCall;
    }

    /**
     * Returns the  source call peer (the one that this event is about).
     *
     * @return a reference to the source <tt>CallPeer</tt> instance.
     */
    public CallPeer getSourceCallPeer()
    {
        return (CallPeer)getSource();
    }

    /**
     * Returns a String representation of this <tt>CallPeerEvent</tt>.
     *
     * @return  a String representation of this <tt>CallPeerEvent</tt>.
     */
    @Override
    public String toString()
    {
        return "CallPeerEvent: ID=" + getEventID()
               + " source peer=" + getSourceCallPeer()
               + " source call=" + getSourceCall();
    }
}
