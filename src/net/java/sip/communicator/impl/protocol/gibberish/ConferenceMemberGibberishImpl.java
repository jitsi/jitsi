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
package net.java.sip.communicator.impl.protocol.gibberish;

import net.java.sip.communicator.service.protocol.*;

/**
 * A Gibberish implementation of the <tt>ConferenceMember</tt> interface.
 *
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
    public ConferenceMemberGibberishImpl(
            CallPeer conferenceFocusCallPeer,
            String address)
    {
        super(conferenceFocusCallPeer, address);
    }
}
