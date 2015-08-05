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
 * User roles packet extension.
 *
 * @author Sebastien Vincent
 */
public class UserRolesPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that user roles belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the user roles data.
     */
    public static final String ELEMENT_NAME = "roles";

    /**
     * Subject element name.
     */
    public static final String ELEMENT_ROLE = "entry";

    /**
     * List of roles.
     */
    private List<String> roles = new ArrayList<String>();

    /**
     * Constructor.
     */
    public UserRolesPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Add roles.
     *
     * @param role role to add
     */
    public void addRoles(String role)
    {
        roles.add(role);
    }

    /**
     * Get list of roles.
     *
     * @return list of roles
     */
    public List<String> getRoles()
    {
        return roles;
    }

    /**
     * Returns an XML representation of this extension.
     *
     * @return an XML representation of this extension.
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

        for(String role : roles)
        {
            bldr.append("<").append(ELEMENT_ROLE).append(">").append(
                    role).append("</").append(ELEMENT_ROLE).append(">");
        }

        for(PacketExtension ext : getChildExtensions())
        {
            bldr.append(ext.toXML());
        }

        bldr.append("</").append(getElementName()).append(">");

        return bldr.toString();
    }
}
