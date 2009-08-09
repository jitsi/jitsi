/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

/**
 * Implements a <code>ConfigurationStore</code> which stores property name-value
 * associations in a <code>Properties</code> instance and supports its
 * serialization format for the configuration file of
 * <code>ConfigurationServiceImpl</code>. Because of the <code>Properties</code>
 * backend which can associate names only <code>String</code> values, instances
 * of <code>PropertyConfigurationStore</code> convert property values to
 * <code>String</code> using <code>Object#toString()</code>.
 *
 * @author Lubomir Marinov
 */
public class PropertyConfigurationStore
    implements ConfigurationStore
{

    /**
     * The <code>Properties</code> instance which stores the property name-value
     * associations of this <code>ConfigurationStore</code> instance and which
     * is effectively adapted by this instance to
     * <code>ConfigurationStore</code>.
     */
    private final Properties properties = new Properties();

    /*
     * Implements ConfigurationStore#getProperty(String). If this
     * ConfigurationStore contains a value associated with the specified
     * property name, returns it. Otherwise, searches for a system property with
     * the specified name and returns its value.
     */
    public Object getProperty(String name)
    {
        Object value = properties.get(name);

        return (value != null) ? value : System.getProperty(name);
    }

    /*
     * Implements ConfigurationStore#getPropertyNames().
     */
    public String[] getPropertyNames()
    {
        synchronized (properties)
        {
            Set<Object> propertyNames = properties.keySet();
            return propertyNames.toArray(new String[propertyNames.size()]);
        }
    }

    /*
     * Implements ConfigurationStore#isSystemProperty(String). Considers a
     * property to be system if the system properties contain a value associated
     * with its name.
     */
    public boolean isSystemProperty(String name)
    {
        return (System.getProperty(name) != null);
    }

    /*
     * Implements ConfigurationStore#reloadConfiguration(File).
     */
    public void reloadConfiguration(File file)
        throws IOException
    {
        properties.clear();

        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try
        {
            properties.load(in);
        }
        finally
        {
            in.close();
        }
    }

    /*
     * Implements ConfigurationStore#removeProperty(String).
     */
    public void removeProperty(String name)
    {
        properties.remove(name);
    }

    /*
     * Implements ConfigurationStore#setNonSystemProperty(String, Object). As
     * the backend of this instance is a Properties instance, it can only store
     * String values and the specified value to be associated with the specified
     * property name is converted to a String.
     */
    public void setNonSystemProperty(String name, Object value)
    {
        properties.setProperty(name, value.toString());
    }

    /*
     * Implements ConfigurationStore#setSystemProperty(String). Since system
     * properties are managed through the System class, setting a property as
     * system in this ConfigurationStore effectively removes any existing value
     * associated with the specified property name from this instance.
     */
    public void setSystemProperty(String name)
    {
        removeProperty(name);
    }

    /*
     * Implements ConfigurationStore#storeConfiguration(OutputStream).
     */
    public void storeConfiguration(OutputStream out)
        throws IOException
    {
        properties.store(out, null);
    }
}
