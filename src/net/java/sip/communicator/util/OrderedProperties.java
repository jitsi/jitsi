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
 * Implementation of Properties that keep order of couples [key, value] added.
 *
 * @author Sebastien Vincent
 */
public class OrderedProperties
    extends Properties
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * A linked hashmap to keep entry in order.
     */
    private final LinkedHashMap<Object, Object> linkedMap =
        new LinkedHashMap<Object, Object>();

    /**
     * Get the object pointed by key.
     *
     * @param key key
     * @return value pointed by key or null if not set
     */
    @Override
    public Object get(Object key)
    {
        return linkedMap.get(key);
    }

    /**
     * Put an couple key, value
     *
     * @param key key
     * @param value value
     * @return previous value pointed by key if any, null otherwise
     */
    @Override
    public Object put(Object key, Object value)
    {
        return linkedMap.put(key, value);
    }

    /**
     * Remove a key entry
     *
     * @param key key
     * @return previous value pointed by key if any, null otherwise
     */
    @Override
    public Object remove(Object key)
    {
        return linkedMap.remove(key);
    }

    /**
     * Clear the entries.
     */
    @Override
    public void clear()
    {
        linkedMap.clear();
    }

    /**
     * Get the keys enumeration.
     *
     * @return keys enumeration
     */
    @Override
    public Enumeration<Object> keys()
    {
        return Collections.<Object>enumeration(linkedMap.keySet());
    }

    /**
     * Get the elements of the <tt>LinkedHashMap</tt>.
     *
     * @return enumeration
     */
    @Override
    public Enumeration<Object> elements()
    {
        return Collections.<Object>enumeration(linkedMap.values());
    }

    /**
     * Return the entry Set.
     *
     * @return entry Set
     */
    @Override
    public Set<Map.Entry<Object, Object>> entrySet()
    {
        return linkedMap.entrySet();
    }

    /**
     * Get number of elements.
     *
     * @return number of elements
     */
    @Override
    public int size()
    {
        return linkedMap.size();
    }
}
