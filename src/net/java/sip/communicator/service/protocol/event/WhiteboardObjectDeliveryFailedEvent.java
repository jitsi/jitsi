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
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 * <tt>WhiteboardObjectDeliveredEvent</tt>s are used to report that delivery of
 * a whiteboardObject has failed.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardObjectDeliveryFailedEvent
        extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The contact that this whiteboard object has been sent to.
     */
    private Contact to = null;

    /**
     * Set when no other error code can describe the exception that occurred.
     */
    public static final int UNKNOWN_ERROR = 1;

    /**
     * Set when delivery fails due to a failure in network communications or
     * a transport error.
     */
    public static final int NETWORK_FAILURE = 2;

    /**
     * Set to indicate that delivery has failed because the provider was not
     * registered.
     */
    public static final int PROVIDER_NOT_REGISTERED = 3;

    /**
     * Set when delivery fails for implementation specific reasons.
     */
    public static final int INTERNAL_ERROR = 4;

    /**
     * Set when delivery fails because we're trying to send a whiteboard object
     * to a contact that is currently offline and the server does not support
     * offline whiteboard objects.
     */
    public static final int OFFLINE_MESSAGES_NOT_SUPPORTED = 5;

    /**
     * An error code indicating the reason for the failure of this delivery.
     */
    private int errorCode = UNKNOWN_ERROR;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * The whiteboard object delivery of which has failed.
     */
    private WhiteboardObject obj = null;

    /**
     * Creates a <tt>WhiteboardObjectDeliveryFailedEvent</tt> indicating failure
     * of delivery of the <tt>obj</tt> WhiteboardObject to the specified
     * <tt>to</tt> contact.
     *
     * @param source the <tt>WhiteboardSession</tt> where the failure has
     * occcurred.
     * @param obj the <tt>WhiteboardObject</tt> the white-board object.
     * @param to the <tt>Contact</tt> that this WhiteboardObject was sent to.
     * @param errorCode an errorCode indicating the reason for the failure.
     * @param timestamp the exact Date when it was determined that delivery
     * had failed.
     */
    public WhiteboardObjectDeliveryFailedEvent( WhiteboardSession source,
                                                WhiteboardObject obj,
                                                Contact to,
                                                int errorCode,
                                                Date timestamp )
    {
        super(source);
        this.obj = obj;
        this.to = to;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that the source (failed)
     * <tt>WhiteboardObject</tt> was sent to.
     *
     * @return a reference to the <tt>Contact</tt> that the source failed
     * <tt>WhiteboardObject</tt> was sent to.
     */
    public Contact getDestinationContact()
    {
        return to;
    }

    /**
     * Returns an error code describing the reason for the failure of the
     * white-board object delivery.
     *
     * @return an error code describing the reason for the failure of the
     * white-board object delivery.
     */
    public int getErrorCode()
    {
        return errorCode;
    }


    /**
     * A timestamp indicating the exact date when the event ocurred (in this
     * case it is the moment when it was determined that whiteboardObject
     * delivery has failed).
     *
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the WhiteboardObject that triggered this event
     *
     * @return the <tt>WhiteboardObject</tt> that triggered this event.
     */
    public WhiteboardObject getSourceWhiteboardObject()
    {
        return obj;
    }

}
