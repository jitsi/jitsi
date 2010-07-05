/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

import java.util.*;
import java.util.concurrent.*;
import java.security.*;

/**
 * Keeps track of entity capabilities.
 *
 * This work is based on Jonas Ådahl's smack fork.
 */
public class EntityCapsManager
{

    /**
     * The hash method we use for generating the ver string.
     */
    //public static final String HASH_METHOD = "sha-1";

    /**
     * The hash method we use for generating the ver string.
     */
    public static final String HASH_METHOD_CAPS = "SHA-1";

    /**
     * The node value to advertise.
     */
    private static String entityNode = "http://sip-communicator.org";

    /**
     * Map of (node, hash algorithm) -&gt; DiscoverInfo.
     */
    private static Map<String, DiscoverInfo> caps
                            = new ConcurrentHashMap<String, DiscoverInfo>();

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource)
     */
    private Map<String, String> userCaps
                                    = new ConcurrentHashMap<String, String>();

    /**
     * CapsVerListeners gets notified when the version string is changed.
     */
    private Set<CapsVerListener> capsVerListeners
                        = new CopyOnWriteArraySet<CapsVerListener>();

    /**
     * The current hash of our version and supported features.
     */
    private String currentCapsVersion = null;

    static
    {
        ProviderManager.getInstance().addExtensionProvider(
            CapsPacketExtension.ELEMENT_NAME, CapsPacketExtension.NAMESPACE,
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

        caps.put(node, info);
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
        if (user != null && node != null)
        {
            userCaps.put(user, node);
        }
    }

    /**
     * Remove a record telling what entity caps node a user has.
     *
     * @param user the user (Full JID)
     */
    public void removeUserCapsNode(String user)
    {
        userCaps.remove(user);
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
        if (capsNode == null)
            return null;

        return getDiscoverInfoByNode(capsNode);
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
        return caps.get(node);
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
            = new AndFilter(new PacketTypeFilter(Presence.class),
                   new PacketExtensionFilter(CapsPacketExtension.ELEMENT_NAME,
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
        {
            listener.capsVerUpdated(currentCapsVersion);
        }
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
     * @param capsString
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

    private static String formFieldValuesToCaps(Iterator<String> i)
    {
        String s = "";
        SortedSet<String> fvs = new TreeSet<String>();
        for (; i.hasNext();)
        {
            fvs.add(i.next());
        }
        for (String fv : fvs)
        {
            s += fv + "<";
        }
        return s;
    }

    void calculateEntityCapsVersion(DiscoverInfo discoverInfo,
                    String identityType, String identityName,
                    List<String> features, DataForm extendedInfo)
    {
        String s = "";

        // Add identity
        // FIXME language
        s += "client/" + identityType + "//" + identityName + "<";

        // Add features
        synchronized (features)
        {
            SortedSet<String> fs = new TreeSet<String>();
            for (String f : features)
            {
                fs.add(f);
            }

            for (String f : fs)
            {
                s += f + "<";
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

                FormField ft = null;

                for (Iterator<FormField> i = extendedInfo.getFields(); i
                                .hasNext();)
                {
                    FormField f = i.next();
                    if (!f.getVariable().equals("FORM_TYPE"))
                    {
                        fs.add(f);
                    } else
                    {
                        ft = f;
                    }
                }

                // Add FORM_TYPE values
                if (ft != null)
                {
                    s += formFieldValuesToCaps(ft.getValues());
                }

                // Add the other values
                for (FormField f : fs)
                {
                    s += f.getVariable() + "<";
                    s += formFieldValuesToCaps(f.getValues());
                }
            }
        }

        setCurrentCapsVersion(discoverInfo, capsToHash(s));
    }

    /**
     * Set our own caps version.
     *
     * @param capsVersion the new caps version
     */
    public void setCurrentCapsVersion(DiscoverInfo discoverInfo,
                    String capsVersion)
    {
        currentCapsVersion = capsVersion;
        addDiscoverInfoByNode(getNode() + "#" + capsVersion, discoverInfo);
        fireCapsVerChanged();
    }

    class CapsPacketListener implements PacketListener
    {

        public void processPacket(Packet packet)
        {
            CapsPacketExtension ext = (CapsPacketExtension) packet.getExtension(
                CapsPacketExtension.ELEMENT_NAME,
                CapsPacketExtension.NAMESPACE);

            String nodeVer = ext.getNode() + "#" + ext.getVersion();
            String user = packet.getFrom();

            addUserCapsNode(user, nodeVer);

            // DEBUG
            // spam();
        }
    }
}
