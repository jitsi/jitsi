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
    @Override
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
