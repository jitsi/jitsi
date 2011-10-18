/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

/**
 * The Authorization Rules validity element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ValidityType
{
    /**
     * The list of from elements.
     */
    private List<String> fromList;

    /**
     * The list of until elements.
     */
    private List<String> untilList;

    /**
     * Gets the value of the fromList property.
     *
     * @return the fromList property.
     */
    public List<String> getFromList()
    {
        if (fromList == null)
        {
            fromList = new ArrayList<String>();
        }
        return fromList;
    }

    /**
     * Gets the value of the untilList property.
     *
     * @return the untilList property.
     */
    public List<String> getUntilList()
    {
        if (untilList == null)
        {
            untilList = new ArrayList<String>();
        }
        return untilList;
    }
}
