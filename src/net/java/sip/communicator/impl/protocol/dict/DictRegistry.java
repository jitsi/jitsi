/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

/**
 * Static registry storing the connexions to dict servers
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictRegistry
{
    private static HashMap<String, DictAdapter> adapters = new HashMap<String,DictAdapter>();
    
    /**
     * Checks if an adapter associated with the given key is stored in the regitry
     * @param key Key to the adapter
     * @return true, if an adapter exists - false otherwise
     */
    public static boolean has(String key)
    {
        return adapters.containsKey(key);
    }
    
    /**
     * Stores a new adapter in the registry 
     * @param key Key
     * @param value DictAdapter class
     */
    public static void put(String key, DictAdapter value)
    {
        adapters.put(key, value);
    }
    
    /**
     * Returns the adapter associated with the given key
     * @param key Key
     * @return the adapter associated with the given key - null otherwise
     */
    public static DictAdapter get(String key)
    {
        if (DictRegistry.has(key))
        {
            return adapters.get(key);
        }
        return null;
    }
    
    /**
     * Removes the adapter associated with the given key from the registry
     * @param key
     */
    public static void remove(String key)
    {
        if (DictRegistry.has(key)) {
            adapters.remove(key);
        }
    }
}
