/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import net.java.sip.communicator.service.protocol.*;

/**
 * A Gibberish implementation of the <tt>ConferenceMember</tt> interface.
 * @author Yana Stamcheva
 */
public class ConferenceMemberGibberishImpl
    extends AbstractConferenceMember
{
    public ConferenceMemberGibberishImpl(CallPeer conferenceFocusCallPeer)
    {
        super(conferenceFocusCallPeer);
    }
}
