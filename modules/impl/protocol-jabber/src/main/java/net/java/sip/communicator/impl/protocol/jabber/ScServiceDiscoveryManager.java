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

import net.java.sip.communicator.impl.protocol.jabber.caps.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.caps.*;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.stringprep.*;

/**
 * An wrapper to smack's default {@link ServiceDiscoveryManager} that adds
 * support for XEP-0115 - Entity Capabilities.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class ScServiceDiscoveryManager
    implements StanzaListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ScServiceDiscoveryManager</tt>
     * class and its instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScServiceDiscoveryManager.class);

    /**
     * The flag which indicates whether we are currently storing non-caps.
     */
    private final boolean cacheNonCaps;

    /**
     * The cache of non-caps. Used only if {@link #cacheNonCaps} is
     * <tt>true</tt>.
     */
    private final Map<Jid, DiscoverInfo> nonCapsCache
        = new ConcurrentHashMap<>();

    /**
     * The <tt>EntityCapsManager</tt> used by this instance to handle entity
     * capabilities.
     */
    private EntityCapsManager capsManager;

    /**
     * The {@link ServiceDiscoveryManager} that we are wrapping.
     */
    private ServiceDiscoveryManager discoveryManager;

    /**
     * The parent provider
     */
    private final ProtocolProviderService parentProvider;

    /**
     * The runnable responsible for retrieving discover info.
     */
    private DiscoveryInfoRetriever retriever = new DiscoveryInfoRetriever();

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource)
     *
     * We duplicate the logic about JID_TO_NODEVER_CACHE from
     * EntityCapsManager, so we can handle events for UserCapsNodeListeners and
     * to be able to extract all jids that match a given bare jid.
     */
    private final Map<Jid, String> userCaps = new ConcurrentHashMap<>();

    /**
     * The list of <tt>UserCapsNodeListener</tt>s interested in events notifying
     * about changes in the list of user caps nodes of this
     * <tt>EntityCapsManager</tt>.
     */
    private final List<UserCapsNodeListener> userCapsNodeListeners
        = new LinkedList<>();

    /**
     * An empty array of <tt>UserCapsNodeListener</tt> elements explicitly
     * defined in order to reduce unnecessary allocations.
     */
    private static final UserCapsNodeListener[] NO_USER_CAPS_NODE_LISTENERS
        = new UserCapsNodeListener[0];

    /**
     * The node value to advertise.
     */
    private static String entityNode
        = OSUtils.IS_ANDROID ? "http://android.jitsi.org" : "http://jitsi.org";


    /**
     * We need to call this before creating any xmpp connection to be sure
     * all further communication will carry our identity.
     */
    public static void initIdentity()
    {
        // we setup supported features no packets are actually sent
        //during feature registration so we'd better do it here so that
        //our first presence update would contain a caps with the right
        //features.
        String name
            = System.getProperty(
            "sip-communicator.application.name",
            "Jitsi ")
            + System.getProperty("sip-communicator.version","SVN");

        ServiceDiscoveryManager.setDefaultIdentity(
            new DiscoverInfo.Identity("client", name, "pc"));

        // Set the default entity node that
        // will be used for new EntityCapsManagers.
        EntityCapsManager.setDefaultEntityNode(entityNode);
    }

    /**
     * Creates a new <tt>ScServiceDiscoveryManager</tt> wrapping the default
     * discovery manager of the specified <tt>connection</tt>.
     *
     * @param parentProvider the parent provider that creates discovery manager.
     * @param configService the current configuration service.
     * @param connection Smack connection object that will be used by this
     * instance to handle XMPP connection.
     * @param featuresToRemove an array of <tt>String</tt>s representing the
     * features to be removed from the <tt>ServiceDiscoveryManager</tt> of the
     * specified <tt>connection</tt> which is to be wrapped by the new instance
     * @param featuresToAdd an array of <tt>String</tt>s representing the
     * features to be added to the new instance and to the
     * <tt>ServiceDiscoveryManager</tt> of the specified <tt>connection</tt>
     * which is to be wrapped by the new instance
     * @param cacheNonCaps <tt>true</tt> if we want to cache entity features
     *                     even though it does not support XEP-0115
     */
    public ScServiceDiscoveryManager(
            ProtocolProviderService parentProvider,
            ConfigurationService configService,
            XMPPConnection connection,
            String[] featuresToRemove,
            String[] featuresToAdd,
            boolean cacheNonCaps)
    {
        this.parentProvider = parentProvider;

        this.discoveryManager
            = ServiceDiscoveryManager.getInstanceFor(connection);

        this.cacheNonCaps = cacheNonCaps;

        /*
         * Reflect featuresToRemove and featuresToAdd before
         * updateEntityCapsVersion() in order to persist only the complete
         * node#ver association with our own DiscoverInfo. Otherwise, we'd
         * persist all intermediate ones upon each addFeature() and
         * removeFeature().
         */
        // featuresToRemove
        if (featuresToRemove != null)
        {
            for (String featureToRemove : featuresToRemove)
                discoveryManager.removeFeature(featureToRemove);
        }
        // featuresToAdd
        if (featuresToAdd != null)
        {
            for (String featureToAdd : featuresToAdd)
                if (!discoveryManager.includesFeature(featureToAdd))
                    discoveryManager.addFeature(featureToAdd);
        }

        // For every XMPPConnection, add one EntityCapsManager.
        this.capsManager = EntityCapsManager.getInstanceFor(connection);
        EntityCapsManager.setPersistentCache(
            new CapsConfigurationPersistence(configService));
        connection.addAsyncStanzaListener(
            this, new StanzaTypeFilter(Presence.class));
    }

    /**
     * Registers that a new feature is supported by this XMPP entity. When this
     * client is queried for its information the registered features will be
     * answered.
     * <p>
     * Since no packet is actually sent to the server it is safe to perform
     * this operation before logging to the server. In fact, you may want to
     * configure the supported features before logging to the server so that
     * the information is already available if it is required upon login.
     *
     * @param feature the feature to register as supported.
     */
    public void addFeature(String feature)
    {
        discoveryManager.addFeature(feature);
    }

    /**
     * Returns a reference to our local copy of the feature list supported by
     * this implementation.
     *
     * @return a reference to our local copy of the feature list supported by
     * this implementation.
     */
    public List<String> getFeatures()
    {
        return discoveryManager.getFeatures();
    }

    /**
     * Returns <tt>true</tt> if the specified feature is registered in our
     * {@link ServiceDiscoveryManager} and <tt>false</tt> otherwise.
     *
     * @param feature the feature to look for.
     *
     * @return a boolean indicating if the specified featured is registered or
     * not.
     */
    public boolean includesFeature(String feature)
    {
        return this.discoveryManager.includesFeature(feature);
    }

    /**
     * Removes the specified feature from the supported features by the
     * encapsulated ServiceDiscoveryManager.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform
     * this operation before logging to the server.
     *
     * @param feature the feature to remove from the supported features.
     */
    public void removeFeature(String feature)
    {
        discoveryManager.removeFeature(feature);
    }

    /**
     * Handles incoming presence packets and maps jids to node#ver strings.
     *
     * @param packet the incoming presence <tt>Packet</tt> to be handled
     */
    @Override
    public void processStanza(Stanza packet)
    {
        // Check it the packet indicates  that the user is online. We
        // will use this information to decide if we're going to send
        // the discover info request.
        boolean online
            = (packet instanceof Presence)
                && ((Presence) packet).isAvailable();

        CapsExtension ext =  packet.getExtension(
            CapsExtension.ELEMENT, CapsExtension.NAMESPACE);

        if(ext != null && online)
        {
            addUserCapsNode(packet.getFrom(), ext.getNode(), ext.getVer());
        }
        else if (!online)
        {
            removeUserCapsNode(packet.getFrom());
        }
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by
     * its JID.
     *
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfo(Jid entityID)
        throws XMPPException,
               NotConnectedException,
               InterruptedException,
               NoResponseException
    {
        return this.discoverInfo(entityID, null, null);
    }

    /**
     * Requests the discovery info and fires the event if
     * retrieved.
     * @param entityID the entity to request
     * @param caps and its capability.
     * @param capabilitiesOpSet operation set ot receive events
     * or null for no events.
     * @return the discovered information.
     */
    private DiscoverInfo discoverInfo(
        final Jid entityID,
        EntityCapsManager.NodeVerHash caps,
        OperationSetContactCapabilitiesJabberImpl capabilitiesOpSet)
            throws XMPPException,
                    NotConnectedException,
                    InterruptedException,
                    NoResponseException
    {
        DiscoverInfo discoverInfo = discoveryManager.discoverInfo(
            entityID,
            (caps == null ) ? null : caps.getNodeVer());

        if (caps != null
            && discoverInfo != null
            && !EntityCapsManager.verifyDiscoverInfoVersion(
                    caps.getVer(), caps.getHash(), discoverInfo))
        {
            if(!caps.getHash().equals(""))
            {
                logger.error("Invalid DiscoverInfo for "
                    + caps.getNodeVer() + ": " + discoverInfo);
            }
            caps = null;
        }

        boolean fireEvent = false;

        if (caps == null)
        {
            if (cacheNonCaps)
            {
                nonCapsCache.put(entityID, discoverInfo);
                fireEvent = true;
            }
        }
        else
        {
            fireEvent = true;
        }

        // fire event
        if(fireEvent && capabilitiesOpSet != null)
        {
            capabilitiesOpSet.fireContactCapabilitiesChanged(
                entityID.asBareJid(),
                getFullJidsByBareJid(entityID.asBareJid())
            );
        }

        return discoverInfo;
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by
     * its JID if locally cached, otherwise schedules for retrieval.
     *
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     */
    public DiscoverInfo discoverInfoNonBlocking(Jid entityID)
    {
        DiscoverInfo discoverInfo = capsManager.getDiscoverInfoByUser(entityID);
        EntityCapsManager.NodeVerHash caps
            = EntityCapsManager.getNodeVerHashByJid(entityID);

        boolean isInfoValid = false;
        if (discoverInfo != null && caps != null)
        {
            isInfoValid = EntityCapsManager.verifyDiscoverInfoVersion(
                caps.getVer(), caps.getHash(), discoverInfo);
        }

        if (discoverInfo != null && isInfoValid)
            return discoverInfo;

        // if caps is not valid, has empty hash
        if (cacheNonCaps)
        {
            discoverInfo = nonCapsCache.get(entityID);
            if (discoverInfo != null)
                return discoverInfo;
        }

        // add to retrieve thread
        retriever.addEntityForRetrieve(
            entityID,
            caps);

        return null;
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID.
     *
     * @param entityID the address of the XMPP entity.
     *
     * @return the discovered information.
     *
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(Jid entityID)
            throws XMPPException,
                NotConnectedException,
                InterruptedException,
                SmackException.NoResponseException
    {
        return discoveryManager.discoverItems(entityID);
    }

    /**
     * Returns <tt>true</tt> if <tt>jid</tt> supports the specified
     * <tt>feature</tt> and <tt>false</tt> otherwise. The method may check the
     * information locally if we've already cached this <tt>jid</tt>'s disco
     * info, or retrieve it from the network.
     *
     * @param jid the jabber ID we'd like to test for support
     * @param feature the URN feature we are interested in
     *
     * @return true if <tt>jid</tt> is discovered to support <tt>feature</tt>
     * and <tt>false</tt> otherwise.
     */
    public boolean supportsFeature(Jid jid, String feature)
    {
        DiscoverInfo info;

        try
        {
            info = this.discoverInfo(jid);
        }
        catch(XMPPException
                | InterruptedException
                | NoResponseException
                | NotConnectedException ex)
        {
            logger.info("failed to retrieve disco info for " + jid
                                + " feature " + feature, ex);
            return false;
        }

        return info != null && info.containsFeature(feature);
    }

    /**
     * Adds a specific <tt>UserCapsNodeListener</tt> to the list of
     * <tt>UserCapsNodeListener</tt>s interested in events notifying about
     * changes in the list of user caps nodes of this
     * <tt>EntityCapsManager</tt>.
     *
     * @param listener the <tt>UserCapsNodeListener</tt> which is interested in
     * events notifying about changes in the list of user caps nodes of this
     * <tt>EntityCapsManager</tt>
     */
    public void addUserCapsNodeListener(UserCapsNodeListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        synchronized (userCapsNodeListeners)
        {
            if (!userCapsNodeListeners.contains(listener))
                userCapsNodeListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>UserCapsNodeListener</tt> from the list of
     * <tt>UserCapsNodeListener</tt>s interested in events notifying about
     * changes in the list of user caps nodes of this
     * <tt>EntityCapsManager</tt>.
     *
     * @param listener the <tt>UserCapsNodeListener</tt> which is no longer
     * interested in events notifying about changes in the list of user caps
     * nodes of this <tt>EntityCapsManager</tt>
     */
    public void removeUserCapsNodeListener(UserCapsNodeListener listener)
    {
        if (listener != null)
        {
            synchronized (userCapsNodeListeners)
            {
                userCapsNodeListeners.remove(listener);
            }
        }
    }

    /**
     * Gets the full Jids (with resources) as Strings.
     *
     * @param bareJid bare Jid
     * @return the full Jids as an ArrayList <tt>user</tt>
     */
    public List<Jid> getFullJidsByBareJid(Jid bareJid)
    {
        List<Jid> jids = new ArrayList<>();
        for(Jid jid : userCaps.keySet())
        {
            if (bareJid.equals(jid.asBareJid()))
            {
                jids.add(jid);
            }
        }

        return jids;
    }

    /**
     * Add a record telling what entity caps node a user has.
     * @param user the user (Full JID)
     * @param node the node (of the caps packet extension)
     * @param ver the version (of the caps packet extension)
     */
    private void addUserCapsNode(Jid user,
                                 String node,
                                 String ver)
    {
        if (user != null
            && node != null
            && ver != null)
        {
            String nodeVer= userCaps.get(user);

            if (nodeVer == null
                || !nodeVer.equals(node + "#" + ver))
            {
                nodeVer = node + "#" + ver;

                userCaps.put(user, nodeVer);
            }
            else
                return;

            fireUserCapsNodeEvent(true, user, nodeVer);
        }
    }

    /**
     * Gets the <tt>Caps</tt> i.e. the node, the hash and the ver of a user.
     *
     * @param user the user (Full JID)
     * @return the <tt>Caps</tt> i.e. the node, the hash and the ver of
     * <tt>user</tt>
     */
    public EntityCapsManager.NodeVerHash getCapsByUser(Jid user)
    {
        return EntityCapsManager.getNodeVerHashByJid(user);
    }

    /**
     * Remove a record telling what entity caps node a user has.
     *
     * @param user the user (Full JID)
     */
    public void removeUserCapsNode(Jid user)
    {
        if (user == null)
        {
            return;
        }

        String nodeVer = userCaps.remove(user);

        // Fire userCapsNodeRemoved.
        if (nodeVer != null)
        {
            fireUserCapsNodeEvent(false, user, nodeVer);
        }
    }

    /**
     * Remove records telling what entity caps node a contact has.
     *
     * @param contact the contact
     */
    public void removeContactCapsNode(Contact contact)
    {
        String nodeVer = null;
        Jid lastRemovedJid = null;

        Iterator<Jid> iter = userCaps.keySet().iterator();
        while(iter.hasNext())
        {
            Jid jid = iter.next();

            if(jid.equals(contact.getAddress()))
            {
                nodeVer = userCaps.get(jid);
                lastRemovedJid = jid;
                iter.remove();
            }
        }

        Jid contactJid = null;
        try
        {
            contactJid =
                ((ProtocolProviderServiceJabberImpl) parentProvider).getFullJid(contact);
        }
        catch (XmppStringprepException e)
        {
            logger.error("Failed to get JID from contact for caps removal", e);
            return;
        }

        EntityCapsManager.removeUserCapsNode(contactJid);

        // fire only for the last one, at the end the event out
        // of the protocol will be one and for the contact
        if(nodeVer != null)
        {
            fireUserCapsNodeEvent(false, lastRemovedJid, nodeVer);
        }
    }

    /**
     * Fires events to UserCapsNodeListener.
     * @param add whether this is add or remove event.
     * @param user the user full jid this is about.
     * @param nodeVer
     */
    private void fireUserCapsNodeEvent(
        boolean add, Jid user, String nodeVer)
    {
        UserCapsNodeListener[] listeners;
        synchronized (userCapsNodeListeners)
        {
            listeners
                = userCapsNodeListeners.toArray(
                NO_USER_CAPS_NODE_LISTENERS);
        }
        if (listeners.length != 0)
        {
            for (UserCapsNodeListener listener : listeners)
            {
                if(add)
                {
                    listener.userCapsNodeAdded(
                        user,
                        getFullJidsByBareJid(user.asBareJid()),
                        nodeVer, true);
                }
                else
                {
                    listener.userCapsNodeRemoved(
                        user,
                        getFullJidsByBareJid(user.asBareJid()),
                        nodeVer, false);
                }
            }
        }
    }

    /**
     * Clears/stops what's needed.
     */
    public void stop()
    {
        if(retriever != null)
            retriever.stop();

        // we need to clean up our reference
        discoveryManager.removeNodeInformationProvider(
            capsManager.getLocalNodeVer());
        this.capsManager = null;
        this.discoveryManager = null;
    }

    /**
     * Thread that runs the discovery info.
     */
    private class DiscoveryInfoRetriever
        implements Runnable
    {
        /**
         * start/stop.
         */
        private boolean stopped = true;

        /**
         * The thread that runs this dispatcher.
         */
        private Thread retrieverThread = null;

        /**
         * Entities to be processed and their caps.
         * HashMap so we can store null caps.
         */
        private final Map<Jid, EntityCapsManager.NodeVerHash> entities
            = new HashMap<>();

        /**
         * Our capability operation set.
         */
        private OperationSetContactCapabilitiesJabberImpl capabilitiesOpSet;

        /**
         * Runs in different thread.
         */
        public void run()
        {
            try
            {
                stopped = false;

                while(!stopped)
                {
                    Map.Entry<Jid, EntityCapsManager.NodeVerHash>
                        entityToProcess = null;

                    synchronized(entities)
                    {
                        if(entities.size() == 0)
                        {
                            try
                            {
                                entities.wait();
                            }
                            catch (InterruptedException iex){}
                        }

                        Iterator<Map.Entry<Jid, EntityCapsManager.NodeVerHash>>
                            iter = entities.entrySet().iterator();
                        if(iter.hasNext())
                        {
                            entityToProcess = iter.next();
                            iter.remove();
                        }
                    }

                    if(entityToProcess != null)
                    {
                        try
                        {
                            // process
                            discoverInfo(
                                entityToProcess.getKey(),
                                entityToProcess.getValue(),
                                capabilitiesOpSet);
                        }
                        catch(XMPPException
                            | InterruptedException
                            | NoResponseException
                            | NotConnectedException ex)
                        {
                            // print discovery info errors only when trace
                            if(logger.isTraceEnabled())
                                logger.error(
                                    "Error requesting discover info for "
                                        + entityToProcess.getKey(), ex);
                        }
                    }
                }
            } catch(Throwable t)
            {
                logger.error("Error requesting discovery info, " +
                    "thread ended unexpectedly", t);
            }
        }

        /**
         * Queue entities for retrieval.
         * @param entityID the entity.
         * @param caps and its capability.
         */
        public void addEntityForRetrieve(Jid entityID,
                                         EntityCapsManager.NodeVerHash caps)
        {
            synchronized(entities)
            {

                if(!entities.containsKey(entityID))
                {
                    entities.put(entityID, caps);
                    entities.notifyAll();

                    if(retrieverThread == null)
                    {
                        start();
                    }
                }
            }
        }

        /**
         * Start thread.
         */
        private void start()
        {
            capabilitiesOpSet = (OperationSetContactCapabilitiesJabberImpl)
                parentProvider.getOperationSet(
                    OperationSetContactCapabilities.class);

            retrieverThread = new Thread(
                this,
                ScServiceDiscoveryManager.class.getName());
            retrieverThread.setDaemon(true);

            retrieverThread.start();


        }

        /**
         * Stops and clears.
         */
        void stop()
        {
            synchronized(entities)
            {
                stopped = true;
                entities.notifyAll();

                retrieverThread = null;
            }
        }
    }
}
