package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Represents an event pertaining to message delivery, reception or failure.
 * @author Emil Ivov
 */
public class MessageEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Construcs a new <tt>MessageEvent</tt> instance.
     *
     * @param source Object on which the Event initially occurred
     */
    public MessageEvent(Object source)
    {
        super(source);
    }
}
