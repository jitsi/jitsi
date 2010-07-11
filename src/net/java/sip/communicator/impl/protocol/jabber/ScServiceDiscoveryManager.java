/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.caps.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An wrapper to smack's default {@link ServiceDiscoveryManager} that adds
 * support for XEP-0115 - Entity Capabilities.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 */
public class ScServiceDiscoveryManager
    implements PacketInterceptor,
               NodeInformationProvider
{
    /**
     * A flag that indicates whether we are currently storing non-caps
     */
    private static boolean cacheNonCaps=true;

    /**
     * The current version of our entity capabilities
     */
    private String currentCapsVersion = null;

    /**
     * currently unused. we'll start using this when we start querying for
     * client capabilities.
     */
    private Map<String, DiscoverInfo> nonCapsCache
        = new ConcurrentHashMap<String, DiscoverInfo>();

    /**
     * The caps manager instance we will be using to handle entity capabilities.
     */
    private final EntityCapsManager capsManager;

    /**
     * The {@link ServiceDiscoveryManager} that we are wrapping.
     */
    private final ServiceDiscoveryManager discoveryManager;

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
     * A {@link List} of the identities we use in our disco answers.
     */
    private final List<DiscoverInfo.Identity> identities;

    /**
     * Creates a new <tt>ScServiceDiscoveryManager</tt> wrapping the default
     * discovery manager of the specified <tt>connection</tt>.
     *
     * @param connection the {@link XMPPConnection} that this discovery manager
     * will be operating in.
     */
    public ScServiceDiscoveryManager(XMPPConnection connection)
    {
        this.discoveryManager
            = ServiceDiscoveryManager.getInstanceFor(connection);

        this.features = new ArrayList<String>();
        this.identities = new ArrayList<DiscoverInfo.Identity>();

        DiscoverInfo.Identity identity = new DiscoverInfo.Identity("client",
                        ServiceDiscoveryManager.getIdentityName());
        identity.setType(ServiceDiscoveryManager.getIdentityType());

        identities.add(identity);

        //add support for capabilities
        discoveryManager.addFeature(CapsPacketExtension.NAMESPACE);

        // For every XMPPConnection, add one EntityCapsManager.
        this.connection = connection;

        this.capsManager = new EntityCapsManager();
        capsManager.addPacketListener(connection);

        updateEntityCapsVersion();

        // Now, make sure we intercept presence packages and add caps data when
        // intended. XEP-0115 specifies that a client SHOULD include entity
        // capabilities with every presence notification it sends.
        PacketFilter capsPacketFilter = new PacketTypeFilter(Presence.class);

        connection.addPacketInterceptor(this, capsPacketFilter);


        initFeatures();
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
        // If a XMPPConnection is the managed one, see that the new
        // version is updated
        if (connection instanceof XMPPConnection)
        {
            if (capsManager != null)
            {
                capsManager.calculateEntityCapsVersion(getOwnDiscoverInfo(),
                        ServiceDiscoveryManager.getIdentityType(),
                        ServiceDiscoveryManager.getIdentityName(),
                        getFeatures(),
                        null);
            }
        }
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
        return features;
    }

    /**
     * Get a DiscoverInfo for the current entity caps node.
     *
     * @return a DiscoverInfo for the current entity caps node
     */
    public DiscoverInfo getOwnDiscoverInfo()
    {
        DiscoverInfo di = new DiscoverInfo();
        di.setType(IQ.Type.RESULT);
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
        if (capsManager != null)
        {
            return capsManager.getCapsVersion();
        }
        else
        {
            return null;
        }
    }

    /*
     * Add discover info response data.
     *
     * @param response the discover info response packet
     */
    // i really think we should remove this one
    public void addDiscoverInfoTo(DiscoverInfo response)
    {
        // Set this client identity
        DiscoverInfo.Identity identity = new DiscoverInfo.Identity("client",
                ServiceDiscoveryManager.getIdentityName());
        identity.setType(ServiceDiscoveryManager.getIdentityType());
        response.addIdentity(identity);
        // Add the registered features to the response

        // Add Entity Capabilities (XEP-0115) feature node.
        response.addFeature(CapsPacketExtension.NAMESPACE);

        for (String feature : getFeatures())
        {
            response.addFeature(feature);
        }
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
     * Intercepts outgoing presence packets and adds entity capabilities at
     * their ends.
     *
     * @param packet the (hopefully presence) packet we need to add a "c"
     * element to.
     */
    public void interceptPacket(Packet packet)
    {
        if(!(packet instanceof Presence))
            return;

        if (capsManager != null)
        {
            String ver = getEntityCapsVersion();
            CapsPacketExtension caps = new CapsPacketExtension(
                            null, capsManager.getNode(),
                            CapsPacketExtension.HASH_METHOD, ver);

            //make sure we'll be able to handle requests for the newly generated
            //node once we've used it.
            discoveryManager.setNodeInformationProvider(
                            caps.getNode() + "#" + caps.getVersion(), this);

            packet.addExtension(caps);
        }
    }

    /**
     * Returns a list of the Items {@link DiscoverItems.Item} defined in the
     * node or in other words <tt>null</tt> since we don't support any.
     *
     * @return always <tt>null</tt> since we don't support items.
     */
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
    public List<String> getNodeFeatures()
    {
        return getFeatures();
    }

    /**
     * Returns a list of the indentities defined in the node. For
     * example, the x-command protocol must provide an identity of
     * category automation and type command-node for each command.
     *
     * @return a list of the Identities defined in the node.
     */
    public List<DiscoverInfo.Identity> getNodeIdentities()
    {
        return identities;
    }

    /**
     * Initialize our local features copy in a way that would
     */
    private void initFeatures()
    {
        Iterator<String> defaultFeatures = discoveryManager.getFeatures();

        synchronized (features)
        {
            while (defaultFeatures.hasNext())
            {
                String feature = defaultFeatures.next();
                this.features.add( feature );
            }
        }
    }
}
