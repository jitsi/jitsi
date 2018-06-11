/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 Atlassian Pty Ltd
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

import junit.framework.*;

/**
 * @author Boris Grozev
 */
public class ColibriStatsExtensionTest
    extends TestCase
{
    /**
     * Test the constructor and adding a new stat.
     */
    public void testAddStat()
    {
        ColibriStatsExtension stats = new ColibriStatsExtension();

        stats.addStat("name", "value");

        assertNotNull(stats.getStat("name"));
        assertEquals(stats.getStat("name").getName(), "name");
        assertEquals(stats.getStat("name").getValue(), "value");

        assertNull(stats.getStat("somethingelse"));
        assertNull(stats.getStatValue("somethingelse"));
    }

    /**
     * Test cloning.
     */
    public void testClone()
    {
        ColibriStatsExtension stats = new ColibriStatsExtension();
        stats.addStat("name", "value");

        ColibriStatsExtension clone = ColibriStatsExtension.clone(stats);

        assertNotNull(clone.getStat("name"));
        assertEquals(clone.getStat("name").getName(), "name");
        assertEquals(clone.getStat("name").getValue(), "value");

        ColibriStatsExtension.Stat cloneNameStat = clone.getStat("name");
        cloneNameStat.setValue("virtue");

        assertEquals(clone.getStatValue("name"), "virtue");
        assertEquals(stats.getStatValue("name"), "value");
    }
}
