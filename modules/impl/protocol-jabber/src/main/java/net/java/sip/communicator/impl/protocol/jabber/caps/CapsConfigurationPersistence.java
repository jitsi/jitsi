/*
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
 *
 */
package net.java.sip.communicator.impl.protocol.jabber.caps;

import org.jitsi.service.configuration.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.xml.*;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.caps.cache.*;
import org.jivesoftware.smackx.disco.packet.*;

import java.io.*;

/**
 * Simple implementation of an EntityCapsPersistentCache that uses a the
 * configuration service to store the Caps information for every known node.
 *
 * @author Damian Minkov
 */
public class CapsConfigurationPersistence
    implements EntityCapsPersistentCache
{
    /**
     * The <tt>Logger</tt> used by the <tt>CapsConfigurationPersistence</tt>
     * class and its instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CapsConfigurationPersistence.class);

    /**
     * Configuration service instance used by this class.
     */
    private final ConfigurationService configService;

    /**
     * The prefix of the <tt>ConfigurationService</tt> properties which persist.
     */
    private static final String CAPS_PROPERTY_NAME_PREFIX
        = "net.java.sip.communicator.impl.protocol.jabber.extensions.caps."
            + "EntityCapsManager.CAPS.";

    /**
     * Constructs new CapsConfigurationPersistence that will be responsible
     * for storing and retrieving caps in the config service.
     * @param configService the current configuration service.
     */
    public CapsConfigurationPersistence(ConfigurationService configService)
    {
        this.configService = configService;
    }

    @Override
    public void addDiscoverInfoByNodePersistent(String nodeVer, DiscoverInfo info)
    {
        DiscoverInfo cleanedDiscoverInfo = info.asBuilder(null)
            .from(null)
            .to(null)
            .setNode(nodeVer).build();

        /*
         * If the specified info is a new association for the specified
         * node, remember it across application instances in order to not
         * query for it over the network.
         */
        String xml = cleanedDiscoverInfo.getChildElementXML().toString();

        if (xml.length() != 0)
        {
            this.configService
                .setProperty(
                    CAPS_PROPERTY_NAME_PREFIX + nodeVer, xml);
        }
    }

    @Override
    public DiscoverInfo lookup(String nodeVer)
    {
        DiscoverInfo discoverInfo = null;
        String capsPropertyName = CAPS_PROPERTY_NAME_PREFIX + nodeVer;
        String xml = this.configService.getString(capsPropertyName);

        if((xml != null) && (xml.length() != 0))
        {
            IqProvider<IQ> discoverInfoProvider
                = ProviderManager.getIQProvider(
                    "query",
                    "http://jabber.org/protocol/disco#info");

            if(discoverInfoProvider != null)
            {
                XmlPullParser parser;

                try
                {
                    parser = SmackXmlParser.newXmlParser(new StringReader(xml));
                    parser.next();
                }
                catch(IOException | org.jivesoftware.smack.xml.XmlPullParserException xppex)
                {
                    parser = null;
                }

                if(parser != null)
                {
                    try
                    {
                        discoverInfo
                            = (DiscoverInfo)
                                discoverInfoProvider.parse(parser, null);
                    }
                    catch(Exception ex)
                    {
                        logger.error(
                            "Invalid DiscoverInfo for "
                                + nodeVer
                                + ": "
                                + discoverInfo);
                        /*
                         * The discoverInfo doesn't seem valid
                         * according to the caps which means that we
                         * must have stored invalid information.
                         * Delete the invalid information in order
                         * to not try to validate it again.
                         */
                        this.configService.removeProperty(
                            capsPropertyName);
                    }
                }
            }
        }

        return discoverInfo;
    }

    @Override
    public void emptyCache()
    {}
}
