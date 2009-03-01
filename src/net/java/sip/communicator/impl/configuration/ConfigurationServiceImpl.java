/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.impl.configuration.xml.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

import org.osgi.framework.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * A straight forward implementation of the ConfigurationService using an xml
 * file for storing properties. Currently only String properties are
 * meaningfully saved (we should probably consider how and whether we should
 * take care of the rest).
 *
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class ConfigurationServiceImpl
    implements ConfigurationService
{
    private final Logger logger = Logger.getLogger(ConfigurationServiceImpl.class);

    /**
     * The XML Document containing the configuration file this service loaded.
     */
    private Document propertiesDocument = null;

    /** Name of the xml attribute containing property values */
    private static final String ATTRIBUTE_VALUE = "value";

    /**
     * Name of the xml attribute indicating that a property is to be resolved
     * in the system properties
     */
    private static final String SYSTEM_ATTRIBUTE_NAME = "system";

    /** The value of the Name of the xml attribute containing property values */
    private static final String SYSTEM_ATTRIBUTE_TRUE = "true";

    /**
     * The name of the system property that stores the name of the configuration
     * file.
     */
    private static final String FILE_NAME_PROPERTY
        = "net.java.sip.communicator.CONFIGURATION_FILE_NAME";

    private static final String SYS_PROPS_FILE_NAME_PROPERTY
        = "net.java.sip.communicator.SYS_PROPS_FILE_NAME";

    /**
     * A reference to the currently used configuration file.
     */
    private File configurationFile = null;

    /**
     * Our event dispatcher.
     */
    private final ChangeEventDispatcher changeEventDispatcher =
        new ChangeEventDispatcher(this);

    /**
     * The list of properties currently registered in the configuration service.
     */
    private Map<String, Object> properties = new Hashtable<String, Object>();

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
     * Indicates whether the service is started or stopped.
     */
    private boolean started = false;
    
    /**
     * a reference to the FileAccessService
     */
    private FileAccessService faService = null;

    /**
     * Sets the property with the specified name to the specified value. Calling
     * this method would first trigger a PropertyChangeEvent that will
     * be dispatched to all VetoableChangeListeners. In case no complaints
     * (PropertyVetoException) have been received, the property will be actually
     * changed and a PropertyChangeEvent will be dispatched.
     * <p>
     * @param propertyName String
     * @param property Object
     * @throws PropertyVetoException in case the changed has been refused by
     * at least one propertychange listener.
     */
    public void setProperty(String propertyName, Object property)
        throws PropertyVetoException
    {
        setProperty(propertyName, property, false);
    }

    /**
     * Sets the property with the specified name to the specified. Calling
     * this method would first trigger a PropertyChangeEvent that will
     * be dispatched to all VetoableChangeListeners. In case no complaints
     * (PropertyVetoException) have been received, the property will be actually
     * changed and a PropertyChangeEvent will be dispatched. This method also
     * allows the caller to specify whether or not the specified property is a
     * system one.
     * <p>
     * @param propertyName the name of the property to change.
     * @param property the new value of the specified property.
     * @param isSystem specifies whether or not the property being is a System
     *                 property and should be resolved against the system
     *                 property set. If the property has previously been
     *                 specified as system then this value is inteernally forced
     *                 to true.
     * @throws PropertyVetoException in case the changed has been refused by
     * at least one propertychange listener.
     */
    public void setProperty(String propertyName, Object property,
                            boolean isSystem)
        throws PropertyVetoException
    {
        Object oldValue = getProperty(propertyName);
        //first check whether the change is ok with everyone
        if (changeEventDispatcher.hasVetoableChangeListeners(propertyName))
            changeEventDispatcher.fireVetoableChange(
                propertyName, oldValue, property);

        //no exception was thrown - lets change the property and fire a
        //change event

        logger.trace(propertyName + "( oldValue=" + oldValue
                     + ", newValue=" + property + ".");

        //once set system, a property remains system event if the user
        //specified sth else

        if (isSystem(propertyName))
            isSystem = true;

        if (property == null)
        {
            properties.remove(propertyName);

            fileExtractedProperties.remove(propertyName);

            if (isSystem)
            {
                //we can't remove or nullset a sys prop so let's "empty" it.
                System.setProperty(propertyName, "");
            }
        }
        else
        {
            if (isSystem)
            {
                //in case this is a system property, we must only store it
                //in the System property set and keep only a ref locally.
                System.setProperty(propertyName, property.toString());
                properties.put(propertyName,
                               new PropertyReference(propertyName));
            }
            else
            {
                properties.put(propertyName, property);
            }
        }
        if (changeEventDispatcher.hasPropertyChangeListeners(propertyName))
            changeEventDispatcher.firePropertyChange(
                propertyName, oldValue, property);

        try
        {
            storeConfiguration();
        }
        catch (IOException ex)
        {
            logger.error("Failed to store configuration after "
                         + "a property change");
        }
    }
    
    /**
     * Removes the property with the specified name. Calling
     * this method would first trigger a PropertyChangeEvent that will
     * be dispatched to all VetoableChangeListeners. In case no complaints
     * (PropertyVetoException) have been received, the property will be actually
     * changed and a PropertyChangeEvent will be dispatched.
     * All properties with prefix propertyName will also be removed.
     * <p>
     * @param propertyName the name of the property to change.
     * @param property the new value of the specified property.
     * @throws PropertyVetoException in case the changed has been refused by
     * at least one propertychange listener.
     */
    public void removeProperty(String propertyName)
        throws PropertyVetoException
    {
        List<String> childPropertyNames = 
            getPropertyNamesByPrefix(propertyName, false);

        //remove all properties
        for (String pName : childPropertyNames)
        {
            removeProperty(pName);
        }
        
        Object oldValue = getProperty(propertyName);
        //first check whether the change is ok with everyone
        if (changeEventDispatcher.hasVetoableChangeListeners(propertyName))
            changeEventDispatcher.fireVetoableChange(
                propertyName, oldValue, null);

        //no exception was thrown - lets change the property and fire a
        //change event

        logger.trace("Will remove prop: " + propertyName + ".");

        properties.remove(propertyName);

        fileExtractedProperties.remove(propertyName);
        
        if (changeEventDispatcher.hasPropertyChangeListeners(propertyName))
            changeEventDispatcher.firePropertyChange(
                propertyName, oldValue, null);

        try
        {
            storeConfiguration();
        }
        catch (IOException ex)
        {
            logger.error("Failed to store configuration after "
                         + "a property change");
        }
    }

    /**
     * Returns the value of the property with the specified name or null if no
     * such property exists.
     * @param propertyName the name of the property that is being queried.
     * @return the value of the property with the specified name.
     */
    public Object getProperty(String propertyName)
    {
        Object value = properties.get(propertyName);

        //if this is a property reference make sure we return the referenced
        //value and not the reference itself
        if(value instanceof PropertyReference)
            return ((PropertyReference)value).getValue();
        else
            return value;
    }

    /**
     * Returns a <tt>java.util.List</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @return a <tt>java.util.List</tt>containing all property name String-s
     * matching the specified conditions.
     */
    public List<String> getPropertyNamesByPrefix(String prefix, boolean exactPrefixMatch)
    {
        List<String> resultKeySet = new LinkedList<String>();

        for (String key : properties.keySet())
        {
            int ix = key.lastIndexOf('.');
            
            if(ix == -1)
                continue;
            
            String keyPrefix = key.substring(0, ix);

            if(exactPrefixMatch)
            {
                if(prefix.equals(keyPrefix))
                    resultKeySet.add(key);
            }
            else
            {
                if(keyPrefix.startsWith(prefix))
                    resultKeySet.add(key);
            }
        }

        return resultKeySet;
    }

    /**
     * Adds a PropertyChangeListener to the listener list.
     *
     * @param listener the PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        changeEventDispatcher.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     *
     * @param listener the PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        changeEventDispatcher.removePropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property.
     *
     * @param propertyName one of the property names listed above
     * @param listener the PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener)
    {
        changeEventDispatcher.
            addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list for a specific
     * property.
     *
     * @param propertyName a valid property name
     * @param listener the PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener)
    {
        changeEventDispatcher.
            removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Adds a VetoableChangeListener to the listener list.
     *
     * @param listener the VetoableChangeListener to be added
     */
    public void addVetoableChangeListener(VetoableChangeListener listener)
    {
        changeEventDispatcher.addVetoableChangeListener(listener);
    }

    /**
     * Removes a VetoableChangeListener from the listener list.
     *
     * @param listener the VetoableChangeListener to be removed
     */
    public void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        changeEventDispatcher.removeVetoableChangeListener(listener);
    }

    /**
     * Adds a VetoableChangeListener to the listener list for a specific
     * property.
     *
     * @param propertyName one of the property names listed above
     * @param listener the VetoableChangeListener to be added
     */
    public void addVetoableChangeListener(String propertyName,
                                          VetoableChangeListener listener)
    {
        changeEventDispatcher.addVetoableChangeListener(propertyName, listener);
    }

    /**
     * Removes a VetoableChangeListener from the listener list for a specific
     * property.
     *
     * @param propertyName a valid property name
     * @param listener the VetoableChangeListener to be removed
     */
    public void removeVetoableChangeListener(String propertyName,
                                             VetoableChangeListener listener)
    {
        changeEventDispatcher.removeVetoableChangeListener(propertyName,
            listener);
    }
    
    /**
     * Called on service stop.
     */
    void stop()
    {
        this.started = false;
    }

    /**
     * Initializes the configuration service impl and makes it load an initial
     * configuration from the conf file.
     */
    void start()
    {
        this.started = true;
        
        // retrieve a reference to the FileAccessService
        BundleContext bc = ConfigurationActivator.bundleContext;
        ServiceReference faServiceReference = bc.getServiceReference(
                FileAccessService.class.getName());
        this.faService = (FileAccessService) bc.getService(faServiceReference);
        
        try
        {
            debugPrintSystemProperties();
            preloadSystemPropertyFiles();
            reloadConfiguration();
        }
        catch (XMLException ex)
        {
            logger.error("Failed to parse the configuration file.", ex);
        }
        catch (IOException ex)
        {
            logger.error("Failed to load the configuration file", ex);
        }
    }

    public void reloadConfiguration()
        throws IOException, XMLException
    {
        properties = new Hashtable<String, Object>();
        this.configurationFile = null;

        fileExtractedProperties =
                loadConfiguration(getConfigurationFile());
        this.properties.putAll(fileExtractedProperties);
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
    Map<String, Object> loadConfiguration(File file)
        throws IOException, XMLException
    {
        // restore the file if needed
        FailSafeTransaction trans = this.faService
            .createFailSafeTransaction(file);
        try {
            trans.restoreFile();
        } catch (Exception e) {
            logger.error("can't restore the configuration file before loading" +
                    " it", e);
        }
        
        try
        {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Map<String, Object> properties = new Hashtable<String, Object>();

            //if the file is empyt (or contains only sth insignificant)
            //ifnore it and create a new document.
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
                    StringBuffer propertyNameBuff = new StringBuffer();
                    propertyNameBuff.append(currentNode.getNodeName());
                    loadNode(currentNode, propertyNameBuff, properties);
                }
            }

            return properties;
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

    public synchronized void storeConfiguration()
        throws IOException
    {
        storeConfiguration(getConfigurationFile());
    }

    private Document createPropertiesDocument()
    {
        if(propertiesDocument == null)
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
            propertiesDocument.appendChild(
                propertiesDocument.createElement("sip-communicator"));
        }
        return propertiesDocument;
    }

    /**
     * Stores local properties in the specified configuration file.
     * @param file a reference to the configuration file where properties should
     *  be stored.
     * @throws IOException if there was a problem writing to the specified file.
     */
    private void storeConfiguration(File file)
        throws IOException
    {
        if(!started)
            throw new IllegalStateException("Service is stopped or has not been started");
        
        //resolve the properties that were initially in the file - back to
        //the document.

        if (propertiesDocument == null)
            propertiesDocument = createPropertiesDocument();

        Node root = propertiesDocument.getFirstChild();

        Node currentNode = null;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            currentNode = children.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                StringBuffer propertyNameBuff = new StringBuffer();
                propertyNameBuff.append(currentNode.getNodeName());
                updateNode(currentNode, propertyNameBuff, properties);
            }
        }

        //create in the document the properties that were added by other
        //bundles after the initial property load.

        Map<String, Object> newlyAddedProperties = cloneProperties();

        //remove those that were originally there;
        for (String propName : fileExtractedProperties.keySet())
            newlyAddedProperties.remove(propName);

        this.processNewProperties(propertiesDocument,
                                  newlyAddedProperties);

        //write the file.
        File config = getConfigurationFile();
        FailSafeTransaction trans = this.faService
                                        .createFailSafeTransaction(config);
        try {
            trans.beginTransaction();
            OutputStream stream = new FileOutputStream(config);
            XMLUtils.indentedWriteXML(
                    propertiesDocument, stream);
            stream.close();
            trans.commit();
        } catch (Exception e) {
            logger.error("can't write data in the configuration file", e);
            trans.rollback();
        }
    }

    /**
     * Loads the contents of the specified node and its children into the local
     * properties. Any nodes marked as "system" will also be resolved in the
     * system properties.
     * @param node the root node that we shold load together with its children
     * @param propertyNameBuff a StringBuffer containing the prefix describing
     * the route to the specified node including its one name
     * @param properties the dictionary object where all properties extracted
     * from this node and its children should be recorded.
     */
    private void loadNode(Node         node,
                          StringBuffer propertyNameBuff,
                          Map<String, Object>          properties)
    {
        Node currentNode = null;
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); i++)
        {
            currentNode = children.item(i);

            if(currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                StringBuffer newPropBuff =
                    new StringBuffer(propertyNameBuff
                                     + "." +currentNode.getNodeName());
                String value = XMLConfUtils.getAttribute(
                    currentNode, ATTRIBUTE_VALUE);

                String propertyType =
                    XMLConfUtils.getAttribute(currentNode, SYSTEM_ATTRIBUTE_NAME);

                // the value attr is present we must handle the desired property
                if(value != null)
                {

                    //if the property is marked as "system", we should resolve
                    //it against the system properties and only store a
                    //reference locally. this is normally done for properties
                    //that are supposed to configure underlying libraries.
                    if(propertyType != null
                       && propertyType.equals(SYSTEM_ATTRIBUTE_TRUE))
                    {
                        properties.put(
                            newPropBuff.toString(),
                            new PropertyReference(newPropBuff.toString()));
                        System.setProperty(newPropBuff.toString(), value);
                    }
                    else
                    {
                        properties.put(newPropBuff.toString(), value);
                    }
                }

                //load child nodes
                loadNode(currentNode, newPropBuff, properties);
            }
        }
    }

    /**
     * Updates the value of the specified node and its children to reflect those
     * in the properties file. Nodes marked as "system" will be updated from
     * the specified properties object and not from the system properties since
     * if any intentional change (through a configuration form) has occurred
     * it will have been made there.
     *
     * @param node the root node that we shold update together with its children
     * @param propertyNameBuff a StringBuffer containing the prefix describing
     * the dot separated route to the specified node including its one name
     * @param properties the dictionary object where the up to date values of
     * the node should be queried.
     */
    private void updateNode(Node         node,
                            StringBuffer propertyNameBuff,
                            Map<String, Object>          properties)
    {
        Node currentNode = null;
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); i++)
        {
            currentNode = children.item(i);

            if(currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                StringBuffer newPropBuff =
                    new StringBuffer(propertyNameBuff.toString()).append(".")
                                     .append(currentNode.getNodeName());

                Attr attr =
                    ((Element)currentNode).getAttributeNode(ATTRIBUTE_VALUE);

                if(attr != null)
                {
                    //update the corresponding node
                    Object value = properties.get(newPropBuff.toString());

                    if(value == null)
                    {
                        node.removeChild(currentNode);
                        continue;
                    }

                    boolean isSystem = value instanceof PropertyReference;
                    String prop = isSystem
                        ?((PropertyReference)value).getValue().toString()
                        :value.toString();

                    attr.setNodeValue(prop);

                    //in case the property has changed to system since the last
                    //load - update the conf file accordingly.
                    if(isSystem)
                        ((Element)currentNode).setAttribute(
                                SYSTEM_ATTRIBUTE_NAME, SYSTEM_ATTRIBUTE_TRUE);
                    else
                        ((Element)currentNode).removeAttribute(
                                SYSTEM_ATTRIBUTE_NAME);

                }

                //update child nodes
                updateNode(currentNode, newPropBuff, properties);
            }
        }
    }

    /**
     * Returns the configuration file currently used by the implementation.
     * If there is no such file or this is the first time we reference it
     * a new one is created.
     * @return the configuration File currently used by the implementation.
     */
    private File getConfigurationFile()
    {
        if ( configurationFile == null )
            configurationFile = createConfigurationFile();

        return configurationFile;
    }

    /**
     * Returns the location of the directory where SIP Communicator is to store
     * user specific data such as configuration files, message and call history
     * as well as is bundle repository.
     *
     * @return the location of the directory where SIP Communicator is to store
     * user specific data such as configuration files, message and call history
     * as well as is bundle repository.
     */
    public String getScHomeDirLocation()
    {
        //first let's check whether we already have the name of the directory
        //set as a configuration property
        String scHomeDirLocation = getString(PNAME_SC_HOME_DIR_LOCATION);

        if (scHomeDirLocation == null)
        {
            //no luck, check whether user has specified a custom name in the
            //system properties
            scHomeDirLocation
                = getSystemProperty(PNAME_SC_HOME_DIR_LOCATION);

            if (scHomeDirLocation == null)
            {
                scHomeDirLocation = getSystemProperty("user.home");
            }

            //now save all this as a configuration property so that we don't
            //have to look for it in the sys props next time and so that it is
            //available for other bundles to consult.
            properties.put(PNAME_SC_HOME_DIR_LOCATION, scHomeDirLocation);
        }

        return scHomeDirLocation;
    }

    /**
     * Returns the name of the directory where SIP Communicator is to store user
     * specific data such as configuration files, message and call history
     * as well as is bundle repository.
     *
     * @return the name of the directory where SIP Communicator is to store
     * user specific data such as configuration files, message and call history
     * as well as is bundle repository.
     */
    public String getScHomeDirName()
    {
        //first let's check whether we already have the name of the directory
        //set as a configuration property
        String scHomeDirName = getString(PNAME_SC_HOME_DIR_NAME);

        if (scHomeDirName == null)
        {
            //no luck, check whether user has specified a custom name in the
            //system properties
            scHomeDirName
                = getSystemProperty(PNAME_SC_HOME_DIR_NAME);

            if (scHomeDirName == null)
            {
                scHomeDirName = ".sip-communicator";
            }

            //now save all this as a configuration property so that we don't
            //have to look for it in the sys props next time and so that it is
            //available for other bundles to consult.
            properties.put(PNAME_SC_HOME_DIR_NAME, scHomeDirName);
        }

        return scHomeDirName;
    }

    /**
     * Returns a reference to the configuration file that the service should
     * load. The method would try to load a file with the name
     * sip-communicator.xml unless a different one is specified in the system
     * property net.java.sip.communicator.PROPERTIES_FILE_NAME . The method
     * would first try to load the file from the current directory if it exists
     * this is not the case a load would be attempted from the
     * $HOME/.sip-communicator directory. In case it was not found there either
     * we'll look for it in all locations currently present in the $CLASSPATH.
     * In case we find it in there we will copy it to the
     * $HOME/.sip-communicator directory in case it was in a jar archive and
     * return the reference to the newly created file. In case the file is
     * to be found noweher - a new empty file in the user home directory and
     * returns a link to that one.
     *
     *
     * @return the configuration file currently used by the implementation.
     */
    File createConfigurationFile()
    {
        try
        {
            //see whether we have a user specified name for the conf file
            String pFileName = getSystemProperty(
                FILE_NAME_PROPERTY);
            if (pFileName == null)
            {
                pFileName = "sip-communicator.xml";
            }

            // try to open the file in current directory
            File configFileInCurrentDir = new File(pFileName);
            if (configFileInCurrentDir.exists())
            {
                logger.debug("Using config file in current dir: "
                             + configFileInCurrentDir.getCanonicalPath());
                return configFileInCurrentDir;
            }

            // we didn't find it in ".", try the SIP Communicator home directory
            // first check whether a custom SC home directory is specified

            //name of the sip-communicator home directory
            String scHomeDirName = getScHomeDirName();

            //location of the sip-communicator home directory
            String scHomeDirLocation = getScHomeDirLocation();

            File configDir = new File( scHomeDirLocation
                                       + File.separator + scHomeDirName);

            File configFileInUserHomeDir =
                new File(configDir, pFileName);

            if (configFileInUserHomeDir.exists())
            {
                logger.debug("Using config file in $HOME/.sip-communicator: "
                             + configFileInCurrentDir.getCanonicalPath());
                return configFileInUserHomeDir;
            }

            // If we are in a jar - copy config file from jar to user home.
            logger.trace("Copying config file.");

            configDir.mkdirs();
            InputStream in = getClass().getClassLoader().
                getResourceAsStream(pFileName);

            //Return an empty file if there wasn't any in the jar
            //null check report from John J. Barton - IBM
            if (in == null)
            {
                configFileInUserHomeDir.createNewFile();
                logger.debug("Created an empty file in $HOME: "
                             + configFileInCurrentDir.getCanonicalPath());
                return configFileInUserHomeDir;
            }
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(in));

            PrintWriter writer = new PrintWriter(new FileWriter(
                configFileInUserHomeDir));

            String line = null;
            logger.debug("Copying properties file:");
            while ( (line = reader.readLine()) != null)
            {
                writer.println(line);
                logger.debug(line);
            }
            writer.flush();
            return configFileInUserHomeDir;
        }
        catch (IOException ex)
        {
            logger.error("Error creating config file", ex);
            return null;
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
            String key = entry.getKey();
            Object value = entry.getValue();
            boolean isSystem = value instanceof PropertyReference;
            value = isSystem
                        ?((PropertyReference)value).getValue()
                        :value;
            processNewProperty(doc, key, value.toString(), isSystem);
        }
    }

    /**
     * Creates an entry in the xml <tt>doc</tt> for the specified key value
     * pair.
     * @param doc the XML <tt>document</tt> to update.
     * @param key the value of the <tt>name</tt> attribute for the new entry
     * @param value the value of the <tt>value</tt> attribue for the new
     * @param isSystem specifies whether this is a system property (system
     *                 attribute will be set to true).
     * entry.
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
            toks[i++] = tokenizer.nextToken();

        String[] chain = new String[toks.length - 1];
        for (int j = 0; j < chain.length; j++)
        {
            chain[j] = toks[j];
        }

        String nodeName = toks[toks.length - 1];

        Element parent = XMLConfUtils.createLastPathComponent(doc, chain);
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


    /**
     * Returns the value of the specified java system property. In case the
     * value was a zero length String or one that only contained whitespaces,
     * null is returned. This method is for internal use only. Users of the
     * configuration service are to use the getProperty() or getString() methods
     * which would automatically determine whether a property is system or not.
     * @param propertyName the name of the property whose value we need.
     * @return the value of the property with name propertyName or null if
     * the value had length 0 or only contained spaces tabs or new lines.
     */
    private static String getSystemProperty(String propertyName)
    {
        String retval = System.getProperty(propertyName);
        if (retval == null){
            return retval;
        }

        if (retval.trim().length() == 0){
            return null;
        }
        return retval;
    }

    /**
     * Returns the String value of the specified property (minus all
     * encompasssing whitespaces)and null in case no property value was mapped
     * against the specified propertyName, or in case the returned property
     * string had zero length or contained whitespaces only.
     *
     * @param propertyName the name of the property that is being queried.
     * @return the result of calling the property's toString method and null in
     * case there was no vlaue mapped against the specified
     * <tt>propertyName</tt>, or the returned string had zero length or
     * contained whitespaces only.
     */
    public String getString(String propertyName)
    {
        Object propValue = getProperty(propertyName);
        if (propValue == null)
            return null;

        String propStrValue = propValue.toString().trim();

        return (propStrValue.length() > 0)
                    ? propStrValue
                    : null;
    }

    public boolean getBoolean(String propertyName, boolean defaultValue)
    {
        String stringValue = getString(propertyName);

        return (stringValue == null) ? defaultValue : Boolean
            .parseBoolean(stringValue);
    }

    public int getInt(String propertyName, int defaultValue)
    {
        String stringValue = getString(propertyName);
        int intValue = defaultValue;

        if (stringValue != null)
        {
            try
            {
                intValue = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException ex)
            {
                logger.error(propertyName
                    + " does not appear to be an integer. " + "Defaulting to "
                    + defaultValue + ".", ex);
            }
        }
        return intValue;
    }

    /**
     * We use property references when we'd like to store system properties.
     * Simply storing System properties in our properties Map would not be
     * enough since it will lead to mismatching values for the same property in
     * the System property set and in our local set of properties. Storing them
     * only in the System property set OTOH is a bit clumsy since it obliges
     * bundles to use to different configuration property sources. For that
     * reason, every time we get handed a property labeled as System, in stead
     * of storing its actual value in the local property set we store a
     * PropertyReference instance that will retrive it from the system
     * properties when necessary.
     */
    private class PropertyReference
    {
        private String propertyName = null;

        PropertyReference(String propertyName)
        {
            this.propertyName = propertyName;
        }

        /**
         * Return the actual value of the property as recorded in the System
         * properties.
         * @return the valued of the property as recorded in the System props.
         */
        public Object getValue()
        {
            return System.getProperty(propertyName);
        }
    }

    /**
     * Returns a copy of the Map containing all configuration properties
     * @return a Map clone of the current configuration property set.
     */
    private Map<String, Object> cloneProperties()
    {
        // at the time I'm writing this method we're implementing the
        // configuration service through the use of a hashtable. this may very
        // well change one day so let's not be presumptuous
        if (properties instanceof Hashtable)
            return (Map<String, Object>) ((Hashtable) properties).clone();
        if (properties instanceof HashMap)
            return (Map<String, Object>) ((HashMap) properties).clone();
        if (properties instanceof TreeMap)
            return (Map<String, Object>) ((TreeMap) properties).clone();

        // well you can't say that I didn't try!!!

        return new Hashtable<String, Object>(properties);
    }

    /**
     * Determines whether the property with the specified
     * <tt>propertyName</tt> has been previously declared as System
     *
     * @param propertyName the name of the property to verify
     * @return true if someone at some point specified that property to be
     * system. (This could have been either through a call to
     * setProperty(string, true)) or by setting the system attribute in the
     * xml conf file to true.
     */
    private boolean isSystem(String propertyName)
    {
        return properties.containsKey(propertyName)
               && properties.get(propertyName) instanceof PropertyReference;
    }

    /**
     * Deletes the configuration file currently used by this implementation.
     */
    public void purgeStoredConfiguration()
    {
        if (this.configurationFile != null)
        {
            configurationFile.delete();
            configurationFile = null;
        }
    }

    /**
     * Goes over all system properties and outputs their names and values for
     * debug purposes. The method has no effect if the logger is at a log level
     * other than DEBUG or TRACE (FINE or FINEST).
     */
    private void debugPrintSystemProperties()
    {
        if (logger.isDebugEnabled())
        {
            Properties pValues = System.getProperties();
            for (Map.Entry<Object, Object> entry : pValues.entrySet())
                logger.debug(entry.getKey() + "=" + entry.getValue());
        }
    }

    /**
     * The method scans the contents of the SYS_PROPS_FILE_NAME_PROPERTY where
     * it expects to find a comma separated list of names of files that should
     * be loaded as system properties. The method then parses these files and
     * loads their contents as system properties. All such files have to be in
     * a location that's in the classpath.
     */
    public void preloadSystemPropertyFiles()
    {
        String propertyFilesListStr
            = System.getProperty( SYS_PROPS_FILE_NAME_PROPERTY );

        if(propertyFilesListStr == null || propertyFilesListStr.trim().length() == 0)
            return;

        StringTokenizer tokenizer
            = new StringTokenizer(propertyFilesListStr, ";,", false);

        while( tokenizer.hasMoreTokens())
        {
            String fileName = tokenizer.nextToken();
            try
            {
                fileName = fileName.trim();

                Properties fileProps = new Properties();

                fileProps.load(ClassLoader.getSystemResourceAsStream(fileName));

                // now set all of this file's properties as system properties
                for (Map.Entry<Object, Object> entry : fileProps.entrySet())
                    System.setProperty((String) entry.getKey(), (String) entry
                        .getValue());
            }
            catch (Exception ex)
            {
                //this is an insignificant method that should never affect
                //the rest of the application so we'll afford ourselves to
                //kind of silence all possible exceptions (which would most
                //often be IOExceptions). We will however log them in case
                //anyone would be interested.
                logger.error("Failed to load property file: "
                    + fileName
                    , ex);
            }
        }
    }
}
