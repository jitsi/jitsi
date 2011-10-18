/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import java.util.*;

/**
 * The XCAP-CAPS namespaces element.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NamespacesType
{
    /**
     * The list of the namespace elements.
     */
    protected List<String> namespace;

    /**
     * Gets the value of the auid property.
     *
     * @return the namespace property.
     */
    public List<String> getNamespace()
    {
        if (namespace == null)
        {
            namespace = new ArrayList<String>();
        }
        return this.namespace;
    }
}
