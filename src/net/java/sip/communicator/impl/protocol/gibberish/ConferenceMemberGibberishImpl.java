/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
    /**
     * Creates an instance of <tt>ConferenceMemberGibberishImpl</tt> by
     * specifying the parent call peer and the address of the member.
     * @param conferenceFocusCallPeer the parent call peer
     * @param address the protocol address of the member
     */
    public ConferenceMemberGibberishImpl(   CallPeer conferenceFocusCallPeer,
                                            String address)
    {
        super(conferenceFocusCallPeer, address);
    }
}
