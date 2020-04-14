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

import org.junit.*;

public class ResultTest
{
    @Test
    public void testConstruction()
    {
        Result<Object, Exception> result = new Result<Object, Exception>();
        assertNotNull(result);
        assertFalse(result.isDone());
        assertNull(result.getValue());
        assertNull(result.getException());
    }

    @Test
    public void testConstructionWithInitialValue()
    {
        Object initial = new Object();
        Result<Object, Exception> result =
            new Result<Object, Exception>(initial);
        assertNotNull(result);
        assertFalse(result.isDone());
        assertSame(initial, result.getValue());
        assertNull(result.getException());
    }

    @Test
    public void testSetDone()
    {
        Result<Object, Exception> result = new Result<>();
        result.setDone();
        assertTrue(result.isDone());
        assertNull(result.getValue());
        assertNull(result.getException());
    }

    @Test
    public void testSetDoneWithValue()
    {
        Object v = new Object();
        Result<Object, Exception> result = new Result<>();
        result.setDone(v);
        assertTrue(result.isDone());
        assertSame(v, result.getValue());
        assertNull(result.getException());
    }

    @Test
    public void testSetDoneWithException()
    {
        Exception e =
            new IllegalStateException("the world is going to explode");
        Result<Object, Exception> result = new Result<>();
        result.setDone(e);
        assertTrue(result.isDone());
        assertNull(result.getValue());
        assertSame(e, result.getException());
    }

    @Test
    public void testSetDoneWithBoth()
    {
        Object v = new Object();
        Exception e =
            new IllegalStateException("the world is going to explode");
        Result<Object, Exception> result = new Result<>();
        result.setDone(v, e);
        assertTrue(result.isDone());
        assertSame(v, result.getValue());
        assertSame(e, result.getException());
    }
}
