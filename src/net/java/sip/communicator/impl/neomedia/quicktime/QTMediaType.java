/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Enumerates the QTKit media types.
 *
 * @author Lubomir Marinov
 */
public enum QTMediaType
{

    /**
     * The QTKit type of multiplexed media which may contain audio, video, and
     * other data in a single stream.
     */
    Muxed,

    /**
     * The QTKit type of media which contains only audio frames.
     */
    Sound,

    /**
     * The QTKit type of media which contains only video frames.
     */
    Video
}
