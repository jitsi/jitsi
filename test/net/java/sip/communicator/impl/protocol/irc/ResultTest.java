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

import junit.framework.*;

public class ResultTest
    extends TestCase
{
    public void testConstruction()
    {
        Result<Object, Exception> result = new Result<Object, Exception>();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isDone());
        Assert.assertNull(result.getValue());
        Assert.assertNull(result.getException());
    }
    
    public void testConstructionWithInitialValue()
    {
        Object initial = new Object();
        Result<Object, Exception> result =
            new Result<Object, Exception>(initial);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isDone());
        Assert.assertSame(initial, result.getValue());
        Assert.assertNull(result.getException());
    }

    public void testSetDone()
    {
        Result<Object, Exception> result = new Result<Object, Exception>();
        result.setDone();
        Assert.assertTrue(result.isDone());
        Assert.assertNull(result.getValue());
        Assert.assertNull(result.getException());
    }

    public void testSetDoneWithValue()
    {
        Object v = new Object();
        Result<Object, Exception> result = new Result<Object, Exception>();
        result.setDone(v);
        Assert.assertTrue(result.isDone());
        Assert.assertSame(v, result.getValue());
        Assert.assertNull(result.getException());
    }

    public void testSetDoneWithException()
    {
        Exception e =
            new IllegalStateException("the world is going to explode");
        Result<Object, Exception> result = new Result<Object, Exception>();
        result.setDone(e);
        Assert.assertTrue(result.isDone());
        Assert.assertNull(result.getValue());
        Assert.assertSame(e, result.getException());
    }

    public void testSetDoneWithBoth()
    {
        Object v = new Object();
        Exception e =
            new IllegalStateException("the world is going to explode");
        Result<Object, Exception> result = new Result<Object, Exception>();
        result.setDone(v, e);
        Assert.assertTrue(result.isDone());
        Assert.assertSame(v, result.getValue());
        Assert.assertSame(e, result.getException());
    }
}
