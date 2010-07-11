/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * The element transporting encryption information during jingle session
 * establishment.
 *
 * @author Emil Ivov
 */
public class EncryptionPacketExtension
    implements PacketExtension
{
    /**
     * There's no namespace for the <tt>encryption</tt> element itself.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the "encryption" element.
     */
    public static final String ELEMENT_NAME = "encryption";

    /**
     * The name of the <tt>required</tt> attribute.
     */
    public static final String REQUIRED_ARG_NAME = "required";

    /**
     * Indicates whether encryption is required for this session or not.
     */
    private boolean required = false;

    /**
     * The list of <tt>crypto</tt> elements transported by this
     * <tt>encryption</tt> element.
     */
    private List<CryptoPacketExtension> cryptoList
                            = new ArrayList<CryptoPacketExtension>();


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
        this.required = required;
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
        return required;
    }

    /**
     * Returns the name of the <tt>encryption</tt> element.
     *
     * @return the name of the <tt>encryption</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns <tt>null</tt> since there's no encryption specific ns.
     *
     * @return <tt>null</tt> since there's no encryption specific ns.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of this <tt>description</tt> packet
     * extension including all child elements.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder(
            "<" + ELEMENT_NAME+ " ");

        if(isRequired())
            bldr.append(REQUIRED_ARG_NAME + "='1'");

        bldr.append(">");

        //we need to have at least one crypto element.
        for(CryptoPacketExtension crypto : cryptoList)
        {
            bldr.append(crypto.toXML());
        }

        bldr.append("</" + ELEMENT_NAME + ">");
        return bldr.toString();
    }
}
