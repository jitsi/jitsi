/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP resource identifier.
 *
 * @author Grigorii Balutsel
 */
public class XCapResourceId
{
    /**
     * Delimeter between document and node selectors.
     */
    private static String DELIMETER = "/~~";

    /**
     * Document selector.
     */
    private String document;

    /**
     * Node selector.
     */
    private String node;

    /**
     * Creates XCAP resource identifier with document selector.
     *
     * @param document the document selector.
     */
    public XCapResourceId(String document)
    {
        this(document, null);
    }

    /**
     * Creates XCAP resource identifier with document and node selector.
     *
     * @param document the document selector.
     * @param node     the node selector.
     */
    public XCapResourceId(String document, String node)
    {
        if (document == null || document.length() == 0)
        {
            throw new IllegalArgumentException(
                    "XCAP resource document cannot be null or empty");
        }
        this.document = document;
        this.node = node;
    }

    /**
     * Gets document selector.
     *
     * @return the document selector.
     */
    public String getDocument()
    {
        return document;
    }

    /**
     * Gets node selector.
     *
     * @return the node selector.
     */
    public String getNode()
    {
        return node;
    }

    /**
     * Gets XCAP resource identifier object as single string.
     *
     * @return the XCAP resource identifier object as single string.
     */
    public String toString()
    {
        StringBuilder builder = new StringBuilder(document);
        if (node != null && node.length() != 0)
        {
            builder.append(DELIMETER).append(node);
        }
        return builder.toString();
    }

    /**
     * Creates XCAP resource identifier object from single string.
     *
     * @param resourceId the XCAP resource identifier as single string.
     * @return the XCAP resource identifier.
     * @throws IllegalArgumentException if resourceId is null or emty or has
     *                                  invalid format.
     */
    public static XCapResourceId create(String resourceId)
    {
        if (resourceId == null || resourceId.trim().length() == 0)
        {
            throw new IllegalArgumentException(
                    "Resource identifier cannot be null or empty");
        }
        int index = resourceId.indexOf(DELIMETER);
        if (index == -1)
        {
            throw new IllegalArgumentException(
                    "Resource identifier has invalid format");
        }
        String document = resourceId.substring(0, index);
        String node = resourceId.substring(index + DELIMETER.length());
        return new XCapResourceId(document, node);
    }
}
