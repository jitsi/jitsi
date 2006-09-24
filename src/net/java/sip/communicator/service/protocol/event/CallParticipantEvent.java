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
public class CallParticipantEvent
    extends EventObject
{
    /**
     * The call that the source call participant is associated with.
     */
    private Call sourceCall = null;

    /**
     * An event id value indicating that this event is about the fact that
     * the source call participant has joined the source call.
     */
    public static final int CALL_PARTICIPANT_ADDED = 1;

    /**
     * An event id value indicating that this event is about the fact that
     * the source call participant has left the source call.
     */
    public static final int CALL_PARTICIPANT_REMVOVED = 2;

    /**
     * The id indicating the type of this event.
     */
    private int eventID = -1;

    /**
     * Creates a call participant event instance indicating that an event with
     * id <tt>eventID</tt> has happened to <tt>sourceCallParticipant</tt> in
     * <tt>sourceCall</tt>
     * @param sourceCallParticipant the call participant that this event is
     * about.
     * @param sourceCall the call that the source call participant is associated
     * with.
     * @param eventID one of the CALL_PARTICIPANT_XXX member ints indicating
     * the type of this event.
     */
    public CallParticipantEvent(CallParticipant sourceCallParticipant,
                                Call            sourceCall,
                                int             eventID)
    {
        super(sourceCallParticipant);
        this.sourceCall = sourceCall;
        this.eventID = eventID;
    }

    /**
     * Returnst one of the CALL_PARTICIPANT_XXX member ints indicating
     * the type of this event.
     * @return one of the CALL_PARTICIPANT_XXX member ints indicating
     * the type of this event.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns the call that the source call participant is associated with.
     *
     * @return a reference to the <tt>Call</tt> that the source call participant
     * is associated with.
     */
    public Call getSourceCall()
    {
        return sourceCall;
    }

    /**
     * Returns the  source call participant (the one that this event is about).
     *
     * @return a reference to the source <tt>CallParticipant</tt> instance.
     */
    public CallParticipant getSourceCallParticipant()
    {
        return (CallParticipant)getSource();
    }

    /**
     * Returns a String representation of this <tt>CallParticipantEvent</tt>.
     *
     * @return  a String representation of this <tt>CallParticipantEvent</tt>.
     */
    public String toString()
    {

        return "CallParticipantEvent: ID=" + getEventID()
               + " source participant=" + getSourceCallParticipant()
               + " source call=" + getSourceCall();
    }

}
