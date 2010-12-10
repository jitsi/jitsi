/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    private DatagramSocket socket = null;

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
        super(transportAddress, parentComponent,
                CandidateType.RELAYED_CANDIDATE);
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
    public DatagramSocket getSocket()
    {
        if (socket == null)
        {
            try
            {
                socket
                    = new MultiplexingDatagramSocket(
                            getRelayedCandidateDatagramSocket());
            }
            catch (SocketException sex)
            {
                throw new UndeclaredThrowableException(sex);
            }
        }
        return socket;
    }
}
