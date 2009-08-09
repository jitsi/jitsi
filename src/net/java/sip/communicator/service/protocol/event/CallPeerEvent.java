/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public static final int CALL_PEER_REMVOVED = 2;

    /**
     * The id indicating the type of this event.
     */
    private final int eventID;

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
        super(sourceCallPeer);
        this.sourceCall = sourceCall;
        this.eventID = eventID;
    }

    /**
     * Returnst one of the CALL_PEER_XXX member ints indicating
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
    public String toString()
    {

        return "CallPeerEvent: ID=" + getEventID()
               + " source peer=" + getSourceCallPeer()
               + " source call=" + getSourceCall();
    }

}
