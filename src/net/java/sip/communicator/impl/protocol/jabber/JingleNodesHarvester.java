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

import java.util.*;

import net.java.sip.communicator.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.socket.*;
import org.jivesoftware.smack.*;
import org.xmpp.jnodes.smack.*;

/**
 * Implements a <tt>CandidateHarvester</tt> which gathers <tt>Candidate</tt>s
 * for a specified {@link Component} using Jingle Nodes as defined in
 * XEP 278 "Jingle Relay Nodes".
 *
 * @author Sebastien Vincent
 */
public class JingleNodesHarvester
    extends CandidateHarvester
{
    /**
     * The <tt>Logger</tt> used by the <tt>JingleNodesHarvester</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(JingleNodesHarvester.class.getName());

    /**
     * XMPP connection.
     */
    private SmackServiceNode serviceNode = null;

    /**
     * JingleNodes relay allocate two address/port couple for us. Due to the
     * architecture of Ice4j that harvest address for each component, we store
     * the second address/port couple.
     */
    private TransportAddress localAddressSecond = null;

    /**
     * JingleNodes relay allocate two address/port couple for us. Due to the
     * architecture of Ice4j that harvest address for each component, we store
     * the second address/port couple.
     */
    private TransportAddress relayedAddressSecond = null;

    /**
     * Constructor.
     *
     * @param serviceNode the <tt>SmackServiceNode</tt>
     */
    public JingleNodesHarvester(SmackServiceNode serviceNode)
    {
        this.serviceNode = serviceNode;
    }

    /**
     * Gathers Jingle Nodes candidates for all host <tt>Candidate</tt>s that are
     * already present in the specified <tt>component</tt>. This method relies
     * on the specified <tt>component</tt> to already contain all its host
     * candidates so that it would resolve them.
     *
     * @param component the {@link Component} that we'd like to gather candidate
     * Jingle Nodes <tt>Candidate</tt>s for
     * @return  the <tt>LocalCandidate</tt>s gathered by this
     * <tt>CandidateHarvester</tt>
     */
    @Override
    public synchronized Collection<LocalCandidate> harvest(Component component)
    {
        logger.info("harvest Jingle Nodes");

        Collection<LocalCandidate> candidates = new HashSet<LocalCandidate>();
        String ip = null;
        int port = -1;

        /* if we have already a candidate (RTCP) allocated, get it */
        if(localAddressSecond != null && relayedAddressSecond != null)
        {
            LocalCandidate candidate = createJingleNodesCandidate(
                    relayedAddressSecond, component, localAddressSecond);

            //try to add the candidate to the component and then only add it to
            //the harvest not redundant (not sure how it could be red. but ...)
            if( component.addLocalCandidate(candidate))
            {
                candidates.add(candidate);
            }

            localAddressSecond = null;
            relayedAddressSecond = null;
            return candidates;
        }

        XMPPConnection conn = serviceNode.getConnection();
        JingleChannelIQ ciq = null;

        if (serviceNode != null)
        {
            final TrackerEntry preferred = serviceNode.getPreferedRelay();

            if (preferred != null)
            {
                ciq = SmackServiceNode.getChannel(conn, preferred.getJid());
            }
        }

        if (ciq != null && ciq.getRemoteport() > 0)
        {
            ip = ciq.getHost();
            port = ciq.getRemoteport();

            if(logger.isInfoEnabled())
            {
                logger.info("JN relay: " + ip + " remote port:" + port +
                        " local port: " + ciq.getLocalport());
            }

            /* RTP */
            TransportAddress relayedAddress = new TransportAddress(ip, port,
                    Transport.UDP);
            TransportAddress localAddress = new TransportAddress(ip,
                    ciq.getLocalport(), Transport.UDP);

            LocalCandidate local = createJingleNodesCandidate(
                    relayedAddress, component, localAddress);

            /* RTCP */
            relayedAddressSecond
                = new TransportAddress(ip, port + 1,Transport.UDP);
            localAddressSecond
                = new TransportAddress(ip, ciq.getLocalport() + 1,
                        Transport.UDP);


            //try to add the candidate to the component and then only add it to
            //the harvest not redundant (not sure how it could be red. but ...)
            if( component.addLocalCandidate(local))
            {
                candidates.add(local);
            }
        }
        return candidates;
    }

    /**
     * Creates a new <tt>JingleNodesRelayedCandidate</tt> instance which is to
     * represent a specific <tt>TransportAddress</tt>.
     *
     * @param transportAddress the <tt>TransportAddress</tt> allocated by the
     * relay
     * @param component the <tt>Component</tt> for which the candidate will be
     * added
     * @param localEndPoint <tt>TransportAddress</tt> of the Jingle Nodes relay
     * where we will send our packet.
     * @return a new <tt>JingleNodesRelayedCandidate</tt> instance which
     * represents the specified <tt>TransportAddress</tt>
     */
    protected JingleNodesCandidate createJingleNodesCandidate(
            TransportAddress transportAddress, Component component,
            TransportAddress localEndPoint)
    {
        JingleNodesCandidate cand = null;

        try
        {
            cand = new JingleNodesCandidate(transportAddress,
                        component,
                        localEndPoint);
            IceSocketWrapper stunSocket = cand.getStunSocket(null);
            cand.getStunStack().addSocket(stunSocket);
        }
        catch(Throwable e)
        {
            logger.debug(
                    "Exception occurred when creating JingleNodesCandidate: " +
                    e);
        }

        return cand;
    }
}
