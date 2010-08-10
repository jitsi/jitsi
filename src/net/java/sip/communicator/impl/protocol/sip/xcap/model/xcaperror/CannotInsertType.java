/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP cannot-insert element. Indicates that the requested PUT operation
 * could not be performed because a GET of that resource after the PUT would not
 * yield the content of the PUT request.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class CannotInsertType extends BaseXCapError
{
    /**
     * Creates the XCAP cannot-insert error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public CannotInsertType(String phrase)
    {
        super(phrase);
    }
}
