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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.message.*;

import java.util.*;

import javax.sip.*;

import net.java.sip.communicator.util.*;

/**
 * The only Object with should be used as parameter for any JAIN-SIP class
 * setApplicationData() method (available for instance with Dialog-s and
 * Transaction-s). Allows several parts of SC code to interact independently
 * with setApplicationData(...)/getApplicationData() without stepping on
 * eachother's toes. Utility functions are provided to set/get data from
 * a supported object.
 *
 * @author Sebastien Mazy
 */
public class SipApplicationData
{
    /**
     * Key service.
     */
    public static final String KEY_SERVICE = "service";

    /**
     * Key subscriptions.
     */
    public static final String KEY_SUBSCRIPTIONS = "subscriptions";

    /**
     * Key user request.
     */
    public static final String KEY_USER_REQUEST = "userRequest";

    /**
     * Logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(SipApplicationData.class);

    /**
     * Internal representation of the store.
     */
    private final Map<String, Object> storage_ = new HashMap<String, Object>();

    /**
     * Stores a <tt>value</tt> associated to the a <tt>key</tt> string in the
     * <tt>container</tt>. Currently <tt>SIPMessage</tt>, <tt>Transaction</tt>
     * and <tt>Dialog</tt> are supported as container.
     *
     * @param container the <tt>Object</tt> to attach the
     * <tt>key</tt>/<tt>value</tt> pair to.
     * @param key the key string to retrieve the value later with get()
     * @param value the value to store
     */
    public static void setApplicationData(
            Object container, String key, Object value)
    {
        if (container == null)
        {
            logger.warn("container is null");
            return;
        }
        if (key == null)
        {
            logger.warn("key is null");
            return;
        }

        SipApplicationData appData = getSipApplicationData(container);
        if (appData == null)
        {
            appData = new SipApplicationData();
            if (container instanceof SIPMessage)
                ((SIPMessage) container).setApplicationData(appData);
            else if (container instanceof Transaction)
                ((Transaction) container).setApplicationData(appData);
            else if (container instanceof Dialog)
                ((Dialog) container).setApplicationData(appData);
            else
                logger.error("container should be of type " +
                        "SIPMessage, Transaction or Dialog");
        }

        appData.put(key, value);
    }

    /**
     * Retrieves a value associated to the a <tt>key</tt> string in the
     * <tt>container</tt>. Currently <tt>SIPMessage</tt>, <tt>Transaction</tt>
     * and <tt>Dialog</tt> are supported as container.
     *
     * @param container the <tt>Object</tt> to retrieve a value from.
     * @param key the key string to identify the value to retrieve
     * @return the returned value or null if it is not found
     */
    public static Object getApplicationData(Object container, String key)
    {
        if (container == null)
        {
            logger.debug("container is null");
            return null;
        }
        if (key == null)
        {
            logger.warn("key is null");
            return null;
        }

        SipApplicationData appData = getSipApplicationData(container);
        if (appData == null)
            return null;
        return appData.get(key);
    }

    /**
     * Stores a <tt>value</tt> associated to the a <tt>key</tt> string in the
     * <tt>SipApplicationData</tt>.
     *
     * @param key the key string to retrieve the value later with get()
     * @param value the value to store
     */
    private void put(String key, Object value)
    {
        this.storage_.put(key, value);
    }

    /**
     * Retrieves a value stored in <tt>SipApplicationData</tt>.
     *
     * @param key the key string to identify the value to retrieve
     * @return the returned value or null if it is not found
     */
    private Object get(String key)
    {
        return this.storage_.get(key);
    }

    /**
     * Tries to use the setApplicationData() method on the provided container
     * and returns the SipApplicationData stored there, or null if there is none
     * or if another type of instance is found.
     *
     * @param container the <tt>Object</tt> to retrieve a
     * <tt>SipApplicationData</tt> from.
     * @return the <tt>SipApplicationData</tt> rerieved, or null.
     */
    private static SipApplicationData getSipApplicationData(Object container)
    {
        Object appData;
        if (container instanceof SIPMessage)
            appData = ((SIPMessage) container).getApplicationData();
        else if (container instanceof Transaction)
            appData = ((Transaction) container).getApplicationData();
        else if (container instanceof Dialog)
            appData = ((Dialog) container).getApplicationData();
        else
        {
            logger.error("container should be of type " +
                    "SIPMessage, Transaction or Dialog");
            appData = null;
        }

        if (appData == null)
            return null;
        if (appData instanceof SipApplicationData)
            return (SipApplicationData) appData;

        logger.error("application data should be of type " +
                "SipApplicationData");
        return null;
    }
}

