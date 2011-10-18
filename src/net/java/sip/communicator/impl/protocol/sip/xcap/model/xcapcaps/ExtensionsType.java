/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import java.util.*;

/**
 * The XCAP-CAPS extensions element.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class ExtensionsType
{
    /**
     * The list of the extension elements.
     */
    private List<String> extension;

    /**
     * Gets the value of the extension property.
     *
     * @return the extension property.
     */
    public List<String> getExtension()
    {
        if (extension == null)
        {
            extension = new ArrayList<String>();
        }
        return this.extension;
    }
}
