/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration.xml;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import net.java.sip.communicator.impl.configuration.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

/**
 * Implements a <code>ConfigurationStore</code> which serializes property
 * name-value associations in XML format.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class XMLConfigurationStore
    implements ConfigurationStore
{
    /**
     * The <tt>Logger</tt> used by the <tt>XMLConfigurationStore</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(XMLConfigurationStore.class);

    /** Name of the xml attribute containing property values */
    private static final String ATTRIBUTE_VALUE = "value";

    /**
     * Name of the xml attribute indicating that a property is to be resolved in
     * the system properties
     */
    private static final String SYSTEM_ATTRIBUTE_NAME = "system";

    /** The value of the Name of the xml attribute containing property values */
    private static final String SYSTEM_ATTRIBUTE_TRUE = "true";

    /**
     * The list of properties currently registered in the configuration service.
     */
    private Hashtable<String, Object> properties
        = new Hashtable<String, Object>();

    /**
     * Contains the properties that were initially loaded from the configuration
     * file or (if the properties have been modified and saved since initially
     * loaded) those that were last written to the file.We use the property so
     * that we could determine which properties are new and do not have a
     * corresponding node in the XMLDocument object.
     */
    private Map<String, Object> fileExtractedProperties =
        new Hashtable<String, Object>();

    /**
     * The XML Document containing the configuration file this service loaded.
     */
    private Document propertiesDocument;

    /**
     * Returns a copy of the Map containing all configuration properties
     * @return a Map clone of the current configuration property set.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> cloneProperties()
    {
        return (Map<String, Object>) properties.clone();
    }

    private Document createPropertiesDocument()
    {
        if (propertiesDocument == null)
        {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;
            try
            {
                builder = factory.newDocumentBuilder();
            }
            catch (ParserConfigurationException ex)
            {
                logger.error("Failed to create a DocumentBuilder", ex);
                return null;
            }
            propertiesDocument = builder.newDocument();
            propertiesDocument.appendChild(propertiesDocument
                .createElement("sip-communicator"));
        }
        return propertiesDocument;
    }

    /*
     * Implements ConfigurationStore#getProperty(String).
     */
    public Object getProperty(String propertyName)
    {
        Object value = properties.get(propertyName);

        // if this is a property reference make sure we return the referenced
        // value and not the reference itself
        if (value instanceof PropertyReference)
            return ((PropertyReference) value).getValue();
        else
            return value;
    }

    /*
     * Implements ConfigurationStore#getPropertyNames().
     */
    public String[] getPropertyNames()
    {
        Set<String> propertyNames = properties.keySet();
        return propertyNames.toArray(new String[propertyNames.size()]);
    }

    /*
     * Implements ConfigurationStore#isSystemProperty(String).
     */
    public boolean isSystemProperty(String propertyName)
    {
        return properties.get(propertyName) instanceof PropertyReference;
    }

    /**
     * Loads the contents of the specified configuration file into the local
     * properties object.
     * @param file a reference to the configuration file to load.
     * @return a hashtable containing all properties extracted from the
     * specified file.
     *
     * @throws IOException if the specified file does not exist
     * @throws XMLException if there is a problem with the file syntax.
     */
    private Map<String, Object> loadConfiguration(File file)
        throws IOException,
        XMLException
    {
        try
        {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Map<String, Object> props = new Hashtable<String, Object>();

            //if the file is empty (or contains only sth insignificant)
            //ignore it and create a new document.
            if(file.length() < "<sip-communicator>".length()*2)
                propertiesDocument = createPropertiesDocument();
            else
                propertiesDocument = builder.parse(file);

            Node root = propertiesDocument.getFirstChild();

            Node currentNode = null;
            NodeList children = root.getChildNodes();
            for(int i = 0; i < children.getLength(); i++)
            {
                currentNode = children.item(i);

                if(currentNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    String propertyName
                        = DOMElementWriter.decodeName(
                                currentNode.getNodeName());

                    loadNode(currentNode, propertyName, props);
                }
            }

            return props;
        }
        catch(SAXException ex)
        {
            logger.error("Error parsing configuration file", ex);
            throw new XMLException(ex.getMessage(), ex);
        }
        catch(ParserConfigurationException ex)
        {
            //it is not highly probable that this might happen - so lets just
            //log it.
            logger.error("Error finding configuration for default parsers", ex);
            return new Hashtable<String, Object>();
        }
    }

    /**
     * Loads the contents of the specified node and its children into the local
     * properties. Any nodes marked as "system" will also be resolved in the
     * system properties.
     * @param node the root node that we should load together with its children
     * @param propertyName a String containing the prefix describing the route
     * to the specified node including its one name
     * @param props the dictionary object where all properties extracted
     * from this node and its children should be recorded.
     */
    private void loadNode(Node         node,
                          String propertyName,
                          Map<String, Object>          props)
    {
        Node currentNode = null;
        NodeList children = node.getChildNodes();

        for(int i = 0; i < children.getLength(); i++)
        {
            currentNode = children.item(i);

            if(currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                String newProp
                    = propertyName
                        + "."
                        + DOMElementWriter.decodeName(
                                currentNode.getNodeName());
                String value = XMLConfUtils.getAttribute(
                    currentNode, ATTRIBUTE_VALUE);

                // the value attr is present we must handle the desired property
                if(value != null)
                {
                    String propertyType
                        = XMLConfUtils.getAttribute(
                                currentNode,
                                SYSTEM_ATTRIBUTE_NAME);

                    //if the property is marked as "system", we should resolve
                    //it against the system properties and only store a
                    //reference locally. this is normally done for properties
                    //that are supposed to configure underlying libraries.
                    if((propertyType != null)
                            && propertyType.equals(SYSTEM_ATTRIBUTE_TRUE))
                    {
                        props.put(newProp, new PropertyReference(newProp));
                        System.setProperty(newProp, value);
                    }
                    else
                        props.put(newProp, value);
                }

                //load child nodes
                loadNode(currentNode, newProp, props);
            }
        }
    }

    /**
     * Creates new entries in the XML <tt>doc</tt> for every element in the
     * <tt>newProperties</tt> table.
     * 
     * @param doc the XML <tt>Document</tt> where the new entries should be
     *            created
     * @param newProperties the table containing the properties that are to be
     *            introduced in the document.
     */
    private void processNewProperties(Document doc,
                                      Map<String, Object>      newProperties)
    {
        for (Map.Entry<String, Object> entry : newProperties.entrySet())
        {
            Object value = entry.getValue();
            boolean system;

            if (system = (value instanceof PropertyReference))
                value = ((PropertyReference) value).getValue();
            processNewProperty(doc, entry.getKey(), value.toString(), system);
        }
    }

    /**
     * Creates an entry in the XML <tt>doc</tt> for the specified key value
     * pair.
     * @param doc the XML <tt>document</tt> to update.
     * @param key the value of the <tt>name</tt> attribute for the new entry
     * @param value the value of the <tt>value</tt> attribute for the new entry
     * @param isSystem specifies whether this is a system property (system
     * attribute will be set to true).
     */
    private void processNewProperty(Document doc,
                                    String key,
                                    String value,
                                    boolean isSystem)
    {
        StringTokenizer tokenizer = new StringTokenizer(key, ".");
        String[] toks = new String[tokenizer.countTokens()];
        int i = 0;

        while(tokenizer.hasMoreTokens())
            toks[i++] = DOMElementWriter.encodeName(tokenizer.nextToken());

        String nodeName = toks[toks.length - 1];
        Element parent
            = XMLConfUtils.createLastPathComponent(doc, toks, toks.length - 1);
        Element newNode = XMLConfUtils.findChild(parent, nodeName);

        if (newNode == null)
        {
            newNode = doc.createElement(nodeName);
            parent.appendChild(newNode);
        }
        newNode.setAttribute("value", value);

        if(isSystem)
            newNode.setAttribute(SYSTEM_ATTRIBUTE_NAME, SYSTEM_ATTRIBUTE_TRUE);
    }

    /*
     * Implements ConfigurationStore#reloadConfiguration(File).
     */
    public void reloadConfiguration(File file)
        throws IOException,
               XMLException
    {
        properties = new Hashtable<String, Object>();

        fileExtractedProperties = loadConfiguration(file);
        properties.putAll(fileExtractedProperties);
    }

    /*
     * Implements ConfigurationStore#removeProperty(String).
     */
    public void removeProperty(String propertyName)
    {
        properties.remove(propertyName);

        fileExtractedProperties.remove(propertyName);
    }

    /*
     * Implements ConfigurationStore#setNonSystemProperty(String, Object).
     */
    public void setNonSystemProperty(String propertyName, Object property)
    {
        properties.put(propertyName, property);
    }

    /*
     * Implements ConfigurationStore#setSystemProperty(String).
     */
    public void setSystemProperty(String propertyName)
    {
        setNonSystemProperty(propertyName, new PropertyReference(propertyName));
    }

    /*
     * Implements ConfigurationStore#storeConfiguration(OutputStream).
     */
    public void storeConfiguration(OutputStream out)
    {
        // resolve the properties that were initially in the file - back to
        // the document.
        if (propertiesDocument == null)
            propertiesDocument = createPropertiesDocument();

        Node root = propertiesDocument.getFirstChild();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                String propertyName
                    = DOMElementWriter.decodeName(currentNode.getNodeName());

                updateNode(currentNode, propertyName, properties);
            }
        }

        // create in the document the properties that were added by other
        // bundles after the initial property load.
        Map<String, Object> newlyAddedProperties = cloneProperties();

        // remove those that were originally there;
        for (String propName : fileExtractedProperties.keySet())
            newlyAddedProperties.remove(propName);

        this.processNewProperties(propertiesDocument, newlyAddedProperties);

        XMLUtils.indentedWriteXML(propertiesDocument, out);
    }

    /**
     * Updates the value of the specified node and its children to reflect those
     * in the properties file. Nodes marked as "system" will be updated from the
     * specified properties object and not from the system properties since if
     * any intentional change (through a configuration form) has occurred it
     * will have been made there.
     * 
     * @param node the root node that we should update together with its
     * children
     * @param propertyName a String containing the prefix describing the
     * dot-separated route to the specified node including its one name
     * @param props the dictionary object where the up to date values of the
     * node should be queried.
     */
    private void updateNode(Node node, String propertyName,
        Map<String, Object> props)
    {
        Node currentNode = null;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            currentNode = children.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                String newProp
                    = propertyName
                        + "."
                        + DOMElementWriter.decodeName(
                                currentNode.getNodeName());
                Attr attr
                    = ((Element) currentNode).getAttributeNode(ATTRIBUTE_VALUE);

                if (attr != null)
                {
                    // update the corresponding node
                    Object value = props.get(newProp);

                    if (value == null)
                    {
                        node.removeChild(currentNode);
                        continue;
                    }

                    boolean isSystem = (value instanceof PropertyReference);
                    String prop
                        = isSystem
                            ? ((PropertyReference) value).getValue().toString()
                            : value.toString();

                    attr.setNodeValue(prop);

                    // in case the property has changed to system since the last
                    // load - update the conf file accordingly.
                    if (isSystem)
                        ((Element) currentNode).setAttribute(
                            SYSTEM_ATTRIBUTE_NAME, SYSTEM_ATTRIBUTE_TRUE);
                    else
                        ((Element) currentNode)
                            .removeAttribute(SYSTEM_ATTRIBUTE_NAME);

                }

                // update child nodes
                updateNode(currentNode, newProp, props);
            }
        }
    }

    /**
     * We use property references when we'd like to store system properties.
     * Simply storing System properties in our properties Map would not be
     * enough since it will lead to mismatching values for the same property in
     * the System property set and in our local set of properties. Storing them
     * only in the System property set OTOH is a bit clumsy since it obliges
     * bundles to use to different configuration property sources. For that
     * reason, every time we get handed a property labeled as System, instead
     * of storing its actual value in the local property set we store a
     * PropertyReference instance that will retrieve it from the system
     * properties when necessary.
     */
    private static class PropertyReference
    {
        /**
         * The name of the system property represented by this instance.
         */
        private final String propertyName;

        /**
         * Initializes a new <tt>PropertyReference</tt> instance which is to
         * represent a system property with a specific name.
         *
         * @param propertyName the name of the system property to be represented
         * by the new instance
         */
        public PropertyReference(String propertyName)
        {
            this.propertyName = propertyName;
        }

        /**
         * Return the actual value of the property as recorded in the System
         * properties.
         * 
         * @return the valued of the property as recorded in the System props.
         */
        public Object getValue()
        {
            return System.getProperty(propertyName);
        }
    }
}
