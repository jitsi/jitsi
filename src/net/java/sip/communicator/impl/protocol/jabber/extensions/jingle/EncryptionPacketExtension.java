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

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * The element transporting encryption information during jingle session
 * establishment.
 *
 * @author Emil Ivov
 */
public class EncryptionPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace of the "encryption" element.
     * It it set to "not null" only for Gtalk SDES support (may be set to null
     * once gtalk supports jingle).
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    /**
     * The name of the "encryption" element.
     */
    public static final String ELEMENT_NAME = "encryption";

    /**
     * The name of the <tt>required</tt> attribute.
     */
    public static final String REQUIRED_ATTR_NAME = "required";

    /**
     * The list of <tt>crypto</tt> elements transported by this
     * <tt>encryption</tt> element.
     */
    private List<CryptoPacketExtension> cryptoList
                            = new ArrayList<CryptoPacketExtension>();

    /**
     * Creates a new instance of this <tt>EncryptionPacketExtension</tt>.
     */
    public EncryptionPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Adds a new <tt>crypto</tt> element to this encryption element.
     *
     * @param crypto the new <tt>crypto</tt> element to add.
     */
    public void addCrypto(CryptoPacketExtension crypto)
    {
        if(!cryptoList.contains(crypto))
        {
            cryptoList.add(crypto);
        }
    }

    /**
     * Returns a <b>reference</b> to the list of <tt>crypto</tt> elements that
     * we have registered with this encryption element so far.
     *
     * @return  a <b>reference</b> to the list of <tt>crypto</tt> elements that
     * we have registered with this encryption element so far.
     */
    public List<CryptoPacketExtension> getCryptoList()
    {
        return cryptoList;
    }

    /**
     * Specifies whether encryption is required for this session or not.
     *
     * @param required <tt>true</tt> if encryption is required for this session
     * and <tt>false</tt> otherwise.
     */
    public void setRequired(boolean required)
    {
        if(required)
            super.setAttribute(REQUIRED_ATTR_NAME, required);
        else
            super.removeAttribute(REQUIRED_ATTR_NAME);
    }

    /**
     * Returns <tt>true</tt> if encryption is required for this session and
     * <tt>false</tt> otherwise. Default value is <tt>false</tt>.
     *
     * @return <tt>true</tt> if encryption is required for this session and
     * <tt>false</tt> otherwise.
     */
    public boolean isRequired()
    {
        String required = getAttributeAsString(REQUIRED_ATTR_NAME);

        return Boolean.valueOf(required) || "1".equals(required);
    }

    /**
     * Returns a list containing all <tt>crypto</tt> sub-elements.
     *
     * @return a {@link List} containing all our <tt>crypto</tt> sub-elements.
     */
    @Override
    public List<? extends PacketExtension> getChildExtensions()
    {
        List<PacketExtension> ret = new ArrayList<PacketExtension>();

        ret.addAll(super.getChildExtensions());
        return ret;
    }

    /**
     * Adds the specified <tt>childExtension</tt> to the list of extensions
     * registered with this packet.
     * <p/>
     * Overriding extensions may need to override this method if they would like
     * to have anything more elaborate than just a list of extensions (e.g.
     * casting separate instances to more specific.
     *
     * @param childExtension the extension we'd like to add here.
     */
    @Override
    public void addChildExtension(PacketExtension childExtension)
    {
        super.addChildExtension(childExtension);

        if(childExtension instanceof CryptoPacketExtension)
        {
            this.addCrypto(((CryptoPacketExtension) childExtension));
        }
    }
}
