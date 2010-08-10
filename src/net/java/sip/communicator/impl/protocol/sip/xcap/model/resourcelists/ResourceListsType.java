/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import java.util.*;

/**
 * The XCAP resource-lists element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class ResourceListsType
{
    /**
     * The list of the list elements.
     */
    private List<ListType> list;

    /**
     * Gets the value of the list property.
     *
     * @return the list property.
     */
    public List<ListType> getList()
    {
        if (list == null)
        {
            list = new ArrayList<ListType>();
        }
        return this.list;
    }
}
