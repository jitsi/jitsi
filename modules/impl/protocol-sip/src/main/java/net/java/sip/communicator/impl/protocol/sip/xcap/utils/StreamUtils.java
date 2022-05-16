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
package net.java.sip.communicator.impl.protocol.sip.xcap.utils;

import java.io.*;

/**
 * Base HTTP XCAP client implementation.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class StreamUtils
{
    /**
     * The buffer size (1kb).
     */
    private static int BUFFER_SIZE = 1024;

    /**
     * This class cannot be instanced.
     */
    private StreamUtils()
    {
    }

    /**
     * Reads content from the stream.
     *
     * @param source the input stream.
     * @return the content.
     * @throws IOException              if there is some error during read
     *                                  operation.
     * @throws IllegalArgumentException if source is null.
     */
    public static byte[] read(InputStream source)
            throws IOException
    {
        if (source == null)
        {
            throw new IllegalArgumentException("Input parameter can't be null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = source.read(buffer)) > -1)
            {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
        finally
        {
            source.close();
            out.close();
        }
    }
}

