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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;

import java.util.*;

/**
 * Utility class for extracting info from responses received from the JVB and
 * keeping track of conference state.
 *
 * @author Pawel Domas
 */
public class ColibriAnalyser
{
    /**
     * The logger used by this instance.
     */
    private final static Logger logger
        = Logger.getLogger(ColibriAnalyser.class);

    /**
     * Colibri IQ instance used to store conference state.
     */
    private final ColibriConferenceIQ conferenceState;

    /**
     * Creates new instance of analyser that will used given Colibri IQ instance
     * for storing conference state.
     * @param conferenceStateHolder the Colibri IQ instance that will be used
     *        for storing conference state.
     */
    public ColibriAnalyser(ColibriConferenceIQ conferenceStateHolder)
    {
        this.conferenceState = conferenceStateHolder;
    }

    /**
     * Processes channels allocation response from the JVB and stores info about
     * new channels in {@link #conferenceState}.
     * @param allocateResponse the Colibri IQ that describes JVB response to
     *                         allocate request.
     */
    public void processChannelAllocResp(ColibriConferenceIQ allocateResponse)
    {
        String conferenceResponseID = allocateResponse.getID();
        String colibriID = conferenceState.getID();

        if (colibriID == null)
            conferenceState.setID(conferenceResponseID);
        else if (!colibriID.equals(conferenceResponseID))
            throw new IllegalStateException("conference.id");

        /*
         * XXX We must remember the JID of the Jitsi Videobridge because
         * (1) we do not want to re-discover it in every method
         * invocation on this Call instance and (2) we want to use one
         * and the same for all CallPeers within this Call instance.
         */
        conferenceState.setFrom(allocateResponse.getFrom());

        for (ColibriConferenceIQ.Content contentResponse
            : allocateResponse.getContents())
        {
            String contentName = contentResponse.getName();
            ColibriConferenceIQ.Content content
                = conferenceState.getOrCreateContent(contentName);

            // FIXME: we do not check if allocated channel does not clash
            // with any existing one
            for (ColibriConferenceIQ.Channel channelResponse
                : contentResponse.getChannels())
            {
                content.addChannel(channelResponse);
            }
            for (ColibriConferenceIQ.SctpConnection sctpConnResponse
                : contentResponse.getSctpConnections())
            {
                content.addSctpConnection(sctpConnResponse);
            }
        }

        for (ColibriConferenceIQ.ChannelBundle bundle
            : allocateResponse.getChannelBundles())
        {
            conferenceState.addChannelBundle(bundle);
        }
    }

    /**
     * Utility method for extracting info about channels allocated from JVB
     * response.
     * FIXME: this might not work as expected when channels for multiple peers
     *        with single query were allocated.
     * @param conferenceResponse JVB response to allocate channels request.
     * @param peerContents list of peer media contents that has to be matched
     *                     with allocated channels.
     * @return the Colibri IQ that describes allocated channels.
     */
    public static ColibriConferenceIQ getResponseContents(
            ColibriConferenceIQ conferenceResponse,
            List<ContentPacketExtension> peerContents)
    {
        ColibriConferenceIQ conferenceResult = new ColibriConferenceIQ();

        conferenceResult.setFrom(conferenceResponse.getFrom());
        conferenceResult.setID(conferenceResponse.getID());

        // FIXME: we support single bundle for all channels
        String bundleId = null;
        for (ContentPacketExtension content : peerContents)
        {
            MediaType mediaType
                = JingleUtils.getMediaType(content);

            ColibriConferenceIQ.Content contentResponse
                = conferenceResponse.getContent(mediaType.toString());

            if (contentResponse != null)
            {
                String contentName = contentResponse.getName();
                ColibriConferenceIQ.Content contentResult
                    = new ColibriConferenceIQ.Content(contentName);

                conferenceResult.addContent(contentResult);

                for (ColibriConferenceIQ.Channel channelResponse
                    : contentResponse.getChannels())
                {
                    contentResult.addChannel(channelResponse);

                    bundleId = readChannelBundle(channelResponse, bundleId);
                }

                for (ColibriConferenceIQ.SctpConnection sctpConnResponse
                    : contentResponse.getSctpConnections())
                {
                    contentResult.addSctpConnection(sctpConnResponse);

                    bundleId = readChannelBundle(sctpConnResponse, bundleId);
                }
            }
        }

        // Copy only peer's bundle(JVB returns all bundles)
        if (bundleId != null)
        {
            for (ColibriConferenceIQ.ChannelBundle bundle
                : conferenceResponse.getChannelBundles())
            {
                if (bundleId.equals(bundle.getId()))
                {
                    conferenceResult.addChannelBundle(bundle);
                    break;
                }
            }
        }

        return conferenceResult;
    }

    /**
     * Utility method for getting actual channel bundle. If
     * <tt>currentBundle</tt> is <tt>null</tt> then <tt>channels</tt> bundle is
     * returned(and vice-versa). If both channel's and given bundle IDs are not
     * null then they are compared and error is logged, but channel's bundle is
     * returned in the last place anyway.
     */
    private static String readChannelBundle(
            ColibriConferenceIQ.ChannelCommon channel, String currentBundle)
    {
        String channelBundle = channel.getChannelBundleId();

        if (channelBundle == null)
        {
            return currentBundle;
        }

        if (currentBundle == null)
        {
            return channel.getChannelBundleId();
        }
        else
        {
            // Compare to detect problems
            if (!currentBundle.equals(channelBundle))
            {
                logger.error(
                    "Replaced bundle: " + currentBundle
                        + " with " + channelBundle);
            }
            return channelBundle;
        }
    }
}
