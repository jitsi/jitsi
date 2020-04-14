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
package net.java.sip.communicator.util;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests the methods of the Base64 class.
 *
 * @author Emil Ivov
 */
public class TestBase64
{
    /**
     * Encodes a sample string, decodes it and makes sure that the decoded
     * string has the same value as the original
     */
    @Test
    public void testEncodeDecode()
    {
        String data = "string to encode";
        byte[] expectedReturn = data.getBytes();
        byte[] encodedData = Base64.encode(data.getBytes());
        byte[] actualReturn = Base64.decode(encodedData);
        assertArrayEquals("encode decode failed.",
            expectedReturn, actualReturn);
    }

    /**
     * Encodes a sample string, decodes it and makes sure that the decoded
     * string has the same value as the original
     */
    @Test
    public void testEncodeDecode1()
    {
        String data = "string to encode";
        byte[] expectedReturn = data.getBytes();
        byte[] encodedData = Base64.encode(data.getBytes());
        String encodedString = new String(encodedData);
        byte[] actualReturn = Base64.decode(encodedString);
        assertArrayEquals("encode decode failed.",
            expectedReturn, actualReturn);
        Assert.assertEquals("Original and destination string do not match",
            data, new String(actualReturn));
    }
}
