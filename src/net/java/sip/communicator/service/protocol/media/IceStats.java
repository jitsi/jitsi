/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import org.ice4j.ice.*;

import java.net.*;

/**
 * Contains some basic ICE statistics that can be displayed to users during
 * calls.
 *
 * @author Emil Ivov
 */
public class IceStats
{
    /**
     * The ICE agent whose statistics we will be retrieving.
     */
    private final Agent iceAgent;

    /**
     * Instantiate ICE statistics for the specified <tt>iceAgent</tt>
     *
     * @param iceAgent the agent that we'd like to return statistics on.
     */
    public IceStats(Agent iceAgent)
    {
        this.iceAgent = iceAgent;
    }

    /**
     * Returns the ICE candidate extended type selected by the given agent.
     *
     * @param iceAgent The ICE agent managing the ICE offer/answer exchange,
     * collecting and selecting the candidate.
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return The ICE candidate extended type selected by the given agent. null
     * if the iceAgent is null or if there is no candidate selected or
     * available.
     */
    private static String getICECandidateExtendedType( Agent iceAgent,
                                                       String streamName)
    {
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getExtendedType().toString();
        }
        return null;
    }


    /**
     * Returns the extended type of the selected candidate or <tt>null</tt>
     * if we are not using ICE..
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return The extended type of the selected candidate or <tt>null</tt> if
     * we are not using ICE.
     */
    public String getICECandidateExtendedType(String streamName)
    {
        if (iceAgent == null)
            return null;

        return
            IceStats.getICECandidateExtendedType(iceAgent, streamName);
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing.
     */
    public String getICEState()
    {
        if (iceAgent == null)
            return null;

        return iceAgent.getState().toString();
    }

    /**
     * Returns the ICE local host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public InetSocketAddress getICELocalHostAddress(String streamName)
    {
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getHostAddress();
        }
        return null;
    }

    /**
     * Returns the ICE remote host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public InetSocketAddress getICERemoteHostAddress(String streamName)
    {
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getHostAddress();
        }
        return null;
    }

    /**
     * Returns the ICE local reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * local candidate used.
     */
    public InetSocketAddress getICELocalReflexiveAddress(String streamName)
    {
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getReflexiveAddress();
        }
        return null;
    }

    /**
     * Returns the ICE remote reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * remote candidate used.
     */
    public InetSocketAddress getICERemoteReflexiveAddress(String streamName)
    {
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getReflexiveAddress();
        }
        return null;
    }

    /**
     * Returns the ICE local relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * local candidate used.
     */
    public InetSocketAddress getICELocalRelayedAddress(String streamName)
    {
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getRelayedAddress();
        }
        return null;
    }

    /**
     * Returns the ICE remote relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * remote candidate used.
     */
    public InetSocketAddress getICERemoteRelayedAddress(String streamName)
    {
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getRelayedAddress();
        }
        return null;
    }

    /**
     * Returns the total harvesting time (in ms) for all harvesters.
     *
     * @return The total harvesting time (in ms) for all the harvesters. 0 if
     * the ICE agent is null, or if the agent has nevers harvested.
     */
    public long getTotalHarvestingTime()
    {
        return (iceAgent == null) ? 0 : iceAgent.getTotalHarvestingTime();
    }

    /**
     * Returns the harvesting time (in ms) for the harvester given in parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The harvesting time (in ms) for the harvester given in parameter.
     * 0 if this harvester does not exists, if the ICE agent is null, or if the
     * agent has never harvested with this harvester.
     */
    public long getHarvestingTime(String harvesterName)
    {
        return
            (iceAgent == null) ? 0 : iceAgent.getHarvestingTime(harvesterName);
    }

    /**
     * Returns the number of harvesting for this agent.
     *
     * @return The number of harvesting for this agent.
     */
    public int getNbHarvesting()
    {
        return (iceAgent == null) ? 0 : iceAgent.getHarvestCount();
    }

    /**
     * Returns the number of harvesting time for the harvester given in
     * parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The number of harvesting time for the harvester given in
     * parameter.
     */
    public int getNbHarvesting(String harvesterName)
    {
        return (iceAgent == null) ? 0 : iceAgent.getHarvestCount(harvesterName);
    }
}
