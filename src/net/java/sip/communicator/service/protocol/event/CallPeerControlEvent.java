/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The CallPeerControlEvent is issued by the PhoneUIService as a result
 * of a user request to modify the way a CallPeer is associated with a
 * call, or in other words "Answer" the incoming call of a CallPeer or
 * "Hangup" and thus and the participation of a CallPeer in a call. The
 * source of the event is considered to be the CallPeer that is being
 * controlled. As the event might also be used to indicate a user request to
 * transfer a given call peer to a different number, the class also contains
 * a targetURI field, containing the address that a client is being redirected to
 * (the target uri might also have slightly different meanings depending on the
 * method dispatching the event).
 *
 * @author Emil Ivov
 */
public class CallPeerControlEvent
    extends java.util.EventObject
{
    private final String targetURI;

    /**
     * Creates a new event instance with the specified source CallPeer
     * and targetURI, if any.
     * @param source the CallPeer that this event is pertaining to.
     * @param targetURI the URI to transfer to if this is a "Transfer" event
     * or null otherwise.
     */
    public CallPeerControlEvent(CallPeer source, String targetURI)
    {
        super(source);
        this.targetURI = targetURI;
    }

    /**
     * Returns the CallPeer that this event is pertaining to.
     * @return the CallPeer that this event is pertaining to.
     */
    public CallPeer getAssociatedCallPeer()
    {
        return (CallPeer) source;
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
