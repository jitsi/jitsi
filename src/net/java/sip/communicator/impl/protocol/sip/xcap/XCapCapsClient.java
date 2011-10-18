/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps.*;

/**
 * XCAP xcap-caps client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface XCapCapsClient
{
    /**
     * Xcap-caps uri format
     */
    public static String DOCUMENT_FORMAT = "xcap-caps/global/index";

    /**
     * Xcap-caps content type
     */
    public static String CONTENT_TYPE = "application/xcap-caps+xml";

    /**
     * Gets the xcap-caps from the server.
     *
     * @return the xcap-caps.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapCapsType getXCapCaps()
            throws XCapException;
}
