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

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.irc.ModeParser.ModeEntry;

public class ModeParserTest
    extends TestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }
        
    public void testConstructionStringNull()
    {
        try
        {
            new ModeParser((String) null);
            Assert.fail("Expected NPE");
        }
        catch (NullPointerException e)
        {
        }
    }
    
    public void testConstructionStringEmpty()
    {
        ModeParser parser = new ModeParser("");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(0, modes.size());
    }
    
    public void testConstructionStringSingleBad()
    {
        try
        {
            new ModeParser("p");
            Assert.fail();
        }
        catch (IllegalStateException e)
        {
        }
    }
    
    public void testConstructionStringSingleGood()
    {
        ModeParser parser = new ModeParser("+p");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        Assert.assertTrue(modes.get(0).isAdded());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testParseRemoval()
    {
        ModeParser parser = new ModeParser("-p");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1,  modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        Assert.assertFalse(modes.get(0).isAdded());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testParseUnknownMode()
    {
        ModeParser parser = new ModeParser("+?");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.UNKNOWN, modes.get(0).getMode());
        Assert.assertEquals(1, modes.get(0).getParams().length);
        Assert.assertEquals("?", modes.get(0).getParams()[0]);
    }
    
    public void testModeMissingExtraParameters()
    {
        try
        {
            new ModeParser("+l");
            Assert.fail("this line should not be reached");
        }
        catch (IllegalArgumentException e)
        {
            // expect illegal argument exception
        }
    }

    public void testModeExtraParameters()
    {
        ModeParser parser = new ModeParser("+l 123 141");
        Assert.assertEquals(1, parser.getModes().size());
        // Parse modes, expect 1 and ignore the extra parameter.
    }

    public void testModeOwner()
    {
        ModeParser parser = new ModeParser("+O dude");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.OWNER, modes.get(0).getMode());
        Assert.assertEquals(1, modes.get(0).getParams().length);
        Assert.assertEquals("dude", modes.get(0).getParams()[0]);
    }

    public void testModeOperator()
    {
        ModeParser parser = new ModeParser("+o dude");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.OPERATOR, modes.get(0).getMode());
        Assert.assertEquals(1, modes.get(0).getParams().length);
        Assert.assertEquals("dude", modes.get(0).getParams()[0]);
    }

    public void testModeVoice()
    {
        ModeParser parser = new ModeParser("+v dude");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.VOICE, modes.get(0).getMode());
        Assert.assertEquals(1, modes.get(0).getParams().length);
        Assert.assertEquals("dude", modes.get(0).getParams()[0]);
    }

    public void testModeLimitAddition()
    {
        ModeParser parser = new ModeParser("+l 13");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.LIMIT, modes.get(0).getMode());
        Assert.assertEquals(1, modes.get(0).getParams().length);
        Assert.assertEquals("13", modes.get(0).getParams()[0]);
    }

    public void testModeLimitRemoval()
    {
        ModeParser parser = new ModeParser("-l");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.LIMIT, modes.get(0).getMode());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testModePrivate()
    {
        ModeParser parser = new ModeParser("+p");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.PRIVATE, modes.get(0).getMode());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testModeSecret()
    {
        ModeParser parser = new ModeParser("+s");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.SECRET, modes.get(0).getMode());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testModeInvite()
    {
        ModeParser parser = new ModeParser("+i");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        Assert.assertTrue(modes.get(0) instanceof ModeEntry);
        Assert.assertEquals(Mode.INVITE, modes.get(0).getMode());
        Assert.assertEquals(0, modes.get(0).getParams().length);
    }
    
    public void testModeBan()
    {
        ModeParser parser = new ModeParser("+b *!*@some-ip.dynamicIP.provider.net");
        List<ModeEntry> modes = parser.getModes();
        Assert.assertNotNull(modes);
        Assert.assertEquals(1, modes.size());
        ModeEntry entry = modes.get(0);
        Assert.assertTrue(entry instanceof ModeEntry);
        Assert.assertEquals(Mode.BAN, entry.getMode());
        Assert.assertEquals(1, entry.getParams().length);
        Assert.assertEquals("*!*@some-ip.dynamicIP.provider.net", entry.getParams()[0]);
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
