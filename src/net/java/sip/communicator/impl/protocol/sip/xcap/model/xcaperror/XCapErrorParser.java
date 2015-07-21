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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

import static org.jitsi.util.StringUtils.isNullOrEmpty;
import static org.jitsi.util.xml.XMLUtils.createDocument;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts xcap-error xml to the object model and
 * object model to the xcap-error xml.
 *
 * @author Grigorii Balutsel
 */

public final class XCapErrorParser
{
    private static final String NAMESPACE = "urn:ietf:params:xml:ns:xcap-error";

    private static final String XCAP_ERROR_ELEMENT = "xcap-error";

    private static final String CANNOT_DELETE_ELEMENT = "cannot-delete";

    private static final String CANNOT_INSERT_ELEMENT = "cannot-insert";

    private static final String CONSTRAINT_FAILURE_ELEMENT =
            "constraint-failure";

    private static final String EXTENSION_ELEMENT = "extension";

    private static final String NOPARENT_ELEMENT = "no-parent";

    private static final String NOPARENT_ANCESTOR_ELEMENT = "ancestor";

    private static final String NOT_UTF8_ELEMENT = "not-utf-8";

    private static final String NOT_WELL_FORMED_ELEMENT = "not-well-formed";

    private static final String NOT_XML_ATT_VALUE_ELEMENT = "not-xml-att-value";

    private static final String NOT_XMLF_FRAG_ELEMENT = "not-xml-frag";

    private static final String SCHEMA_VALIDATION_ERROR_ELEMENT =
            "schema-validation-error";

    private static final String UNIQUENESS_FAILURE_ELEMENT =
            "uniqueness-failure";

    private static final String UNIQUENESS_FAILURE_EXISTS_ELEMENT =
            "exists";

    private static final String UNIQUENESS_FAILURE_EXISTS_FIELD_ATTR =
            "field";

    private static final String UNIQUENESS_FAILURE_EXISTS_ALT_VALUE_ELEMENT =
            "alt-value";

    private static final String PHRASE_ATTR = "phrase";

