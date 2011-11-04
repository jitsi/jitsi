/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>ConfigurationStore</tt> which stores property name-value
 * associations in a <tt>Properties</tt> instance and supports its
 * serialization format for the configuration file of
 * <tt>ConfigurationServiceImpl</tt>. Because of the <tt>Properties</tt>
 * backend which can associate names only <tt>String</tt> values, instances
 * of <tt>PropertyConfigurationStore</tt> convert property values to
 * <tt>String</tt> using <tt>Object#toString()</tt>.
 *
 * @author Lyubomir Marinov
 */
public class PropertyConfigurationStore
    extends HashtableConfigurationStore<Properties>
{
    /**
     * Initializes a new <tt>PropertyConfigurationStore</tt> instance.
     */
    public PropertyConfigurationStore()
    {
        super(new SortedProperties());
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
     * Overrides
     * {@link HashtableConfigurationStore#setNonSystemProperty(String, Object)}.
     * As the backend of this instance is a <tt>Properties</tt> instance, it can
     * only store <tt>String</tt> values and the specified value to be
     * associated with the specified property name is converted to a
     * <tt>String</tt>.
     *
     * @param name the name of the non-system property to be set to the
     * specified value in this <tt>ConfigurationStore</tt>
     * @param value the value to be assigned to the non-system property with the
     * specified name in this <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#setNonSystemProperty(String, Object)
     */
    @Override
    public void setNonSystemProperty(String name, Object value)
    {
        properties.setProperty(name, value.toString());
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
