/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP not-well-formed element. Indicates that the body of the request was
 * not a well-formed XML document.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NotWellFormedType extends BaseXCapError
{
    /**
     * Creates the XCAP not-well-formed error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NotWellFormedType(String phrase)
    {
        super(phrase);
    }
}
