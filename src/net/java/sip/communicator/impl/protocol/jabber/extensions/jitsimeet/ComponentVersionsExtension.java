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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * The packet extension is used by Jicofo to broadcast versions of all video
 * conferencing system components. This packets extension is added to jicofo's
 * MUC presence. It will contain {@link Component} children which carry each
 * component's name and version.
 *
 * @author Pawel Domas
 */
public class ComponentVersionsExtension
    extends AbstractPacketExtension
{
    /**
     * The XML element name of {@link ComponentVersionsExtension}.
     */
    public static final String ELEMENT_NAME = "versions";

    /**
     * The name of XML sub-elements which carry the info about particular
     * component's version.
     */
    public static final String COMPONENT_ELEMENT_NAME = "component";

    /**
     * Constant for {@link Component} name used to signal the version of
     * conference focus.
     */
    public static final String COMPONENT_FOCUS = "focus";

    /**
     * Constant for {@link Component} name used to signal the version of
     * XMPP server.
     */
    public static final String COMPONENT_XMPP_SERVER = "xmpp";

    /**
     * Constant for {@link Component} name used to signal the version of
     * the videobridge.
     */
    public static final String COMPONENT_VIDEOBRIDGE = "videobridge";

    /**
     * The XML element namespace of {@link ComponentVersionsExtension}.
     */
    public static final String NAMESPACE = "http://jitsi.org/jitmeet";

    /**
     * Creates an {@link AbstractPacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     */
    public ComponentVersionsExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Adds component's version to this extension.
     *
     * @param componentName the name of the component for which
     *        child {@link Component} extension will be added.
     * @param versionStr human readable string that describes component's
     *        version.
     */
    public void addComponentVersion(String componentName, String versionStr)
    {
        Component v = new Component();

        v.setName(componentName);
        v.setText(versionStr);

        addChildExtension(v);
    }

    /**
     * Component child element of {@link ComponentVersionsExtension}. The name
     * of the component is carried in name attribute and the version string is
     * the text value.
     */
    public class Component
        extends AbstractPacketExtension
    {
        /**
         * The name of that attribute that carries component's name.
         */
        private final String NAME_ATTR_NAME = "name";

        /**
         * Creates new instance of {@link Component} packet extension.
         */
        public Component()
        {
            super(NAMESPACE, COMPONENT_ELEMENT_NAME);
        }

        /**
         * Returns the value of the name attribute.
         * @return <tt>String</tt> which describes the name of video
         *         conferencing system component.
         */
        public String getName()
        {
            return getAttributeAsString(NAME_ATTR_NAME);
        }

        /**
         * Sets new value for the component's name attribute.
         * @param name a <tt>String</tt> which describes the name of video
         *        conferencing system component.
         */
        public void setName(String name)
        {
            setAttribute(NAME_ATTR_NAME, name);
        }
    }
}
