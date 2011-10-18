/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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

