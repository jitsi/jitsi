/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * The element containing details about an encryption algorithm that could be
 * used during a jingle session.
 *
 * @author Emil Ivov
 */
public class CryptoPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the 'crypto-suite' argument.
     */
    public static final String CRYPTO_SUITE_ARG_NAME = "crypto-suite";

    /**
     * The name of the 'key-params' argument.
     */
    public static final String KEY_PARAMS_ARG_NAME = "key-params";

    /**
     * The name of the 'session-params' argument.
     */
    public static final String SESSION_PARAMS_ARG_NAME = "session-params";

    /**
     * The name of the 'tag' argument.
     */
    public static final String TAG_ARG_NAME = "tag";

    /**
     * Creates a new {@link CryptoPacketExtension} instance with the proper
     * element name and namespace.
     */
    public CryptoPacketExtension()
    {
        super(null, "crypto");
    }

    /**
     * Sets the value of the <tt>crypto-suite</tt> attribute: an identifier that
     * describes the encryption and authentication algorithms.
     *
     * @param cryptoSuite a <tt>String</tt> that describes the encryption and
     * authentication algorithms.
     */
    public void setCryptoSuite(String cryptoSuite)
    {
        super.setAttribtue(CRYPTO_SUITE_ARG_NAME, cryptoSuite);
    }

    /**
     * Returns the value of the <tt>crypto-suite</tt> attribute.
     *
     * @return a <tt>String</tt> that describes the encryption and
     * authentication algorithms.
     */
    public String getCryptoSuite()
    {
        return getAttribtueString(CRYPTO_SUITE_ARG_NAME);
    }
}
