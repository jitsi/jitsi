/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * SRC-ID packet extension.
 *
 * @author Sebastien Vincent
 */
public class SrcIdPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that URIs belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the URIs data.
     */
    public static final String ELEMENT_NAME = "src-id";

    /**
     * Constructor.
     *
     * @param namespace namespace
     */
     public SrcIdPacketExtension(String namespace)
     {
         super(namespace, ELEMENT_NAME);
     }

    /**
     * Set source ID.
     *
     * @param srcId source ID
     */
     public void setSrcId(String srcId)
     {
         this.setText(srcId);
     }
}