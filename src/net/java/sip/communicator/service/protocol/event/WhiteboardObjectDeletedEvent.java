/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>WhiteboardObjectDeletedEvent</tt> indicates reception of a new
 * <tt>WhiteboardObject</tt> in the corresponding whiteboard session.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardObjectDeletedEvent
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
     * A String that uniquely identifies this WhiteboardObject.
     */
    private String id;

    /**
     * Creates a <tt>WhiteboardObjectReceivedEvent</tt>
     * representing reception of the <tt>source</tt> WhiteboardObject
     * received from the specified <tt>from</tt> contact.
     *
     *
     *
     * @param source the <tt>WhiteboardSession</tt>
     * @param id the identification of the <tt>WhiteboardObject</tt> whose reception
     * this event represents.
     * @param from the <tt>Contact</tt> that has sent this WhiteboardObject.
     * @param timestamp the exact date when the event ocurred.
     */
    public WhiteboardObjectDeletedEvent (WhiteboardSession source,
      String id, Contact from, Date timestamp)
    {
        super (source);
        this.id = id;
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
     * Returns a reference to the <tt>Contact</tt> that has send the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>Contact</tt> that has send the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     */
    public Contact getSourceContact ()
    {
        return from;
    }

    /**
     * Returns the identification of the deleted WhiteboardObject
     * that triggered this event
     *
     * @return the <tt>WhiteboardObject</tt> that triggered this event.
     */
    public String getId ()
    {
        return id;
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp ()
    {
        return timestamp;
    }
}