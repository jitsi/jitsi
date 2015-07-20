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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import java.util.*;

import org.w3c.dom.*;

/**
 * Contains the capabilities of an XCAP server.
 *
 * @author Grigorii Balutsel
 */
public class XCapCapsType
{
    /**
     * The auids elemet.
     */
    protected AuidsType auids;

    /**
     * The extensions elemet.
     */
    protected ExtensionsType extensions;

    /**
     * The namespaces elemet.
     */
    protected NamespacesType namespaces;

    /**
     * The list of any elements.
     */
    protected List<Element> any;

    /**
     * Gets the value of the auids property.
     *
     * @return the auids property.
     */
    public AuidsType getAuids()
    {
        return auids;
    }

    /**
     * Sets the value of the auids property.
     *
     * @param auids the auids to set.
     */
    public void setAuids(AuidsType auids)
    {
        this.auids = auids;
    }

    /**
     * Gets the value of the extensions property.
     *
     * @return the extensions property.
     */
    public ExtensionsType getExtensions()
    {
        return extensions;
    }

    /**
     * Sets the value of the extensions property.
     *
     * @param extensions the extensions to set.
     */
    public void setExtensions(ExtensionsType extensions)
    {
        this.extensions = extensions;
    }

    /**
     * Gets the value of the namespaces property.
     *
     * @return the namespaces property.
     */
    public NamespacesType getNamespaces()
    {
        return namespaces;
    }

    /**
     * Sets the value of the namespaces property.
     *
     * @param namespaces the namespaces to set.
     */
    public void setNamespaces(NamespacesType namespaces)
    {
        this.namespaces = namespaces;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Element>();
        }
        return this.any;
    }
}
