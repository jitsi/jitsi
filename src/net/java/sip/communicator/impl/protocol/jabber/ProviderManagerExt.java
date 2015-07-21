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

import net.java.sip.communicator.impl.protocol.jabber.extensions.usersearch.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.bytestreams.ibb.provider.*;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;

/**
 * Our Provider Manager that loads providers and extensions we use.
 * If we receive query and packets for unknown providers
 * service-unavailable is sent according RFC 6120.
 *
 * @author Damian Minkov
 */
public class ProviderManagerExt
    extends ProviderManager
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(ProviderManagerExt.class);

    /**
     * Creates and loads the providers and extensions used by us.
     */
    ProviderManagerExt()
    {
        load();
    }

    /**
     * Loads the providers and extensions used by us.
     */
    public void load()
    {
        //<!-- Private Data Storage -->
        //addProvider("query", "jabber:iq:private",
        //    org.jivesoftware.smackx.PrivateDataManager.PrivateDataIQProvider.class);

        //<!-- Time -->
        //addProvider("query", "jabber:iq:time",
        //    org.jivesoftware.smackx.packet.Time.class);

        //<!-- Roster Exchange -->
        addExtProvider("x", "jabber:x:roster",
            RosterExchangeProvider.class);
        //<!-- Message Events -->
        addExtProvider("x", "jabber:x:event",
            MessageEventProvider.class);

        //<!-- Chat State -->
        addExtProvider(
            "active",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtension.Provider.class);
        addExtProvider(
            "composing",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtension.Provider.class);
        addExtProvider(
            "paused",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtension.Provider.class);
        addExtProvider(
            "inactive",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtension.Provider.class);
        addExtProvider(
            "gone",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtension.Provider.class);


        //<!-- XHTML -->
        addExtProvider("html", "http://jabber.org/protocol/xhtml-im",
            XHTMLExtensionProvider.class);

        //<!-- Group Chat Invitations -->
        addExtProvider("x", "jabber:x:conference",
            GroupChatInvitation.Provider.class);

        //<!-- Service Discovery # Items -->
        addProvider("query", "http://jabber.org/protocol/disco#items",
            DiscoverItemsProvider.class);
        //<!-- Service Discovery # Info -->
        addProvider("query", "http://jabber.org/protocol/disco#info",
            DiscoverInfoProvider.class);

        //<!-- Data Forms-->
        addExtProvider("x", "jabber:x:data",
            org.jivesoftware.smackx.provider.DataFormProvider.class);

        //<!-- MUC User -->
        addExtProvider("x", "http://jabber.org/protocol/muc#user",
            MUCUserProvider.class);
        //<!-- MUC Admin -->
        addProvider("query", "http://jabber.org/protocol/muc#admin",
            MUCAdminProvider.class);
        //<!-- MUC Owner -->
        addProvider("query", "http://jabber.org/protocol/muc#owner",
            MUCOwnerProvider.class);

        //<!-- Delayed Delivery -->
        addExtProvider("x", "jabber:x:delay",
            DelayInformationProvider.class);
        addExtProvider("delay", "urn:xmpp:delay",
            DelayInfoProvider.class);

        //<!-- Version -->
        addProvider("query", "jabber:iq:version",
            Version.class);

        //<!-- VCard -->
        addProvider("vCard", "vcard-temp",
            VCardProvider.class);

        //<!-- Offline Message Requests -->
        addProvider("offline", "http://jabber.org/protocol/offline",
            OfflineMessageRequest.Provider.class);

        //<!-- Offline Message Indicator -->
        addExtProvider("offline", "http://jabber.org/protocol/offline",
            OfflineMessageInfo.Provider.class);

        //<!-- Last Activity -->
        addProvider("query", "jabber:iq:last",
            LastActivity.class);

        //<!-- User Search -->
        addProvider("query", "jabber:iq:search",UserSearchProvider.class);

        //<!-- SharedGroupsInfo -->
        //addProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup",
        //    org.jivesoftware.smackx.packet.SharedGroupsInfo.Provider.class);


        //<!-- JEP-33: Extended Stanza Addressing -->
        //addProvider("addresses", "http://jabber.org/protocol/address",
        //    org.jivesoftware.smackx.provider.MultipleAddressesProvider.class);

        //<!-- FileTransfer -->
        addProvider("si", "http://jabber.org/protocol/si",
            StreamInitiationProvider.class);
        addProvider("query", "http://jabber.org/protocol/bytestreams",
            org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider.class);
        addProvider("open", "http://jabber.org/protocol/ibb",
            OpenIQProvider.class);
        addProvider("data", "http://jabber.org/protocol/ibb",
            DataPacketProvider.class);
        addProvider("close", "http://jabber.org/protocol/ibb",
            CloseIQProvider.class);
        addExtProvider("data", "http://jabber.org/protocol/ibb",
            DataPacketProvider.class);


        //<!-- Privacy -->
        //addProvider("query", "jabber:iq:privacy",
        //    org.jivesoftware.smack.provider.PrivacyProvider.class);

        //<!-- Ad-Hoc Command -->
        //addProvider("command", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.class);

        //addExtProvider("bad-action", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadActionError.class);

        //addExtProvider("malformed-actionn", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.MalformedActionError.class);

        //addExtProvider("bad-locale", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadLocaleError.class);

        //addExtProvider("bad-payload", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadPayloadError.class);

        //addExtProvider("bad-sessionid", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadSessionIDError.class);

        //addExtProvider("session-expired", "http://jabber.org/protocol/commands",
        //    org.jivesoftware.smackx.provider.AdHocCommandDataProvider.SessionExpiredError.class);


        //<!-- Fastpath providers -->
        //addProvider("offer", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OfferRequestProvider.class);

        //addProvider("offer-revoke", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OfferRevokeProvider.class);

        //addProvider("agent-status-request", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentStatusRequest.Provider.class);

        //addProvider("transcripts", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptsProvider.class);

        //addProvider("transcript", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptProvider.class);

        //addProvider("workgroups", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentWorkgroups.Provider.class);

        //addProvider("agent-info", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentInfo.Provider.class);

                //addProvider("transcript-search", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptSearch.Provider.class);

        //addProvider("occupants-info", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OccupantsInfo.Provider.class);

        //addProvider("chat-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.ChatSettings.InternalProvider.class);

        //addProvider("chat-notes", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.notes.ChatNotes.Provider.class);

        //addProvider("chat-sessions", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.history.AgentChatHistory.InternalProvider.class);

        //addProvider("offline-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.OfflineSettings.InternalProvider.class);

        //addProvider("sound-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.SoundSettings.InternalProvider.class);

         //addProvider("workgroup-properties", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.WorkgroupProperties.InternalProvider.class);


        //addProvider("search-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.SearchSettings.InternalProvider.class);

        //addProvider("workgroup-form", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.forms.WorkgroupForm.InternalProvider.class);

        //addProvider("macros", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.macros.Macros.InternalProvider.class);

        //addProvider("chat-metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.history.ChatMetadata.Provider.class);

        //<!--
        //org.jivesoftware.smackx.workgroup.site is missing ...

        //addProvider("site-user", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteUser.Provider.class);

        //addProvider("site-invite", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteInvitation.Provider.class);

        //addProvider("site-user-history", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteUserHistory.Provider.class);
        //-->
        //addProvider("generic-metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.GenericSettings.InternalProvider.class);

        //addProvider("monitor", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.MonitorPacket.InternalProvider.class);

        //<!-- Packet Extension Providers -->
        //addExtProvider("queue-status", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueUpdate.Provider.class);

        //addExtProvider("workgroup", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.WorkgroupInformation.Provider.class);

        //addExtProvider("metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.MetaDataProvider.class);

        //addExtProvider("session", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.SessionID.Provider.class);

        //addExtProvider("user", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.UserID.Provider.class);

        //addExtProvider("agent-status", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentStatus.Provider.class);

        //addExtProvider("notify-queue-details", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueDetails.Provider.class);

        //addExtProvider("notify-queue", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueOverview.Provider.class);

        //addExtProvider("invite", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.RoomInvitation.Provider.class);

        //addExtProvider("transfer", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.RoomTransfer.Provider.class);

        //<!-- SHIM -->
        //addExtProvider("headers", "http://jabber.org/protocol/shim",
        //    org.jivesoftware.smackx.provider.HeadersProvider.class);

        //addExtProvider("header", "http://jabber.org/protocol/shim",
        //    org.jivesoftware.smackx.provider.HeaderProvider.class);

        //<!-- XEP-0060 pubsub -->
        //addProvider("pubsub", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.PubSubProvider.class);

        //addExtProvider("create", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //addExtProvider("items", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.ItemsProvider.class);

        //addExtProvider("item", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.ItemProvider.class);

        //addExtProvider("subscriptions", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider.class);

        //addExtProvider("subscription", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider.class);

        //addExtProvider("affiliations", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider.class);

        //addExtProvider("affiliation", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.AffiliationProvider.class);

        //addExtProvider("options", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //<!-- XEP-0060 pubsub#owner -->
        //addProvider("pubsub", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.PubSubProvider.class);

        //addExtProvider("configure", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //addExtProvider("default", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //<!-- XEP-0060 pubsub#event -->
        //addExtProvider("event", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.EventProvider.class);

        //addExtProvider("configuration", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider.class);

        //addExtProvider("delete", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //addExtProvider("options", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //addExtProvider("items", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ItemsProvider.class);

        //addExtProvider("item", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ItemProvider.class);

        //addExtProvider("retract", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.RetractEventProvider.class);

        //addExtProvider("purge", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //<!-- Nick Exchange -->
        //addExtProvider("nick", "http://jabber.org/protocol/nick",
        //    org.jivesoftware.smackx.packet.Nick.Provider.class);

        //<!-- Attention -->
        //addExtProvider("attention", "urn:xmpp:attention:0",
        //    org.jivesoftware.smackx.packet.AttentionExtension.Provider.class);
    }

    /**
     * Adds an IQ provider (must be an instance of IQProvider or Class object that is an IQ)
     * with the specified element name and name space. The provider will override any providers
     * loaded through the classpath.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the IQ provider class.
     */
    private void addProvider(
            String elementName,
            String namespace,
            Class<?> provider)
    {
        // Attempt to load the provider class and then create
        // a new instance if it's an IQProvider. Otherwise, if it's
        // an IQ class, add the class object itself, then we'll use
        // reflection later to create instances of the class.
        try
        {
            // Add the provider to the map.
            if (IQProvider.class.isAssignableFrom(provider))
            {
                addIQProvider(elementName, namespace, provider.newInstance());
            }
            else if (IQ.class.isAssignableFrom(provider))
            {
                addIQProvider(elementName, namespace, provider);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error adding iq provider.", t);
        }
    }

    /**
     * Adds an extension provider with the specified element name and name space. The provider
     * will override any providers loaded through the classpath. The provider must be either
     * a PacketExtensionProvider instance, or a Class object of a Javabean.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the extension provider class.
     */
    public void addExtProvider(
            String elementName,
            String namespace,
            Class<?> provider)
    {
        // Attempt to load the provider class and then create
        // a new instance if it's a Provider. Otherwise, if it's
        // a PacketExtension, add the class object itself and
        // then we'll use reflection later to create instances
        // of the class.
        try
        {
            // Add the provider to the map.
            if (PacketExtensionProvider.class.isAssignableFrom(provider))
            {
                addExtensionProvider(
                        elementName,
                        namespace,
                        provider.newInstance());
            }
            else if (PacketExtension.class.isAssignableFrom(
                    provider))
            {
                addExtensionProvider(elementName, namespace, provider);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error adding extension provider.", t);
        }
    }
}
