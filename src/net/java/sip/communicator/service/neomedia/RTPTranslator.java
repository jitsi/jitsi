/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Represents an RTP translator which forwards RTP and RTCP traffic between
 * multiple <tt>MediaStream</tt>s.
 *
 * @author Lyubomir Marinov
 */
public interface RTPTranslator
{
    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     */
    public void dispose();
}
