/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * The name of the "encryption" element.
     */
    public static final String ELEMENT_NAME = "encryption";

    /**
     * The name of the <tt>required</tt> attribute.
     */
    public static final String REQUIRED_ARG_NAME = "required";

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
        super(null, ELEMENT_NAME);
    }


    /**
     * Adds a new <tt>crypto</tt> element to this encryption element.
     *
     * @param crypto the new <tt>crypto</tt> element to add.
     */
    public void addCrypto(CryptoPacketExtension crypto)
    {
        cryptoList.add(crypto);
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
            super.setAttribute(REQUIRED_ARG_NAME, required);
        else
            super.removeAttribute(REQUIRED_ARG_NAME);
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
        String required = getAttributeAsString(REQUIRED_ARG_NAME);

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
        return getCryptoList();
    }
}
