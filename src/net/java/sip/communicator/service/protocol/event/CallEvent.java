/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * An event class representing that an incoming or an outgoing call has been
 * created. The event id indicates the exact reason for this event.
 *
 * @author Emil Ivov
 */
public class CallEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event id value indicating that this event has been triggered as a
     * result of an outgoing call.
     */
    public static final int CALL_INITIATED = 1;

    /**
     * An event id value indicating that this event has been triggered as a
     * result of an incoming call.
     */
    public static final int CALL_RECEIVED  = 2;

    /**
     * An event id value indicating that this event has been triggered as a
     * result of a call being ended (all its peers have left).
     */
    public static final int CALL_ENDED  = 3;


    /**
     * Determines whether this event has been fired to indicate an incoming or
     * an outgoing call.
     */
    private final int eventID;

    /**
     * The media types supported by this call, if information is
     * available.
     */
    private List<MediaType> mediaTypes = new ArrayList<MediaType>();

    /**
     * Creates an event instance indicating that an incoming/outgoing call
     * has been created
     *
     * @param call the call that triggered this event.
     * @param eventID determines whether this is an incoming or an outgoing
     * call.
     */
    public CallEvent(Call call, int eventID)
    {
        super(call);
        this.eventID = eventID;
    }

    /**
     * Returns the <tt>Call</tt> that trigered this event.
     *
     * @return the <tt>Call</tt> that trigered this event.
     */
    public Call getSourceCall()
    {
        return (Call)getSource();
    }

    /**
     * Returns an event ID int indicating whether this event was triggered by
     * an outgoing or an incoming call.
     *
     * @return on of the CALL_XXX static member ints.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns a String representation of this CallEvent.
     *
     * @return  A a String representation of this CallEvent.
     */
    public String toString()
    {
        return "CallEvent:[ id=" + getEventID()
            + " Call=" + getSourceCall() + "]";
    }

    /**
     * Return the media types supported by this call, if information is
     * available. It can be empty list if information wasn't provided for this
     * event and call.
     * @return the supported media types of current call.
     */
    public List<MediaType> getMediaTypes()
    {
        return mediaTypes;
    }

    /**
     * Update media types for the event.
     * @param mediaTypes the new media types.
     */
    public void setMediaTypes(List<MediaType> mediaTypes)
    {
        this.mediaTypes = mediaTypes;
    }
}
