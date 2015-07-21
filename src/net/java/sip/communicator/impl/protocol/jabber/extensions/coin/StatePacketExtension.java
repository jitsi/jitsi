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
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * State packet extension.
 *
 * @author Sebastien Vincent
 */
public class StatePacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that state belongs to.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the element that contains the state data.
     */
    public static final String ELEMENT_NAME = "conference-state";

    /**
     * Users count element name.
     */
    public static final String ELEMENT_USER_COUNT = "user-count";

    /**
     * Active element name.
     */
    public static final String ELEMENT_ACTIVE = "active";

    /**
     * Locked element name.
     */
    public static final String ELEMENT_LOCKED = "locked";

    /**
     * User count.
     */
    private int userCount = 0;

    /**
     * Active state.
     */
    private int active = -1;

    /**
     * Locked state.
     */
    private int locked = -1;

    /**
     * Constructor.
     */
    public StatePacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set the user count.
     *
     * @param userCount user count
     */
    public void setUserCount(int userCount)
    {
        this.userCount = userCount;
    }

    /**
     * Set the active state.
     *
     * @param active state
     */
    public void setActive(int active)
    {
        this.active = active;
    }

    /**
     * Set the locked state.
     *
     * @param locked locked state
     */
    public void setLocked(int locked)
    {
        this.locked = locked;
    }

    /**
     * Get the user count.
     *
     * @return user count
     */
    public int getUserCount()
    {
        return userCount;
    }

    /**
     * Get the active state.
     *
     * @return active state
     */
    public int getActive()
    {
        return active;
    }

    /**
     * Get the locked state.
     *
     * @return locked state
     */
    public int getLocked()
    {
        return locked;
    }

    /**
     * Get an XML string representation.
     *
     * @return XML string representation
     */
    @Override
    public String toXML()
    {
       StringBuilder bldr = new StringBuilder();

       bldr.append("<").append(getElementName()).append(" ");

       if(getNamespace() != null)
           bldr.append("xmlns='").append(getNamespace()).append("'");

       //add the rest of the attributes if any
       for(Map.Entry<String, Object> entry : attributes.entrySet())
       {
           bldr.append(" ")
                   .append(entry.getKey())
                       .append("='")
                           .append(entry.getValue())
                               .append("'");
       }

       bldr.append(">");

       if(userCount != 0)
           bldr.append("<").append(ELEMENT_USER_COUNT).append(">").append(
                   userCount).append("</").append(ELEMENT_USER_COUNT).append(
                           ">");

       if(active != -1)
           bldr.append("<").append(ELEMENT_ACTIVE).append(">").append(
                   (active > 0)).append("</").append(ELEMENT_ACTIVE).append(
                           ">");

       if(locked != -1)
           bldr.append("<").append(ELEMENT_LOCKED).append(">").append(
                   (active > 0)).append("</").append(ELEMENT_LOCKED).append(
                           ">");

       for(PacketExtension ext : getChildExtensions())
       {
           bldr.append(ext.toXML());
       }

       bldr.append("</").append(getElementName()).append(">");
       return bldr.toString();
    }
}
