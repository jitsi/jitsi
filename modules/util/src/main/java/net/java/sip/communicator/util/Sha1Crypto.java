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

import java.io.*;
import java.nio.charset.*;
import java.security.*;

public class Sha1Crypto
{
    /**
     * Encodes the given text with the SHA-1 algorithm.
     *
     * @param text the text to encode
     * @return the encoded text
     */
    public static String encode(String text)
        throws  NoSuchAlgorithmException,
                UnsupportedEncodingException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(text.getBytes(StandardCharsets.ISO_8859_1));
        byte[] sha1hash = messageDigest.digest();
        return convertToHex(sha1hash);
    }

    /**
     * Converts the given byte data into Hex string.
     *
     * @param data the byte array to convert
     * @return the Hex string representation of the given byte array
     */
    private static String convertToHex(byte[] data)
    {
        StringBuilder buf = new StringBuilder();
        for (byte datum : data)
        {
            int halfbyte = (datum >>> 4) & 0x0F;
            int two_halfs = 0;
            do
            {
                if (halfbyte <= 9)
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = datum & 0x0F;
            }
            while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
