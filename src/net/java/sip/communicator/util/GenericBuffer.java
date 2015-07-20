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

import java.util.*;

/**
 * The GenericBuffer class provides a way to minimize the effort needed to
 * buffer any kind of information. This class is particularly suited to
 * optimizations based on reusing already computed data.
 *
 * @author Benoit Pradelle
 */
public class GenericBuffer<T>
{
    private final Hashtable<String, GenericBufferPair> buffer;

    private int minAge = 0;

    private int curAge = 0;

    private final int maxCapacity;

    /**
     * Sole constructor.
     *
     * @param bufferSize The buffer size. Adding data to a full buffer will
     *            cause the oldest data present in the buffer to be overwritten;
     */
    public GenericBuffer(final int bufferSize)
    {
        assert bufferSize > 0;

        buffer = new Hashtable<String, GenericBufferPair>(bufferSize);
        maxCapacity = bufferSize;
    }

    /**
     * Adds a value to the buffer. If the buffer is full, the oldest value in
     * the buffer will be overwritten by this new value.
     *
     * @param value The value to add. Can't be null.
     * @param context The context for which this value is valid. This basically
     *            represents the current value of all the variables which
     *            control the value is correct. The context is used to find this
     *            value in the buffer. If the context is already associated in
     *            the buffer with a value, nothing is added nor modified.
     */
    public void addValue(final T value, final String context)
    {
        assert value != null && context != null;

        GenericBufferPair storage = buffer.get(context);

        if (storage == null)
        {
            storage = new GenericBufferPair();
        }
        else
        {
            return; // don't override values
        }

        // if the amount of data has reach the limit, search the oldest data
        if (buffer.size() == maxCapacity)
        {
            for (Map.Entry<String, GenericBufferPair> e : buffer.entrySet())
            {
                if (e.getValue().age == minAge)
                {
                    buffer.remove(e.getKey());
                    minAge++;
                    break;
                }
            }
        }

        storage.age = curAge++;
        storage.value = value;

        buffer.put(context, storage);
    }

    /**
     * Retrieves the value in the buffer corresponding to the context if it
     * exists.
     *
     * @param context The context of the searched value. The context represents
     *            all the variables values for which this value is correct.
     * @return The bufferized value with the searched context if it exists or
     *         null if no value is found.
     */
    public T getValue(final String context)
    {
        assert context != null;

        GenericBufferPair res = buffer.get(context);

        if (res == null)
        {
            return null;
        }

        return res.value;
    }

    /**
     * This class is a simple structure to store a pair context-value
     */
    private class GenericBufferPair
    {
        public T value = null;

        public int age = 0;
    }
}
