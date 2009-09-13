/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * The <tt>MediaStream</tt> class represents a (generally) bidirectional RTP
 * stream between exactly two parties. The class reflects one media stream, in
 * the SDP sense of the word. <tt>MediaStream</tt> instances are created through
 * the <tt>openMediaStream()</tt> method of the <tt>MediaService</tt>.
 * <p>
 *
 * @author Emil Ivov
 */
public interface MediaStream
{
    /**
     * The name of the property containing the number of binds that a Media
     * Service Implementation should execute in case a port is already
     * bound to (each retry would be on a new port in the allowed boundaries).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.BIND_RETRIES";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MIN_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MIN_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MAX_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MAX_PORT_NUMBER";

    /**
     * The default number of binds that a Media Service Implementation should
     * execute in case a port is already bound to (each retry would be on a
     * new random port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * With this property video support can be disabled
     * (enabled by default).
     */
    public static final String DISABLE_VIDEO_SUPPORT_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT";
}
