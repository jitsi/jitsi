package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Represents an event pertaining to message delivery, reception or failure.
 * @author Emil Ivov
 */
public class MessageEvent
    extends EventObject
{
    public MessageEvent(Object source)
    {
        super(source);
    }
}
