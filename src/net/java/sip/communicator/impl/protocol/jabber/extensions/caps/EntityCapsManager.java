/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.impl.protocol.jabber.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;
import org.xmlpull.mxp1.*;
import org.xmlpull.v1.*;

/**
 * Keeps track of entity capabilities.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class EntityCapsManager
{
    /**
     * The prefix of the <tt>ConfigurationService</tt> properties which persist
     * {@link #caps}.
     */
    private static final String CAPS_PROPERTY_NAME_PREFIX
        = "net.java.sip.communicator.impl.protocol.jabber.extensions.caps."
            + "EntityCapsManager.CAPS.";

    /**
     * The hash method we use for generating the ver string.
     */
    public static final String HASH_METHOD_CAPS = "SHA-1";

    /**
     * An empty array of <tt>UserCapsNodeListener</tt> elements explicitly
     * defined in order to reduce unnecessary allocations.
     */
    private static final UserCapsNodeListener[] NO_USER_CAPS_NODE_LISTENERS
        = new UserCapsNodeListener[0];

    /**
     * The node value to advertise.
     */
    private static String entityNode = "http://sip-communicator.org";

    /**
     * Map of (node, hash algorithm) -&gt; DiscoverInfo.
     */
    private static final Map<String, DiscoverInfo> caps
        = new ConcurrentHashMap<String, DiscoverInfo>();

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource)
     */
    private final Map<String, String> userCaps
        = new ConcurrentHashMap<String, String>();

    /**
     * CapsVerListeners gets notified when the version string is changed.
     */
    private final Set<CapsVerListener> capsVerListeners
        = new CopyOnWriteArraySet<CapsVerListener>();

    /**
     * The current hash of our version and supported features.
     */
    private String currentCapsVersion = null;

    /**
     * The list of <tt>UserCapsNodeListener</tt>s interested in events notifying
     * about changes in the list of user caps nodes of this
     * <tt>EntityCapsManager</tt>.
     */
    private final List<UserCapsNodeListener> userCapsNodeListeners
        = new LinkedList<UserCapsNodeListener>();

    static
    {
        ProviderManager.getInstance().addExtensionProvider(
                CapsPacketExtension.ELEMENT_NAME,
                CapsPacketExtension.NAMESPACE,
                new CapsProvider());
    }

    /**
     * Add {@link DiscoverInfo} to the our caps database.
     *
     * @param node The node name. Could be for example
     * "http://sip-communicator.org#q07IKJEyjvHSyhy//CH0CxmKi8w=".
     * @param info {@link DiscoverInfo} for the specified node.
     */
    public static void addDiscoverInfoByNode(String node, DiscoverInfo info)
    {
        cleanupDicsoverInfo(info);

        synchronized (caps)
        {
            DiscoverInfo oldInfo = caps.put(node, info);

            /*
             * If the specified info is a new association for the specified
             * node, remember it across application instances in order to not
             * query for it over the network.
             */
            if ((oldInfo == null) || !oldInfo.equals(info))
            {
                String xml = info.getChildElementXML();

                if ((xml != null) && (xml.length() != 0))
                {
                    JabberActivator
                        .getConfigurationService()
                            .setProperty(CAPS_PROPERTY_NAME_PREFIX + node, xml);
                }
            }
        }
    }

    /**
     * Add a record telling what entity caps node a user has. The entity caps
     * node has the format node#ver.
     *
     * @param user the user (Full JID)
     * @param node the entity caps node#ver
     */
    public void addUserCapsNode(String user, String node)
    {
        if ((user != null) && (node != null))
        {
            String oldNode = userCaps.put(user, node);

            // Fire userCapsNodeAdded.
            if ((oldNode == null) || !oldNode.equals(node))
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
                        listener.userCapsNodeAdded(user, node);
                }
            }
        }
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
     * Remove a record telling what entity caps node a user has.
     *
     * @param user the user (Full JID)
     */
    public void removeUserCapsNode(String user)
    {
        String node = userCaps.remove(user);

        // Fire userCapsNodeRemoved.
        if (node != null)
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
                    listener.userCapsNodeRemoved(user, node);
            }
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
     * Get the Node version (node#ver) of a user.
     *
     * @param user the user (Full JID)
     * @return the node version.
     */
    public String getNodeVersionByUser(String user)
    {
        return userCaps.get(user);
    }

    /**
     * Get the discover info given a user name. The discover info is returned if
     * the user has a node#ver associated with it and the node#ver has a
     * discover info associated with it.
     *
     * @param user user name (Full JID)
     * @return the discovered info
     */
    public DiscoverInfo getDiscoverInfoByUser(String user)
    {
        String capsNode = userCaps.get(user);

        return (capsNode == null) ? null : getDiscoverInfoByNode(capsNode);
    }

    /**
     * Get our own caps version.
     *
     * @return our own caps version
     */
    public String getCapsVersion()
    {
        return currentCapsVersion;
    }

    /**
     * Get our own entity node.
     *
     * @return our own entity node.
     */
    public String getNode()
    {
        return entityNode;
    }

    /**
     * Set our own entity node.
     *
     * @param node the new node
     */
    public void setNode(String node)
    {
        entityNode = node;
    }

    /**
     * Retrieve DiscoverInfo for a specific node.
     *
     * @param node The node name.
     * @return The corresponding DiscoverInfo or null if none is known.
     */
    public static DiscoverInfo getDiscoverInfoByNode(String node)
    {
        synchronized (caps)
        {
            DiscoverInfo discoverInfo = caps.get(node);

            /*
             * If we don't have the discoverInfo in the runtime cache yet, we
             * may have it remembered in a previous application instance. 
             */
            if (discoverInfo == null)
            {
                String xml
                    = JabberActivator.getConfigurationService().getString(
                            CAPS_PROPERTY_NAME_PREFIX + node);

                if ((xml != null) && (xml.length() != 0))
                {
                    IQProvider discoverInfoProvider
                        = (IQProvider)
                            ProviderManager.getInstance().getIQProvider(
                                    "query",
                                    "http://jabber.org/protocol/disco#info");

                    if (discoverInfoProvider != null)
                    {
                        XmlPullParser parser = new MXParser();

                        try
                        {
                            parser.setFeature(
                                    XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                                    true);
                            parser.setInput(new StringReader(xml));
                            // Start the parser.
                            parser.next();
                        }
                        catch (XmlPullParserException xppex)
                        {
                            parser = null;
                        }
                        catch (IOException ioex)
                        {
                            parser = null;
                        }

                        if (parser != null)
                        {
                            try
                            {
                                discoverInfo
                                    = (DiscoverInfo)
                                        discoverInfoProvider.parseIQ(parser);
                            }
                            catch (Exception ex)
                            {
                            }

                            if (discoverInfo != null)
                                caps.put(node, discoverInfo);
                        }
                    }
                }
            }
            return discoverInfo;
        }
    }

    /**
     * Removes from, to and packet-id from <tt>info</tt>.
     *
     * @param info the {@link DiscoverInfo} that we'd like to cleanup.
     */
    private static void cleanupDicsoverInfo(DiscoverInfo info)
    {
        info.setFrom(null);
        info.setTo(null);
        info.setPacketID(null);
    }

    /**
     * Registers this Manager's listener with <tt>connection</tt>.
     *
     * @param connection the connection that we'd like this manager to register
     * with.
     */
    public void addPacketListener(XMPPConnection connection)
    {
        PacketFilter filter
            = new AndFilter(
                    new PacketTypeFilter(Presence.class),
                    new PacketExtensionFilter(
                            CapsPacketExtension.ELEMENT_NAME,
                            CapsPacketExtension.NAMESPACE));

        connection.addPacketListener(new CapsPacketListener(), filter);
    }

    /**
     * Adds <tt>listener</tt> to the list of {@link CapsVerListener}s that we
     * notify when new features occur and the version hash needs to be
     * regenerated. The method would also notify <tt>listener</tt> if our
     * current caps version has been generated and is different than
     * <tt>null</tt>.
     *
     * @param listener the {@link CapsVerListener} we'd like to register.
     */
    public void addCapsVerListener(CapsVerListener listener)
    {
        synchronized (capsVerListeners)
        {
            if (capsVerListeners.contains(listener))
                return;

            capsVerListeners.add(listener);

            if (currentCapsVersion != null)
                listener.capsVerUpdated(currentCapsVersion);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of currently registered
     * {@link CapsVerListener}s.
     *
     * @param listener the {@link CapsVerListener} we'd like to unregister.
     */
    public void removeCapsVerListener(CapsVerListener listener)
    {
        synchronized(capsVerListeners)
        {
            capsVerListeners.remove(listener);
        }
    }

    /**
     * Notifies all currently registered {@link CapsVerListener}s that the
     * version hash has changed.
     */
    private void fireCapsVerChanged()
    {
        List<CapsVerListener> listenersCopy = null;

        synchronized(capsVerListeners)
        {
            listenersCopy = new ArrayList<CapsVerListener>(capsVerListeners);
        }

        for (CapsVerListener listener : listenersCopy)
            listener.capsVerUpdated(currentCapsVersion);
    }

    /*
     * public void spam() { System.err.println("User nodes:"); for
     * (Map.Entry<String,String> e : userCaps.entrySet()) {
     * System.err.println(" * " + e.getKey() + " -> " + e.getValue()); }
     *
     * System.err.println("Caps versions:"); for (Map.Entry<String,DiscoverInfo>
     * e : caps.entrySet()) { System.err.println(" * " + e.getKey() + " -> " +
     * e.getValue()); } }
     */

    // /////////
    // Calculate Entity Caps Version String
    // /////////

    /**
     * Computes and returns the SHA-1 hash of the specified <tt>capsString</tt>.
     *
     * @param capsString the capabilities string that we'd like to compute a
     * hash for.
     *
     * @return the SHA-1 hash of <tt>capsString</tt> or <tt>null</tt> if
     * generating the hash has failed.
     */
    private static String capsToHash(String capsString)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance(HASH_METHOD_CAPS);
            byte[] digest = md.digest(capsString.getBytes());
            return Base64.encodeBytes(digest);
        }
        catch (NoSuchAlgorithmException nsae)
        {
            return null;
        }
    }

    /**
     * Converts the form field values in the <tt>ffValuesIter</tt> into a
     * caps string.
     *
     * @param ffValuesIter the {@link Iterator} containing the form field
     * values.
     *
     * @return a <tt>String</tt> containing all the form field values.
     */
    private static String formFieldValuesToCaps(Iterator<String> ffValuesIter)
    {
        StringBuilder bldr = new StringBuilder();
        SortedSet<String> fvs = new TreeSet<String>();

        while( ffValuesIter.hasNext())
        {
            fvs.add(ffValuesIter.next());
        }

        for (String fv : fvs)
        {
            bldr.append( fv + "<");
        }

        return bldr.toString();
    }

    /**
     * Calculates the ver string for the specified <tt>discoverInfo</tt>,
     * identity type, name features, and extendedInfo.
     *
     * @param discoverInfo the {@link DiscoverInfo} we'd be creating a ver
     * <tt>String</tt> for.
     * @param identityType identity type (e.g. pc).
     * @param identityName our identity name (the name of the application)
     * @param features the list of features we'd like to encode.
     * @param extendedInfo any extended information we'd like to add to the ver
     * <tt>String</tt>
     */
    public void calculateEntityCapsVersion(DiscoverInfo     discoverInfo,
                                           String           identityType,
                                           String           identityName,
                                           List<String>     features,
                                           DataForm         extendedInfo)
    {
        StringBuilder bldr = new StringBuilder();

        // Add identity
        // FIXME language
        bldr.append( "client/" + identityType + "//" + identityName + "<");

        // Add features
        synchronized (features)
        {
            SortedSet<String> fs = new TreeSet<String>();
            for(String f : features)
            {
                fs.add(f);
            }

            for (String f : fs)
            {
                bldr.append( f + "<" );
            }
        }

        if (extendedInfo != null)
        {
            synchronized (extendedInfo)
            {
                SortedSet<FormField> fs = new TreeSet<FormField>(
                                new Comparator<FormField>()
                                {
                                    public int compare(FormField f1,
                                                    FormField f2)
                                    {
                                        return f1.getVariable().compareTo(
                                                        f2.getVariable());
                                    }
                                });

                FormField formType = null;

                for (Iterator<FormField> fieldsIter = extendedInfo.getFields();
                     fieldsIter.hasNext();)
                {
                    FormField f = fieldsIter.next();
                    if (!f.getVariable().equals("FORM_TYPE"))
                    {
                        fs.add(f);
                    } else
                    {
                        formType = f;
                    }
                }

                // Add FORM_TYPE values
                if (formType != null)
                {
                    bldr.append( formFieldValuesToCaps(formType.getValues()) );
                }

                // Add the other values
                for (FormField f : fs)
                {
                    bldr.append( f.getVariable() + "<" );
                    bldr.append( formFieldValuesToCaps(f.getValues()) );
                }
            }
        }

        setCurrentCapsVersion(discoverInfo, capsToHash(bldr.toString()));
    }

    /**
     * Set our own caps version.
     *
     * @param discoverInfo the {@link DiscoverInfo} that we'd like to map to the
     * <tt>capsVersion</tt>.
     * @param capsVersion the new caps version
     */
    public void setCurrentCapsVersion(DiscoverInfo discoverInfo,
                                      String capsVersion)
    {
        currentCapsVersion = capsVersion;
        addDiscoverInfoByNode(getNode() + "#" + capsVersion, discoverInfo);
        fireCapsVerChanged();
    }

    /**
     * The {@link PacketListener} that will be registering incoming caps.
     */
    private class CapsPacketListener
        implements PacketListener
    {
        /**
         * Handles incoming presence packets and maps jids to node#ver strings.
         *
         * @param packet the incoming presence <tt>Packet</tt> to be handled
         */
        public void processPacket(Packet packet)
        {
            CapsPacketExtension ext
                = (CapsPacketExtension)
                    packet.getExtension(
                            CapsPacketExtension.ELEMENT_NAME,
                            CapsPacketExtension.NAMESPACE);
            String nodeVer = ext.getNode() + "#" + ext.getVersion();
            String user = packet.getFrom();

            addUserCapsNode(user, nodeVer);
        }
    }
}
