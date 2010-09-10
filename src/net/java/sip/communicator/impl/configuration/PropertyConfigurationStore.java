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

    /**
     * Implements {@link ConfigurationStore#getProperty(String)}. If this
     * <tt>ConfigurationStore</tt> contains a value associated with the
     * specified property name, returns it. Otherwise, searches for a system
     * property with the specified name and returns its value.
     *
     * @param name the name of the property to get the value of
     * @return the value in this <tt>ConfigurationStore</tt> of the property
     * with the specified name; <tt>null</tt> if the property with the specified
     * name does not have an association with a value in this
     * <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#getProperty(String)
     */
    public Object getProperty(String name)
    {
        Object value = properties.get(name);

        return (value != null) ? value : System.getProperty(name);
    }

    /**
     * Implements {@link ConfigurationStore#getPropertyNames()}. Gets the names
     * of the properties which have values associated in this
     * <tt>ConfigurationStore</tt>.
     *
     * @return an array of <tt>String</tt>s which specify the names of the
     * properties that have values associated in this
     * <tt>ConfigurationStore</tt>; an empty array if this instance contains no
     * property values
     * @see ConfigurationStore#getPropertyNames()
     */
    public String[] getPropertyNames()
    {
        synchronized (properties)
        {
            Set<Object> propertyNames = properties.keySet();
            return propertyNames.toArray(new String[propertyNames.size()]);
        }
    }

    /**
     * Implements {@link ConfigurationStore#isSystemProperty(String)}. Considers
     * a property to be system if the system properties contain a value
     * associated with its name.
     *
     * @param name the name of a property which is to be determined whether it
     * is a system property
     * @return <tt>true</tt> if the specified name stands for a system property;
     * <tt>false</tt>, otherwise
     * @see ConfigurationStore#isSystemProperty(String)
     */
    public boolean isSystemProperty(String name)
    {
        return (System.getProperty(name) != null);
    }

    /**
     * Implements {@link ConfigurationStore#reloadConfiguration(File)}. Removes
     * all property name-value associations currently present in this
     * <tt>ConfigurationStore</tt> and deserializes new property name-value
     * associations from a specific <tt>File</tt> which presumably is in the
     * format represented by this instance.
     *
     * @param file the <tt>File</tt> to be read and to deserialize new property
     * name-value associations from into this instance
     * @throws IOException if there is an input error while reading from the
     * specified <tt>file</tt>
     * @see ConfigurationStore#reloadConfiguration(File)
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

    /**
     * Implements {@link ConfigurationStore#removeProperty(String)}. Removes the
     * value association in this <tt>ConfigurationStore</tt> of the property
     * with a specific name. If the property with the specified name is not
     * associated with a value in this <tt>ConfigurationStore</tt>, does
     * nothing.
     *
     * @param name the name of the property which is to have its value
     * association in this <tt>ConfigurationStore</tt> removed
     * @see ConfigurationStore#removeProperty(String)
     */
    public void removeProperty(String name)
    {
        properties.remove(name);
    }

    /**
     * Implements
     * {@link ConfigurationStore#setNonSystemProperty(String, Object)}. As the
     * backend of this instance is a <tt>Properties</tt> instance, it can only
     * store <tt>String</tt> values and the specified value to be associated
     * with the specified property name is converted to a <tt>String</tt>.
     *
     * @param name the name of the non-system property to be set to the
     * specified value in this <tt>ConfigurationStore</tt>
     * @param value the value to be assigned to the non-system property with the
     * specified name in this <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#setNonSystemProperty(String, Object)
     */
    public void setNonSystemProperty(String name, Object value)
    {
        properties.setProperty(name, value.toString());
    }

    /**
     * Implements {@link ConfigurationStore#setSystemProperty(String)}. Since
     * system properties are managed through the <tt>System</tt> class, setting
     * a property as system in this <tt>ConfigurationStore</tt> effectively
     * removes any existing value associated with the specified property name
     * from this instance.
     *
     * @param name the name of the property to be set as a system property in
     * this <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#setSystemProperty(String)
     */
    public void setSystemProperty(String name)
    {
        removeProperty(name);
    }

    /**
     * Implements {@link ConfigurationStore#storeConfiguration(OutputStream)}.
     * Stores/serializes the property name-value associations currently present
     * in this <tt>ConfigurationStore</tt> into a specific <tt>OutputStream</tt>
     * in the format represented by this instance.
     *
     * @param out the <tt>OutputStream</tt> to receive the serialized form of
     * the property name-value associations currently present in this
     * <tt>ConfigurationStore</tt>
     * @throws IOException if there is an output error while storing the
     * properties managed by this <tt>ConfigurationStore</tt> into the specified
     * <tt>file</tt>
     * @see ConfigurationStore#storeConfiguration(OutputStream)
     */
    public void storeConfiguration(OutputStream out)
        throws IOException
    {
        properties.store(out, null);
    }
}
