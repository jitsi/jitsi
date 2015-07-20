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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import static org.jitsi.util.StringUtils.isNullOrEmpty;
import static org.jitsi.util.xml.XMLUtils.createDocument;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts xcap-caps xml to the object model and
 * object model to the xcap-caps xml.
 *
 * @author Grigorii Balutsel
 */
public final class XCapCapsParser
{
    private static final String NAMESPACE = "urn:ietf:params:xml:ns:xcap-caps";

    private static String XCAPCAPS_ELEMENT = "xcap-caps";

    private static String AUIDS_ELEMENT = "auids";

    private static String AUID_ELEMENT = "auid";

    private static String NAMESPACES_ELEMENT = "namespaces";

    private static String NAMESPACE_ELEMENT = "namespace";

    private static String EXTENSIONS_ELEMENT = "extensions";

    private static String EXTENSION_ELEMENT = "extension";

    /**
     * Creates xcap-caps object from the element.
     *
     * @param xml the XML to analyze.
     * @return the xcap-caps object.
     * @throws ParsingException if there is some error during parsing.
     */
    public static XCapCapsType fromXml(String xml)
            throws ParsingException
    {
        if (isNullOrEmpty(xml))
        {
            throw new IllegalArgumentException("XML cannot be null or empty");
        }
        try
        {
            XCapCapsType xCapCaps = new XCapCapsType();
            Document document = createDocument(xml);
            Element xCapCapsElement = document.getDocumentElement();
            if (XCAPCAPS_ELEMENT.equals(xCapCapsElement.getLocalName()) &&
                    !NAMESPACE.equals(xCapCapsElement.getNamespaceURI()))
            {
                throw new Exception(
                        "Document doesn't contain xcap-caps element");
            }
            boolean auidsFound = false;
            boolean namespacesFound = false;
            // Process attributes
            NamedNodeMap attributes = xCapCapsElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                String namespaceUri = getNamespaceUri(attribute);
                if ((namespaceUri == null)
                        || !isStandartXmlNamespace(namespaceUri))
                    throw new Exception("xcap-caps element is invalid");
            }
            // Process elements
            NodeList childNodes = xCapCapsElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node node = childNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }
                Element element = (Element) node;
                String namespaceUri = getNamespaceUri(element);
                if (namespaceUri == null)
                {
                    throw new Exception("xcap-caps element is invalid");
                }
                String localName = node.getLocalName();

                if (NAMESPACE.equals(namespaceUri))
                {
                    // auids
                    if (AUIDS_ELEMENT.equals(localName))
                    {
                        xCapCaps.setAuids(auidsFromElement(element));
                        auidsFound = true;
                    }
                    // namspaces
                    else if (NAMESPACES_ELEMENT.equals(localName))
                    {
                        xCapCaps.setNamespaces(namespacesFromElement(element));
                        namespacesFound = true;
                    }
                    // extensions
                    else if (EXTENSIONS_ELEMENT.equals(localName))
                    {
                        xCapCaps.setExtensions(extensionsFromElement(element));
                    }
                    else
                    {
                        throw new Exception("xcap-caps element is invalid");
                    }
                }
                else
                {
                    // any
                    xCapCaps.getAny().add(element);
                }
            }
            if (!auidsFound)
            {
                throw new ParsingException("xcap-caps auids element is missed");
            }
            if (!namespacesFound)
            {
                throw new ParsingException(
                        "xcap-caps namespaces element is missed");
            }
            return xCapCaps;
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates auids object from the element.
     *
     * @param auidsElement the element to analyze.
     * @return the auids object.
     * @throws Exception if there is some error during parsing.
     */
    private static AuidsType auidsFromElement(
            Element auidsElement) throws Exception
    {
        AuidsType auidsType = new AuidsType();
        if (!AUIDS_ELEMENT.equals(auidsElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(auidsElement)))
        {
            throw new Exception("auids element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = auidsElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if ((namespaceUri == null) || !isStandartXmlNamespace(namespaceUri))
                throw new Exception("auids element is invalid");
        }
        // Process elements
        NodeList childNodes = auidsElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element element = (Element) node;
            String namespaceUri = getNamespaceUri(element);
            if (namespaceUri == null)
            {
                throw new Exception("auids element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri) &&
                    AUID_ELEMENT.equals(element.getLocalName()))
            {
                auidsType.getAuid().add(element.getTextContent());
            }
            else
            {
                throw new Exception("auids element is invalid");
            }
        }
        return auidsType;
    }

    /**
     * Creates namespaces object from the element.
     *
     * @param namespacesElement the element to analyze.
     * @return the namespaces object.
     * @throws Exception if there is some error during parsing.
     */
    private static NamespacesType namespacesFromElement(
            Element namespacesElement) throws Exception
    {
        NamespacesType namespaces = new NamespacesType();
        if (!NAMESPACES_ELEMENT.equals(namespacesElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(namespacesElement)))
        {
            throw new Exception("namespaces element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = namespacesElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if ((namespaceUri == null) || !isStandartXmlNamespace(namespaceUri))
                throw new Exception("namespaces element is invalid");
        }
        // Process elements
        NodeList childNodes = namespacesElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element element = (Element) node;
            String namespaceUri = getNamespaceUri(element);
            if (namespaceUri == null)
            {
                throw new Exception("namespaces element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri) &&
                    NAMESPACE_ELEMENT.equals(element.getLocalName()))
            {
                namespaces.getNamespace().add(element.getTextContent());
            }
            else
            {
                throw new Exception("namespaces element is invalid");
            }
        }
        return namespaces;
    }

    /**
     * Creates extensions object from the element.
     *
     * @param extensionsElement the element to analyze.
     * @return the namespaces object.
     * @throws Exception if there is some error during parsing.
     */
    private static ExtensionsType extensionsFromElement(
            Element extensionsElement) throws Exception
    {
        ExtensionsType extensions = new ExtensionsType();
        if (!EXTENSIONS_ELEMENT.equals(extensionsElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(extensionsElement)))
        {
            throw new Exception("extensions element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = extensionsElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if ((namespaceUri == null) || !isStandartXmlNamespace(namespaceUri))
                throw new Exception("extensions element is invalid");
        }
        // Process elements
        NodeList childNodes = extensionsElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element element = (Element) node;
            String namespaceUri = getNamespaceUri(element);
            if (namespaceUri == null)
            {
                throw new Exception("extensions element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri) &&
                    EXTENSION_ELEMENT.equals(element.getLocalName()))
            {
                extensions.getExtension().add(element.getTextContent());
            }
            else
            {
                throw new Exception("extensions element is invalid");
            }
        }
        return extensions;
    }
}
