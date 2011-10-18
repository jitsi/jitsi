/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * The Authorization Rules except element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ExceptType
{
    /**
     * The domain attribute.
     */
    private String id;

    /**
     * The domain attribute.
     */
    private String domain;

    /**
     * Gets the value of the id property.
     *
     * @return the id property.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param id the id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Gets the value of the domain property.
     *
     * @return the domain property.
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * Sets the value of the domain property.
     *
     * @param domain the domain to set.
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }
}
