/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP not-xml-frag element. Indicates that the request was supposed to
 * contain a valid XML fragment body, but did not.  Most likely this is because
 * the XML in the body was malformed or not balanced.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NotXmlFragType extends BaseXCapError
{
    /**
     * Creates the XCAP not-xml-frag error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NotXmlFragType(String phrase)
    {
        super(phrase);
    }
}
