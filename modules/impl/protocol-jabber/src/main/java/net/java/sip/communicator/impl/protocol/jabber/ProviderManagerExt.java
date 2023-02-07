/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 - present 8x8, Inc.
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

import org.jitsi.xmpp.extensions.*;
import org.jitsi.xmpp.extensions.coin.*;
import org.jitsi.xmpp.extensions.colibri.*;
import org.jitsi.xmpp.extensions.condesc.*;
import org.jitsi.xmpp.extensions.inputevt.*;
import org.jitsi.xmpp.extensions.jibri.*;
import org.jitsi.xmpp.extensions.jingle.*;
import org.jitsi.xmpp.extensions.jingleinfo.*;
import org.jitsi.xmpp.extensions.jitsimeet.*;
import org.jitsi.xmpp.extensions.thumbnail.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.nick.packet.Nick;
import org.jivesoftware.smackx.nick.provider.*;
import org.jivesoftware.smackx.si.packet.*;

/**
 * Our Provider Manager that loads providers and extensions we use.
 * If we receive query and packets for unknown providers
 * service-unavailable is sent according RFC 6120.
 *
 * @author Damian Minkov
 */
public class ProviderManagerExt
{
    /**
     * Loads the providers and extensions used by us.
     */
    public static void load()
    {
        //register our jingle provider
        ProviderManager.addIQProvider(JingleIQ.ELEMENT,
            JingleIQ.NAMESPACE,
            new JingleIQProvider());

        // register our input event provider
        ProviderManager.addIQProvider(InputEvtIQ.ELEMENT,
            InputEvtIQ.NAMESPACE,
            new InputEvtIQProvider());

        // replace the default StreamInitiationProvider with our
        // custom provider that handles the XEP-0264 <File/> element
        ProviderManager.addIQProvider(
            StreamInitiation.ELEMENT,
            StreamInitiation.NAMESPACE,
            new ThumbnailStreamInitiationProvider());

        // register our coin provider
        ProviderManager.addIQProvider(
            CoinIQ.ELEMENT,
            CoinIQ.NAMESPACE,
            new CoinIQProvider());

        // register our JingleInfo provider
        ProviderManager.addIQProvider(JingleInfoQueryIQ.ELEMENT,
            JingleInfoQueryIQ.NAMESPACE,
            new JingleInfoQueryIQProvider());

        ProviderManager.addIQProvider(
            GracefulShutdownIQ.ELEMENT,
            GracefulShutdownIQ.NAMESPACE,
            new GracefulShutdownIqProvider());

        ProviderManager.addIQProvider(
            ForcefulShutdownIQ.ELEMENT,
            ForcefulShutdownIQ.NAMESPACE,
            new ForcefulShutdownIqProvider());

        ProviderManager.addIQProvider(
            JibriIq.ELEMENT,
            JibriIq.NAMESPACE,
            new JibriIqProvider()
        );

        ProviderManager.addExtensionProvider(
            ConferenceDescriptionExtension.ELEMENT,
            ConferenceDescriptionExtension.NAMESPACE,
            new ConferenceDescriptionExtensionProvider());

        ProviderManager.addExtensionProvider(
            Nick.QNAME.getLocalPart(),
            Nick.NAMESPACE,
            new NickProvider());

        ProviderManager.addExtensionProvider(
            Email.ELEMENT,
            Email.NAMESPACE,
            new Email.Provider());

        ProviderManager.addExtensionProvider(
            AvatarUrl.ELEMENT,
            AvatarUrl.NAMESPACE,
            new AvatarUrl.Provider());

        ProviderManager.addExtensionProvider(
            StatsId.ELEMENT,
            StatsId.NAMESPACE,
            new StatsId.Provider());

        ProviderManager.addExtensionProvider(
            IdentityPacketExtension.ELEMENT,
            IdentityPacketExtension.NAMESPACE,
            new IdentityPacketExtension.Provider()
        );

        ProviderManager.addExtensionProvider(
            AvatarIdPacketExtension.ELEMENT,
            AvatarIdPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                AvatarIdPacketExtension.class));

        ProviderManager.addExtensionProvider(
            JsonMessageExtension.ELEMENT,
            JsonMessageExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                JsonMessageExtension.class));

        ProviderManager.addExtensionProvider(
            TranslationLanguageExtension.ELEMENT,
            TranslationLanguageExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                TranslationLanguageExtension.class));

        ProviderManager.addExtensionProvider(
            TranscriptionLanguageExtension.ELEMENT,
            TranscriptionLanguageExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                TranscriptionLanguageExtension.class));

        ProviderManager.addExtensionProvider(
            TranscriptionStatusExtension.ELEMENT,
            TranscriptionStatusExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                TranscriptionStatusExtension.class
            )
        );

        ProviderManager.addExtensionProvider(
            TranscriptionRequestExtension.ELEMENT,
            TranscriptionRequestExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                TranscriptionRequestExtension.class
            )
        );
    }
}
