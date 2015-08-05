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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;
import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAny;
import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAnyAttributes;
import static org.jitsi.util.StringUtils.isNullOrEmpty;
import static org.jitsi.util.xml.XMLUtils.createDocument;
import static org.jitsi.util.xml.XMLUtils.createXml;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;

import javax.xml.namespace.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts resource-lists xml to the object model
 * and object model to the resource-lists xml.
 *
 * @author Grigorii Balutsel
 */
public final class ResourceListsParser
{
    private static String NAMESPACE = "urn:ietf:params:xml:ns:resource-lists";

    private static String RESOURCE_LISTS_ELEMENT = "resource-lists";

    private static String LIST_ELEMENT = "list";

    private static String LIST_NAME_ATTR = "name";

    private static String ENTRY_ELEMENT = "entry";

    private static String ENTRY_URI_ATTR = "uri";

    private static String ENTRYREF_ELEMENT = "entry-ref";

    private static String ENTRYREF_REF_ATTR = "ref";

    private static String EXTERNAL_ELEMENT = "external";

    private static String EXTERNAL_ANCHOR_ATTR = "anchor";

    private static String DISPALY_NAME_ELEMENT = "display-name";

    private static String DISPALY_NAME_LANG_ATTR = "lang";

    private ResourceListsParser()
    {
    }

