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
import org.junit.*;

/**
 * @author Danny van Heumen
 */
public class ISupportTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testParseNullDestination()
    {
        ISupport.parseChanLimit(null, "");
        fail("Should not reach this, since it should fail on null destination.");
    }

    @Test
    public void testParseNullValue()
    {
        HashMap<Character, Integer> dest = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(dest, null);
        assertEquals(0, dest.size());
    }

    @Test
    public void testParseEmptyValue()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "");
        assertEquals(0, destination.size());
    }

    @Test
    public void testParseSingleSimpleValidValue()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "#:10");
        assertEquals(1, destination.size());
        assertEquals(10, destination.get('#').intValue());
    }

    @Test
    public void testParseSingleCombinedValidValue()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "#&+:25");
        assertEquals(3, destination.size());
        assertEquals(25, destination.get('#').intValue());
        assertEquals(25, destination.get('&').intValue());
        assertEquals(25, destination.get('+').intValue());
    }

    @Test
    public void testParseMultipleSimpleValidValues()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "#:10,&:20,+:30");
        assertEquals(3, destination.size());
        assertEquals(10, destination.get('#').intValue());
        assertEquals(20, destination.get('&').intValue());
        assertEquals(30, destination.get('+').intValue());
    }

    @Test
    public void testParseMultipleSimpleValidValuesWithInvalidStuff()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "#:10,^:20,jadie:abc,+:30,#:abc");
        assertEquals(2, destination.size());
        assertEquals(10, destination.get('#').intValue());
        assertEquals(30, destination.get('+').intValue());
    }

    @Test
    public void testParseMultipleCombinedValidValues()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "#&:100,+:30");
        assertEquals(3, destination.size());
        assertEquals(100, destination.get('#').intValue());
        assertEquals(100, destination.get('&').intValue());
        assertEquals(30, destination.get('+').intValue());
    }

    @Test
    public void testParseSimpleInvalidValue()
    {
        Map<Character, Integer> destination = new HashMap<Character, Integer>();
        ISupport.parseChanLimit(destination, "bla");
        assertEquals(0, destination.size());
    }
}
