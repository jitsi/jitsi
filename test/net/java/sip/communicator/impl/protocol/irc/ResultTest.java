/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
