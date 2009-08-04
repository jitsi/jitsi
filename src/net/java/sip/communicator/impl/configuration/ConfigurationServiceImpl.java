/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.configuration.xml.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

import org.osgi.framework.*;

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
     * Indicates whether the service is started or stopped.
     */
    private boolean started = false;
    
    /**
     * a reference to the FileAccessService
     */
    private FileAccessService faService = null;

    private ConfigurationStore store;

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
     *                 specified as system then this value is internally forced
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

        doSetProperty(propertyName, property, isSystem);

        try
        {
            storeConfiguration();
        }
        catch (IOException ex)
        {
            logger.error(
                "Failed to store configuration after a property change");
        }

        if (changeEventDispatcher.hasPropertyChangeListeners(propertyName))
            changeEventDispatcher.firePropertyChange(
                propertyName, oldValue, property);
    }

    public void setProperties(Map<String, Object> properties)
        throws PropertyVetoException
    {
        //first check whether the changes are ok with everyone
        Map<String, Object> oldValues
            = new HashMap<String, Object>(properties.size());
        for (Map.Entry<String, Object> property : properties.entrySet())
        {
            String propertyName = property.getKey();
            Object oldValue = getProperty(propertyName);

            oldValues.put(propertyName, oldValue);

            if (changeEventDispatcher.hasVetoableChangeListeners(propertyName))
                changeEventDispatcher
                    .fireVetoableChange(
                        propertyName,
                        oldValue,
                        property.getValue());
        }

        for (Map.Entry<String, Object> property : properties.entrySet())
            doSetProperty(property.getKey(), property.getValue(), false);

        try
        {
            storeConfiguration();
        }
        catch (IOException ex)
        {
            logger.error(
                "Failed to store configuration after property changes");
        }

        for (Map.Entry<String, Object> property : properties.entrySet())
        {
            String propertyName = property.getKey();

            if (changeEventDispatcher.hasPropertyChangeListeners(propertyName))
                changeEventDispatcher
                    .firePropertyChange(
                        propertyName,
                        oldValues.get(propertyName),
                        property.getValue());
        }
    }

    private void doSetProperty(
        String propertyName, Object property, boolean isSystem)
    {
        //once set system, a property remains system even if the user
        //specified sth else

        if (isSystem(propertyName))
            isSystem = true;

        if (property == null)
        {
            store.removeProperty(propertyName);

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
                store.setSystemProperty(propertyName);
            }
            else
            {
                store.setNonSystemProperty(propertyName, property);
            }
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

        store.removeProperty(propertyName);
        
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
     * 
     * @param propertyName the name of the property that is being queried.
     * @return the value of the property with the specified name.
     */
    public Object getProperty(String propertyName)
    {
        return store.getProperty(propertyName);
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

        for (String key : store.getPropertyNames())
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
     * 
     * @param bc
     */
    void start(BundleContext bc)
    {
        this.started = true;
        
        // retrieve a reference to the FileAccessService
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
        this.configurationFile = null;

        File file = getConfigurationFile();

        // restore the file if needed
        FailSafeTransaction trans = this.faService
            .createFailSafeTransaction(file);
        try {
            trans.restoreFile();
        } catch (Exception e) {
            logger.error("can't restore the configuration file before loading" +
                    " it", e);
        }

        store.reloadConfiguration(file);
    }

    public synchronized void storeConfiguration()
        throws IOException
    {
        storeConfiguration(getConfigurationFile());
    }

    /**
     * Stores local properties in the specified configuration file.
     * 
     * @param file a reference to the configuration file where properties should
     *            be stored.
     * @throws IOException if there was a problem writing to the specified file.
     */
    private void storeConfiguration(File file)
        throws IOException
    {
        if (!started)
            throw new IllegalStateException(
                "Service is stopped or has not been started");

        // write the file.
        File config = getConfigurationFile();
        FailSafeTransaction trans =
            this.faService.createFailSafeTransaction(config);
        try
        {
            trans.beginTransaction();
            OutputStream stream = new FileOutputStream(config);

            store.storeConfiguration(stream);

            stream.close();
            trans.commit();
        }
        catch (Exception e)
        {
            logger.error("can't write data in the configuration file", e);
            trans.rollback();
        }
    }

    /**
     * Returns the configuration file currently used by the implementation.
     * If there is no such file or this is the first time we reference it
     * a new one is created.
     * @return the configuration File currently used by the implementation.
     */
    private File getConfigurationFile()
        throws IOException
    {
        if (this.configurationFile == null)
        {
            File configurationFile = getConfigurationFile("xml", false);

            if (configurationFile == null)
            {
                /*
                 * Quite strange but let it play out as it did when the
                 * configuration file was in XML format.
                 */
                this.configurationFile = getConfigurationFile("xml", true);
                if (!(this.store instanceof XMLConfigurationStore))
                    this.store = new XMLConfigurationStore();
            }
            else if (configurationFile.exists())
            {
                String name = configurationFile.getName();
                int extensionBeginIndex = name.lastIndexOf('.');
                String extension
                    = (extensionBeginIndex > -1)
                            ? name.substring(extensionBeginIndex)
                            : null;

                if (".properties".equalsIgnoreCase(extension))
                {
                    this.configurationFile = configurationFile;
                    if (!(this.store instanceof PropertyConfigurationStore))
                        this.store = new PropertyConfigurationStore();
                }
                else
                {
                    File newConfigurationFile
                        = new File(
                                configurationFile.getParentFile(),
                                ((extensionBeginIndex > -1)
                                        ? name.substring(0, extensionBeginIndex)
                                        : name)
                                    + ".properties");

                    if (newConfigurationFile.exists())
                    {
                        this.configurationFile = newConfigurationFile;
                        if (!(this.store instanceof PropertyConfigurationStore))
                            this.store = new PropertyConfigurationStore();
                    }
                    else
                    {
                        this.configurationFile = configurationFile;
                        if (!(this.store instanceof XMLConfigurationStore))
                            this.store = new XMLConfigurationStore();
                    }
                }
            }
            else
            {
                this.configurationFile
                        = getConfigurationFile("properties", true);
                if (!(this.store instanceof PropertyConfigurationStore))
                    this.store = new PropertyConfigurationStore();
            }

            /*
             * Make sure that the properties SC_HOME_DIR_LOCATION and
             * SC_HOME_DIR_NAME are available in the store of this instance so
             * that users don't have to ask the system properties again.
             */
            getScHomeDirLocation();
            getScHomeDirName();
        }
        return this.configurationFile;
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
        String scHomeDirLocation = null;

        if (store != null)
            scHomeDirLocation = getString(PNAME_SC_HOME_DIR_LOCATION);

        if (scHomeDirLocation == null)
        {
            //no luck, check whether user has specified a custom name in the
            //system properties
            scHomeDirLocation
                = getSystemProperty(PNAME_SC_HOME_DIR_LOCATION);

            if (scHomeDirLocation == null)
                scHomeDirLocation = getSystemProperty("user.home");

            //now save all this as a configuration property so that we don't
            //have to look for it in the sys props next time and so that it is
            //available for other bundles to consult.
            if (store != null)
                store
                    .setNonSystemProperty(
                        PNAME_SC_HOME_DIR_LOCATION,
                        scHomeDirLocation);
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
        String scHomeDirName = null;

        if (store != null)
            scHomeDirName = getString(PNAME_SC_HOME_DIR_NAME);

        if (scHomeDirName == null)
        {
            //no luck, check whether user has specified a custom name in the
            //system properties
            scHomeDirName
                = getSystemProperty(PNAME_SC_HOME_DIR_NAME);

            if (scHomeDirName == null)
                scHomeDirName = ".sip-communicator";

            //now save all this as a configuration property so that we don't
            //have to look for it in the sys props next time and so that it is
            // available for other bundles to consult.
            if (store != null)
                store
                    .setNonSystemProperty(
                        PNAME_SC_HOME_DIR_NAME,
                        scHomeDirName);
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
     * return the reference to the newly created file. In case the file is to be
     * found nowhere - a new empty file in the user home directory and returns a
     * link to that one.
     * 
     * @param extension
     *            the extension of the file name of the configuration file. The
     *            specified extension may not be taken into account if the the
     *            configuration file name is forced through a system property.
     * @param create
     *            <tt>true</tt> to create the configuration file with the
     *            determined file name if it does not exist; <tt>false</tt> to
     *            only figure out the file name of the configuration file
     *            without creating it
     * @return the configuration file currently used by the implementation.
     */
    private File getConfigurationFile(String extension, boolean create)
        throws IOException
    {
        //see whether we have a user specified name for the conf file
        String pFileName = getSystemProperty(FILE_NAME_PROPERTY);
        if (pFileName == null)
            pFileName = "sip-communicator." + extension;

        // try to open the file in current directory
        File configFileInCurrentDir = new File(pFileName);
        if (configFileInCurrentDir.exists())
        {
            logger.debug(
                "Using config file in current dir: "
                    + configFileInCurrentDir.getAbsolutePath());
            return configFileInCurrentDir;
        }

        // we didn't find it in ".", try the SIP Communicator home directory
        // first check whether a custom SC home directory is specified

        File configDir
            = new File(
                    getScHomeDirLocation()
                        + File.separator
                        + getScHomeDirName());
        File configFileInUserHomeDir = new File(configDir, pFileName);

        if (configFileInUserHomeDir.exists())
        {
            logger.debug(
                "Using config file in $HOME/.sip-communicator: "
                    + configFileInUserHomeDir.getAbsolutePath());
            return configFileInUserHomeDir;
        }

        // If we are in a jar - copy config file from jar to user home.
        InputStream in
            = getClass().getClassLoader().getResourceAsStream(pFileName);

        //Return an empty file if there wasn't any in the jar
        //null check report from John J. Barton - IBM
        if (in == null)
        {
            if (create)
            {
                configDir.mkdirs();
                configFileInUserHomeDir.createNewFile();
            }
            logger.debug(
                "Created an empty file in $HOME: "
                    + configFileInUserHomeDir.getAbsolutePath());
            return configFileInUserHomeDir;
        }

        logger.trace(
            "Copying config file from JAR into "
                + configFileInUserHomeDir.getAbsolutePath());
        configDir.mkdirs();
        try
        {
            copy(in, configFileInUserHomeDir);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException ioex)
            {
                /*
                 * Ignore it because it doesn't matter and, most importantly, it
                 * shouldn't prevent us from using the configuration file.
                 */
            }
        }
        return configFileInUserHomeDir;
    }

    private static void copy(InputStream inputStream, File outputFile)
        throws IOException
    {
        OutputStream outputStream = new FileOutputStream(outputFile);

        try
        {
            byte[] bytes = new byte[4 * 1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytes)) != -1)
                outputStream.write(bytes, 0, bytesRead);
        }
        finally
        {
            outputStream.close();
        }
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
        if ((retval != null) && (retval.trim().length() == 0))
            retval = null;
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
        return store.isSystem(propertyName);
    }

    /**
     * Deletes the configuration file currently used by this implementation.
     */
    public void purgeStoredConfiguration()
    {
        if (configurationFile != null)
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
            for (Map.Entry<Object, Object> entry
                    : System.getProperties().entrySet())
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
    private void preloadSystemPropertyFiles()
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
