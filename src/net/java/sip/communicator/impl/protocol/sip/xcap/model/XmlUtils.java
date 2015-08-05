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
