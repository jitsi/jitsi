/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
    public static final int RESULT_ABORT = 2;

    public static final int RESULT_COMPLETE = 1;

    public static final int RESULT_CONTINUE = 0;

    public int callback(ByteBuffer input, ByteBuffer output);

    public void finishedCallback();
}
