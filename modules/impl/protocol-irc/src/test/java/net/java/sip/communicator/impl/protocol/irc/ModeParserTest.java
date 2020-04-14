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
package net.java.sip.communicator.impl.protocol.irc;

import static org.junit.Assert.*;

import java.util.*;
import net.java.sip.communicator.impl.protocol.irc.ModeParser.*;
import org.junit.*;

public class ModeParserTest
{
    @Test(expected = NullPointerException.class)
    public void testConstructionStringNull()
    {
        new ModeParser(null);
        fail("Expected NPE");
    }

    @Test
    public void testConstructionStringEmpty()
    {
        ModeParser parser = new ModeParser("");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(0, modes.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructionStringSingleBad()
    {
        new ModeParser("p");
        fail();
    }

    @Test
    public void testConstructionStringSingleGood()
    {
        ModeParser parser = new ModeParser("+p");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        assertTrue(modes.get(0).isAdded());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testParseRemoval()
    {
        ModeParser parser = new ModeParser("-p");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        assertFalse(modes.get(0).isAdded());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testParseUnknownMode()
    {
        ModeParser parser = new ModeParser("+?");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.UNKNOWN, modes.get(0).getMode());
        assertEquals(1, modes.get(0).getParams().length);
        assertEquals("?", modes.get(0).getParams()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModeMissingExtraParameters()
    {
        new ModeParser("+l");
        fail("this line should not be reached");
    }

    @Test
    public void testModeExtraParameters()
    {
        ModeParser parser = new ModeParser("+l 123 141");
        assertEquals(1, parser.getModes().size());
        // Parse modes, expect 1 and ignore the extra parameter.
    }

    @Test
    public void testModeOwner()
    {
        ModeParser parser = new ModeParser("+O dude");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.OWNER, modes.get(0).getMode());
        assertEquals(1, modes.get(0).getParams().length);
        assertEquals("dude", modes.get(0).getParams()[0]);
    }

    @Test
    public void testModeOperator()
    {
        ModeParser parser = new ModeParser("+o dude");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.OPERATOR, modes.get(0).getMode());
        assertEquals(1, modes.get(0).getParams().length);
        assertEquals("dude", modes.get(0).getParams()[0]);
    }

    @Test
    public void testModeVoice()
    {
        ModeParser parser = new ModeParser("+v dude");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.VOICE, modes.get(0).getMode());
        assertEquals(1, modes.get(0).getParams().length);
        assertEquals("dude", modes.get(0).getParams()[0]);
    }

    @Test
    public void testModeLimitAddition()
    {
        ModeParser parser = new ModeParser("+l 13");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.LIMIT, modes.get(0).getMode());
        assertEquals(1, modes.get(0).getParams().length);
        assertEquals("13", modes.get(0).getParams()[0]);
    }

    @Test
    public void testModeLimitRemoval()
    {
        ModeParser parser = new ModeParser("-l");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.LIMIT, modes.get(0).getMode());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testModePrivate()
    {
        ModeParser parser = new ModeParser("+p");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testModeSecret()
    {
        ModeParser parser = new ModeParser("+s");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.SECRET, modes.get(0).getMode());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testModeInvite()
    {
        ModeParser parser = new ModeParser("+i");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.get(0) instanceof ModeEntry);
        assertEquals(Mode.INVITE, modes.get(0).getMode());
        assertEquals(0, modes.get(0).getParams().length);
    }

    @Test
    public void testModeBan()
    {
        ModeParser parser =
            new ModeParser("+b *!*@some-ip.dynamicIP.provider.net");
        List<ModeEntry> modes = parser.getModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        ModeEntry entry = modes.get(0);
        assertTrue(entry instanceof ModeEntry);
        assertEquals(Mode.BAN, entry.getMode());
        assertEquals(1, entry.getParams().length);
        assertEquals("*!*@some-ip.dynamicIP.provider.net",
            entry.getParams()[0]);
    }
}
