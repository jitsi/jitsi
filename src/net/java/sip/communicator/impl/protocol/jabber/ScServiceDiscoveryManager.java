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
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An wrapper to smack's default {@link ServiceDiscoveryManager} that adds
 * support for XEP-0115 - Entity Capabilities.
 *
 * This work is based on Jonas Ådahl's smack fork.
 *
 * @author Emil Ivov
 */
public class ScServiceDiscoveryManager
{
    /**
     * A flag that indicates whether we are currently storing non-caps
     */
    private static boolean cacheNonCaps=true;

    /**
     * The current version of our entity capabilities
     */
    private String currentCapsVersion = null;

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

        //add support for capabilities
        discoveryManager.addFeature(CapsPacketExtension.NAMESPACE);

        // For every XMPPConnection, add one EntityCapsManager.
        this.connection = connection;

        this.capsManager = new EntityCapsManager();
        capsManager.addPacketListener(connection);

        updateEntityCapsVersion();
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
                        discoveryManager.getFeatures(),
                        null);
            }
        }
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

    /**
     * Add discover info response data.
     *
     * @param response the discover info response packet
     */
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

        for (Iterator<String> it = discoveryManager.getFeatures(); it.hasNext();)
        {
            response.addFeature(it.next());
        }
    }


}
