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
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import org.jivesoftware.smack.packet.*;

/**
 * A {@link PacketExtension} implementation for Entity Capabilities packets.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 */
public class CapsPacketExtension implements PacketExtension
{
    /**
     * The hash method we use for generating the ver string.
     */
    public static final String HASH_METHOD = "sha-1";

    /**
     * The name space that the Entity Capabilities elements belong to.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/caps";

    /**
     * The name of the "content" element.
     */
    public static final String ELEMENT_NAME = "c";

    /**
     * A set of name tokens specifying additional feature bundles; this
     * attribute is deprecated.
     */
    private String ext;

    /**
     * A URI that uniquely identifies a software application, typically a URL
     * at the web site of the project or company that produces the software.
     */
    private String node;

    /**
     * The hashing algorithm used to generate the verification string.
     */
    private String hash;

    /**
     * A string that is used to verify the identity and supported features of
     * the entity.
     */
    private String ver;

    /**
     * An empty constructor for the <tt>CapsPacketExtension</tt>.
     */
    public CapsPacketExtension()
    {
    }

    /**
     * Creates a new <tt>CapsPacketExtension</tt> using the specified
     * parameters.
     *
     * @param ext a set of name tokens specifying additional info, that is
     * deprecated and that we keep for backward compatibility.
     * @param node a URI that uniquely identifies a software application
     * @param ver a version String.representing the identity of the supported
     * features.
     * @param hash The hashing algorithm used to generate the verification
     * <tt>String</tt>.
     */
    public CapsPacketExtension(String ext,
                               String node,
                               String hash,
                               String ver)
    {
        this.ext = ext;
        this.node = node;
        this.ver = ver;
        this.hash = hash;
    }

    /**
     * Returns the name of the caps element.
     *
     * @return the name of the caps element..
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the Entity Capabilities namespace.
     *
     * @return the Entity Capabilities namespace.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the <tt>node</tt> URI. The node URI uniquely identifies a
     * software application, typically a URL at the web site of the project or
     * company that produces the software.
     *
     * @return the value of the <tt>node</tt> URI
     */
    public String getNode()
    {
        return node;
    }

    /**
     * Specifies the value of the <tt>node</tt> URI. The node URI uniquely
     * identifies a software application, typically a URL at the web site of
     * the project or company that produces the software.
     *
     * @param node a URI uniquely identifying the application.
     */
    public void setNode(String node)
    {
        this.node = node;
    }

    /**
     * Returns the version associated with this packet. A version is a
     * string that is used to verify the identity and supported features of the
     * entity
     *
     * @return the value of the <tt>ver</tt> parameter.
     */
    public String getVersion()
    {
        return ver;
    }

    /**
     * Sets the value of the <tt>ver</tt> parameter. A version is a string that
     * is used to verify the identity and supported features of the entity.
     *
     * @param version the value of the <tt>ver</tt> parameter.
     */
    public void setVersion(String version)
    {
        this.ver = version;
    }

    /**
     * Returns the value of the <tt>hash</tt> parameter indicating the hashing
     * algorithm used to generate the verification string.
     *
     * @return the value of the <tt>hash</tt> parameter indicating the hashing
     * algorithm used to generate the verification string.
     */
    public String getHash()
    {
        return hash;
    }

    /**
     * Sets the value of the <tt>hash</tt> parameter indicating the hashing
     * algorithm used to generate the verification string.
     *
     * @param hash the hashing algorithm used to generate the verification
     * string.
     */
    public void setHash(String hash)
    {
        this.hash = hash;
    }

    /**
     * Returns the value of the <tt>ext</tt> parameter as a string set of
     * name tokens specifying additional feature bundles. This attribute is
     * deprecated indicating the hashing algorithm used to generate the
     * verification string.
     *
     * @return the value of the <tt>ext</tt> parameter or <tt>null</tt> if it
     * hasn't been specified.
     */
    public String getExtensions()
    {
        return ext;
    }

    /**
     * Sets the value of the <tt>ext</tt> parameter as a string set of
     * name tokens specifying additional feature bundles. This attribute is
     * deprecated indicating the hashing algorithm used to generate the
     * verification string.
     *
     * @param extensions the value of the <tt>ext</tt> parameter.
     */
    public void setExtensions(String extensions)
    {
        this.ext = extensions;
    }

    /**
     * Returns the XML representation of the caps packet extension.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        /* this is the kind of string that we will be generating here:
         *
         * <presence from='bard@shakespeare.lit/globe'>
         *   <c xmlns='http://jabber.org/protocol/caps'
         *      hash='sha-1'
         *      node='http://jitsi.org'
         *      ver='zHyEOgxTrkpSdGcQKH8EFPLsriY='/>
         * </presence>
         */

        StringBuilder bldr
            = new StringBuilder("<c xmlns='" + getNamespace() + "' ");

        if(getExtensions() != null)
            bldr.append("ext='" + getExtensions() + "' ");

        bldr.append("hash='" + getHash() + "' ");
        bldr.append("node='" + getNode() + "' ");
        bldr.append("ver='"  + getVersion() + "'/>");

        return bldr.toString();
    }
}
