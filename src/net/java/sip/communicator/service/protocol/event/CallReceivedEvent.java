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
 * A class representing the event of a call reception.
 *
 * @author Emil Ivov
 */
public class CallReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructor.
     *
     * @param call the <tt>Call</tt> received
     */
    public CallReceivedEvent(Call call)
    {
        super(call);
    }

    /**
     * Returns the received call.
     *
     * @return received <tt>Call</tt>
     */
    public Call getCall()
    {
        return (Call) getSource();
    }
}
