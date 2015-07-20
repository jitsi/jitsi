/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * The element containing details about an encryption algorithm that could be
 * used during a jingle session.
 *
 * @author Emil Ivov
 * @author Vincent Lucas
 */
public class CryptoPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "crypto" element.
     */
    public static final String ELEMENT_NAME = "crypto";

    /**
     * The namespace for the "crypto" element.
     * It it set to "not null" only for Gtalk SDES support (may be set to null
     * once gtalk supports jingle).
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    /**
     * The name of the 'crypto-suite' argument.
     */
    public static final String CRYPTO_SUITE_ATTR_NAME = "crypto-suite";

    /**
     * The name of the 'key-params' argument.
     */
    public static final String KEY_PARAMS_ATTR_NAME = "key-params";

    /**
     * The name of the 'session-params' argument.
     */
    public static final String SESSION_PARAMS_ATTR_NAME = "session-params";

    /**
     * The name of the 'tag' argument.
     */
    public static final String TAG_ATTR_NAME = "tag";

    /**
     * Creates a new {@link CryptoPacketExtension} instance with the proper
     * element name and namespace.
     */
    public CryptoPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Creates a new {@link CryptoPacketExtension} instance with the proper
     * element name and namespace and initialises it with the parameters
     * contained by the cryptoAttribute.
     *
     * @param cryptoAttribute The cryptoAttribute containing the crypto-suite,
     * key-params, session-params and key information.
     */
    public CryptoPacketExtension(SrtpCryptoAttribute cryptoAttribute)
    {
        this();

        initialize(cryptoAttribute);
    }

    /**
     * Initialises it with the parameters contained by the cryptoAttribute.
     *
     * @param cryptoAttribute The cryptoAttribute containing the crypto-suite,
     * key-params, session-params and key information.
     */
    private void initialize(SrtpCryptoAttribute cryptoAttribute)
    {
        // Encode the tag element.
        this.setTag(Integer.toString(cryptoAttribute.getTag()));

        // Encode the crypto-suite element.
        this.setCryptoSuite(cryptoAttribute.getCryptoSuite().encode());

        // Encode the key-params element.
        this.setKeyParams(cryptoAttribute.getKeyParamsString());

        // Encode the session-params element (optional).
        String sessionParamsString = cryptoAttribute.getSessionParamsString();
        if (sessionParamsString != null)
        {
            this.setSessionParams(sessionParamsString);
        }
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
        super.setAttribute(CRYPTO_SUITE_ATTR_NAME, cryptoSuite);
    }

    /**
     * Returns the value of the <tt>crypto-suite</tt> attribute.
     *
     * @return a <tt>String</tt> that describes the encryption and
     * authentication algorithms.
     */
    public String getCryptoSuite()
    {
        return getAttributeAsString(CRYPTO_SUITE_ATTR_NAME);
    }

    /**
     * Returns if the current crypto suite equals the one given in parameter.
     *
     * @param cryptoSuite a <tt>String</tt> that describes the encryption and
     * authentication algorithms.
     *
     * @return True if the current crypto suite equals the one given in
     * parameter. False, otherwise.
     */
    public boolean equalsCryptoSuite(String cryptoSuite)
    {
        String currentCryptoSuite = this.getCryptoSuite();
        return CryptoPacketExtension.equalsStrings(
                currentCryptoSuite,
                cryptoSuite);
    }

    /**
     * Sets the value of the <tt>key-params</tt> attribute that provides one or
     * more sets of keying material for the crypto-suite in question).
     *
     * @param keyParams a <tt>String</tt> that provides one or more sets of
     * keying material for the crypto-suite in question.
     */
    public void setKeyParams(String keyParams)
    {
        super.setAttribute(KEY_PARAMS_ATTR_NAME, keyParams);
    }

    /**
     * Returns the value of the <tt>key-params</tt> attribute.
     *
     * @return a <tt>String</tt> that provides one or more sets of keying
     * material for the crypto-suite in question).
     */
    public String getKeyParams()
    {
        return getAttributeAsString(KEY_PARAMS_ATTR_NAME);
    }

    /**
     * Returns if the current key params equals the one given in parameter.
     *
     * @param keyParams a <tt>String</tt> that provides one or more sets of
     * keying material for the crypto-suite in question.
     *
     * @return True if the current key params equals the one given in
     * parameter. False, otherwise.
     */
    public boolean equalsKeyParams(String keyParams)
    {
        String currentKeyParams = this.getKeyParams();
        return CryptoPacketExtension.equalsStrings(
                currentKeyParams,
                keyParams);
    }

    /**
     * Sets the value of the <tt>session-params</tt> attribute that provides
     * transport-specific parameters for SRTP negotiation.
     *
     * @param sessionParams a <tt>String</tt> that provides transport-specific
     * parameters for SRTP negotiation.
     */
    public void setSessionParams(String sessionParams)
    {
        super.setAttribute(SESSION_PARAMS_ATTR_NAME, sessionParams);
    }

    /**
     * Returns the value of the <tt>session-params</tt> attribute.
     *
     * @return a <tt>String</tt> that provides transport-specific parameters
     * for SRTP negotiation.
     */
    public String getSessionParams()
    {
        return getAttributeAsString(SESSION_PARAMS_ATTR_NAME);
    }

    /**
     * Returns if the current session params equals the one given in parameter.
     *
     * @param sessionParams a <tt>String</tt> that provides transport-specific
     * parameters for SRTP negotiation.
     *
     * @return True if the current session params equals the one given in
     * parameter. False, otherwise.
     */
    public boolean equalsSessionParams(String sessionParams)
    {
        String currentSessionParams = this.getSessionParams();
        return CryptoPacketExtension.equalsStrings(
                currentSessionParams,
                sessionParams);
    }

    /**
     * Sets the value of the <tt>tag</tt> attribute: a decimal number used as
     * an identifier for a particular crypto element.
     *
     * @param tag a <tt>String</tt> containing a decimal number used as an
     * identifier for a particular crypto element.
     */
    public void setTag(String tag)
    {
        super.setAttribute(TAG_ATTR_NAME, tag);
    }

    /**
     * Returns the value of the <tt>tag</tt> attribute.
     *
     * @return a <tt>String</tt> containing a decimal number used as an
     * identifier for a particular crypto element.
     */
    public String getTag()
    {
        return getAttributeAsString(TAG_ATTR_NAME);
    }

    /**
     * Returns if the current tag equals the one given in parameter.
     *
     * @param tag a <tt>String</tt> containing a decimal number used as an
     * identifier for a particular crypto element.
     *
     * @return True if the current tag equals the one given in parameter. False,
     * otherwise.
     */
    public boolean equalsTag(String tag)
    {
        String currentTag = this.getTag();
        return CryptoPacketExtension.equalsStrings(
                currentTag,
                tag);
    }

    /**
     * Returns a SrtpCryptoAttribute corresponding to this
     * CryptoPAcketExtension.
     *
     * @return A SrtpCryptoAttribute corresponding to this
     * CryptoPAcketExtension.
     */
    public SrtpCryptoAttribute toSrtpCryptoAttribute()
    {
        // Creaates the new SrtpCryptoAttribute.
        return SrtpCryptoAttribute.create(
                this.getTag(),
                this.getCryptoSuite(),
                this.getKeyParams(),
                this.getSessionParams());
    }

    /**
     * Returns if the first String equals the second one.
     *
     * @param string1 A String to be compared with the second one.
     * @param string2 A String to be compared with the fisrt one.
     *
     * @return True if both strings are null, or if they represent the same
     * sequane of characters. False, otherwise.
     */
    private static boolean equalsStrings(String string1, String string2)
    {
        return (
                ((string1 == null) && (string2 == null))
                || string1.equals(string2)
               );
    }

    /**
     * Returns if the current CryptoPacketExtension equals the one given in
     * parameter.
     *
     * @param obj an object which might be an instance of CryptoPacketExtension.
     *
     * @return True if the object in parameter is a CryptoPAcketExtension with
     * all fields (crypto-suite, key-params, session-params and tag)
     * corresponding to the current one. False, otherwsise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof CryptoPacketExtension)
        {
            CryptoPacketExtension crypto = (CryptoPacketExtension) obj;

            return (
                    crypto.equalsCryptoSuite(this.getCryptoSuite())
                    && crypto.equalsKeyParams(this.getKeyParams())
                    && crypto.equalsSessionParams(this.getSessionParams())
                    && crypto.equalsTag(this.getTag())
                   );
        }
        return false;
    }
}
