/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP resource.
 * </p>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class XCapResource
{
    /**
     * XCAP resource identifier.
     */
    private XCapResourceId id;

    /**
     * XCAP resource content in UTF-8 enconding.
     */
    private String content;

    /**
     * XCAP resource content type.
     */
    private String contentType;

    /**
     * Creates XCAP resource with XCAP resource identifier, XCAP resource
     * content and content-type.
     *
     * @param id          the XCAP resource identifier.
     * @param content     the XCAP resource content.
     * @param contentType the XCAP resource content type.
     */
    public XCapResource(XCapResourceId id, String content, String contentType)
    {
        this.id = id;
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * Gets XCAP resource identifier.
     *
     * @return the XCAP resource identifier.
     */
    public XCapResourceId getId()
    {
        return id;
    }

    /**
     * Gets XCAP resource content.
     *
     * @return the XCAP resource content.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * Gets XCAP resource content type.
     *
     * @return the XCAP resource content type.
     */
    public String getContentType()
    {
        return contentType;
    }
}
