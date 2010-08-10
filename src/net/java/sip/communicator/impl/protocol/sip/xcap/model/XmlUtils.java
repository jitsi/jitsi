/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model;

import static net.java.sip.communicator.impl.protocol.sip.xcap.model.StringUtils.*;
import org.w3c.dom.*;

import javax.xml.*;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static javax.xml.XMLConstants.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Utility class that helps to convert classes into xml and from xml to the
 * classes.
 *
 * @author Grigorii Balutsel
 */
public final class XmlUtils
{
    /**
     * Indicates whether namespace is one of the standart xml namespace.
     *
     * @param namespace the namespace to analyze.
     * @return true is namespace is one of the standart xml namespace otherwise
     *         false.
     */
    public static boolean isStandartXmlNamespace(String namespace)
    {
        namespace = normalizeNamespace(namespace);
        return normalizeNamespace(XML_NS_URI).equals(namespace) 
            || normalizeNamespace(XMLNS_ATTRIBUTE_NS_URI).equals(namespace)
            || normalizeNamespace(W3C_XML_SCHEMA_NS_URI).equals(namespace)
            || normalizeNamespace(W3C_XML_SCHEMA_INSTANCE_NS_URI)
                .equals(namespace);
    }

    /**
     * Gets the node namespace.
     *
     * @param node the <tt>Element</tt> or <tt>Attr</tt> node to analyze.
     * @return the node namespace or null.
     */
    public static String getNamespaceUri(Node node)
    {
        String prefix = node.getPrefix();
        String namespaceUri = node.getNamespaceURI();
        if (!isNullOrEmpty(namespaceUri))
        {
            return normalizeNamespace(namespaceUri);
        }
        if (XMLConstants.XMLNS_ATTRIBUTE.equals(node.getNodeName()) ||
                XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
        {
            return normalizeNamespace(XMLNS_ATTRIBUTE_NS_URI);
        }
        Element rootElement = node.getOwnerDocument().getDocumentElement();
        Node parentNode = null;
        while (parentNode != rootElement)
        {
            if (parentNode == null)
            {
                if (node.getNodeType() == Node.ATTRIBUTE_NODE)
                {
                    parentNode = ((Attr) node).getOwnerElement();
                }
                else if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    parentNode = node.getParentNode();
                }
                else
                {
                    return null;
                }
            }
            else
            {
                parentNode = parentNode.getParentNode();
            }
            String parentPrefix = parentNode.getPrefix();
            String parentNamespaceUri = parentNode.getNamespaceURI();
            if (isNullOrEmpty(prefix))
            {
                Node xmlnsAttribute =
                        parentNode.getAttributes().getNamedItem("xmlns");
                if (xmlnsAttribute != null)
                {
                    return ((Attr) xmlnsAttribute).getValue();
                }
            }
            else if (isEquals(prefix, parentPrefix))
            {
                if (!isNullOrEmpty(parentNamespaceUri))
                {
                    return normalizeNamespace(parentNamespaceUri);
                }
            }
        }
        if ("xml".equals(prefix))
        {
            return normalizeNamespace(XML_NS_URI);
        }
        return null;
    }

    /**
     * Normalizes the namespace.
     *
     * @param namespace the namespace to normalize.
     * @return normalized namespace.
     */
    private static String normalizeNamespace(String namespace)
    {
        if (namespace.endsWith("/"))
        {
            return namespace.substring(0, namespace.length() - 1);
        }
        return namespace;
    }

    /**
     * Creates copy of the node in the scope of the document. Attributes,
     * Elements, and Tex will be include.
     *
     * @param document the xml document.
     * @param node     the node to create the copy.
     * @return the copy of the node in the scope of the document.
     * @throws Exception if there is some error during processing.
     */
    public static Node importNode(Document document, Node node)
        throws Exception
    {
        if (node.getNodeType() == Node.ATTRIBUTE_NODE)
        {
            String namespace = getNamespaceUri(node);
            if (namespace == null)
            {
                throw new Exception("Namespace cannot be null");
            }
            Attr attribute =
                    document.createAttributeNS(namespace, node.getNodeName());
            attribute.setValue(((Attr) node).getValue());
            return attribute;
        }
        else if (node.getNodeType() == Node.ELEMENT_NODE)
        {
            String namespace = getNamespaceUri(node);
            if (namespace == null)
            {
                throw new Exception("Namespace cannot be null");
            }

            Element element =
                    document.createElementNS(namespace, node.getNodeName());

            // Process attributes
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                if ("xmlns".equals(attribute.getPrefix()))
                {
                    continue;
                }
                String namespaceUri = getNamespaceUri(attribute);
                if (namespaceUri == null)
                {
                    throw new Exception("entry element is invalid");
                }
                element.getAttributes()
                        .setNamedItemNS(importNode(document, attribute));
            }
            // Process elements
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    element.appendChild(importNode(document, childNode));
                }
                else if (childNode.getNodeType() == Node.TEXT_NODE)
                {
                    element.appendChild(document.createTextNode(
                            childNode.getTextContent()));
                }
            }
            return element;
        }
        else
        {
            throw new Exception("Node cannot be processed " + node.toString());
        }
    }

    /**
     * Creates W3C Document.
     *
     * @return the W3C Document.
     * @throws Exception is there is some error during operation.
     */
    public static Document createDocument()
        throws Exception
    {
        return createDocument(null);
    }

    /**
     * Creates W3C Document from the xml.
     *
     * @param xml the xml that needs to be converted.
     * @return the W3C Document.
     * @throws Exception is there is some error during operation.
     */
    public static Document createDocument(String xml)
        throws Exception
    {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        if (!isNullOrEmpty(xml))
        {
            InputStream input = fromString(xml);
            return documentBuilder.parse(input);
        }
        else
        {
            return documentBuilder.newDocument();
        }
    }

    /**
     * Creates XML from W3C Document from the xml.
     *
     * @param document the xml that needs to be converted.
     * @return the XML.
     * @throws Exception is there is some error during operation.
     */
    public static String createXml(Document document)
            throws Exception
    {
        TransformerFactory transformerFactory =
                TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
        return stringWriter.toString();
    }

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
            Element importedElement =
                    (Element) importNode(element.getOwnerDocument(),
                            anyElement);
            element.appendChild(importedElement);
        }
    }
}
