/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sip.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Listener receiving events for incoming messages that need processing.
 * 
 * @author Damian Minkov
 */
public interface SipMessageProcessor 
{
    /**
     * Process the incoming message
     * @param requestEvent the incoming event holding the message
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processMessage(RequestEvent requestEvent);
    
    /**
     * Process the responses of sent messages
     * @param responseEvent the incoming event holding the response
     * @param sentMessages map containing sent messages
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processResponse(ResponseEvent responseEvent,
                                   Map<String, Message> sentMessages);
    
    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction}handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param sentMessages map containing sent messages
     * @param timeoutEvent the timeoutEvent received indicating either the
     * message retransmit or transaction timed out.
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent,
                                  Map<String, Message> sentMessages);
}
