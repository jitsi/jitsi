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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

/**
 * Represents functionality which allows a <tt>TransportManagerJabberImpl</tt>
 * implementation to send <tt>transport-info</tt> {@link JingleIQ}s for the
 * purposes of expediting candidate negotiation.
 *
 * @author Lyubomir Marinov
 */
public interface TransportInfoSender
{
    /**
     * Sends specific {@link ContentPacketExtension}s in a
     * <tt>transport-info</tt> {@link JingleIQ} from the local peer to the
     * remote peer.
     *
     * @param contents the <tt>ContentPacketExtension</tt>s to be sent in a
     * <tt>transport-info</tt> <tt>JingleIQ</tt> from the local peer to the
     * remote peer
     */
    public void sendTransportInfo(Iterable<ContentPacketExtension> contents);
}
