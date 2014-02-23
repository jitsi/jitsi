/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for the <tt>fingerprint</tt>
 * element defined by XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
 *
 * @author Lyubomir Marinov
 */
public class DtlsFingerprintPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The XML name of the <tt>fingerprint</tt> element defined by XEP-0320: Use
     * of DTLS-SRTP in Jingle Sessions.
     */
    public static final String ELEMENT_NAME = "fingerprint";

    /**
     * The XML name of the <tt>fingerprint</tt> element's attribute which
     * specifies the hash function utilized to calculate the fingerprint.
     */
    private static final String HASH_ATTR_NAME = "hash";

    /**
     * The XML namespace of the <tt>fingerprint</tt> element defined by
     * XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:dtls:0";

    /**
     * The <tt>required</tt> attribute has been removed in version 0.2 of
     * XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    private static final String REQUIRED_ATTR_NAME = "required";

    /** Initializes a new <tt>DtlsFingerprintPacketExtension</tt> instance. */
    public DtlsFingerprintPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Gets the fingerprint carried/represented by this instance.
     *
     * @return the fingerprint carried/represented by this instance
     */
    public String getFingerprint()
    {
        return getText();
    }

    /**
     * Gets the hash function utilized to calculate the fingerprint
     * carried/represented by this instance.
     *
     * @return the hash function utilized to calculate the fingerprint
     * carried/represented by this instance
     */
    public String getHash()
    {
        return getAttributeAsString(HASH_ATTR_NAME);
    }

    /**
     * The <tt>required</tt> attribute has been removed in version 0.2 of
     * XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    public boolean getRequired()
    {
        String attr = getAttributeAsString(REQUIRED_ATTR_NAME);

        return (attr == null) ? false : Boolean.parseBoolean(attr);
    }

    /**
     * Sets the fingerprint to be carried/represented by this instance.
     *
     * @param fingerprint the fingerprint to be carried/represented by this
     * instance
     */
    public void setFingerprint(String fingerprint)
    {
        setText(fingerprint);
    }

    /**
     * Sets the hash function utilized to calculate the fingerprint
     * carried/represented by this instance.
     *
     * @param hash the hash function utilized to calculate the fingerprint
     * carried/represented by this instance
     */
    public void setHash(String hash)
    {
        setAttribute(HASH_ATTR_NAME, hash);
    }

    /**
     * The <tt>required</tt> attribute has been removed in version 0.2 of
     * XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    public void setRequired(boolean required)
    {
        setAttribute(REQUIRED_ATTR_NAME, Boolean.valueOf(required));
    }
}
