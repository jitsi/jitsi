/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * URIs packet extension.
 *
 * @author Sebastien Vincent
 */
public class UsagePacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that URIs belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the URIs data.
     */
    public static final String ELEMENT_NAME = "usage";

    /**
     * Constructor.
     */
     public UsagePacketExtension()
     {
         super(NAMESPACE, ELEMENT_NAME);
     }
}
