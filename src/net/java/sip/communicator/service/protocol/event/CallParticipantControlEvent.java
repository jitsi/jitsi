/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The CallParticipantControlEvent is issued by the PhoneUIService as a result
 * of a user request to modify the way a CallParticipant is associated with a
 * call, or in other words "Answer" the incoming call of a CallParticipant or
 * "Hangup" and thus and the participation of a CallParticipant in a call. The
 * source of the event is considered to be the CallParticipant that is being
 * controlled. As the event might also be used to indicate a user request to
 * transfer a given participant to a different number, the calss also contains
 * a targetURI field, containing the adress that a client is being redirected to
 * (the target uri might also have slightly different meanings depending on the
 * method dispatching the event).
 * @author Emil Ivov
 *
 */
public class CallParticipantControlEvent
    extends java.util.EventObject
{
    private String targetURI = null;

    /**
     * Creates a new event instance with the specifieed source CallParticipant
     * and targetURI, if any.
     * @param source the CallParticipant that this event is pertaining to.
     * @param targetURI the URI to transfer to if this is a "Transfer" event
     * or null otherwise.
     */
    public CallParticipantControlEvent(CallParticipant source, String targetURI)
    {
        super(source);
        this.targetURI = targetURI;
    }

    /**
     * Returns the CallParticipant that this event is pertaining to.
     * @return the CallParticipant that this event is pertaining to.
     */
    public CallParticipant getAssociatedCallparticipant()
    {
        return (CallParticipant) source;
    }

    /**
     * Returns the target URI if this is event is triggered by a transfer
     * request or null if not.
     * @return null or a tranfer URI.
     */
    public String getTargetURI()
    {
        return targetURI;
    }
}
