/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.slickless.util;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.util.*;

/**
 * Tests the methods of the Base64 class.
 * @author Emil Ivov
 */
public class TestBase64 extends TestCase
{
    private Base64 base64 = null;

    /**
     * Create a TestBase64 wrapper over the test with the specified name.
     * @param name the name of the test to run
     */
    public TestBase64(String name)
    {
        super(name);
    }

    /**
     * Initializes the fixture.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        base64 = new Base64();

    }

    /**
     * Finalization
     * @throws Exception if anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        base64 = null;

        super.tearDown();
    }

    /**
     * Encodes a sample string, decodes it and makes sure that the decoded
     * string has the same value as the original
     */
    public void testEncodeDecode()
    {
        String data = "string to encode";
        byte[] expectedReturn = data.getBytes();

        byte[] encodedData = base64.encode(data.getBytes());
        byte[] actualReturn = base64.decode(encodedData);
        assertTrue("encode decode failed.", Arrays.equals(
            expectedReturn, actualReturn));
    }

    /**
     * Encodes a sample string, decodes it and makes sure that the decoded
     * string has the same value as the original
     */
    public void testEncodeDecode1()
    {
        String data = "string to encode";
        byte[] expectedReturn = data.getBytes();

        byte[] encodedData = base64.encode(data.getBytes());

        String encodedString = new String(encodedData);

        byte[] actualReturn = base64.decode(encodedString);
        assertTrue("encode decode failed.", Arrays.equals(
            expectedReturn, actualReturn));

        assertEquals("Original and destination string do not match"
                     , data, new String(actualReturn));
    }

}
