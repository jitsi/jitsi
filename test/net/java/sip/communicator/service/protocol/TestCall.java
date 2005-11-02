/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import junit.framework.*;


/**
 * Tests the call abstract class
 *
 * @author Emil Ivov
 */
public class TestCall extends TestCase
{
    private Call call = null;
    private String callID = "identifier0123456789";

//    private class DummyCall extends Call
//    {
//        public DummyCall()
//        {
//            super(callID);
//        }
//    };

//    protected void setUp() throws Exception
//    {
//        super.setUp();
//        call = new DummyCall();
//    }
//
    protected void tearDown() throws Exception
    {
        call = null;
        super.tearDown();
    }

    public void testEquals()
    {
        Object obj = null;
        boolean expectedReturn = false;
        boolean actualReturn = call.equals(obj);
        assertEquals("return value", expectedReturn, actualReturn);

//        obj = new DummyCall();
//        expectedReturn = true;
//        actualReturn = call.equals(obj);
//        assertEquals("return value", expectedReturn, actualReturn);
    }

    public void testGetCallID()
    {
        String expectedReturn = callID;
        String actualReturn = call.getCallID();
        assertEquals("return value", expectedReturn, actualReturn);
    }

}
