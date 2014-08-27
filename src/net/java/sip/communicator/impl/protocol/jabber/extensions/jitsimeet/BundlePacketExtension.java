/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Jitsi Meet specifics bundle packet extension.
 *
 * @author Pawel Domas
 */
public class BundlePacketExtension
    extends AbstractPacketExtension
{
    /**
     * The XML element name of {@link BundlePacketExtension}.
     */
    public static final String ELEMENT_NAME = "bundle";

    /**
     * The XML element namespace of {@link BundlePacketExtension}.
     */
    public static final String NAMESPACE = "http://estos.de/ns/bundle";

    /**
     * Creates an {@link BundlePacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     *
     */
    public BundlePacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
