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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent;

import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;
import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAny;
import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAnyAttributes;
import static org.jitsi.util.StringUtils.isNullOrEmpty;
import static org.jitsi.util.xml.XMLUtils.createDocument;
import static org.jitsi.util.xml.XMLUtils.createXml;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;

import java.util.*;

import javax.xml.namespace.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts pres-content xml to the object model and
 * object model to the pres-content xml.
 *
 * @author Grigorii Balutsel
 */
public class PresContentParser
{
    private static String NAMESPACE = "urn:oma:xml:prs:pres-content";

    private static String CONTENT_ELEMENT = "content";

    private static String MIMETYPE_ELEMENT = "mime-type";

    private static String ENCODING_ELEMENT = "encoding";

    private static String DESCRIPTION_ELEMENT = "description";

    private static String DESCRIPTION_LANG_ATTR = "lang";

    private static String DATA_ELEMENT = "data";

    /**
     * Creates xcap-caps object from the element.
     *
     * @param xml the XML to analyze.
     * @return the xcap-caps object.
     * @throws ParsingException if there is some error during parsing.
     */
    public static ContentType fromXml(String xml)
            throws ParsingException
    {
        if (isNullOrEmpty(xml))
        {
            throw new IllegalArgumentException("XML cannot be null or empty");
        }
        try
        {
            ContentType content = new ContentType();
            Document document = createDocument(xml);
            Element contentElement = document.getDocumentElement();
            if (!NAMESPACE.equals(contentElement.getNamespaceURI()) ||
                    !CONTENT_ELEMENT.equals(contentElement.getLocalName()))
            {
                throw new Exception(
                        "Document doesn't contain content element");
            }
            // Process attributes
            NamedNodeMap attributes = contentElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                String namespaceUri = getNamespaceUri(attribute);
                if (namespaceUri == null)
                {
                    throw new Exception("content element is invalid");
                }
                if (isStandartXmlNamespace(namespaceUri))
                {
                    continue;
                }
                if (NAMESPACE.equals(namespaceUri))
                {
                    throw new Exception("content element is invalid");
                }
                QName qName = new QName(namespaceUri,
                        attribute.getLocalName(),
                        attribute.getPrefix() == null ? "" :
                                attribute.getPrefix());
                content.getAnyAttributes().put(qName, attribute.getValue());
            }
            // Process elements
            NodeList childNodes = contentElement.getChildNodes();
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
                    throw new Exception("content element is invalid");
                }
                String localName = node.getLocalName();
                if (NAMESPACE.equals(namespaceUri))
                {
                    // data
                    if (DATA_ELEMENT.equals(localName))
                    {
                        content.setData(dataFromElement(element));
                    }
                    // mime-type
                    else if (MIMETYPE_ELEMENT.equals(localName))
                    {
                        content.setMimeType(mimeTypeFromElement(element));
                    }
                    // encoding
                    else if (ENCODING_ELEMENT.equals(localName))
                    {
                        content.setEncoding(encodingFromElement(element));
                    }
                    // descritpion
                    else if (DESCRIPTION_ELEMENT.equals(localName))
                    {
                        content.getDescription()
                                .add(descriptionFromElement(element));
                    }
                    else
                    {
                        throw new Exception("content element is invalid");
                    }
                }
                else
                {
                    // any
                    content.getAny().add(element);
                }
            }
            return content;
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates XML from the pres-content element.
     *
     * @param content the pres-content to analyze.
     * @return the pres-content xml.
     * @throws ParsingException if there is some error during parsing.
     */
    public static String toXml(ContentType content)
            throws ParsingException
    {
        if (content == null)
        {
            throw new IllegalArgumentException("pres-content cannot be null");
        }
        try
        {
            Document document = createDocument();
            Element presContentElement =
                    document.createElementNS(NAMESPACE, CONTENT_ELEMENT);
            if (content.getData() != null)
            {
                presContentElement.appendChild(elementFromValue(
                        document,
                        DATA_ELEMENT,
                        content.getData().getValue(),
                        content.getData().getAnyAttributes()));
            }
            if (content.getEncoding() != null)
            {
                presContentElement.appendChild(elementFromValue(
                        document,
                        ENCODING_ELEMENT,
                        content.getEncoding().getValue(),
                        content.getEncoding().getAnyAttributes()));
            }
            if (content.getMimeType() != null)
            {
                presContentElement.appendChild(elementFromValue(
                        document,
                        MIMETYPE_ELEMENT,
                        content.getMimeType().getValue(),
                        content.getMimeType().getAnyAttributes()));
            }
            for (DescriptionType description : content.getDescription())
            {
                presContentElement.appendChild(
                        elementFromDescription(document, description));
            }
            processAnyAttributes(presContentElement,
                    content.getAnyAttributes());
            processAny(presContentElement, content.getAny());
            document.appendChild(presContentElement);
            return createXml(document);
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates display-name element from the value and attributes.
     *
     * @param document      the xml document.
     * @param nodeName      the local node name.
     * @param value         the xml node value.
     * @param anyAttributes the map of any attributes
     * @return the xml element.
     */
    private static Element elementFromValue(
            Document document, String nodeName, String value,
            Map<QName, String> anyAttributes)
    {
        Element element = document.createElementNS(NAMESPACE, nodeName);
        if (value != null)
        {
            element.setTextContent(value);
        }
        processAnyAttributes(element, anyAttributes);
        return element;
    }

    /**
     * Creates ddescriptionelement from the object.
     *
     * @param document    the xml document.
     * @param description the description to analyze.
     * @return the description element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromDescription(
            Document document,
            DescriptionType description)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE,
                DESCRIPTION_ELEMENT);
        if (description.getLang() != null)
        {
            element.setAttribute(
                    XML_NS_PREFIX + ":" + DESCRIPTION_LANG_ATTR,
                    description.getLang());
        }
        if (description.getValue() != null)
        {
            element.setTextContent(description.getValue());
        }
        processAnyAttributes(element, description.getAnyAttributes());
        return element;
    }

    /**
     * Creates data object from the element.
     *
     * @param element the element to analyze.
     * @return the display-name object.
     * @throws Exception if there is some error during parsing.
     */
    private static DataType dataFromElement(Element element)
            throws Exception
    {
        DataType result = new DataType();
        if (!DATA_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("data element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("data element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("data element is invalid");
            }
            QName qName = new QName(namespaceUri,
                    attribute.getLocalName(),
                    attribute.getPrefix() == null ? "" :
                            attribute.getPrefix());
            result.getAnyAttributes().put(qName, attribute.getValue());
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("data element is invalid");
            }
        }
        result.setValue(element.getTextContent());
        return result;
    }

    /**
     * Creates data object from the element.
     *
     * @param element the element to analyze.
     * @return the data object.
     * @throws Exception if there is some error during parsing.
     */
    private static EncodingType encodingFromElement(Element element)
            throws Exception
    {
        EncodingType result = new EncodingType();
        if (!ENCODING_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("encoding element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("encoding element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("encoding element is invalid");
            }
            QName qName = new QName(namespaceUri,
                    attribute.getLocalName(),
                    attribute.getPrefix() == null ? "" :
                            attribute.getPrefix());
            result.getAnyAttributes().put(qName, attribute.getValue());
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("encoding element is invalid");
            }
        }
        result.setValue(element.getTextContent());
        return result;
    }

    /**
     * Creates mime-type object from the element.
     *
     * @param element the element to analyze.
     * @return the mime-type object.
     * @throws Exception if there is some error during parsing.
     */
    private static MimeType mimeTypeFromElement(Element element)
            throws Exception
    {
        MimeType result = new MimeType();
        if (!MIMETYPE_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("mime-type element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("mime-type element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("mime-type element is invalid");
            }
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("encoding element is invalid");
            }
        }
        result.setValue(element.getTextContent());
        return result;
    }

    /**
     * Creates description object from the element.
     *
     * @param element the element to analyze.
     * @return the description object.
     * @throws Exception if there is some error during parsing.
     */
    private static DescriptionType descriptionFromElement(Element element)
            throws Exception
    {
        DescriptionType result = new DescriptionType();
        if (!DESCRIPTION_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("description element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("description element is invalid");
            }
            if (DESCRIPTION_LANG_ATTR.equals(attribute.getLocalName()) &&
                    XML_NS_URI.equals(namespaceUri))
            {
                result.setLang(attribute.getValue());
                continue;
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("description element is invalid");
            }
            QName qName = new QName(namespaceUri,
                    attribute.getLocalName(),
                    attribute.getPrefix() == null ? "" :
                            attribute.getPrefix());
            result.getAnyAttributes().put(qName, attribute.getValue());
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("description element is invalid");
            }
        }
        result.setValue(element.getTextContent());
        return result;
    }
}
