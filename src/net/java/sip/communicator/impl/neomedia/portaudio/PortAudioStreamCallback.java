/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

import java.nio.*;

/**
 * @author Lubomir Marinov
 */
public interface PortAudioStreamCallback
{
    /**
     * "Abort" resut code.
     */
    public static final int RESULT_ABORT = 2;

    /**
     * "Complete" resut code.
     */
    public static final int RESULT_COMPLETE = 1;

    /**
     * "Continue" resut code.
     */
    public static final int RESULT_CONTINUE = 0;

    /**
     * Callback.
     *
     * @param input input <tt>ByteBuffer</tt>
     * @param output output <tt>ByteBuffer</tt>
     * @return
     */
    public int callback(ByteBuffer input, ByteBuffer output);

    /**
     * Finished callback.
     */
    public void finishedCallback();
}