    /**
     * Creates resource-lists object from the element.
     *
     * @param xml the XML to analyze.
     * @return the resource-lists object.
     * @throws ParsingException if there is some error during parsing.
     */
    public static ResourceListsType fromXml(String xml)
            throws ParsingException
    {
        if (isNullOrEmpty(xml))
        {
            throw new IllegalArgumentException("XML cannot be null or empty");
        }
        try
        {
            ResourceListsType resourceLists = new ResourceListsType();
            Document document = createDocument(xml);
            Element resourceListsElement = document.getDocumentElement();
            String localName = resourceListsElement.getLocalName();
            if (!NAMESPACE.equals(resourceListsElement.getNamespaceURI()) ||
                    !RESOURCE_LISTS_ELEMENT.equals(localName))
            {
                throw new Exception("Document doesn't contain resource-lists " +
                        "element");
            }
            // Process attributes
            NamedNodeMap attributes = resourceListsElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                String namespaceUri = getNamespaceUri(attribute);
                if (namespaceUri == null)
                {
                    throw new Exception("resource-lists element is invalid");
                }
                if (isStandartXmlNamespace(namespaceUri))
                {
                    continue;
                }
                throw new Exception("resource-lists element is invalid");
            }
            // Process elements
            NodeList childNodes = resourceListsElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node node = childNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }
                Element listElement = (Element) node;
                resourceLists.getList().add(listFromElement(listElement));
            }
            return resourceLists;
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates XML from the resource-lists element.
     *
     * @param resourceLists the resource-lists to analyze.
     * @return the resource-lists xml.
     * @throws ParsingException if there is some error during parsing.
     */
    public static String toXml(ResourceListsType resourceLists)
            throws ParsingException
    {
        if (resourceLists == null)
        {
            throw new IllegalArgumentException("resource-lists cannot be null");
        }
        try
        {
            Document document = createDocument();
            Element resourceListsElement =
                    document.createElementNS(NAMESPACE, RESOURCE_LISTS_ELEMENT);
            for (ListType list : resourceLists.getList())
            {
                resourceListsElement
                        .appendChild(elementFromList(document, list));
            }
            document.appendChild(resourceListsElement);
            return createXml(document);
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates list object from the element.
     *
     * @param listElement the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static ListType listFromElement(Element listElement)
            throws Exception
    {
        ListType list = new ListType();
        if (!LIST_ELEMENT.equals(listElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(listElement)))
        {
            throw new Exception("list element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = listElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("list element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                if (LIST_NAME_ATTR.equals(attribute.getLocalName()))
                {
                    list.setName(attribute.getValue());
                    continue;
                }
                else
                {
                    throw new Exception("list element is invalid");
                }
            }
            QName qName = new QName(namespaceUri,
                    attribute.getLocalName(),
                    attribute.getPrefix() == null ? "" : attribute.getPrefix());
            list.getAnyAttributes().put(qName, attribute.getValue());
        }
        // Process elements
        NodeList childNodes = listElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element element = (Element) node;
            String localName = element.getLocalName();
            String namespaceUri = getNamespaceUri(element);
            if (NAMESPACE.equals(namespaceUri))
            {
                // display-name
                if (DISPALY_NAME_ELEMENT.equals(localName))
                {
                    list.setDisplayName(displayNameFromElement(element));
                }
                // entry
                else if (ENTRY_ELEMENT.equals(localName))
                {
                    list.getEntries().add(entryFromElement(element));
                }
                // entry-ref
                else if (ENTRYREF_ELEMENT.equals(localName))
                {
                    list.getEntryRefs().add(entryRefFromElement(element));
                }
                // list
                else if (LIST_ELEMENT.equals(localName))
                {
                    list.getLists().add(listFromElement(element));
                }
                // extenal
                else if (EXTERNAL_ELEMENT.equals(localName))
                {
                    list.getExternals().add(externalFromElement(element));
                }
                else
                {
                    throw new Exception("list element is invalid");
                }
            }
            else
            {
                // any
                list.getAny().add(element);
            }
        }
        return list;
    }

    /**
     * Creates list element from the object.
     *
     * @param document the xml document.
     * @param list     the list to analyze.
     * @return the list element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromList(Document document, ListType list)
            throws Exception
    {
        Element listElement = document.createElementNS(NAMESPACE, LIST_ELEMENT);
        if (list.getName() != null)
        {
            listElement.setAttribute(LIST_NAME_ATTR, list.getName());
        }
        // display-name
        if (list.getDisplayName() != null)
        {
            listElement.appendChild(elementFromDisplayName(document,
                    list.getDisplayName()));
        }
        // entry
        for (EntryType entry : list.getEntries())
        {
            listElement.appendChild(elementFromEntry(document, entry));
        }
        // entry-ref
        for (EntryRefType entryRef : list.getEntryRefs())
        {
            listElement.appendChild(elementFromEntryRef(document, entryRef));
        }
        // list
        for (ListType subList : list.getLists())
        {
            listElement.appendChild(elementFromList(document, subList));
        }
        // external
        for (ExternalType external : list.getExternals())
        {
            listElement.appendChild(elementFromExternal(document, external));
        }
        processAnyAttributes(listElement, list.getAnyAttributes());
        processAny(listElement, list.getAny());
        return listElement;
    }

    /**
     * Creates entry element from the object.
     *
     * @param document the xml document.
     * @param entry    the entry to analyze.
     * @return the entry element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromEntry(Document document, EntryType entry)
            throws Exception
    {
        Element entryElement = document.createElementNS(NAMESPACE,
                ENTRY_ELEMENT);
        if (isNullOrEmpty(entry.getUri()))
        {
            throw new Exception("entry uri attribute is missed");
        }
        entryElement.setAttribute(ENTRY_URI_ATTR, entry.getUri());
        if (entry.getDisplayName() != null)
        {
            entryElement.appendChild(elementFromDisplayName(document,
                    entry.getDisplayName()));
        }
        processAnyAttributes(entryElement, entry.getAnyAttributes());
        processAny(entryElement, entry.getAny());
        return entryElement;
    }

    /**
     * Creates entryRef element from the object.
     *
     * @param document the xml document.
     * @param entryRef the entry to analyze.
     * @return the entryRef element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromEntryRef(
            Document document,
            EntryRefType entryRef)
            throws Exception
    {
        Element entryRefElement = document.createElementNS(NAMESPACE,
                ENTRYREF_ELEMENT);
        if (isNullOrEmpty(entryRef.getRef()))
        {
            throw new Exception("entry-ref ref attribute is missed");
        }
        entryRefElement.setAttribute(ENTRYREF_REF_ATTR, entryRef.getRef());
        if (entryRef.getDisplayName() != null)
        {
            entryRefElement.appendChild(elementFromDisplayName(document,
                    entryRef.getDisplayName()));
        }
        processAnyAttributes(entryRefElement, entryRef.getAnyAttributes());
        processAny(entryRefElement, entryRef.getAny());
        return entryRefElement;
    }

    /**
     * Creates external element from the object.
     *
     * @param document the xml document.
     * @param external the entry to analyze.
     * @return the external element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromExternal(
            Document document,
            ExternalType external)
            throws Exception
    {
        Element externalElement = document.createElementNS(NAMESPACE,
                EXTERNAL_ELEMENT);
        if (!isNullOrEmpty(external.getAnchor()))
        {
            externalElement.setAttribute(EXTERNAL_ANCHOR_ATTR,
                    external.getAnchor());
        }
        if (external.getDisplayName() != null)
        {
            externalElement.appendChild(elementFromDisplayName(document,
                    external.getDisplayName()));
        }
        processAnyAttributes(externalElement, external.getAnyAttributes());
        processAny(externalElement, external.getAny());
        return externalElement;
    }

    /**
     * Creates display-name element from the object.
     *
     * @param document    the xml document.
     * @param displayName the display-name to analyze.
     * @return the external element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromDisplayName(
            Document document,
            DisplayNameType displayName)
            throws Exception
    {
        Element displayNameElement = document.createElementNS(NAMESPACE,
                DISPALY_NAME_ELEMENT);
        if (displayName.getLang() != null)
        {
            displayNameElement.setAttribute(
                    XML_NS_PREFIX + ":" + DISPALY_NAME_LANG_ATTR,
                    displayName.getLang());
        }
        if (displayName.getValue() != null)
        {
            displayNameElement.setTextContent(displayName.getValue());
        }
        return displayNameElement;
    }

    /**
     * Creates entry object from the element.
     *
     * @param entryElement the element to analyze.
     * @return the entry object.
     * @throws Exception if there is some error during parsing.
     */
    private static EntryType entryFromElement(Element entryElement)
            throws Exception
    {
        EntryType entry = new EntryType();
        if (!ENTRY_ELEMENT.equals(entryElement.getNodeName()) ||
                !NAMESPACE.equals(getNamespaceUri(entryElement)))
        {
            throw new Exception("entry element is invalid");
        }
        String uri = null;
        // Process attributes
        NamedNodeMap attributes = entryElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("entry element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                if (ENTRY_URI_ATTR.equals(attribute.getLocalName()))
                {
                    uri = attribute.getValue();
                    continue;
                }
                else
                {
                    throw new Exception("entry element is invalid");
                }
            }
            QName qName = new QName(namespaceUri,
                    attribute.getLocalName(),
                    attribute.getPrefix() == null ? "" : attribute.getPrefix());
            entry.getAnyAttributes().put(qName, attribute.getValue());
        }
        if (uri == null)
        {
            throw new Exception("entry uri attribute is missed");
        }
        entry.setUri(uri);
        // Process elements
        NodeList childNodes = entryElement.getChildNodes();
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
                throw new Exception("entry element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                // display-name
                if (DISPALY_NAME_ELEMENT.equals(element.getLocalName()))
                {
                    entry.setDisplayName(displayNameFromElement(element));
                    continue;
                }
                else
                {
                    throw new Exception("entry element is invalid");
                }
            }
            // any
            entry.getAny().add(element);
        }
        return entry;
    }

    /**
     * Creates entry-ref object from the element.
     *
     * @param entryRefElement the element to analyze.
     * @return the entry-ref object.
     * @throws Exception if there is some error during parsing.
     */
    private static EntryRefType entryRefFromElement(Element entryRefElement)
            throws Exception
    {
        EntryRefType entryRef = new EntryRefType();
        if (!ENTRYREF_ELEMENT.equals(entryRefElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(entryRefElement)))
        {
            throw new Exception("entry-ref element is invalid");
        }
        String ref = null;
        // Process attributes
        NamedNodeMap attributes = entryRefElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("entry-ref element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                if (ENTRYREF_REF_ATTR.equals(attribute.getLocalName()))
                {
                    ref = attribute.getValue();
                    continue;
                }
                else
                {
                    throw new Exception("entry-ref element is invalid");
                }
            }
            QName qName = new QName(attribute.getNamespaceURI(),
                    attribute.getName(),
                    attribute.getPrefix() == null ? "" : attribute.getPrefix());
            entryRef.getAnyAttributes().put(qName, attribute.getValue());
        }
        if (ref == null)
        {
            throw new Exception("entry-ref ref attribute is missed");
        }
        entryRef.setRef(ref);
        // Process elements
        NodeList childNodes = entryRefElement.getChildNodes();
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
                throw new Exception("entry-ref element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                // display-name
                if (DISPALY_NAME_ELEMENT.equals(element.getLocalName()))
                {
                    entryRef.setDisplayName(displayNameFromElement(element));
                    continue;
                }
                else
                {
                    throw new Exception("entry-ref element is invalid");
                }
            }
            // any
            entryRef.getAny().add(element);
        }
        return entryRef;
    }

    /**
     * Creates external object from the element.
     *
     * @param entryElement the element to analyze.
     * @return the external object.
     * @throws Exception if there is some error during parsing.
     */
    private static ExternalType externalFromElement(Element entryElement)
            throws Exception
    {
        ExternalType external = new ExternalType();
        if (!EXTERNAL_ELEMENT.equals(entryElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(entryElement)))
        {
            throw new Exception("external element is invalid");
        }

        // Process attributes
        NamedNodeMap attributes = entryElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("external element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                if (EXTERNAL_ANCHOR_ATTR.equals(attribute.getLocalName()))
                {
                    external.setAnchor(attribute.getValue());
                    continue;
                }
                else
                {
                    throw new Exception("external element is invalid");
                }
            }
            QName qName = new QName(attribute.getNamespaceURI(),
                    attribute.getName(),
                    attribute.getPrefix() == null ? "" : attribute.getPrefix());
            external.getAnyAttributes().put(qName, attribute.getValue());
        }
        // Process elements
        NodeList childNodes = entryElement.getChildNodes();
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
                throw new Exception("external element is invalid");
            }
            if (NAMESPACE.equals(namespaceUri))
            {
                // display-name
                if (DISPALY_NAME_ELEMENT.equals(element.getLocalName()))
                {
                    external.setDisplayName(displayNameFromElement(element));
                    continue;
                }
                else
                {
                    throw new Exception("external element is invalid");
                }
            }
            // any
            external.getAny().add(element);
        }
        return external;
    }

    /**
     * Creates display-name object from the element.
     *
     * @param displayNameElement the element to analyze.
     * @return the display-name object.
     * @throws Exception if there is some error during parsing.
     */
    private static DisplayNameType displayNameFromElement(
            Element displayNameElement) throws Exception
    {
        DisplayNameType displayName = new DisplayNameType();
        if (!DISPALY_NAME_ELEMENT.equals(displayNameElement.getLocalName()) ||
                !NAMESPACE.equals(getNamespaceUri(displayNameElement)))
        {
            throw new Exception("display-name element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = displayNameElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("display-name element is invalid");
            }
            if (DISPALY_NAME_LANG_ATTR.equals(attribute.getLocalName()) &&
                    XML_NS_URI.equals(namespaceUri))
            {
                displayName.setLang(attribute.getValue());
            }
            else if (!isStandartXmlNamespace(namespaceUri))
            {
                throw new Exception("display-name element is invalid");
            }
        }
        // Process elements
        NodeList childNodes = displayNameElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                throw new Exception("display-name element is invalid");
            }
        }
        displayName.setValue(displayNameElement.getTextContent());
        return displayName;
    }
}
