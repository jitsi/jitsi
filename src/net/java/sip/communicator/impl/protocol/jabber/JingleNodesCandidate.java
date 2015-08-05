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

import java.lang.reflect.*;
import java.net.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.socket.*;

/**
 * Represents a <tt>Candidate</tt> obtained via Jingle Nodes.
 *
 * @author Sebastien Vincent
 */
public class JingleNodesCandidate
    extends LocalCandidate
{
    /**
     * The socket used to communicate with relay.
     */
    private IceSocketWrapper socket = null;

    /**
     * The <tt>RelayedCandidateDatagramSocket</tt> of this
     * <tt>JingleNodesCandidate</tt>.
     */
    private JingleNodesCandidateDatagramSocket
        jingleNodesCandidateDatagramSocket = null;

    /**
     * <tt>TransportAddress</tt> of the Jingle Nodes relay where we will send
     * our packet.
     */
    private TransportAddress localEndPoint = null;

    /**
     * Creates a <tt>JingleNodesRelayedCandidate</tt> for the specified
     * transport, address, and base.
     *
     * @param transportAddress  the transport address that this candidate is
     * encapsulating.
     * @param parentComponent the <tt>Component</tt> that this candidate
     * belongs to.
     * @param localEndPoint <tt>TransportAddress</tt> of the Jingle Nodes relay
     * where we will send our packet.
     */
    public JingleNodesCandidate(TransportAddress transportAddress,
            Component parentComponent, TransportAddress localEndPoint)
    {
        super(
                transportAddress,
                parentComponent,
                CandidateType.RELAYED_CANDIDATE,
                CandidateExtendedType.JINGLE_NODE_CANDIDATE,
                null);
        setBase(this);
        setRelayServerAddress(localEndPoint);
        this.localEndPoint = localEndPoint;
    }

    /**
     * Gets the <tt>JingleNodesCandidateDatagramSocket</tt> of this
     * <tt>JingleNodesCandidate</tt>.
     * <p>
     * <b>Note</b>: The method is part of the internal API of
     * <tt>RelayedCandidate</tt> and <tt>TurnCandidateHarvest</tt> and is not
     * intended for public use.
     * </p>
     *
     * @return the <tt>RelayedCandidateDatagramSocket</tt> of this
     * <tt>RelayedCandidate</tt>
     */
    public synchronized JingleNodesCandidateDatagramSocket
        getRelayedCandidateDatagramSocket()
    {
        if (jingleNodesCandidateDatagramSocket == null)
        {
            try
            {
                jingleNodesCandidateDatagramSocket
                    = new JingleNodesCandidateDatagramSocket(
                            this, localEndPoint);
            }
            catch (SocketException sex)
            {
                throw new UndeclaredThrowableException(sex);
            }
        }
        return jingleNodesCandidateDatagramSocket;
    }

    /**
     * Gets the <tt>DatagramSocket</tt> associated with this <tt>Candidate</tt>.
     *
     * @return the <tt>DatagramSocket</tt> associated with this
     * <tt>Candidate</tt>
     */
    @Override
    public IceSocketWrapper getIceSocketWrapper()
    {
        if (socket == null)
        {
            try
            {
                socket
                    = new IceUdpSocketWrapper(new MultiplexingDatagramSocket(
                            getRelayedCandidateDatagramSocket()));
            }
            catch (SocketException sex)
            {
                throw new UndeclaredThrowableException(sex);
            }
        }
        return socket;
    }
}
