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
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.OSUtils;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.packet.*;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.osgi.framework.*;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Keeps track of entity capabilities.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class EntityCapsManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>EntityCapsManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(EntityCapsManager.class);

    /**
     * Static OSGi bundle context used by this class.
     */
    private static BundleContext bundleContext;

    /**
     * Configuration service instance used by this class.
     */
    private static ConfigurationService configService;

    /**
     * The prefix of the <tt>ConfigurationService</tt> properties which persist
     * {@link #caps2discoverInfo}.
     */
    private static final String CAPS_PROPERTY_NAME_PREFIX
        = "net.java.sip.communicator.impl.protocol.jabber.extensions.caps."
            + "EntityCapsManager.CAPS.";

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
     * The <tt>Map</tt> of <tt>Caps</tt> to <tt>DiscoverInfo</tt> which
     * associates a node#ver with the entity capabilities so that they don't
     * have to be retrieved every time their necessary. Because ver is
     * constructed from the entity capabilities using a specific hash method,
     * the hash method is also associated with the entity capabilities along
     * with the node and the ver in order to disambiguate cases of equal ver
     * values for different entity capabilities constructed using different hash
     * methods.
     */
    private static final Map<Caps, DiscoverInfo> caps2discoverInfo
        = new ConcurrentHashMap<Caps, DiscoverInfo>();

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource)
     */
    private final Map<Jid, Caps> userCaps
        = new ConcurrentHashMap<>();

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

    /**
     * Add {@link DiscoverInfo} to our caps database.
     * <p>
     * <b>Warning</b>: The specified <tt>DiscoverInfo</tt> is trusted to be
     * valid with respect to the specified <tt>Caps</tt> for performance reasons
     * because the <tt>DiscoverInfo</tt> should have already been validated in
     * order to be used elsewhere anyway.
     * </p>
     *
     * @param caps the <tt>Caps<tt/> i.e. the node, the hash and the ver for
     * which a <tt>DiscoverInfo</tt> is to be added to our caps database.
     * @param info {@link DiscoverInfo} for the specified <tt>Caps</tt>.
     */
    public static void addDiscoverInfoByCaps(Caps caps, DiscoverInfo info)
    {
        cleanupDiscoverInfo(info);
        /*
         * DiscoverInfo carries the node we're now associating it with a
         * specific node so we'd better keep them in sync.
         */
        info.setNode(caps.getNodeVer());

        synchronized (caps2discoverInfo)
        {
            DiscoverInfo oldInfo = caps2discoverInfo.put(caps, info);

            /*
             * If the specified info is a new association for the specified
             * node, remember it across application instances in order to not
             * query for it over the network.
             */
            if ((oldInfo == null) || !oldInfo.equals(info))
            {
                String xml = info.getChildElementXML().toString();

                if ((xml != null) && (xml.length() != 0))
                {
                    getConfigService()
                        .setProperty(getCapsPropertyName(caps), xml);
                }
            }
        }
    }

    /**
     * Gets the name of the property in the <tt>ConfigurationService</tt> which
     * is or is to be associated with a specific <tt>Caps</tt> value.
     *
     * @param caps the <tt>Caps</tt> value for which the associated
     * <tt>ConfigurationService</tt> property name is to be returned
     * @return the name of the property in the <tt>ConfigurationService</tt>
     * which is or is to be associated with a specific <tt>Caps</tt> value
     */
    private static String getCapsPropertyName(Caps caps)
    {
        return
            CAPS_PROPERTY_NAME_PREFIX
                + caps.node + '#' + caps.hash + '#' + caps.ver;
    }

    /**
     * Returns cached instance of {@link ConfigurationService}.
     */
    private static ConfigurationService getConfigService()
    {
        if (configService == null)
        {
            configService = ServiceUtils.getService(
                bundleContext, ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Sets OSGi bundle context instance that will be used by this class.
     * @param bundleContext the <tt>BundleContext</tt> instance to be used by
     *                      this class or <tt>null</tt> to clear the reference.
     */
    public static void setBundleContext(BundleContext bundleContext)
    {
        if (bundleContext == null)
        {
            configService = null;
        }
        EntityCapsManager.bundleContext = bundleContext;
    }

    /**
     * Add a record telling what entity caps node a user has.
     * @param user the user (Full JID)
     * @param node the node (of the caps packet extension)
     * @param hash the hashing algorithm used to calculate <tt>ver</tt>
     * @param ver the version (of the caps packet extension)
     * @param ext the ext (of the caps packet extension)
     * @param online indicates if the user is online
     */
    private void addUserCapsNode(Jid user,
                                 String node,
                                 String hash,
                                 String ver,
                                 boolean online)
    {
        if ((user != null) && (node != null) && (hash != null) && (ver != null))
        {
            Caps caps = userCaps.get(user);

            if ((caps == null)
                    || !caps.node.equals(node)
                    || !caps.hash.equals(hash)
                    || !caps.ver.equals(ver))
            {
                caps = new Caps(node, hash, ver);

                userCaps.put(user, caps);
            }
            else
                return;

            // Fire userCapsNodeAdded.
            UserCapsNodeListener[] listeners;

            synchronized (userCapsNodeListeners)
            {
                listeners
                    = userCapsNodeListeners.toArray(
                            NO_USER_CAPS_NODE_LISTENERS);
            }
            if (listeners.length != 0)
            {
                String nodeVer = caps.getNodeVer();

                for (UserCapsNodeListener listener : listeners)
                    listener.userCapsNodeAdded(user,
                        getFullJidsByBareJid(user.asBareJid()),
                        nodeVer, online);
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
     * Remove records telling what entity caps node a contact has.
     *
     * @param contact the contact
     */
    public void removeContactCapsNode(Contact contact)
    {
        Caps caps = null;
        Jid lastRemovedJid = null;
        Jid bareJid = null;
        try
        {
            bareJid = JidCreate.bareFrom(contact.getAddress());
        }
        catch (XmppStringprepException e)
        {
            logger.error("Contact address " + contact.getAddress()
                    + " is not a valid JID", e);
        }

        Iterator<Jid> iter = userCaps.keySet().iterator();
        while(iter.hasNext())
        {
            Jid jid = iter.next();

            if(jid.equals(contact.getAddress()))
            {
                caps = userCaps.get(jid);
                lastRemovedJid = jid;
                iter.remove();
            }
        }

        // fire only for the last one, at the end the event out
        // of the protocol will be one and for the contact
        if(caps != null)
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
                String nodeVer = caps.getNodeVer();

                for (UserCapsNodeListener listener : listeners)
                    listener.userCapsNodeRemoved(
                        lastRemovedJid,
                        getFullJidsByBareJid(bareJid),
                        nodeVer, false);
            }
        }
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

        Caps caps = userCaps.remove(user);
        Jid bareJid = user.asBareJid();

        // Fire userCapsNodeRemoved.
        if (caps != null)
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
                String nodeVer = caps.getNodeVer();

                for (UserCapsNodeListener listener : listeners)
                    listener.userCapsNodeRemoved(user,
                        getFullJidsByBareJid(bareJid),
                        nodeVer, false);
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
     * Gets the <tt>Caps</tt> i.e. the node, the hash and the ver of a user.
     *
     * @param user the user (Full JID)
     * @return the <tt>Caps</tt> i.e. the node, the hash and the ver of
     * <tt>user</tt>
     */
    public Caps getCapsByUser(Jid user)
    {
        return userCaps.get(user);
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
     * Get the discover info given a user name. The discover info is returned if
     * the user has a node#ver associated with it and the node#ver has a
     * discover info associated with it.
     *
     * @param user user name (Full JID)
     * @return the discovered info
     */
    public DiscoverInfo getDiscoverInfoByUser(Jid user)
    {
        Caps caps = userCaps.get(user);

        return (caps == null) ? null : getDiscoverInfoByCaps(caps);
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
     * @param caps the <tt>Caps</tt> i.e. the node, the hash and the ver
     * @return The corresponding DiscoverInfo or null if none is known.
     */
    public static DiscoverInfo getDiscoverInfoByCaps(Caps caps)
    {
        synchronized (caps2discoverInfo)
        {
            DiscoverInfo discoverInfo = caps2discoverInfo.get(caps);

            /*
             * If we don't have the discoverInfo in the runtime cache yet, we
             * may have it remembered in a previous application instance.
             */
            if (discoverInfo == null)
            {
                ConfigurationService configurationService
                    = getConfigService();
                String capsPropertyName = getCapsPropertyName(caps);
                String xml = configurationService.getString(capsPropertyName);

                if ((xml != null) && (xml.length() != 0))
                {
                    IQProvider discoverInfoProvider
                        = (IQProvider)
                            ProviderManager.getIQProvider(
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
                                        discoverInfoProvider.parse(parser);
                            }
                            catch (Exception ex)
                            {
                            }

                            if (discoverInfo != null)
                            {
                                if (caps.isValid(discoverInfo))
                                    caps2discoverInfo.put(caps, discoverInfo);
                                else
                                {
                                    logger.error(
                                            "Invalid DiscoverInfo for "
                                                + caps.getNodeVer()
                                                + ": "
                                                + discoverInfo);
                                    /*
                                     * The discoverInfo doesn't seem valid
                                     * according to the caps which means that we
                                     * must have stored invalid information.
                                     * Delete the invalid information in order
                                     * to not try to validate it again.
                                     */
                                    configurationService.removeProperty(
                                            capsPropertyName);
                                }
                            }
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
    private static void cleanupDiscoverInfo(DiscoverInfo info)
    {
        info.setFrom((Jid) null);
        info.setTo((Jid) null);
        info.setStanzaId(null);
    }

    /**
     * Registers this Manager's listener with <tt>connection</tt>.
     *
     * @param connection the connection that we'd like this manager to register
     * with.
     */
    public void addPacketListener(XMPPConnection connection)
    {
        StanzaFilter filter = new StanzaTypeFilter(Presence.class);

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

    /**
     * Computes and returns the hash of the specified <tt>capsString</tt> using
     * the specified <tt>hashAlgorithm</tt>.
     *
     * @param hashAlgorithm the name of the algorithm to be used to generate the
     * hash
     * @param capsString the capabilities string that we'd like to compute a
     * hash for.
     *
     * @return the hash of <tt>capsString</tt> computed by the specified
     * <tt>hashAlgorithm</tt> or <tt>null</tt> if generating the hash has failed
     */
    private static String capsToHash(String hashAlgorithm, String capsString)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            byte[] digest = md.digest(capsString.getBytes());
            return java.util.Base64.getEncoder().encodeToString(digest);
        }
        catch (NoSuchAlgorithmException nsae)
        {
            logger.error(
                    "Unsupported XEP-0115: Entity Capabilities hash algorithm: "
                        + hashAlgorithm);
            return null;
        }
    }

    /**
     * Converts the form field values in the <tt>ffValuesIter</tt> into a
     * caps string.
     *
     * @param ffValuesIter the {@link Iterator} containing the form field
     * values.
     * @param capsBldr a <tt>StringBuilder</tt> to which the caps string
     * representing the form field values is to be appended
     */
    private static void formFieldValuesToCaps(
            Iterator<String> ffValuesIter,
            StringBuilder capsBldr)
    {
        SortedSet<String> fvs = new TreeSet<String>();

        while( ffValuesIter.hasNext())
            fvs.add(ffValuesIter.next());

        for (String fv : fvs)
            capsBldr.append(fv).append('<');
    }

    /**
     * Calculates the <tt>String</tt> for a specific <tt>DiscoverInfo</tt> which
     * is to be hashed in order to compute the ver string for that
     * <tt>DiscoverInfo</tt>.
     *
     * @param discoverInfo the <tt>DiscoverInfo</tt> for which the
     * <tt>String</tt> to be hashed in order to compute its ver string is to be
     * calculated
     * @return the <tt>String</tt> for <tt>discoverInfo</tt> which is to be
     * hashed in order to compute its ver string
     */
    private static String calculateEntityCapsString(DiscoverInfo discoverInfo)
    {
        StringBuilder bldr = new StringBuilder();

        // Add identities
        {
            Iterator<DiscoverInfo.Identity> identities = discoverInfo.getIdentities().iterator();
            SortedSet<DiscoverInfo.Identity> is
                = new TreeSet<DiscoverInfo.Identity>(
                        new Comparator<DiscoverInfo.Identity>()
                        {
                            public int compare(
                                    DiscoverInfo.Identity i1,
                                    DiscoverInfo.Identity i2)
                            {
                                int category
                                    = i1.getCategory().compareTo(
                                            i2.getCategory());

                                if (category != 0)
                                    return category;

                                int type = i1.getType().compareTo(i2.getType());

                                if (type != 0)
                                    return type;

                                /*
                                 * TODO Sort by xml:lang.
                                 *
                                 * Since sort by xml:lang is currently missing,
                                 * use the last supported sort criterion i.e.
                                 * type.
                                 */
                                return type;
                            }
                        });

            if (identities != null)
                while (identities.hasNext())
                    is.add(identities.next());

            for (DiscoverInfo.Identity i : is)
            {
                bldr
                    .append(i.getCategory())
                        .append('/')
                            .append(i.getType())
                                .append("//")
                                    .append(i.getName())
                                        .append('<');
            }
        }

        // Add features
        {
            List<Feature> features = discoverInfo.getFeatures();
            SortedSet<String> fs = new TreeSet<String>();

            if (features != null)
            {
                for (Feature f : features)
                {
                    fs.add(f.getVar());
                }
            }

            for (String f : fs)
                bldr.append(f).append('<');
        }

        DataForm extendedInfo
            = (DataForm) discoverInfo.getExtension("x", "jabber:x:data");

        if (extendedInfo != null)
        {
            synchronized (extendedInfo)
            {
                SortedSet<FormField> fs
                    = new TreeSet<FormField>(
                            new Comparator<FormField>()
                            {
                                public int compare(FormField f1,
                                                FormField f2)
                                {
                                    return
                                        f1.getVariable().compareTo(
                                                f2.getVariable());
                                }
                            });

                FormField formType = null;

                for (Iterator<FormField> fieldsIter = extendedInfo.getFields().iterator();
                     fieldsIter.hasNext();)
                {
                    FormField f = fieldsIter.next();
                    if (!f.getVariable().equals("FORM_TYPE"))
                        fs.add(f);
                    else
                        formType = f;
                }

                // Add FORM_TYPE values
                if (formType != null)
                    formFieldValuesToCaps(formType.getValues().iterator(), bldr);

                // Add the other values
                for (FormField f : fs)
                {
                    bldr.append(f.getVariable()).append('<');
                    formFieldValuesToCaps(f.getValues().iterator(), bldr);
                }
            }
        }

        return bldr.toString();
    }

    /**
     * Calculates the ver string for the specified <tt>discoverInfo</tt>,
     * identity type, name features, and extendedInfo.
     *
     * @param discoverInfo the {@link DiscoverInfo} we'd be creating a ver
     * <tt>String</tt> for
     */
    public void calculateEntityCapsVersion(DiscoverInfo discoverInfo)
    {
        setCurrentCapsVersion(
            discoverInfo,
            capsToHash(
                "sha-1",
                calculateEntityCapsString(discoverInfo)));
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
        Caps caps = new Caps(getNode(), "sha-1", capsVersion);

        /*
         * DiscoverInfo carries the node and the ver and we're now setting a new
         * ver so we should update the DiscoveryInfo.
         */
        discoverInfo.setNode(caps.getNodeVer());

        if (!caps.isValid(discoverInfo))
        {
            throw
                new IllegalArgumentException(
                        "The specified discoverInfo must be valid with respect"
                            + " to the specified capsVersion");
        }

        currentCapsVersion = capsVersion;
        addDiscoverInfoByCaps(caps, discoverInfo);
        fireCapsVerChanged();
    }

    /**
     * The {@link StanzaListener} that will be registering incoming caps.
     */
    private class CapsPacketListener
        implements StanzaListener
    {
        /**
         * Handles incoming presence packets and maps jids to node#ver strings.
         *
         * @param packet the incoming presence <tt>Packet</tt> to be handled
         */
        public void processStanza(Stanza packet)
        {
            // Check it the packet indicates  that the user is online. We
            // will use this information to decide if we're going to send
            // the discover info request.
            boolean online
                = (packet instanceof Presence)
                        && ((Presence) packet).isAvailable();

            CapsExtension ext
                = (CapsExtension)
                    packet.getExtension(
                            CapsExtension.ELEMENT,
                            CapsExtension.NAMESPACE);

            if(ext != null && online)
            {
                /*
                 * Before Version 1.4 of XEP-0115: Entity Capabilities,
                 * the 'ver' attribute was generated differently and the 'hash'
                 * attribute was absent. The 'ver' attribute in Version 1.3
                 * represents the specific version of the client and thus does
                 * not provide a way to validate the DiscoverInfo sent by
                 * the client. If EntityCapsManager receives no 'hash'
                 * attribute, it will assume the legacy format and will not
                 * cache it because the DiscoverInfo to be received from
                 * the client later on will not be trustworthy.
                 */
                String hash = ext.getHash();

                /* Google Talk web does not set hash, but we need it to
                 * be cached
                 */
                if (hash == null)
                    hash = "";

                addUserCapsNode(
                        packet.getFrom(),
                        ext.getNode(), hash, ext.getVer(), online);
            }
            else if (!online)
            {
                removeUserCapsNode(packet.getFrom());
            }
        }
    }

    /**
     * Implements an immutable value which stands for a specific node, a
     * specific hash (algorithm) and a specific ver.
     *
     * @author Lyubomir Marinov
     */
    public static class Caps
    {
        /** The hash (algorithm) of this <tt>Caps</tt> value. */
        public final String hash;

        /** The node of this <tt>Caps</tt> value. */
        public final String node;

        /**
         * The String which is the concatenation of {@link #node} and the
         * {@link #ver} separated by the character '#'. Cached for the sake of
         * efficiency.
         */
        private final String nodeVer;

        /** The ver of this <tt>Caps</tt> value. */
        public final String ver;

        /**
         * Initializes a new <tt>Caps</tt> instance which is to represent a
         * specific node, a specific hash (algorithm) and a specific ver.
         *
         * @param node the node to be represented by the new instance
         * @param hash the hash (algorithm) to be represented by the new
         * instance
         * @param ver the ver to be represented by the new instance
         */
        public Caps(String node, String hash, String ver)
        {
            if (node == null)
                throw new NullPointerException("node");
            if (hash == null)
                throw new NullPointerException("hash");
            if (ver == null)
                throw new NullPointerException("ver");

            this.node = node;
            this.hash = hash;
            this.ver = ver;

            this.nodeVer = this.node + '#' + this.ver;
        }

        /**
         * Gets a <tt>String</tt> which represents the concatenation of the
         * <tt>node</tt> property of this instance, the character '#' and the
         * <tt>ver</tt> property of this instance.
         *
         * @return a <tt>String</tt> which represents the concatenation of the
         * <tt>node</tt> property of this instance, the character '#' and the
         * <tt>ver</tt> property of this instance
         */
        public final String getNodeVer()
        {
            return nodeVer;
        }

        /**
         * Determines whether a specific <tt>DiscoverInfo</tt> is valid
         * according to this <tt>Caps</tt> i.e. whether the
         * <tt>discoverInfo</tt> has the node and the ver of this <tt>Caps</tt>
         * and the ver calculated from the <tt>discoverInfo</tt> using the hash
         * (algorithm) of this <tt>Caps</tt> is equal to the ver of this
         * <tt>Caps</tt>.
         *
         * @param discoverInfo the <tt>DiscoverInfo</tt> to be validated by this
         * <tt>Caps</tt>
         * @return <tt>true</tt> if the specified <tt>DiscoverInfo</tt> has the
         * node and the ver of this <tt>Caps</tt> and the ver calculated from
         * the <tt>discoverInfo</tt> using the hash (algorithm) of this
         * <tt>Caps</tt> is equal to the ver of this <tt>Caps</tt>; otherwise,
         * <tt>false</tt>
         */
        public boolean isValid(DiscoverInfo discoverInfo)
        {
            if(discoverInfo != null)
            {
                // The "node" attribute is not necessary in the query element.
                // For example, Swift does not send back the "node" attribute in
                // the Disco#info response. Thus, if the node of the IQ response
                // is null, then we set it to the request one.
                if(discoverInfo.getNode() == null)
                {
                    discoverInfo.setNode(getNodeVer());
                }

                if(getNodeVer().equals(discoverInfo.getNode())
                    && !hash.equals("")
                    && ver.equals(
                            capsToHash(
                                hash,
                                calculateEntityCapsString(discoverInfo))))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Caps caps = (Caps) o;

            if(!hash.equals(caps.hash)) return false;
            if(!node.equals(caps.node)) return false;
            if(!ver.equals(caps.ver)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = hash.hashCode();
            result = 31 * result + node.hashCode();
            result = 31 * result + ver.hashCode();
            return result;
        }
    }
}
