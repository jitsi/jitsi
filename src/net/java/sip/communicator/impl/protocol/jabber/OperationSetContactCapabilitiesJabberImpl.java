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

import net.java.sip.communicator.impl.protocol.jabber.extensions.caps.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Represents an <tt>OperationSet</tt> to query the <tt>OperationSet</tt>s
 * supported for a specific Jabber <tt>Contact</tt>. The <tt>OperationSet</tt>s
 * reported as supported for a specific Jabber <tt>Contact</tt> are considered
 * by the associated protocol provider to be capabilities possessed by the
 * Jabber <tt>Contact</tt> in question.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public class OperationSetContactCapabilitiesJabberImpl
    extends AbstractOperationSetContactCapabilities<ProtocolProviderServiceJabberImpl>
    implements  UserCapsNodeListener,
                ContactPresenceStatusListener
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetContactCapabilitiesJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetContactCapabilitiesJabberImpl.class);

    /**
     * The list of <tt>OperationSet</tt> capabilities presumed to be supported
     * by a <tt>Contact</tt> when it is offline.
     */
    private static final Set<Class<? extends OperationSet>>
        OFFLINE_OPERATION_SETS
            = new HashSet<Class<? extends OperationSet>>();

    /**
     * The <tt>Map</tt> which associates specific <tt>OperationSet</tt> classes
     * with the features to be supported by a <tt>Contact</tt> in order to
     * consider the <tt>Contact</tt> to possess the respective
     * <tt>OperationSet</tt> capability.
     */
    private static final Map<Class<? extends OperationSet>, String[]>
        OPERATION_SETS_TO_FEATURES
            = new HashMap<Class<? extends OperationSet>, String[]>();

    static
    {
        OFFLINE_OPERATION_SETS.add(OperationSetBasicInstantMessaging.class);
        OFFLINE_OPERATION_SETS.add(OperationSetMessageCorrection.class);
        OFFLINE_OPERATION_SETS.add(OperationSetServerStoredContactInfo.class);

        OPERATION_SETS_TO_FEATURES.put(
                OperationSetBasicTelephony.class,
                new String[]
                {
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE,
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP,
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP_AUDIO
                });

        OPERATION_SETS_TO_FEATURES.put(
            OperationSetVideoTelephony.class,
            new String[]
            {
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE,
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP,
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP_VIDEO
            });

        OPERATION_SETS_TO_FEATURES.put(
                OperationSetDesktopSharingServer.class,
                new String[]
                {
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE,
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP,
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RTP_VIDEO
                });

        OPERATION_SETS_TO_FEATURES.put(
                OperationSetMessageCorrection.class,
                new String[]
                {
                    MessageCorrectionExtension.NAMESPACE
                });
    }

    /**
     * The <tt>EntityCapsManager</tt> associated with the
     * <tt>discoveryManager</tt> of {@link #parentProvider}.
     */
    private EntityCapsManager capsManager;

    /**
     * Initializes a new <tt>OperationSetContactCapabilitiesJabberImpl</tt>
     * instance which is to be provided by a specific
     * <tt>ProtocolProviderServiceJabberImpl</tt>.
     *
     * @param parentProvider the <tt>ProtocolProviderServiceJabberImpl</tt>
     * which will provide the new instance
     */
    public OperationSetContactCapabilitiesJabberImpl(
            ProtocolProviderServiceJabberImpl parentProvider)
    {
        super(parentProvider);

        OperationSetPresence presenceOpSet
            = parentProvider.getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null)
            presenceOpSet.addContactPresenceStatusListener(this);
    }

    /**
     * Gets the <tt>OperationSet</tt> corresponding to the specified
     * <tt>Class</tt> and supported by the specified <tt>Contact</tt>. If the
     * returned value is non-<tt>null</tt>, it indicates that the
     * <tt>Contact</tt> is considered by the associated protocol provider to
     * possess the <tt>opsetClass</tt> capability. Otherwise, the associated
     * protocol provider considers <tt>contact</tt> to not have the
     * <tt>opsetClass</tt> capability.
     *
     * @param <U> the type extending <tt>OperationSet</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param contact the <tt>Contact</tt> for which the <tt>opsetClass</tt>
     * capability is to be queried
     * @param opsetClass the <tt>OperationSet</tt> <tt>Class</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return the <tt>OperationSet</tt> corresponding to the specified
     * <tt>opsetClass</tt> which is considered by the associated protocol
     * provider to be possessed as a capability by the specified
     * <tt>contact</tt>; otherwise, <tt>null</tt>
     * @see AbstractOperationSetContactCapabilities#getOperationSet(Contact,
     * Class)
     */
    @Override
    protected <U extends OperationSet> U getOperationSet(
            Contact contact,
            Class<U> opsetClass,
            boolean online)
    {
        String jid = parentProvider.getFullJid(contact);
        if (jid == null)
            jid = contact.getAddress();

        return getOperationSet(jid, opsetClass, online);
    }

    /**
     * Gets the <tt>OperationSet</tt>s supported by a specific <tt>Contact</tt>.
     * The returned <tt>OperationSet</tt>s are considered by the associated
     * protocol provider to capabilities possessed by the specified
     * <tt>contact</tt>.
     *
     * @param contact the <tt>Contact</tt> for which the supported
     * <tt>OperationSet</tt> capabilities are to be retrieved
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return a <tt>Map</tt> listing the <tt>OperationSet</tt>s considered by
     * the associated protocol provider to be supported by the specified
     * <tt>contact</tt> (i.e. to be possessed as capabilities). Each supported
     * <tt>OperationSet</tt> capability is represented by a <tt>Map.Entry</tt>
     * with key equal to the <tt>OperationSet</tt> class name and value equal to
     * the respective <tt>OperationSet</tt> instance
     * @see AbstractOperationSetContactCapabilities#getSupportedOperationSets(
     * Contact)
     */
    @Override
    protected Map<String, OperationSet> getSupportedOperationSets(
            Contact contact,
            boolean online)
    {
        String jid = parentProvider.getFullJid(contact);
        if (jid == null)
            jid = contact.getAddress();

        return getSupportedOperationSets(jid, online);
    }

    /**
     * Gets the <tt>OperationSet</tt>s supported by a specific <tt>Contact</tt>.
     * The returned <tt>OperationSet</tt>s are considered by the associated
     * protocol provider to capabilities possessed by the specified
     * <tt>contact</tt>.
     *
     * @param jid the <tt>Contact</tt> for which the supported
     * <tt>OperationSet</tt> capabilities are to be retrieved
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return a <tt>Map</tt> listing the <tt>OperationSet</tt>s considered by
     * the associated protocol provider to be supported by the specified
     * <tt>contact</tt> (i.e. to be possessed as capabilities). Each supported
     * <tt>OperationSet</tt> capability is represented by a <tt>Map.Entry</tt>
     * with key equal to the <tt>OperationSet</tt> class name and value equal to
     * the respective <tt>OperationSet</tt> instance
     * @see AbstractOperationSetContactCapabilities#getSupportedOperationSets(
     * Contact)
     */
    @SuppressWarnings("unchecked")
    private Map<String, OperationSet> getSupportedOperationSets(String jid,
                                                                boolean online)
    {
        Map<String, OperationSet> supportedOperationSets
            = parentProvider.getSupportedOperationSets();

        int supportedOperationSetCount = supportedOperationSets.size();
        Map<String, OperationSet> contactSupportedOperationSets
            = new HashMap<String, OperationSet>(supportedOperationSetCount);

        if (supportedOperationSetCount != 0)
        {
            for (Map.Entry<String, OperationSet> supportedOperationSetEntry
                    : supportedOperationSets.entrySet())
            {
                String opsetClassName = supportedOperationSetEntry.getKey();
                Class<? extends OperationSet> opsetClass;

                try
                {
                    opsetClass
                        = (Class<? extends OperationSet>)
                            Class.forName(opsetClassName);
                }
                catch (ClassNotFoundException cnfex)
                {
                    opsetClass = null;
                    logger.error(
                            "Failed to get OperationSet class for name: "
                                + opsetClassName,
                            cnfex);
                }
                if (opsetClass != null)
                {
                    OperationSet opset
                        = getOperationSet(jid, opsetClass, online);

                    if (opset != null)
                    {
                        contactSupportedOperationSets.put(
                                opsetClassName,
                                opset);
                    }
                }
            }
        }
        return contactSupportedOperationSets;
    }

    /**
     * Gets the <tt>OperationSet</tt> corresponding to the specified
     * <tt>Class</tt> and supported by the specified <tt>Contact</tt>. If the
     * returned value is non-<tt>null</tt>, it indicates that the
     * <tt>Contact</tt> is considered by the associated protocol provider to
     * possess the <tt>opsetClass</tt> capability. Otherwise, the associated
     * protocol provider considers <tt>contact</tt> to not have the
     * <tt>opsetClass</tt> capability.
     *
     * @param <U> the type extending <tt>OperationSet</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param jid the Jabber id for which we're checking supported operation
     * sets
     * @param opsetClass the <tt>OperationSet</tt> <tt>Class</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return the <tt>OperationSet</tt> corresponding to the specified
     * <tt>opsetClass</tt> which is considered by the associated protocol
     * provider to be possessed as a capability by the specified
     * <tt>contact</tt>; otherwise, <tt>null</tt>
     * @see AbstractOperationSetContactCapabilities#getOperationSet(Contact,
     * Class)
     */
    private <U extends OperationSet> U getOperationSet(String jid,
                                                       Class<U> opsetClass,
                                                       boolean online)
    {
        U opset = parentProvider.getOperationSet(opsetClass);

        if (opset == null)
            return null;

        /*
         * If the specified contact is offline, don't query its features (they
         * should fail anyway).
         */
        if (!online)
            return OFFLINE_OPERATION_SETS.contains(opsetClass) ? opset : null;

        /*
         * If we know the features required for the support of opsetClass, check
         * whether the contact supports them. Otherwise, presume the contact
         * possesses the opsetClass capability in light of the fact that we miss
         * any knowledge of the opsetClass whatsoever.
         */
        if (OPERATION_SETS_TO_FEATURES.containsKey(opsetClass))
        {
            String[] features = OPERATION_SETS_TO_FEATURES.get(opsetClass);

            /*
             * Either we've completely disabled the opsetClass capability by
             * mapping it to the null list of features or we've mapped it to an
             * actual list of features which are to be checked whether the
             * contact supports them.
             */
            if ((features == null)
                    || ((features.length != 0)
                            && !parentProvider.isFeatureListSupported(
                                    jid,
                                    features)))
            {
                opset = null;
            }
        }

        return opset;
    }

    /**
     * Sets the <tt>EntityCapsManager</tt> which is associated with the
     * <tt>discoveryManager</tt> of {@link #parentProvider}.
     *
     * @param capsManager the <tt>EntityCapsManager</tt> which is associated
     * with the <tt>discoveryManager</tt> of {@link #parentProvider}
     */
    private void setCapsManager(EntityCapsManager capsManager)
    {
        if (this.capsManager != capsManager)
        {
            if (this.capsManager != null)
                this.capsManager.removeUserCapsNodeListener(this);

            this.capsManager = capsManager;

            if (this.capsManager != null)
                this.capsManager.addUserCapsNodeListener(this);
        }
    }

    /**
     * Sets the <tt>ScServiceDiscoveryManager</tt> which is the
     * <tt>discoveryManager</tt> of {@link #parentProvider}.
     *
     * @param discoveryManager the <tt>ScServiceDiscoveryManager</tt> which is
     * the <tt>discoveryManager</tt> of {@link #parentProvider}
     */
    void setDiscoveryManager(ScServiceDiscoveryManager discoveryManager)
    {
        setCapsManager(
            (discoveryManager == null)
                ? null
                : discoveryManager.getCapsManager());
    }

    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has added a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user is currently online
     * @see UserCapsNodeListener#userCapsNodeAdded(String, String, boolean)
     */
    public void userCapsNodeAdded(String user, String node, boolean online)
    {
        /*
         * It doesn't matter to us whether a caps node has been added or removed
         * for the specified user because we report all changes.
         */
        userCapsNodeRemoved(user, node, online);
    }

    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has removed a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the given user is online
     * @see UserCapsNodeListener#userCapsNodeRemoved(String, String, boolean)
     */
    public void userCapsNodeRemoved(String user, String node, boolean online)
    {
        OperationSetPresence opsetPresence
            = parentProvider.getOperationSet(OperationSetPresence.class);

        if (opsetPresence != null)
        {
            String jid = StringUtils.parseBareAddress(user);
            Contact contact = opsetPresence.findContactByID(jid);

            // If the contact isn't null and is online we try to discover the
            // new set of operation sets and to notify interested parties.
            // Otherwise we ignore the event.
            if (contact != null)
            {
                if(online)
                {
                    // when going online we have received a presence
                    // and make sure we discover this particular jid
                    // for getSupportedOperationSets
                    fireContactCapabilitiesEvent(
                        contact,
                        ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED,
                        getSupportedOperationSets(user,
                            online));
                }
                else
                {
                    // when offline, we use the contact, and selecting
                    // the most connected jid
                    // for getSupportedOperationSets
                    fireContactCapabilitiesEvent(
                        contact,
                        ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED,
                        getSupportedOperationSets(contact));
                }
            }
        }
    }

    /**
     * Removes the capabilities when the user goes offline.
     *
     * @param evt the <tt>ContactPresenceStatusChangeEvent</tt> that notified
     * us
     */
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        // If the user goes offline we ensure to remove the caps node.
        if (capsManager != null
            && evt.getNewStatus().getStatus() < PresenceStatus.ONLINE_THRESHOLD)
        {
            capsManager.removeContactCapsNode(evt.getSourceContact());
        }
    }

    /**
     * Fires event that contact capabilities has changed.
     * @param user the user to search for its contact.
     */
    public void fireContactCapabilitiesChanged(String user)
    {
        OperationSetPresence opsetPresence
            = parentProvider.getOperationSet(OperationSetPresence.class);

        if (opsetPresence != null)
        {
            String userID = StringUtils.parseBareAddress(user);
            Contact contact = opsetPresence.findContactByID(userID);

            // this called by received discovery info for particular jid
            // so we use its online and opsets for this particular jid
            boolean online = false;
            Presence presence = parentProvider.getConnection().getRoster()
                .getPresence(user);
            if(presence != null)
                online = presence.isAvailable();

            if(contact != null)
            {
                fireContactCapabilitiesEvent(
                    contact,
                    ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED,
                    getSupportedOperationSets(user, online));
            }
        }
    }
}
