/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;

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
    extends IQ
{
    public static final String SOURCE_PROVIDER_HASH = "src-provider-hash";
    public static final String SOURCE_OPSET_HASH = "src-opset-hash";

    private int srcProviderHash = -1;
    private int srcOpSetHash = -1;

    /**
     * Constructs empty packet
     */
    public KeepAliveEvent()
    {}

    /**
     * Construct packet for sending.
     *
     * @param to the address of the contact that the packet is to be sent to.
     */
    public KeepAliveEvent(String to)
    {
        if (to == null)
        {
            throw new IllegalArgumentException("Parameter cannot be null");
        }
        setTo(to);
    }

    /**
     * Returns the sub-element XML section of this packet
     *
     * @return the packet as XML.
     */
    public String getChildElementXML()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(KeepAliveEventProvider.ELEMENT_NAME).
            append(" xmlns=\"").append(KeepAliveEventProvider.NAMESPACE).
            append("\">");

        buf.append("<").
            append(SOURCE_PROVIDER_HASH).append(">").
                append(getSrcProviderHash()).append("</").
            append(SOURCE_PROVIDER_HASH).append(">");

        buf.append("<").
            append(SOURCE_OPSET_HASH).append(">").
                append(getSrcOpSetHash()).append("</").
            append(SOURCE_OPSET_HASH).append(">");

        buf.append("</").append(KeepAliveEventProvider.ELEMENT_NAME).append(">");
        return buf.toString();
    }

    /**
     * The user id sending this packet
     * @return String user id
     */
    public String getFromUserID()
    {
        if(getFrom() != null)
            return StringUtils.parseBareAddress(getFrom());
        else
            return null;
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
}
