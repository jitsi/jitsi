/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model;

import java.util.*;

import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to convert classes into xml and from xml to the
 * classes.
 *
 * @author Grigorii Balutsel
 */
public final class XmlUtils
{
    /**
     * Processes any attributes and add them into the element.
     *
     * @param element       the element where to add the attributes.
     * @param anyAttributes the any attributes to process.
     */
    public static void processAnyAttributes(
            Element element, Map<QName, String> anyAttributes)
    {
        for (Map.Entry<QName, String> attribute : anyAttributes.entrySet())
        {
            String localName = attribute.getKey().getLocalPart();
            String prefix = attribute.getKey().getPrefix();
            String namespace = attribute.getKey().getNamespaceURI();
            element.setAttributeNS(namespace, prefix + ":" + localName,
                    attribute.getValue());
        }
    }

    /**
     * Processes any element and add them into the element.
     *
     * @param element the element where to add the elements.
     * @param any     the any elements to process.
     * @throws Exception if there is some error during processing.
     */
    public static void processAny(Element element, List<Element> any)
            throws Exception
    {
        for (Element anyElement : any)
        {
            Node importedElement =
                element.getOwnerDocument().importNode(anyElement, true);
            element.appendChild(importedElement);
        }
    }
}
