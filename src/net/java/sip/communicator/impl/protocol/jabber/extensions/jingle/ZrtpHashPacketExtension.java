/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * An implementation of the "zrtp-hash" attribute as described in the currently
 * deferred XEP-0262.
 *
 * @author Emil Ivov
 */
public class ZrtpHashPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "zrtp-hash" element.
     */
    public static final String ELEMENT_NAME = "zrtp-hash";

    /**
     * The namespace for the "zrtp-hash" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:zrtp:1";

    /**
     * The name of the <tt>version</tt> attribute.
     */
    public static final String VERSION_ATTR_NAME = "version";

    /**
     * Creates a {@link ZrtpHashPacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     */
    public ZrtpHashPacketExtension()
    {
        super (NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the ZRTP version used by the implementation that created the
     * hash.
     *
     * @return the ZRTP version used by the implementation that created the
     * hash.
     */
    public String getVersion()
    {
        return getAttributeAsString(VERSION_ATTR_NAME);
    }

    /**
     * Sets the ZRTP version used by the implementation that created the
     * hash.
     *
     * @param version the ZRTP version used by the implementation that created
     * the hash.
     */
    public void setVersion(String version)
    {
        setAttribute(VERSION_ATTR_NAME, version);
    }

    /**
     * Returns the value of the ZRTP hash this element is carrying.
     *
     * @return the value of the ZRTP hash this element is carrying.
     */
    public String getValue()
    {
        return getText();
    }

    /**
     * Sets the value of the ZRTP hash this element will be carrying.
     *
     * @param value the value of the ZRTP hash this element will be carrying.
     */
    public void setValue(String value)
    {
        setText(value);
    }
}
