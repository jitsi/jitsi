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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * An enumeration containing allowed types for {@link
 * CandidatePacketExtension}s.
 *
 * @author Emil Ivov
 */
public enum CandidateType
{
    /**
     * Indicates that a candidate is a Host Candidate: <br/>
     * A candidate obtained by binding to a specific port from an IP address
     * on the host.  This includes IP addresses on physical interfaces and
     * logical ones, such as ones obtained through Virtual Private Networks
     * (VPNs) and Realm Specific IP.
     */
    host,

    /**
     * Indicates that a candidate is a Peer Reflexive Candidate:  <br/>
     * A candidate whose IP address and port are a binding allocated by a NAT
     * for an agent when it sent a STUN Binding request through the NAT to its
     * peer.
     */
    prflx,


    /**
     * Indicates that a candidate is a Relayed Candidate:<br/>
     * A candidate obtained by sending a TURN Allocate request from a host
     * candidate to a TURN server. The relayed candidate is resident on the
     * TURN server, and the TURN server relays packets back towards the agent.
     */
    relay,

    /**
     * Indicates that a candidate is a Server Reflexive Candidate:<br/>
     * A candidate whose IP address and port are a binding allocated by a
     * NAT for an agent when it sent a packet through the NAT to a server.
     * Server reflexive candidates can be learned by STUN servers using the
     * Binding request, or TURN servers, which provides both a relayed and
     * server reflexive candidate.
     */
    srflx,

    /**
     * Old name for Server Reflexive Candidate used by Google Talk.
     */
    stun,

    /**
     * Old name for Host Candidate used by Google Talk.
     */
    local;
}
