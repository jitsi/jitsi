/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface XCapClient extends HttpXCapClient, 
        XCapCapsClient, ResourceListsClient,
        PresRulesClient, PresContentClient
{
    /**
     * Gets information about XCAP resource-lists support information.
     *
     * @return true if resource-lists is supported.
     */
    public boolean isResourceListsSupported();

    /**
     * Gets information about XCAP pres-rules support information.
     *
     * @return true if pres-rules is supported.
     */
    public boolean isPresRulesSupported();

    /**
     * Gets information about XCAP pres-content support information.
     *
     * @return true if pres-content is supported.
     */
    public boolean isPresContentSupported();

}
