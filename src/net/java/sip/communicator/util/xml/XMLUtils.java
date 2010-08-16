/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.xml;

import java.io.*;
import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

import javax.xml.*;
import static javax.xml.XMLConstants.*;
import javax.xml.parsers.*;

import static net.java.sip.communicator.util.StringUtils.*;
import net.java.sip.communicator.util.*;

/**
 * Common XML Tasks
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class XMLUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>XMLUtils</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(XMLUtils.class);

    /**
     * Extracts from node the attribute with the specified name.
     * @param node the node whose attribute we'd like to extract.
     * @param name the name of the attribute to extract.
     * @return a String containing the trimmed value of the attribute or null
     * if no such attribute exists
     */
    public static String getAttribute(Node node, String name)
    {
        if (node == null)
            return null;

        Node attribute = node.getAttributes().getNamedItem(name);
        return (attribute == null)
                    ? null
                    : attribute.getNodeValue().trim();
    }

    /**
     * Extracts the String content of a TXT element.
     *
     * @param parentNode the node containing the data that we'd like to get.
     * @return the string contained by the node or null if none existed.
     */
    public static String getText(Element parentNode)
    {
        Text text = getTextNode(parentNode);

        return (text == null) ? null : text.getData();
    }

    /**
     * Sets data to be the TEXT content of element
     *
     * @param parentNode the parent element.
     * @param data the data to set.
     */
    public static void setText(Element parentNode, String data)
    {
        if(data == null)
            return;

        Text txt = getTextNode(parentNode);

        if (txt != null)
            txt.setData(data);
        else
        {
            txt = parentNode.getOwnerDocument().createTextNode(data);
            parentNode.appendChild(txt);
        }
    }

    /**
     * Sets data to be the CDATA content of element
     *
     * @param element the parent element.
     * @param data the data to set.
     */
    public static void setCData(Element element, String data)
    {
        if(data == null)
            return;

        CDATASection txt = getCDataNode(element);
        if (txt != null)
            txt.setData(data);
        else
        {
            txt = element.getOwnerDocument().createCDATASection(data);
            element.appendChild(txt);
        }
    }

    /**
     * Extract the CDATA content of the specified element.
     * @param element the element whose data we need
     * @return a String containing the CDATA value of element.
     */
    public static String getCData(Element element)
    {
        CDATASection text = getCDataNode(element);

        return (text == null) ? null : text.getData().trim();
    }


    /**
     * Returns element's CDATA child node (if it has one).
     * @param element the element whose CDATA we need to get.
     * @return a CDATASection object containing the specified element's CDATA
     * content
     */
    public static CDATASection getCDataNode(Element element)
    {
        return (CDATASection)getChildByType(element,
                                            Node.CDATA_SECTION_NODE);
    }

    /**
     * Returns element's TEXT child node (if it has one).
     * @param element the element whose TEXT we need to get.
     * @return a <tt>Text</tt> object containing the specified element's
     * text content.
     */
    public static Text getTextNode(Element element)
    {
        return (Text)getChildByType(element, Node.TEXT_NODE);
    }

    /**
     * Returns first of the <tt>element</tt>'s child nodes that is of type
     * <tt>nodeType</tt>.
     * @param element the element whose child we need.
     * @param nodeType the type of the child we need.
     * @return a child of the specified <tt>nodeType</tt> or null if none
     * was found.
     */
    public static Node getChildByType(Element element, short nodeType)
    {
        if (element == null)
            return null;

        NodeList nodes = element.getChildNodes();
        if (nodes == null || nodes.getLength() < 1)
            return null;

        Node node;
        String data;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            node = nodes.item(i);
            short type = node.getNodeType();
            if (type == nodeType)
            {
                if (type == Node.TEXT_NODE ||
                    type == Node.CDATA_SECTION_NODE)
                {
                    data = ( (Text) node).getData();
                    if (data == null || data.trim().length() < 1)
                        continue;
                }

                return node;
            }
        }

        return null;
    }

    /**
     * Writes the specified document to the given file adding indentatation.
     * The default encoding is UTF-8.
     *
     * @param out the output File
     * @param document the document to write
     *
     * @throws java.io.IOException in case a TransformerException is thrown by
     * the underlying Transformer.
     */
    public static void writeXML(Document document, File out)
        throws java.io.IOException
    {
//        indentedWriteXML(document, new FileOutputStream(out));
        writeXML(document
                 , new StreamResult(
                        new OutputStreamWriter(
                                new FileOutputStream(out), "UTF-8"))
                 , null
                 , null);
    }

    /**
     * Writes the specified document to the given file adding indentatation.
     * The default encoding is UTF-8.
     *
     * @param writer the writer to use when writing the File
     * @param document the document to write
     *
     * @throws java.io.IOException in case a TransformerException is thrown by
     * the underlying Transformer.
     */
    public static void writeXML(Document document,
                                Writer   writer)
        throws java.io.IOException
    {
        writeXML(document, new StreamResult(writer), null, null);
    }

    /**
     * Writes the specified document to the given file adding indentatation.
     * The default encoding is UTF-8.
     *
     * @param streamResult the streamResult object where the document should be
     * written
     * @param document the document to write
     * @param doctypeSystem the doctype system of the xml document that we should
     * record in the file or null if none is specified.
     * @param doctypePublic the public identifier to be used in the document
     * type declaration.
     *
     * @throws java.io.IOException in case a TransformerException is thrown by
     * the underlying Transformer.
     */
    public static void writeXML(Document document,
                                StreamResult streamResult,
                                String   doctypeSystem,
                                String   doctypePublic)
        throws java.io.IOException
    {
        try
        {
           DOMSource domSource = new DOMSource(document);
           TransformerFactory tf = TransformerFactory.newInstance();

           // not working for jdk 1.4
           try
           {
                tf.setAttribute("indent-number", 4);
           }catch(Exception e){}

           Transformer serializer = tf.newTransformer();
           if(doctypeSystem != null)
                   serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                                                doctypeSystem);
            if(doctypePublic != null)
                   serializer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
                                                doctypePublic);
           // not working for jdk 1.5
           serializer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
           serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
           serializer.setOutputProperty(OutputKeys.INDENT, "yes");
           serializer.transform(domSource, streamResult);
       }
        catch (TransformerException ex) {
            logger.error("Error saving configuration file", ex);
            throw new java.io.IOException(
                "Failed to write the configuration file: "
                + ex.getMessageAndLocation());
        }
        catch (IllegalArgumentException ex) {
            //this one is thrown by the setOutputProperty or in other words -
            //shoudln't happen. so let's just log it down in case ...
            logger.error("Error saving configuration file", ex);
        }
    }

    /**
     * A simple implementation of XML writing that also allows for indentation.
     * @param doc the Document that we will be writing.
     * @param out an OutputStream to write the document through.
     */
    public static void indentedWriteXML(Document doc, OutputStream out)
    {
        if (out != null)
        {
            try
            {
                Writer wri = new OutputStreamWriter(out, "UTF-8");
//                wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+lSep);
//                (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
//                wri.flush();
//                wri.close();
                writeXML(doc
                 , new StreamResult(wri)
                 , null
                 , null);
                out.close();
            }
            catch (IOException exc)
            {
                throw new RuntimeException("Unable to write xml", exc);
            }
        }
    }


    /**
     * Whenever you'd need to print a configuration node and/or its children.
     *
     * @param root the root node to print.
     * @param out the print stream that should be used to outpu
     * @param recurse boolean
     * @param prefix String
     */
    public static void printChildElements(Element root,
                                          PrintStream out,
                                          boolean recurse,
                                          String prefix)
    {
        out.print(prefix + "<" + root.getNodeName());
        NamedNodeMap attrs = root.getAttributes();
        Node node;
        for(int i = 0; i < attrs.getLength(); i++)
        {
            node = attrs.item(i);
            out.print(" " + node.getNodeName() + "=\""
                      + node.getNodeValue() + "\"");
        }
        out.println(">");

        String data = getText(root);
        if(data != null && data.trim().length() > 0)
            out.println(prefix + "\t" + data);

        data = getCData(root);
        if(data != null && data.trim().length() > 0)
            out.println(prefix + "\t<![CDATA[" + data + "]]>");

        NodeList nodes = root.getChildNodes();
        for(int i = 0; i < nodes.getLength(); i++)
        {
            node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE)
            {
                if(recurse)
                    printChildElements((Element)node, out, recurse, prefix
                                       + "\t");
                else
                    out.println(prefix + node.getNodeName());
            }
        }

        out.println(prefix + "</" + root.getNodeName() + ">");
    }

    /**
     * Returns the child element with the specified tagName for the specified
     * parent element.
     * @param parent The parent whose child we're looking for.
     * @param tagName the name of the child to find
     * @return The child with the specified name or null if no such child was
     *         found.
     * @throws NullPointerException if parent or tagName are null
     */
    public static Element findChild(Element parent, String tagName)
    {
        if(parent == null || tagName == null)
            throw new NullPointerException("Parent or tagname were null! "
                + "parent = " + parent + "; tagName = " + tagName);

        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for(int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE
               && ((Element)node).getNodeName().equals(tagName))
                return (Element)node;
        }

        return null;
    }

    /**
     * Returns the children elements with the specified tagName for the
     * specified parent element.
     *
     * @param parent The parent whose children we're looking for.
     * @param tagName the name of the child to find
     * @return List of the children with the specified name
     * @throws NullPointerException if parent or tagName are null
     */
    public static List<Element> findChildren(Element parent, String tagName)
    {
        if (parent == null || tagName == null)
            throw new NullPointerException("Parent or tagname were null! "
                + "parent = " + parent + "; tagName = " + tagName);

        List<Element> result = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) node;
                if (element.getNodeName().equals(tagName))
                    result.add(element);
            }
        }

        return result;
    }

    /**
     * Looks through all child elements of the specified root (recursively)
     * and returns the first element that corresponds to all parameters.
     *
     * @param root the Element where the search should begin
     * @param tagName the name of the node we're looking for
     * @param keyAttributeName the name of an attribute that the node has to
     * have
     * @param keyAttributeValue the value that attribute must have
     * @return the Element in the tree under root that matches the specified
     * parameters.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static Element locateElement(Element root,
                                        String tagName,
                                        String keyAttributeName,
                                        String keyAttributeValue)
    {
        NodeList nodes = root.getChildNodes();
        int len = nodes.getLength();

        for(int i = 0; i < len; i++)
        {
            Node node = nodes.item(i);

            if(node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element element = (Element) node;

            // is this the node we're looking for?
            if(node.getNodeName().equals(tagName))
            {
                String attr = element.getAttribute(keyAttributeName);

                if((attr != null) && attr.equals(keyAttributeValue))
                    return element;
            }

            //look inside.
            Element child
                = locateElement(
                        element,
                        tagName, keyAttributeName, keyAttributeValue);

            if (child != null)
                return child;
        }
        return null;
    }

    /**
     * Looks through all child elements of the specified root (recursively) and
     * returns the elements that corresponds to all parameters.
     *
     * @param root the Element where the search should begin
     * @param tagName the name of the node we're looking for
     * @param keyAttributeName the name of an attribute that the node has to
     *            have
     * @param keyAttributeValue the value that attribute must have
     * @return list of Elements in the tree under root that match the specified
     *         parameters.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static List<Element> locateElements(Element root, String tagName,
        String keyAttributeName, String keyAttributeValue)
    {
        List<Element> result = new ArrayList<Element>();
        NodeList nodes = root.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            // is this the node we're looking for?
            if (node.getNodeName().equals(tagName))
            {
                Element element = (Element) node;
                String attr = element.getAttribute(keyAttributeName);

                if (attr != null && attr.equals(keyAttributeValue))
                    result.add(element);
            }

            // look inside.

            List<Element> childs =
                locateElements((Element) node, tagName, keyAttributeName,
                    keyAttributeValue);

            if (childs != null)
                result.addAll(childs);

        }

        return result;
    }

    /**
     * Indicates whether namespace is one of the standart xml namespace.
     *
     * @param namespace the namespace to analyze.
     * @return true if namespace is one of the standart xml namespace otherwise
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
                    // If attribute doesn't have prefix - it has its parent
                    // namespace
                    if (isNullOrEmpty(prefix))
                    {
                        prefix = parentNode.getPrefix();
                    }
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
     * Indicates whether element has any child element.
     *
     * @param element the namespace to analyze.
     * @return true if element has any child element otherwise false.
     */
    public static boolean hasChildElements(Element element)
    {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                return true;
            }
        }
        return false;
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

}
