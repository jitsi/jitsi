/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.*;

/**
 * Listener receiving events for incoming messages
 * that need processing
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
}