    /**
     * Creates xcap-error object from the element.
     *
     * @param xml the XML to analyze.
     * @return the resource-lists object.
     * @throws ParsingException if there is some error during parsing.
     */
    public static XCapErrorType fromXml(String xml)
            throws ParsingException
    {
        if (isNullOrEmpty(xml))
        {
            throw new IllegalArgumentException("XML cannot be null or empty");
        }
        try
        {
            XCapErrorType error = new XCapErrorType();
            Document document = createDocument(xml);
            Element xCapErrorElement = document.getDocumentElement();
            if (!NAMESPACE.equals(xCapErrorElement.getNamespaceURI()) ||
                    !XCAP_ERROR_ELEMENT.equals(xCapErrorElement.getLocalName()))
            {
                throw new Exception("Document doesn't contain xcap-error " +
                        "element");
            }
            // Process attributes
            NamedNodeMap attributes = xCapErrorElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                String namespaceUri = getNamespaceUri(attribute);
                if ((namespaceUri == null)
                        || !isStandartXmlNamespace(namespaceUri))
                    throw new Exception("xcap-error element is invalid");
            }
            // Process elements
            NodeList childNodes = xCapErrorElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node node = childNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }
                Element element = (Element) node;
                String namespaceUri = getNamespaceUri(element);
                if (!NAMESPACE.equals(namespaceUri))
                {
                    throw new Exception("xcap-error element is invalid");
                }
                error.setError(errorFromElement(element));
            }
            return error;
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates xcap-error object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static XCapError errorFromElement(Element element)
            throws Exception
    {
        XCapError error;
        if (!NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("error-element element is invalid");
        }
        String localName = element.getLocalName();
        if (CANNOT_DELETE_ELEMENT.equals(localName))
        {
            error = new CannotDeleteType(getPhraseAttribute(element));
        }
        else if (CANNOT_INSERT_ELEMENT.equals(localName))
        {
            error = new CannotInsertType(getPhraseAttribute(element));
        }
        else if (CONSTRAINT_FAILURE_ELEMENT.equals(localName))
        {
            error = new ConstraintFailureType(getPhraseAttribute(element));
        }
        else if (EXTENSION_ELEMENT.equals(localName))
        {
            error = getExtensionFromElement(element);
        }
        else if (NOPARENT_ELEMENT.equals(localName))
        {
            error = getNoParentFromElement(element);
        }
        else if (NOT_UTF8_ELEMENT.equals(localName))
        {
            error = new NotUtf8Type(getPhraseAttribute(element));
        }
        else if (NOT_WELL_FORMED_ELEMENT.equals(localName))
        {
            error = new NotWellFormedType(getPhraseAttribute(element));
        }
        else if (NOT_XML_ATT_VALUE_ELEMENT.equals(localName))
        {
            error = new NotXmlAttValueType(getPhraseAttribute(element));
        }
        else if (NOT_XMLF_FRAG_ELEMENT.equals(localName))
        {
            error = new NotXmlAttValueType(getPhraseAttribute(element));
        }
        else if (SCHEMA_VALIDATION_ERROR_ELEMENT.equals(localName))
        {
            error = new SchemaValidationErrorType(
                    getPhraseAttribute(element));
        }
        else if (UNIQUENESS_FAILURE_ELEMENT.equals(element.getLocalName()))
        {
            error = getUniquenessFailureFromElement(element);
        }
        else
        {
            throw new Exception("content element is invalid");
        }
        return error;
    }

    /**
     * Gets phrase value from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static String getPhraseAttribute(Element element)
            throws Exception
    {
        String result = null;
        if (!NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("error element is invalid");
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
            if (!NAMESPACE.equals(namespaceUri) ||
                    !PHRASE_ATTR.equals(attribute.getLocalName()) ||
                    result != null)
            {
                throw new Exception("error element is invalid");
            }
            result = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("error element is invalid");
            }
        }
        return result;
    }

    /**
     * Creates extension object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static ExtensionType getExtensionFromElement(Element element)
            throws Exception
    {
        ExtensionType result = new ExtensionType();
        if (!EXTENSION_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("extension element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if ((namespaceUri == null) || !isStandartXmlNamespace(namespaceUri))
                throw new Exception("extension element is invalid");
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            if (namespaceUri == null)
            {
                throw new Exception("extension element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("extension element is invalid");
            }
            result.getAny().add(childElement);
        }
        return result;
    }

    /**
     * Creates no-parent object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static NoParentType getNoParentFromElement(Element element)
            throws Exception
    {
        NoParentType result = new NoParentType();
        if (!NOPARENT_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("no-parent element is invalid");
        }
        String phrase = null;
        String ancestor = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("no-parent element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !PHRASE_ATTR.equals(attribute.getLocalName()) ||
                    phrase != null)
            {
                throw new Exception("no-parent element is invalid");
            }
            phrase = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            String localName = childElement.getLocalName();
            if (namespaceUri == null)
            {
                throw new Exception("no-parent element is invalid");
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !NOPARENT_ANCESTOR_ELEMENT.equals(localName) ||
                    ancestor != null)
            {
                throw new Exception("no-parent element is invalid");
            }
            ancestor = childElement.getTextContent();
        }
        result.setPhrase(phrase);
        result.setAncestor(ancestor);
        return result;
    }

    /**
     * Creates uniqueness-failure object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static UniquenessFailureType getUniquenessFailureFromElement(
            Element element)
            throws Exception
    {
        UniquenessFailureType result = new UniquenessFailureType();
        if (!UNIQUENESS_FAILURE_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("uniqueness-failure element is invalid");
        }
        String phrase = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("uniqueness-failure element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !PHRASE_ATTR.equals(attribute.getLocalName()) ||
                    phrase != null)
            {
                throw new Exception("uniqueness-failure element is invalid");
            }
            phrase = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            String localName = childElement.getLocalName();
            if (namespaceUri == null)
            {
                throw new Exception("uniqueness-failure element is invalid");
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !UNIQUENESS_FAILURE_EXISTS_ELEMENT.equals(localName))
            {
                throw new Exception("uniqueness-failure element is invalid");
            }
            result.getExists().add(getExistsFromElement(childElement));
        }
        result.setPhrase(phrase);
        return result;
    }

    /**
     * Creates exists object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static UniquenessFailureType.ExistsType getExistsFromElement(
            Element element)
            throws Exception
    {
        UniquenessFailureType.ExistsType result =
                new UniquenessFailureType.ExistsType();
        if (!UNIQUENESS_FAILURE_EXISTS_ELEMENT.equals(element.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(element)))
        {
            throw new Exception("exists element is invalid");
        }
        String field = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("exists element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !UNIQUENESS_FAILURE_EXISTS_FIELD_ATTR
                            .equals(attribute.getLocalName()) ||
                    field != null)
            {
                throw new Exception("exists element is invalid");
            }
            field = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            String localName = childElement.getLocalName();
            if (namespaceUri == null)
            {
                throw new Exception("exists element is invalid");
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !UNIQUENESS_FAILURE_EXISTS_ALT_VALUE_ELEMENT
                            .equals(localName))
            {
                throw new Exception("exists element is invalid");
            }
            result.getAltValue().add(childElement.getTextContent());
        }
        if (field == null)
        {
            throw new Exception("exists element is invalid");
        }
        result.setField(field);
        return result;
    }
}
