/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import org.jivesoftware.smack.packet.*;

/**
 * KeepAlive Event. Events are send on specified interval
 * and must be received from the sendin provider.
 * Carries the information for the source ProtocolProvider and
 * source OperationSet - so we can be sure that we are sending and receiving the
 * same package.
 *
 * @author Damian Minkov
 */
public class KeepAliveEvent
    implements PacketExtension
{
    public static final String SOURCE_PROVIDER_HASH = "src-provider-hash";
    public static final String SOURCE_OPSET_HASH = "src-opset-hash";

    private int srcProviderHash = -1;
    private int srcOpSetHash = -1;
    private String fromUserID = null;

    /**
     * Returns the XML element name of the extension sub-packet root element.
     * Always returns "x"
     *
     * @return the XML element name of the packet extension.
     */
    public String getElementName()
    {
        return KeepAliveEventProvider.ELEMENT_NAME;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * The namespace is always "sip-communicator:x:keepalive"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace()
    {
        return KeepAliveEventProvider.NAMESPACE;
    }

    /**
     * Returns the XML reppresentation of the PacketExtension.
     *
     * @return the packet extension as XML.
     * @todo Implement this org.jivesoftware.smack.packet.PacketExtension
     *   method
     */
    public String toXML()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");

        buf.append("<").
            append(SOURCE_PROVIDER_HASH).append(">").
                append(getSrcProviderHash()).append("</").
            append(SOURCE_PROVIDER_HASH).append(">");

        buf.append("<").
            append(SOURCE_OPSET_HASH).append(">").
                append(getSrcOpSetHash()).append("</").
            append(SOURCE_OPSET_HASH).append(">");

        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * Returns the hash of the source opeartion set sending this message
     * @return int hash of the operation set
     */
    public int getSrcOpSetHash()
    {
        return srcOpSetHash;
    }

    /**
     * Returns the hash of the source provider sending this message
     * @return int hash of the provider
     */
    public int getSrcProviderHash()
    {
        return srcProviderHash;
    }

    /**
     * The user id sending this packet
     * @return String user id
     */
    public String getFromUserID()
    {
        return fromUserID;
    }

    /**
     * Sets the hash of the source provider that will send the message
     * @param srcProviderHash int hash of the provider
     */
    public void setSrcProviderHash(int srcProviderHash)
    {
        this.srcProviderHash = srcProviderHash;
    }

    /**
     * Sets the hash of the source opeartion set that will send the message
     * @param srcOpSetHash int hash of the operation set
     */
    public void setSrcOpSetHash(int srcOpSetHash)
    {
        this.srcOpSetHash = srcOpSetHash;
    }

    /**
     * The user id sending this packet
     * @param fromUserID String user id
     */
    public void setFromUserID(String fromUserID)
    {
        this.fromUserID = fromUserID;
    }
}
