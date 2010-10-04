/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.event;

import java.util.*;

import net.java.sip.communicator.service.history.*;

/**
 * The <tt>HistoryQueryStatusEvent</tt> is triggered each time a
 * <tt>HistoryQuery</tt> changes its status. Possible statuses are:
 * QUERY_COMPLETED, QUERY_CANCELED and QUERY_ERROR.
 *
 * @author Yana Stamcheva
 */
public class HistoryQueryStatusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a query has been completed.
     */
    public static final int QUERY_COMPLETED = 0;

    /**
     * Indicates that a query has been canceled.
     */
    public static final int QUERY_CANCELED = 1;

    /**
     * Indicates that a query has been stopped because of an error.
     */
    public static final int QUERY_ERROR = 2;

    /**
     * Indicates the type of this event.
     */
    private final int eventType;

    /**
     * Creates a <tt>HistoryQueryStatusEvent</tt> by specifying the source
     * <tt>HistoryQuery</tt> and the <tt>eventType</tt> indicating why initially
     * this event occurred.
     * @param source the <tt>HistoryQuery</tt> this event is about
     * @param eventType the type of the event. One of the QUERY_XXX constants
     * defined in this class
     */
    public HistoryQueryStatusEvent( HistoryQuery source,
                                    int eventType)
    {
        super(source);

        this.eventType = eventType;
    }

    /**
     * Returns the <tt>HistoryQuery</tt> that triggered this event.
     * @return the <tt>HistoryQuery</tt> that triggered this event
     */
    public HistoryQuery getQuerySource()
    {
        return (HistoryQuery) source;
    }

    /**
     * Returns the type of this event.
     * @return the type of this event
     */
    public int getEventType()
    {
        return eventType;
    }
}
