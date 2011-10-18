/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP cannot-delete element. Indicates that the requested DELETE operation
 * could not be performed because it would not be idempotent.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class CannotDeleteType extends BaseXCapError
{
    /**
     * Creates the XCAP cannot-delete error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public CannotDeleteType(String phrase)
    {
        super(phrase);
    }
}
