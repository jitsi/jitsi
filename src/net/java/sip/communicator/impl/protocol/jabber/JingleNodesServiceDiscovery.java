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
import java.util.concurrent.*;

import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;
import org.xmpp.jnodes.smack.*;

/**
 * Search for jingle nodes.
 *
 * @author Damian Minkov
 */
public class JingleNodesServiceDiscovery
    implements Runnable
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(JingleNodesServiceDiscovery.class);

    /**
     * Property containing jingle nodes prefix to search for.
     */
    private static final String JINGLE_NODES_SEARCH_PREFIX_PROP =
        "net.java.sip.communicator.impl.protocol.jabber.JINGLE_NODES_SEARCH_PREFIXES";

    /**
     * Property containing jingle nodes prefix to search for.
     */
    private static final String JINGLE_NODES_SEARCH_PREFIXES_STOP_ON_FIRST_PROP =
        "net.java.sip.communicator.impl.protocol.jabber.JINGLE_NODES_SEARCH_PREFIXES_STOP_ON_FIRST";

    /**
     * Synchronization object to monitor auto discovery.
     */
    private final Object jingleNodesSyncRoot;

    /**
     * The service.
     */
    private final SmackServiceNode service;

    /**
     * The connection, must be connected.
     */
    private final XMPPConnection connection;

    /**
     * Our account.
     */
    private final JabberAccountIDImpl accountID;

    /**
     * Creates discovery
     * @param service the service.
     * @param connection the connected connection.
     * @param accountID our account.
     * @param syncRoot the synchronization object while discovering.
     */
    JingleNodesServiceDiscovery(SmackServiceNode service,
                                XMPPConnection connection,
                                JabberAccountIDImpl accountID,
                                Object syncRoot)
    {
        this.jingleNodesSyncRoot = syncRoot;
        this.service = service;
        this.connection = connection;
        this.accountID = accountID;
    }

    /**
     * The actual discovery.
     */
    public void run()
    {
        synchronized(jingleNodesSyncRoot)
        {
            long start = System.currentTimeMillis();
            if(logger.isInfoEnabled())
            {
                logger.info("Start Jingle Nodes discovery!");
            }

            SmackServiceNode.MappedNodes nodes;

            String searchNodesWithPrefix =
                JabberActivator.getResources()
                    .getSettingsString(JINGLE_NODES_SEARCH_PREFIX_PROP);
            if(searchNodesWithPrefix == null
                || searchNodesWithPrefix.length() == 0)
                searchNodesWithPrefix =
                    JabberActivator.getConfigurationService()
                        .getString(JINGLE_NODES_SEARCH_PREFIX_PROP);

            // if there are no default prefix settings or
            // this option is turned off, just process with default
            // service discovery making list empty.
            if( searchNodesWithPrefix == null
                || searchNodesWithPrefix.length() == 0
                || searchNodesWithPrefix.equalsIgnoreCase("off"))
            {
                searchNodesWithPrefix = "";
            }

            nodes = searchServicesWithPrefix(
                service,
                connection, 6, 3, 20, JingleChannelIQ.UDP,
                accountID.isJingleNodesSearchBuddiesEnabled(),
                accountID.isJingleNodesAutoDiscoveryEnabled(),
                searchNodesWithPrefix);

            if(logger.isInfoEnabled())
            {
                logger.info("Jingle Nodes discovery terminated! ");
                logger.info("Found " + (nodes != null ?
                                        nodes.getRelayEntries().size() : "0") +
                        " Jingle Nodes relay for account: " +
                        accountID.getAccountAddress()
                    + " in " + (System.currentTimeMillis() - start) + " ms.");
            }

            if(nodes != null)
                service.addEntries(nodes);
        }
    }

    /**
     * Searches for services as the prefix list has priority. If it is set
     * return after first found service.
     *
     * @param service the service.
     * @param xmppConnection the connection.
     * @param maxEntries maximum entries to be searched.
     * @param maxDepth the depth while recursively searching.
     * @param maxSearchNodes number of nodes to query
     * @param protocol the protocol
     * @param searchBuddies should we search our buddies in contactlist.
     * @param autoDiscover is auto discover turned on
     * @param prefix the coma separated list of prefixes to be searched first.
     * @return
     */
    private SmackServiceNode.MappedNodes searchServicesWithPrefix(
            SmackServiceNode service,
            XMPPConnection xmppConnection,
            int maxEntries,
            int maxDepth,
            int maxSearchNodes,
            String protocol,
            boolean searchBuddies,
            boolean autoDiscover,
            String prefix)
        {
            if (xmppConnection == null || !xmppConnection.isConnected())
            {
                return null;
            }

            SmackServiceNode.MappedNodes mappedNodes =
                new SmackServiceNode.MappedNodes();
            ConcurrentHashMap<String, String> visited
                = new ConcurrentHashMap<String, String>();

            // Request to our pre-configured trackerEntries
            for(Map.Entry<String, TrackerEntry> entry
                    : service.getTrackerEntries().entrySet())
            {
                SmackServiceNode.deepSearch(
                    xmppConnection,
                    maxEntries,
                    entry.getValue().getJid(),
                    mappedNodes,
                    maxDepth - 1,
                    maxSearchNodes,
                    protocol,
                    visited);
            }

            if(autoDiscover)
            {
                boolean continueSearch =
                    searchDiscoItems(
                        service,
                        xmppConnection,
                        maxEntries,
                        xmppConnection.getServiceName(),
                        mappedNodes,
                        maxDepth - 1,
                        maxSearchNodes,
                        protocol,
                        visited,
                        prefix);

                // option to stop after first found is turned on, lets exit
                if(!continueSearch)
                    return mappedNodes;

                // Request to Server
                SmackServiceNode.deepSearch(
                    xmppConnection,
                    maxEntries,
                    xmppConnection.getHost(),
                    mappedNodes,
                    maxDepth - 1,
                    maxSearchNodes,
                    protocol,
                    visited);

                // Request to Buddies
                if (xmppConnection.getRoster() != null && searchBuddies)
                {
                    for (final RosterEntry re : xmppConnection.getRoster().getEntries())
                    {
                        for (final Iterator<Presence> i
                                 = xmppConnection.getRoster()
                                    .getPresences(re.getUser());
                             i.hasNext();)
                        {
                            final Presence presence = i.next();
                            if (presence.isAvailable())
                            {
                                SmackServiceNode.deepSearch(
                                    xmppConnection,
                                    maxEntries,
                                    presence.getFrom(),
                                    mappedNodes,
                                    maxDepth - 1,
                                    maxSearchNodes,
                                    protocol,
                                    visited);
                            }
                        }
                    }
                }
            }

            return null;
        }

        /**
         * Discover services and query them.
         * @param service the service.
         * @param xmppConnection the connection.
         * @param maxEntries maximum entries to be searched.
         * @param startPoint the start point to search recursively
         * @param mappedNodes nodes found
         * @param maxDepth the depth while recursively searching.
         * @param maxSearchNodes number of nodes to query
         * @param protocol the protocol
         * @param visited nodes already visited
         * @param prefix the coma separated list of prefixes to be searched first.
         * @return
         */
        private static boolean searchDiscoItems(
            SmackServiceNode service,
            XMPPConnection xmppConnection,
            int maxEntries,
            String startPoint,
            SmackServiceNode.MappedNodes mappedNodes,
            int maxDepth,
            int maxSearchNodes,
            String protocol,
            ConcurrentHashMap<String, String> visited,
            String prefix)
        {
            String[] prefixes = prefix.split(",");

            // default is to stop when first one is found
            boolean stopOnFirst = true;

            String stopOnFirstDefaultValue =
                JabberActivator.getResources().getSettingsString(
                    JINGLE_NODES_SEARCH_PREFIXES_STOP_ON_FIRST_PROP);
            if(stopOnFirstDefaultValue != null)
            {
                stopOnFirst = Boolean.parseBoolean(stopOnFirstDefaultValue);
            }
            stopOnFirst = JabberActivator.getConfigurationService().getBoolean(
                JINGLE_NODES_SEARCH_PREFIXES_STOP_ON_FIRST_PROP,
                stopOnFirst);

            final DiscoverItems items = new DiscoverItems();
            items.setTo(startPoint);
            PacketCollector collector =
                xmppConnection.createPacketCollector(
                    new PacketIDFilter(items.getPacketID()));
            xmppConnection.sendPacket(items);
            DiscoverItems result = (DiscoverItems) collector.nextResult(
                Math.round(SmackConfiguration.getPacketReplyTimeout() * 1.5));

            if (result != null)
            {
                // first search priority items
                Iterator<DiscoverItems.Item> i = result.getItems();
                for (DiscoverItems.Item item = i.hasNext() ? i.next() : null;
                     item != null;
                     item = i.hasNext() ? i.next() : null)
                {
                    for(String pref : prefixes)
                    {
                        if( !StringUtils.isNullOrEmpty(pref)
                            && item.getEntityID().startsWith(pref.trim()))
                        {
                            SmackServiceNode.deepSearch(
                                xmppConnection,
                                maxEntries,
                                item.getEntityID(),
                                mappedNodes,
                                maxDepth,
                                maxSearchNodes,
                                protocol,
                                visited);

                            if(stopOnFirst)
                                return false;// stop and don't continue
                        }
                    }
                }

                // now search rest
                i = result.getItems();
                for (DiscoverItems.Item item = i.hasNext() ? i.next() : null;
                     item != null;
                     item = i.hasNext() ? i.next() : null)
                {
                    // we may searched already this node if it starts
                    // with some of the prefixes
                    if(!visited.containsKey(item.getEntityID()))
                        SmackServiceNode.deepSearch(
                            xmppConnection,
                            maxEntries,
                            item.getEntityID(),
                            mappedNodes,
                            maxDepth,
                            maxSearchNodes,
                            protocol,
                            visited);

                    if(stopOnFirst)
                        return false;// stop and don't continue
                }
            }
            collector.cancel();

            // true we should continue searching
            return true;
        }
}
