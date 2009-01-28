/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

/**
 * The only Object with should be used as parameter for any JAIN-SIP class
 * setApplicationData() method (available for instance with Dialog-s and
 * Transaction-s). Allows several parts of SC code to interact independantly
 * with setApplicationData(...)/getApplicationData() without stepping on
 * eachother's toes.
 *
 * @author Sebastien Mazy
 */
public class SipApplicationData
{
    public static final String APPDATA_KEY_SERVICE = "service";

    /**
     * Internal representation of the store.
     */
    private Map<String, Object> storage_ = new HashMap<String, Object>();

    /**
     * Stores a <tt>value</tt> associated to the a <tt>key</tt> string in the
     * <tt>SipApplicationData</tt>.
     *
     * @param key the key string to retrieve the value later with get()
     * @param value the value to store
     */
    public void put(String key, Object value)
    {
        this.storage_.put(key, value);
    }

    /**
     * Retrieves a value stored in <tt>SipApplicationData</tt>.
     *
     * @param key the key string to identify the value to retrieve
     * @return the returned value or null if it is not found
     */
    public Object get(String key)
    {
        return this.storage_.get(key);
    }
}

