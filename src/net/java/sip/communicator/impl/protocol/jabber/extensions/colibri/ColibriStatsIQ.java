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

import org.jivesoftware.smack.packet.*;

/**
 * The stats IQ that can be used to request Colibri stats on demand
 * (used in server side focus).
 *
 * @author Pawel Domas
 */
public class ColibriStatsIQ
    extends IQ
{
    /**
     * The XML element name of the Jitsi Videobridge <tt>stats</tt> extension.
     */
    public static final String ELEMENT_NAME
        = ColibriStatsExtension.ELEMENT_NAME;

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi
     * Videobridge <tt>stats</tt> extension.
     */
    public static final String NAMESPACE
        = ColibriStatsExtension.NAMESPACE;


    private final ColibriStatsExtension backEnd = new ColibriStatsExtension();

    @Override
    public String getChildElementXML()
    {
        return backEnd.toXML();
    }

    /**
     * Adds stat extension.
     * @param stat the stat to be added
     */
    public void addStat(ColibriStatsExtension.Stat stat)
    {
        backEnd.addStat(stat);
    }
}
