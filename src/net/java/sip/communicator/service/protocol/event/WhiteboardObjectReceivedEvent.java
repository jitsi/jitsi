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
 * <tt>WhiteboardObjectReceivedEvent</tt> indicates reception of a new
 * <tt>WhiteboardObject</tt> in the corresponding whiteboard session.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardObjectReceivedEvent
  extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The contact that has sent this wbObject.
     */
    private Contact from = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * The whiteboard object that has just been received.
     */
    private WhiteboardObject obj = null;

    /**
     * Creates a <tt>WhiteboardObjectReceivedEvent</tt>
     * representing reception of the <tt>source</tt> WhiteboardObject
     * received from the specified <tt>from</tt> contact.
     *
     * @param source the <tt>WhiteboardSession</tt> that the object has been
     *               received in.
     * @param obj the <tt>WhiteboardObject</tt> whose reception this event
     *            represents.
     * @param from the <tt>Contact</tt> that has sent this WhiteboardObject.
     * @param timestamp the exact date when the event ocurred.
     */
    public WhiteboardObjectReceivedEvent (WhiteboardSession source,
      WhiteboardObject obj, Contact from, Date timestamp)
    {
        super (source);
        this.obj = obj;
        this.from = from;
        this.timestamp = timestamp;
    }

    /**
     * Returns the source white-board session, to which the received object
     * belongs.
     *
     * @return the source white-board session, to which the received object
     * belongs
     */
    public WhiteboardSession getSourceWhiteboardSession()
    {
        return (WhiteboardSession) getSource();
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has sent the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>Contact</tt> that has sent the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     */
    public Contact getSourceContact ()
    {
        return from;
    }

    /**
     * Returns the WhiteboardObject that triggered this event
     *
     * @return the <tt>WhiteboardObject</tt> that triggered this event.
     */
    public WhiteboardObject getSourceWhiteboardObject ()
    {
        return obj;
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     *
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp ()
    {
        return timestamp;
    }
}
