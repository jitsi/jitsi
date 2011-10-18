/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import java.util.*;

/**
 * The XCAP-CAPS auids element.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class AuidsType
{
    /**
     * The list of the auid elements.
     */
    private List<String> auid;

    /**
     * Gets the value of the auid property.
     *
     * @return the auid property.
     */
    public List<String> getAuid()
    {
        if (auid == null)
        {
            auid = new ArrayList<String>();
        }
        return this.auid;
    }
}
