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

import net.java.sip.communicator.impl.protocol.jabber.extensions.caps.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.Jid;

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
    implements StanzaListener,
               NodeInformationProvider
{
    /**
     * The <tt>Logger</tt> used by the <tt>ScServiceDiscoveryManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ScServiceDiscoveryManager.class);

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
     * The <tt>EntitiCapsManager</tt> used by this instance to handle entity
     * capabilities.
     */
    private final EntityCapsManager capsManager;

    /**
     * The {@link ServiceDiscoveryManager} that we are wrapping.
     */
    private final ServiceDiscoveryManager discoveryManager;

    /**
     * The parent provider
     */
    private final ProtocolProviderService parentProvider;

    /**
     * The {@link XMPPConnection} that this manager is responsible for.
     */
    private final XMPPConnection connection;

    /**
     * A local copy that we keep in sync with {@link ServiceDiscoveryManager}'s
     * feature list that as we add and remove features to it.
     */
    private final List<String> features;

    /**
     * The unmodifiable view of {@link #features} which can be exposed to the
     * public through {@link #getFeatures()}, for example.
     */
    private final List<String> unmodifiableFeatures;

    /**
     * A {@link List} of the identities we use in our disco answers.
     */
    private final List<DiscoverInfo.Identity> identities;

    /**
     * Capabilities to put in ext attribute of capabilities stanza.
     */
    private final List<String> extCapabilities = new ArrayList<String>();

    /**
     * The runnable responsible for retrieving discover info.
     */
    private DiscoveryInfoRetriever retriever = new DiscoveryInfoRetriever();

    /**
     * Creates a new <tt>ScServiceDiscoveryManager</tt> wrapping the default
     * discovery manager of the specified <tt>connection</tt>.
     *
     * @param parentProvider the parent provider that creates discovery manager.
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
            XMPPConnection connection,
            String[] featuresToRemove,
            String[] featuresToAdd,
            boolean cacheNonCaps)
    {
        this.parentProvider = parentProvider;
        this.connection = connection;

        this.discoveryManager
            = ServiceDiscoveryManager.getInstanceFor(connection);

        this.features = new ArrayList<>();
        this.unmodifiableFeatures = Collections.unmodifiableList(this.features);
        this.identities = new ArrayList<>();

        this.cacheNonCaps = cacheNonCaps;

        DiscoverInfo.Identity identity
            = new DiscoverInfo.Identity(
                    "client",
                    this.discoveryManager.getIdentityName(),
                    this.discoveryManager.getIdentityType());
        identities.add(identity);

        //add support for capabilities
        discoveryManager.addFeature(CapsExtension.NAMESPACE);

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
        this.capsManager = new EntityCapsManager();
        capsManager.addPacketListener(connection);

        /*
         * XXX initFeatures() has to happen before updateEntityCapsVersion().
         * Otherwise, updateEntityCapsVersion() will not include the features of
         * the wrapped discoveryManager.
         */
        initFeatures();
        updateEntityCapsVersion();

        // Now, make sure we intercept presence packages and add caps data when
        // intended. XEP-0115 specifies that a client SHOULD include entity
        // capabilities with every presence notification it sends.
        connection.addPacketInterceptor(
                this,
                new StanzaTypeFilter(Presence.class));
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
        synchronized (features)
        {
            features.add(feature);
            discoveryManager.addFeature(feature);
        }
        updateEntityCapsVersion();
    }

    /**
     * Recalculates the entity capabilities caps ver string according to what's
     * currently available in our own discovery info.
     */
    private void updateEntityCapsVersion()
    {
        // If a XMPPConnection is the managed one, see that the new version is
        // updated
        if ((connection != null) && (capsManager != null))
            capsManager.calculateEntityCapsVersion(getOwnDiscoverInfo());
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
        return unmodifiableFeatures;
    }

    /**
     * Get a DiscoverInfo for the current entity caps node.
     *
     * @return a DiscoverInfo for the current entity caps node
     */
    public DiscoverInfo getOwnDiscoverInfo()
    {
        DiscoverInfo di = new DiscoverInfo();

        di.setType(IQ.Type.result);
        di.setNode(capsManager.getNode() + "#" + getEntityCapsVersion());

        // Add discover info
        addDiscoverInfoTo(di);
        return di;
    }

    /**
     * Returns the caps version as returned by our caps manager or <tt>null</tt>
     * if we don't have a caps manager yet.
     *
     * @return the caps version as returned by our caps manager or <tt>null</tt>
     * if we don't have a caps manager yet.
     */
    private String getEntityCapsVersion()
    {
        return (capsManager == null) ? null : capsManager.getCapsVersion();
    }

    /**
     * Populates a specific <tt>DiscoverInfo</tt> with the identity and features
     * of the current entity caps node.
     *
     * @param response the discover info response packet
     */
    private void addDiscoverInfoTo(DiscoverInfo response)
    {
        // Set this client identity
        DiscoverInfo.Identity identity
            = new DiscoverInfo.Identity(
                    "client",
                    discoveryManager.getIdentityName(),
                    discoveryManager.getIdentityType());
        response.addIdentity(identity);

        // Add the registered features to the response

        // Add Entity Capabilities (XEP-0115) feature node.
        /*
         * XXX Only addFeature if !containsFeature. Otherwise, the DiscoverInfo
         * may end up with repeating features.
         */
        if (!response.containsFeature(CapsExtension.NAMESPACE))
            response.addFeature(CapsExtension.NAMESPACE);

        for (String feature : unmodifiableFeatures)
            if (!response.containsFeature(feature))
                response.addFeature(feature);
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
        synchronized (features)
        {
            features.remove(feature);
            discoveryManager.removeFeature(feature);
        }
        updateEntityCapsVersion();
    }

    /**
     * Add feature to put in "ext" attribute.
     *
     * @param ext ext feature to add
     */
    public void addExtFeature(String ext)
    {
        synchronized(extCapabilities)
        {
            extCapabilities.add(ext);
        }
    }

    /**
     * Remove "ext" feature.
     *
     * @param ext ext feature to remove
     */
    public void removeExtFeature(String ext)
    {
        synchronized(extCapabilities)
        {
            extCapabilities.remove(ext);
        }
    }

    /**
     * Get "ext" value.
     *
     * @return string that represents "ext" value
     */
    public synchronized String getExtFeatures()
    {
        StringBuilder bldr = new StringBuilder("");

        for(String e : extCapabilities)
        {
            bldr.append(e);
            bldr.append(" ");
        }

        return bldr.toString();
    }

    /**
     * Intercepts outgoing presence packets and adds entity capabilities at
     * their ends.
     *
     * @param packet the (hopefully presence) packet we need to add a "c"
     * element to.
     */
    @Override
    public void processStanza(Stanza packet)
    {
        if ((packet instanceof Presence) && (capsManager != null))
        {
            String ver = getEntityCapsVersion();
            CapsExtension caps
                = new CapsExtension(
                    capsManager.getNode(),
                    ver,
                    "sha-1");

            //make sure we'll be able to handle requests for the newly generated
            //node once we've used it.
            discoveryManager.setNodeInformationProvider(
                    caps.getNode() + "#" + caps.getVer(),
                    this);

            // Remove old capabilities extension if present
            ExtensionElement oldCaps
                = packet.getExtension(
                        CapsExtension.ELEMENT,
                        CapsExtension.NAMESPACE);
            if (oldCaps != null)
            {
                packet.removeExtension(oldCaps);
            }
            // Put new capabilities extension
            packet.addExtension(caps);
        }
    }

    /**
     * Returns a list of the Items
     * {@link DiscoverItems.Item} defined in the
     * node or in other words <tt>null</tt> since we don't support any.
     *
     * @return always <tt>null</tt> since we don't support items.
     */
    @Override
    public List<DiscoverItems.Item> getNodeItems()
    {
        return null;
    }

    /**
     * Returns a list of the features defined in the node. For
     * example, the entity caps protocol specifies that an XMPP client
     * should answer with each feature supported by the client version
     * or extension.
     *
     * @return a list of the feature strings defined in the node.
     */
    @Override
    public List<String> getNodeFeatures()
    {
        return getFeatures();
    }

    /**
     * Returns a list of the identities defined in the node. For example, the
     * x-command protocol must provide an identity of category automation and
     * type command-node for each command.
     *
     * @return a list of the Identities defined in the node.
     */
    @Override
    public List<DiscoverInfo.Identity> getNodeIdentities()
    {
        return identities;
    }

    @Override
    public List<ExtensionElement> getNodePacketExtensions()
    {
        return null;
    }

    /**
     * Initialize our local features copy in a way that would
     */
    private void initFeatures()
    {
        synchronized (features)
        {
            for (String feature : discoveryManager.getFeatures())
            {
                this.features.add(feature);
            }
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
            throws XMPPException, NotConnectedException, InterruptedException, NoResponseException
    {
        DiscoverInfo discoverInfo = capsManager.getDiscoverInfoByUser(entityID);
        EntityCapsManager.Caps caps = capsManager.getCapsByUser(entityID);

        if (discoverInfo != null && caps.isValid(discoverInfo))
            return discoverInfo;

        // if caps is not valid, has empty hash
        if (cacheNonCaps && (caps == null || !caps.isValid(discoverInfo)))
        {
            discoverInfo = nonCapsCache.get(entityID);
            if (discoverInfo != null)
                return discoverInfo;
        }

        discoverInfo
            = discoveryManager.discoverInfo(
                    entityID,
                    (caps == null) ? null : caps.getNodeVer());

        if ((caps != null) && !caps.isValid(discoverInfo))
        {
            if(!caps.hash.equals(""))
            {
                logger.error(
                        "Invalid DiscoverInfo for " + caps.getNodeVer() + ": "
                            + discoverInfo);
            }
            caps = null;
        }

        if (caps == null)
        {
            if (cacheNonCaps)
                nonCapsCache.put(entityID, discoverInfo);
        }
        else
            EntityCapsManager.addDiscoverInfoByCaps(caps, discoverInfo);
        return discoverInfo;
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by
     * its JID if locally cached, otherwise schedules for retrieval.
     *
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfoNonBlocking(Jid entityID)
        throws XMPPException
    {
        DiscoverInfo discoverInfo = capsManager.getDiscoverInfoByUser(entityID);
        EntityCapsManager.Caps caps = capsManager.getCapsByUser(entityID);

        if (discoverInfo != null && caps.isValid(discoverInfo))
            return discoverInfo;

        // if caps is not valid, has empty hash
        if (cacheNonCaps && (caps == null || !caps.isValid(discoverInfo)))
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
     * Returns the discovered items of a given XMPP entity addressed by its JID
     * and note attribute. Use this message only when trying to query
     * information which is not directly addressable.
     *
     * @param entityID the address of the XMPP entity.
     * @param node the attribute that supplements the 'jid' attribute.
     *
     * @return the discovered items.
     *
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(Jid entityID, String node)
            throws XMPPException, NotConnectedException, InterruptedException, NoResponseException
    {
        return discoveryManager.discoverItems(entityID, node);
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
     * Gets the <tt>EntityCapsManager</tt> which handles the entity capabilities
     * for this <tt>ScServiceDiscoveryManager</tt>.
     *
     * @return the <tt>EntityCapsManager</tt> which handles the entity
     * capabilities for this <tt>ScServiceDiscoveryManager</tt>
     */
    public EntityCapsManager getCapsManager()
    {
        return capsManager;
    }

    /**
     * Clears/stops what's needed.
     */
    public void stop()
    {
        if(retriever != null)
            retriever.stop();
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
        private final Map<Jid, EntityCapsManager.Caps> entities
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
                    Map.Entry<Jid, EntityCapsManager.Caps>
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

                        Iterator<Map.Entry<Jid, EntityCapsManager.Caps>>
                            iter = entities.entrySet().iterator();
                        if(iter.hasNext())
                        {
                            entityToProcess = iter.next();
                            iter.remove();
                        }
                    }

                    if(entityToProcess != null)
                    {
                        // process
                        requestDiscoveryInfo(
                            entityToProcess.getKey(),
                            entityToProcess.getValue());
                    }

                    entityToProcess = null;
                }
            } catch(Throwable t)
            {
                logger.error("Error requesting discovery info, " +
                    "thread ended unexpectedly", t);
            }
        }

        /**
         * Requests the discovery info and fires the event if
         * retrieved.
         * @param entityID the entity to request
         * @param caps and its capability.
         */
        private void requestDiscoveryInfo(final Jid entityID,
                                          EntityCapsManager.Caps caps)
        {
            try
            {
                DiscoverInfo discoverInfo = discoveryManager.discoverInfo(
                            entityID,
                            (caps == null ) ? null : caps.getNodeVer());

                if ((caps != null) && !caps.isValid(discoverInfo))
                {
                    if(!caps.hash.equals(""))
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
                    EntityCapsManager.addDiscoverInfoByCaps(caps, discoverInfo);
                    fireEvent = true;
                }

                // fire event
                if(fireEvent && capabilitiesOpSet != null)
                {
                    capabilitiesOpSet.fireContactCapabilitiesChanged(
                        entityID.asBareJid(),
                        capsManager.getFullJidsByBareJid(entityID.asBareJid())
                        );
                }
            }
            catch(XMPPException
                    | InterruptedException
                    | NoResponseException
                    | NotConnectedException ex)
            {
                // print discovery info errors only when trace is enabled
                if(logger.isTraceEnabled())
                    logger.error("Error requesting discover info for "
                        + entityID, ex);
            }
        }

        /**
         * Queue entities for retrieval.
         * @param entityID the entity.
         * @param caps and its capability.
         */
        public void addEntityForRetrieve(Jid entityID,
                                         EntityCapsManager.Caps caps)
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
