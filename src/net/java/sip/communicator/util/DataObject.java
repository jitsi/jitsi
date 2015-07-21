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
package net.java.sip.communicator.util;

/**
 * Object which can store user specific key-values.
 *
 * @author Damian Minkov
 */
public class DataObject
{
    /**
     * The user-specific key-value associations stored in this instance.
     * <p>
     * Like the Widget implementation of Eclipse SWT, the storage type takes
     * into account that there are likely to be many
     * <code>DataObject</code> instances and <code>Map</code>s are thus
     * likely to impose increased memory use. While an array may very well
     * perform worse than a <code>Map</code> with respect to search, the
     * mechanism of user-defined key-value associations explicitly states that
     * it is not guaranteed to be optimized for any particular use and only
     * covers the most basic cases and performance-savvy code will likely
     * implement a more optimized solution anyway.
     * </p>
     */
    private Object[] data;

    /**
     * Gets the user data
     * associated with this instance and a specific key.
     *
     * @param key the key of the user data associated with this instance to be
     * retrieved
     * @return an <tt>Object</tt> which represents the value associated with
     * this instance and the specified <tt>key</tt>; <tt>null</tt> if no
     * association with the specified <tt>key</tt> exists in this instance
     */
    public Object getData(Object key)
    {
        if (key == null)
            throw new NullPointerException("key");

        int index = dataIndexOf(key);

        return (index == -1) ? null : data[index + 1];
    }

    /**
     * Determines the index in <code>#data</code> of a specific key.
     *
     * @param key
     *            the key to retrieve the index in <code>#data</code> of
     * @return the index in <code>#data</code> of the specified <code>key</code>
     *         if it is contained; <tt>-1</tt> if <code>key</code> is not
     *         contained in <code>#data</code>
     */
    private int dataIndexOf(Object key)
    {
        if (data != null)
            for (int index = 0; index < data.length; index += 2)
                if (key.equals(data[index]))
                    return index;
        return -1;
    }

    /**
     * Sets a
     * user-specific association in this instance in the form of a key-value
     * pair. If the specified <tt>key</tt> is already associated in this
     * instance with a value, the existing value is overwritten with the
     * specified <tt>value</tt>.
     * <p>
     * The user-defined association created by this method and stored in this
     * instance is not serialized by this instance and is thus only meant for
     * runtime use.
     * </p>
     * <p>
     * The storage of the user data is implementation-specific and is thus not
     * guaranteed to be optimized for execution time and memory use.
     * </p>
     *
     * @param key the key to associate in this instance with the specified value
     * @param value the value to be associated in this instance with the
     * specified <tt>key</tt>
     */
    public void setData(Object key, Object value)
    {
        if (key == null)
            throw new NullPointerException("key");

        int index = dataIndexOf(key);

        if (index == -1)
        {

            /*
             * If value is null, remove the association with key (or just don't
             * add it).
             */
            if (data == null)
            {
                if (value != null)
                    data = new Object[] { key, value };
            }
            else if (value == null)
            {
                int length = data.length - 2;

                if (length > 0)
                {
                    Object[] newData = new Object[length];

                    System.arraycopy(data, 0, newData, 0, index);
                    System.arraycopy(
                        data, index + 2, newData, index, length - index);
                    data = newData;
                }
                else
                    data = null;
            }
            else
            {
                int length = data.length;
                Object[] newData = new Object[length + 2];

                System.arraycopy(data, 0, newData, 0, length);
                data = newData;
                data[length++] = key;
                data[length++] = value;
            }
        }
        else
            data[index + 1] = value;
    }
}
