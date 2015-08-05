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
package net.java.sip.communicator.service.protocol;

import java.io.*;
import java.util.*;

/**
 * A <tt>JingleNodesDescriptor</tt> stores information necessary to create a
 * JingleNodes tracker or relay candidate harvester that we could use with
 * ICE4J. Descriptors are normally initialized by protocol wizards. They are
 * then used to convert the data into a {@link String} form suitable for storage
 * in an accounts properties Map.
 *
 * @author Yana Stamcheva
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public class JingleNodeDescriptor
    implements Serializable
{
    /**
     * JingleNodes prefix to store configuration.
     */
    public static final String JN_PREFIX = "JINGLENODES";

    /**
     * JingleNodes prefix to store server address in configuration.
     */
    public static final String JN_ADDRESS = "ADDRESS";

    /**
     * JingleNodes prefix to store the relay capabilities in configuration.
     */
    public static final String JN_IS_RELAY_SUPPORTED = "IS_RELAY_SUPPORTED";

    /**
     * The maximum number of stun servers that we would allow.
     */
    public static final int MAX_JN_RELAY_COUNT = 100;

    /**
     * The address of the JingleNodes (JID).
     */
    private String address;

    /**
     * If the relay is supported by this JingleNodes.
     */
    private boolean relaySupported;

    /**
     * Creates an instance of <tt>JingleNodes</tt> by specifying all
     * parameters.
     *
     * @param address address of the JingleNodes
     * @param relaySupported if the JingleNodes supports relay
     */
    public JingleNodeDescriptor(String  address,
                                 boolean relaySupported)
    {
        this.address = address;
        this.relaySupported = relaySupported;
    }

    /**
     * Returns the address of the JingleNodes
     *
     * @return the address of the JingleNodes
     */
    public String getJID()
    {
        return address;
    }

    /**
     * Sets the address of the JingleNodes.
     *
     * @param address the JID of the JingleNodes
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Returns if the JID has relay support.
     *
     * @return <tt>true</tt> if relay is supported, <tt>false</tt> otherwise
     */
    public boolean isRelaySupported()
    {
        return relaySupported;
    }

    /**
     * Sets the relay support corresponding to this JID.
     *
     * @param relaySupported relay value to set
     */
    public void setRelay(boolean relaySupported)
    {
        this.relaySupported = relaySupported;
    }

    /**
     * Stores this descriptor into the specified {@link Map}.The method is meant
     * for use with account property maps. It also allows prepending an account
     * prefix to all property names so that multiple descriptors can be stored
     * in a single {@link Map}.
     *
     * @param props the account properties {@link Map} that we'd like to store
     * this descriptor in.
     * @param namePrefix the prefix that we should prepend to every property
     * name.
     */
    public void storeDescriptor(Map<String, String> props, String namePrefix)
    {
        if(namePrefix == null)
            namePrefix = "";

        props.put(namePrefix + JN_ADDRESS, getJID());


        props.put(namePrefix + JN_IS_RELAY_SUPPORTED,
                Boolean.toString(isRelaySupported()));
    }

    /**
     * Loads this descriptor from the specified {@link Map}.The method is meant
     * for use with account property maps. It also allows prepending an account
     * prefix to all property names so that multiple descriptors can be read
     * in a single {@link Map}.
     *
     * @param props the account properties {@link Map} that we'd like to load
     * this descriptor from.
     * @param namePrefix the prefix that we should prepend to every property
     * name.
     *
     * @return the newly created descriptor or null if no descriptor was found.
     */
    public static JingleNodeDescriptor loadDescriptor(
                                        Map<String, String> props,
                                        String              namePrefix)
    {
        if(namePrefix == null)
            namePrefix = "";

        String relayAddress = props.get(namePrefix + JN_ADDRESS);

        if (relayAddress == null)
            return null;

        String relayStr = props.get(namePrefix + JN_IS_RELAY_SUPPORTED);

        boolean relay = false;

        try
        {
            relay = Boolean.parseBoolean(relayStr);
        }
        catch(Throwable t)
        {
        }

        JingleNodeDescriptor relayServer =
                  new JingleNodeDescriptor(relayAddress,
                                            relay);

        return relayServer;
    }

    /**
     * Returns a <tt>String</tt> representation of this descriptor
     *
     * @return a <tt>String</tt> representation of this descriptor.
     */
    @Override
    public String toString()
    {
        return "JingleNodesDesc: " + getJID() + " relay:"
               + isRelaySupported();
    }
}
