/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;

import net.java.sip.communicator.util.xml.*;

/**
 * Abstracts the runtime storage, the serialization and deserialization of the
 * configuration properties and their associated values of
 * <tt>ConfigurationServiceImpl</tt> and the format of the configuration file.
 * Thus <tt>ConfigurationServiceImpl</tt> can operate regardless of these
 * specifics and takes care of asking the <tt>VetoableChangeListener</tt>s,
 * converting the property values to the requested types and notifying the
 * <tt>PropertyChangeListener</tt>s.
 *
 * @author Lubomir Marinov
 */
public interface ConfigurationStore
{

    /**
     * Gets the value in this <tt>ConfigurationStore</tt> of a property with a
     * specific name.
     *
     * @param name the name of the property to get the value of
     * @return the value in this <tt>ConfigurationStore</tt> of the property
     * with the specified name; <tt>null</tt> if the property with the specified
     * name does not have an association with a value in this
     * <tt>ConfigurationStore</tt>
     */
    public Object getProperty(String name);

    /**
     * Gets the names of the properties which have values associated in this
     * <tt>ConfigurationStore</tt>.
     *
     * @return an array of <tt>String</tt>s which specify the names of the
     * properties that have values associated in this
     * <tt>ConfigurationStore</tt>; an empty array if this instance contains no
     * property values
     */
    public String[] getPropertyNames();

    /**
     * Determines whether a specific name stands for a system property.
     * 
     * @param name the name of a property which is to be determined whether it
     * is a system property
     * @return <tt>true</tt> if the specified name stands for a system property;
     * <tt>false</tt>, otherwise
     */
    public boolean isSystemProperty(String name);

    /**
     * Removes all property name-value associations currently present in this
     * <tt>ConfigurationStore</tt> and deserializes new property name-value
     * associations from a specific <tt>File</tt> which presumably is in the
     * format represented by this instance.
     *
     * @param file the <tt>File</tt> to be read and to deserialize new property
     * name-value associations from into this instance
     * @throws IOException if there is an input error while reading from the
     * specified <tt>file</tt>
     * @throws XMLException if parsing the contents of the specified
     * <tt>file</tt> fails
     */
    public void reloadConfiguration(File file)
        throws IOException,
               XMLException;

    /**
     * Removes the value association in this <tt>ConfigurationStore</tt> of the
     * property with a specific name. If the property with the specified name is
     * not associated with a value in this <tt>ConfigurationStore</tt>, does
     * nothing.
     *
     * @param name the name of the property which is to have its value
     * association in this <tt>ConfigurationStore</tt> removed
     */
    public void removeProperty(String name);

    /**
     * Sets the value of a non-system property with a specific name to a
     * specific value in this <tt>ConfigurationStore</tt>.
     *
     * @param name the name of the non-system property to be set to the
     * specified value in this <tt>ConfigurationStore</tt>
     * @param value the value to be assigned to the non-system property with the
     * specified name in this <tt>ConfigurationStore</tt>
     */
    public void setNonSystemProperty(String name, Object value);

    /**
     * Sets a property with a specific name to be considered a system property
     * by the <tt>ConfigurationStore</tt>.
     *
     * @param name the name of the property to be set as a system property in
     * this <tt>ConfigurationStore</tt>
     */
    public void setSystemProperty(String name);

    /**
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
     */
    public void storeConfiguration(OutputStream out)
        throws IOException;
}
