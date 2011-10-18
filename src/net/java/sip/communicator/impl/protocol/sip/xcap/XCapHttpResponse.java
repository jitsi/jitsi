/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP HTTP response.
 *
 * @author Grigorii Balutsel
 */
public class XCapHttpResponse
{
    /**
     * HTTP code.
     */
    private int httpCode;

    /**
     * HTTP Content-Type
     */
    private String contentType;

    /**
     * HTTP response content.
     */
    private byte[] content;

    /**
     * HTTP ETag.
     */
    private String eTag;

    /**
     * Gets HTTP code.
     *
     * @return the HTTP code.
     */
    public int getHttpCode()
    {
        return httpCode;
    }

    /**
     * Sets HTTP code.
     *
     * @param httpCode the HTTP code.
     */
    void setHttpCode(int httpCode)
    {
        this.httpCode = httpCode;
    }

    /**
     * Gets HTTP Content-Type.
     *
     * @return the HTTP Content-Type.
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Sets HTTP Content-Type.
     *
     * @param contentType the HTTP Content-Type.
     */
    void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    /**
     * Gets HTTP response content.
     *
     * @return the HTTP response content.
     */
    public byte[] getContent()
    {
        return content;
    }

    /**
     * Sets HTTP response content.
     *
     * @param content the HTTP response content.
     */
    void setContent(byte[] content)
    {
        this.content = content;
    }

    /**
     * Gets HTTP ETag.
     *
     * @return the HTTP ETag.
     */
    public String getETag()
    {
        return eTag;
    }

    /**
     * Sets HTTP ETag.
     *
     * @param eTag the HTTP ETag.
     */
    void setETag(String eTag)
    {
        this.eTag = eTag;
    }
}
