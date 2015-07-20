/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * A table of DNS entries. This is a hash table which
 * can handle multiple entries with the same name.
 * <p/>
 * Storing multiple entries with the same name is implemented using a
 * linked list of <code>CacheNode</code>'s.
 * <p/>
 * The current implementation of the API of DNSCache does expose the
 * cache nodes to clients. Clients must explicitly deal with the nodes
 * when iterating over entries in the cache. Here's how to iterate over
 * all entries in the cache:
 * <pre>
 * for (Iterator i=dnscache.iterator(); i.hasNext(); )
 * {
 *    for ( DNSCache.CacheNode n = (DNSCache.CacheNode) i.next();
 *          n != null;
 *          n.next())
 *    {
 *       DNSEntry entry = n.getValue();
 *       ...do something with entry...
 *    }
 * }
 * </pre>
 * <p/>
 * And here's how to iterate over all entries having a given name:
 * <pre>
 * for (    DNSCache.CacheNode n = (DNSCache.CacheNode) dnscache.find(name);
 *          n != null;
 *          n.next())
 * {
 *     DNSEntry entry = n.getValue();
 *     ...do something with entry...
 * }
 * </pre>
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Werner Randelshofer, Rick Blair
 */
class DNSCache
{
    private static Logger logger = Logger.getLogger(DNSCache.class.toString());
    // Implementation note:
    // We might completely hide the existence of CacheNode's in a future version
    // of DNSCache. But this will require to implement two (inner) classes for
    // the  iterators that will be returned by method <code>iterator()</code> and
    // method <code>find(name)</code>.
    // Since DNSCache is not a public class, it does not seem worth the effort
    // to clean its API up that much.

    // [PJYF Oct 15 2004] This should implements Collections
    // that would be amuch cleaner implementation

    /**
     * The number of DNSEntry's in the cache.
     */
    private int size;

    /**
     * The hashtable used internally to store the entries of the cache.
     * Keys are instances of String. The String contains an unqualified service
     * name.
     * Values are linked lists of CacheNode instances.
     */
    private HashMap<String, CacheNode> hashtable;

    /**
     * Cache nodes are used to implement storage of multiple DNSEntry's of the
     * same name in the cache.
     */
    public static class CacheNode
    {
        private DNSEntry value;
        private CacheNode next;

        public CacheNode(DNSEntry value)
        {
            this.value = value;
//            String SLevel = System.getProperty("jmdns.debug");
//            if (SLevel == null)
//                SLevel = "INFO";
//            logger.setLevel(Level.parse(SLevel));
        }

        public CacheNode next()
        {
            return next;
        }

        public DNSEntry getValue()
        {
            return value;
        }
    }


    /**
     * Create a table with a given initial size.
     * @param size initial size.
     */
    public DNSCache(final int size)
    {
        hashtable = new HashMap<String, CacheNode>(size);

//        String SLevel = System.getProperty("jmdns.debug");
//        if (SLevel == null) SLevel = "INFO";
//        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear()
    {
        hashtable.clear();
        size = 0;
    }

    /**
     * Adds an entry to the table.
     * @param entry added to the table.
     */
    public synchronized void add(final DNSEntry entry)
    {
        //logger.log("DNSCache.add("+entry.getName()+")");
        CacheNode newValue = new CacheNode(entry);
        CacheNode node = hashtable.get(entry.getName());
        if (node == null)
        {
            hashtable.put(entry.getName(), newValue);
        }
        else
        {
            newValue.next = node.next;
            node.next = newValue;
        }
        size++;
    }

    /**
     * Remove a specific entry from the table.
     * @param entry removed from table.
     * @return Returns true if the entry was found.
     */
    public synchronized boolean remove(DNSEntry entry)
    {
        CacheNode node = hashtable.get(entry.getName());
        if (node != null)
        {
            if (node.value == entry)
            {
                if (node.next == null)
                {
                    hashtable.remove(entry.getName());
                }
                else
                {
                    hashtable.put(entry.getName(), node.next);
                }
                size--;
                return true;
            }

            CacheNode previous = node;
            node = node.next;
            while (node != null)
            {
                if (node.value == entry)
                {
                    previous.next = node.next;
                    size--;
                    return true;
                }
                previous = node;
                node = node.next;
            }
            ;
        }
        return false;
    }

    /**
     * Get a matching DNS entry from the table (using equals).
     * @param entry to be found in table.
     * @return Returns the entry that was found.
     */
    public synchronized DNSEntry get(DNSEntry entry)
    {
        for (CacheNode node = find(entry.getName()); node != null; node = node.next)
        {
            if (node.value.equals(entry))
            {
                return node.value;
            }
        }
        return null;
    }

    /**
     * Get a matching DNS entry from the table.
     * @param name
     * @param type
     * @param clazz
     * @return Return the entry if found, null otherwise.
     */
    public synchronized DNSEntry get(String name, int type, int clazz)
    {
        for (CacheNode node = find(name); node != null; node = node.next)
        {
            if (node.value.type == type && node.value.clazz == clazz)
            {
                return node.value;
            }
        }
        return null;
    }

    /**
     * Iterates over all cache nodes.
     * The iterator returns instances of DNSCache.CacheNode.
     * Each instance returned is the first node of a linked list.
     * To retrieve all entries, one must iterate over this linked list. See
     * code snippets in the header of the class.
     * @return Returns iterator with instances of DNSCache.CacheNode.
     */
    public Iterator<DNSCache.CacheNode> iterator()
    {
        return Collections.unmodifiableCollection(hashtable.values()).iterator();
    }

    /**
     * Iterate only over items with matching name.
     * If an instance is returned, it is the first node of a linked list.
     * To retrieve all entries, one must iterate over this linked list.
     * @param name to be found.
     * @return Returns an instance of DNSCache.CacheNode or null.
     */
    public synchronized CacheNode find(String name)
    {
        return hashtable.get(name);
    }

    /**
     * List all entries for debugging.
     */
    public synchronized void print()
    {
        for (Iterator<CacheNode> i = iterator(); i.hasNext();)
        {
            for (CacheNode n = i.next(); n != null; n = n.next)
            {
                if (logger.isInfoEnabled())
                    logger.info(n.value.toString());
            }
        }
    }

    @Override
    public synchronized String toString()
    {
        StringBuffer aLog = new StringBuffer();
        aLog.append("\t---- cache ----");
        for (Iterator<CacheNode> i = iterator(); i.hasNext();)
        {
            for (CacheNode n = i.next(); n != null; n = n.next)
            {
                aLog.append("\n\t\t" + n.value);
            }
        }
        return aLog.toString();
    }
}
