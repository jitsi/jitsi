/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP not-xml-att-value element. Indicates that the request was supposed
 * to contain a valid XML attribute value, but did not.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NotXmlAttValueType extends BaseXCapError
{
    /**
     * Creates the XCAP not-xml-att-value error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NotXmlAttValueType(String phrase)
    {
        super(phrase);
    }
}
